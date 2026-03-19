#!/bin/bash
# ============================================================
# Office MB — Gateway Comparison Test Suite
#
# Usage:
#   ./test-gateway.sh apisix     # Test APISIX gateway on :9080
#   ./test-gateway.sh krakend    # Test KrakenD gateway on :8888
#   ./test-gateway.sh direct     # Test orchestrator directly on :8080
#
# Prerequisites:
#   docker compose --profile <gateway> up --build
#   (for apisix) ./gateway/apisix/setup-routes.sh
# ============================================================

set -e

# ---- Config ----
GATEWAY="${1:-direct}"
TIMESTAMP=$(date +%s)
TEST_EMAIL="testuser_${TIMESTAMP}@test.com"
TEST_PASS="SecurePass123!"
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'
PASS=0
FAIL=0

case "$GATEWAY" in
  apisix)  BASE_URL="http://localhost:9080" ;;
  krakend) BASE_URL="http://localhost:8888" ;;
  direct)  BASE_URL="http://localhost:8080" ;;
  *)
    echo "Usage: $0 {apisix|krakend|direct}"
    exit 1
    ;;
esac

# ---- Helpers ----
print_header() {
  echo ""
  echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "${CYAN}  $1${NC}"
  echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

assert_status() {
  local test_name="$1"
  local expected="$2"
  local actual="$3"
  local body="$4"

  if [ "$actual" -eq "$expected" ]; then
    echo -e "  ${GREEN}PASS${NC} $test_name (HTTP $actual)"
    PASS=$((PASS + 1))
  else
    echo -e "  ${RED}FAIL${NC} $test_name (expected $expected, got $actual)"
    echo -e "       ${RED}Response: ${body:0:200}${NC}"
    FAIL=$((FAIL + 1))
  fi
}

timed_request() {
  # Returns: status_code|time_total|response_body
  local method="$1"
  shift
  curl -s -w "\n%{http_code}|%{time_total}" -X "$method" "$@"
}

# ============================================================
print_header "OFFICE MB — GATEWAY TEST SUITE"
echo -e "  Gateway:  ${YELLOW}${GATEWAY}${NC}"
echo -e "  Base URL: ${YELLOW}${BASE_URL}${NC}"
echo -e "  Email:    ${YELLOW}${TEST_EMAIL}${NC}"
echo ""

# ============================================================
# TEST 1: Health Check
# ============================================================
print_header "TEST 1: Gateway Reachability"

if [ "$GATEWAY" = "apisix" ]; then
  HEALTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:9180/apisix/admin/routes" -H "X-API-KEY: admin-api-key-local")
  assert_status "APISIX status endpoint" 200 "$HEALTH_STATUS" ""
elif [ "$GATEWAY" = "krakend" ]; then
  HEALTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/__health")
  assert_status "KrakenD health endpoint" 200 "$HEALTH_STATUS" ""
else
  HEALTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/actuator/health")
  assert_status "Orchestrator actuator health" 200 "$HEALTH_STATUS" ""
fi

# ============================================================
# TEST 2: Register
# ============================================================
print_header "TEST 2: User Registration"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"${TEST_EMAIL}\",
    \"password\": \"${TEST_PASS}\",
    \"firstName\": \"Test\",
    \"lastName\": \"User\"
  }")

BODY=$(echo "$RESPONSE" | head -n -1)
STATUS=$(echo "$RESPONSE" | tail -1)
assert_status "Register new user" 201 "$STATUS" "$BODY"
echo -e "  ${YELLOW}Response:${NC} ${BODY:0:150}"

# Test duplicate registration
sleep 1.5
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"${TEST_EMAIL}\",
    \"password\": \"${TEST_PASS}\",
    \"firstName\": \"Test\",
    \"lastName\": \"User\"
  }")
DUP_STATUS=$(echo "$RESPONSE" | tail -1)
assert_status "Reject duplicate email (409)" 409 "$DUP_STATUS" "$(echo "$RESPONSE" | head -n -1)"

# ============================================================
# TEST 3: Login
# ============================================================
print_header "TEST 3: User Login"

sleep 1.5
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"${TEST_EMAIL}\",
    \"password\": \"${TEST_PASS}\"
  }")

BODY=$(echo "$RESPONSE" | head -n -1)
STATUS=$(echo "$RESPONSE" | tail -1)
assert_status "Login with valid credentials" 200 "$STATUS" "$BODY"

# Extract JWT token
TOKEN=$(echo "$BODY" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    # Handle nested data structure
    d = data.get('data', data)
    print(d.get('token', d.get('accessToken', '')))
except:
    print('')
" 2>/dev/null)

if [ -z "$TOKEN" ]; then
  echo -e "  ${RED}FATAL: Could not extract JWT token. Cannot continue.${NC}"
  echo -e "  ${RED}Login response: ${BODY:0:300}${NC}"
  exit 1
fi

echo -e "  ${GREEN}JWT Token:${NC} ${TOKEN:0:50}..."

# Test wrong password
sleep 1.5
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"${TEST_EMAIL}\", \"password\": \"wrongpassword\"}")
BAD_STATUS=$(echo "$RESPONSE" | tail -1)
assert_status "Reject wrong password (401)" 401 "$BAD_STATUS" "$(echo "$RESPONSE" | head -n -1)"

# ============================================================
# TEST 4: Protected Endpoints — with valid JWT
# ============================================================
print_header "TEST 4: Protected Endpoints (valid JWT)"

# Profile
sleep 1.5
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "${BASE_URL}/api/v1/users/profile" \
  -H "Authorization: Bearer ${TOKEN}")
BODY=$(echo "$RESPONSE" | head -n -1)
STATUS=$(echo "$RESPONSE" | tail -1)
assert_status "GET /api/v1/users/profile" 200 "$STATUS" "$BODY"
echo -e "  ${YELLOW}Profile:${NC} ${BODY:0:150}"

# Process payment
sleep 1.5
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/v1/payments/process" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"idempotencyKey": "test-idemp-123", "amount": 99.99, "currency": "USD", "description": "Test payment"}')
BODY=$(echo "$RESPONSE" | head -n -1)
STATUS=$(echo "$RESPONSE" | tail -1)
assert_status "POST /api/v1/payments/process" 201 "$STATUS" "$BODY"
echo -e "  ${YELLOW}Payment:${NC} ${BODY:0:200}"

# Payment history
sleep 1.5
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "${BASE_URL}/api/v1/payments/history" \
  -H "Authorization: Bearer ${TOKEN}")
BODY=$(echo "$RESPONSE" | head -n -1)
STATUS=$(echo "$RESPONSE" | tail -1)
assert_status "GET /api/v1/payments/history" 200 "$STATUS" "$BODY"

# Dashboard (aggregation test)
sleep 1.5
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "${BASE_URL}/api/v1/dashboard" \
  -H "Authorization: Bearer ${TOKEN}")
BODY=$(echo "$RESPONSE" | head -n -1)
STATUS=$(echo "$RESPONSE" | tail -1)
assert_status "GET /api/v1/dashboard (aggregation)" 200 "$STATUS" "$BODY"
echo -e "  ${YELLOW}Dashboard:${NC} ${BODY:0:200}"

# ============================================================
# TEST 5: Security — No JWT / Invalid JWT / Expired JWT
# ============================================================
print_header "TEST 5: Security — Auth Enforcement"

# No token
sleep 1.5
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "${BASE_URL}/api/v1/users/profile")
STATUS=$(echo "$RESPONSE" | tail -1)
assert_status "No JWT → 401" 401 "$STATUS" "$(echo "$RESPONSE" | head -n -1)"

# Invalid token
sleep 1.5
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "${BASE_URL}/api/v1/users/profile" \
  -H "Authorization: Bearer this.is.not.a.valid.jwt.token")
STATUS=$(echo "$RESPONSE" | tail -1)
assert_status "Invalid JWT → 401" 401 "$STATUS" "$(echo "$RESPONSE" | head -n -1)"

# Tampered token (flip last char)
TAMPERED="${TOKEN:0:-1}X"
sleep 1.5
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "${BASE_URL}/api/v1/users/profile" \
  -H "Authorization: Bearer ${TAMPERED}")
STATUS=$(echo "$RESPONSE" | tail -1)
assert_status "Tampered JWT → 401" 401 "$STATUS" "$(echo "$RESPONSE" | head -n -1)"

# ============================================================
# TEST 6: Security — Direct service bypass (should be blocked)
# ============================================================
print_header "TEST 6: Internal Service Protection"

# Try to hit payment-service directly without internal API key
DIRECT_PAYMENT=$(curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:8082/api/v1/payments/process" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: hacker-user" \
  -d '{"idempotencyKey": "test-idemp-123", "amount": 99.99, "currency": "USD", "description": "Hacked payment"}' 2>/dev/null || true)
if [ "$DIRECT_PAYMENT" = "000" ]; then
  echo -e "  ${GREEN}PASS${NC} Payment service not reachable from host (Docker network isolation)"
  PASS=$((PASS + 1))
else
  assert_status "Direct payment-service access blocked (403)" 403 "$DIRECT_PAYMENT" ""
fi

# Try to hit user-service directly without internal API key
DIRECT_USER=$(curl -s -o /dev/null -w "%{http_code}" -X GET "http://localhost:8081/api/v1/users/profile" \
  -H "X-User-Id: hacker-user" 2>/dev/null || true)
if [ "$DIRECT_USER" = "000" ]; then
  echo -e "  ${GREEN}PASS${NC} User service not reachable from host (Docker network isolation)"
  PASS=$((PASS + 1))
else
  assert_status "Direct user-service access blocked (403)" 403 "$DIRECT_USER" ""
fi

# ============================================================
# TEST 7: Rate Limiting (Gateway-specific)
# ============================================================
sleep 2
print_header "TEST 7: Rate Limiting"

sleep 2
if [ "$GATEWAY" = "direct" ]; then
  echo -e "  ${YELLOW}SKIP${NC} Rate limiting only works through gateway"
else
  echo "  Sending 20 rapid requests to login endpoint..."
  RATE_LIMITED=0
  for i in $(seq 1 20); do
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${BASE_URL}/api/v1/auth/login" \
      -H "Content-Type: application/json" \
      -d '{"email":"ratelimit@test.com","password":"test"}')
    if [ "$STATUS" = "429" ]; then
      RATE_LIMITED=$((RATE_LIMITED + 1))
    fi
  done

  if [ "$RATE_LIMITED" -gt 0 ]; then
    echo -e "  ${GREEN}PASS${NC} Rate limit triggered! $RATE_LIMITED/20 requests got 429"
    PASS=$((PASS + 1))
  else
    echo -e "  ${RED}FAIL${NC} Rate limit NOT triggered (0/20 got 429)"
    FAIL=$((FAIL + 1))
  fi

  echo ""
  echo "  Sending 30 rapid requests to protected endpoint..."
  RATE_LIMITED=0
  for i in $(seq 1 30); do
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "${BASE_URL}/api/v1/users/profile" \
      -H "Authorization: Bearer ${TOKEN}")
    if [ "$STATUS" = "429" ]; then
      RATE_LIMITED=$((RATE_LIMITED + 1))
    fi
  done

  if [ "$RATE_LIMITED" -gt 0 ]; then
    echo -e "  ${GREEN}PASS${NC} Protected rate limit triggered! $RATE_LIMITED/30 requests got 429"
    PASS=$((PASS + 1))
  else
    echo -e "  ${YELLOW}WARN${NC} Protected rate limit NOT triggered (burst may be high)"
  fi
fi

# ============================================================
# TEST 8: Latency Comparison (10 requests)
# ============================================================
print_header "TEST 8: Latency Measurement (10 requests)"

echo "  Measuring response times..."
TOTAL_TIME=0
for i in $(seq 1 10); do
  TIME=$(curl -s -o /dev/null -w "%{time_total}" -X GET "${BASE_URL}/api/v1/users/profile" \
    -H "Authorization: Bearer ${TOKEN}")
  TIME_MS=$(echo "$TIME * 1000" | bc 2>/dev/null || python3 -c "print(round($TIME * 1000, 1))")
  TOTAL_TIME=$(echo "$TOTAL_TIME + $TIME" | bc 2>/dev/null || python3 -c "print(round($TOTAL_TIME + $TIME, 4))")
  printf "  Request %2d: %s ms\n" "$i" "$TIME_MS"
done
AVG=$(echo "scale=1; $TOTAL_TIME / 10 * 1000" | bc 2>/dev/null || python3 -c "print(round($TOTAL_TIME / 10 * 1000, 1))")
echo -e "  ${CYAN}Average: ${AVG} ms${NC}"
echo -e "  ${YELLOW}(Run with both gateways to compare!)${NC}"

# ============================================================
# TEST 9: Gateway Hot Reload Test
# ============================================================
print_header "TEST 9: Hot Reload Behavior"

if [ "$GATEWAY" = "apisix" ]; then
  echo -e "  ${GREEN}APISIX:${NC} Routes are updated via Admin API → instant, zero downtime"
  echo "  To test: modify a route via curl to Admin API while traffic is flowing."
  echo "  Example:"
  echo "    # Change rate limit on-the-fly:"
  echo "    curl -X PATCH http://localhost:9180/apisix/admin/routes/2 \\"
  echo "      -H 'X-API-KEY: admin-api-key-local' \\"
  echo "      -H 'Content-Type: application/json' \\"
  echo "      -d '{\"plugins\":{\"limit-req\":{\"rate\":1,\"burst\":0,\"rejected_code\":429,\"key_type\":\"var\",\"key\":\"remote_addr\"}}}'"
  echo ""
  echo "  Then immediately send traffic → new rate limit takes effect instantly."
elif [ "$GATEWAY" = "krakend" ]; then
  echo -e "  ${YELLOW}KrakenD:${NC} Config changes require container restart"
  echo "  To test:"
  echo "    1. Edit gateway/krakend/krakend.json (e.g., change rate limit)"
  echo "    2. docker compose --profile krakend restart krakend"
  echo "    3. Wait for restart → new config takes effect"
  echo ""
  echo "  This causes brief downtime during restart."
else
  echo -e "  ${YELLOW}SKIP${NC} Hot reload only relevant for gateways"
fi

# ============================================================
# RESULTS
# ============================================================
print_header "TEST RESULTS"

TOTAL=$((PASS + FAIL))
echo -e "  Gateway: ${YELLOW}${GATEWAY}${NC}"
echo -e "  ${GREEN}Passed: ${PASS}${NC}"
echo -e "  ${RED}Failed: ${FAIL}${NC}"
echo -e "  Total:  ${TOTAL}"
echo ""

if [ "$FAIL" -eq 0 ]; then
  echo -e "  ${GREEN}ALL TESTS PASSED!${NC}"
else
  echo -e "  ${RED}SOME TESTS FAILED — check output above${NC}"
fi

echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
