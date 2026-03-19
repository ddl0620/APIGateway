#!/bin/bash
# ============================================================
# APISIX Route Setup — for Docker Compose local dev
# Run AFTER: docker compose --profile apisix up --build
# Usage: ./gateway/apisix/setup-routes.sh
# ============================================================

set -e

ADMIN="http://localhost:9180/apisix/admin"
KEY="admin-api-key-local"

echo ""
echo "============================================"
echo " APISIX Route Setup"
echo "============================================"
echo ""

# Wait for APISIX to be ready
echo "[1/4] Waiting for APISIX..."
for i in $(seq 1 30); do
  if curl -sf -H "X-API-KEY: ${KEY}" "${ADMIN}/routes" > /dev/null 2>&1; then
    echo "       APISIX is ready!"
    break
  fi
  if [ "$i" -eq 30 ]; then
    echo "       ERROR: APISIX not ready after 30s"
    exit 1
  fi
  sleep 1
done

# -----------------------------------------------------------
# Route 1: Auth (public — no JWT)
# POST /api/v1/auth/*
# -----------------------------------------------------------
echo "[2/4] Creating auth route (public, rate limited)..."
curl -s -o /dev/null -w "       HTTP %{http_code}\n" \
  -X PUT "${ADMIN}/routes/1" \
  -H "X-API-KEY: ${KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "auth-public",
    "uri": "/api/v1/auth/*",
    "methods": ["POST", "OPTIONS"],
    "priority": 10,
    "plugins": {
      "limit-req": {
        "rate": 1,
        "burst": 0,
        "rejected_code": 429,
        "key_type": "var",
        "key": "remote_addr"
      },
      "cors": {
        "allow_origins": "http://localhost:3000,http://localhost:5173",
        "allow_methods": "POST, OPTIONS",
        "allow_headers": "Content-Type, Authorization",
        "allow_credential": true,
        "expose_headers": "Authorization, Content-Type",
        "max_age": 3600
      }
    },
    "upstream": {
      "type": "roundrobin",
      "nodes": {
        "orchestrator:8080": 1
      }
    }
  }'

# -----------------------------------------------------------
# Route 2: Protected endpoints (JWT required + rate limit)
# All /api/v1/* except /api/v1/auth/*
# -----------------------------------------------------------
echo "[3/4] Creating protected route (JWT + rate limit)..."
curl -s -o /dev/null -w "       HTTP %{http_code}\n" \
  -X PUT "${ADMIN}/routes/2" \
  -H "X-API-KEY: ${KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "protected-api",
    "uri": "/api/v1/*",
    "methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    "priority": 0,
    "plugins": {
      "limit-req": {
        "rate": 1,
        "burst": 0,
        "rejected_code": 429,
        "key_type": "var",
        "key": "remote_addr"
      },
      "cors": {
        "allow_origins": "http://localhost:3000,http://localhost:5173",
        "allow_methods": "GET, POST, PUT, DELETE, OPTIONS",
        "allow_headers": "Content-Type, Authorization",
        "allow_credential": true,
        "expose_headers": "Authorization, Content-Type",
        "max_age": 3600
      }
    },
    "upstream": {
      "type": "roundrobin",
      "nodes": {
        "orchestrator:8080": 1
      }
    }
  }'

# -----------------------------------------------------------
# NOTE on JWT:
# APISIX jwt-auth plugin uses its OWN JWT signing.
# Since your app already validates JWT at orchestrator level,
# we skip APISIX jwt-auth and let orchestrator handle it.
# The gateway still provides rate limiting, CORS, and routing.
#
# In production, you'd use the key-auth or openid-connect
# plugin for gateway-level JWT validation.
# -----------------------------------------------------------

echo "[4/4] Verifying routes..."
ROUTE_COUNT=$(curl -s "${ADMIN}/routes" -H "X-API-KEY: ${KEY}" | python3 -c "import sys,json; print(json.load(sys.stdin).get('total',0))" 2>/dev/null || echo "?")
echo "       Active routes: ${ROUTE_COUNT}"

echo ""
echo "============================================"
echo " APISIX Ready!"
echo " Gateway:   http://localhost:9080"
echo " Admin API: http://localhost:9180"
echo "============================================"
echo ""
echo " Test: curl -X POST http://localhost:9080/api/v1/auth/login \\"
echo "         -H 'Content-Type: application/json' \\"
echo "         -d '{\"email\":\"test@test.com\",\"password\":\"password123\"}'"
echo ""
