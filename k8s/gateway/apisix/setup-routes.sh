#!/bin/bash
# ============================================================
# APISIX Route Setup Script
# Run AFTER APISIX is deployed in K8s
# Usage: ./setup-routes.sh
# ============================================================

APISIX_ADMIN="http://localhost:9180"
API_KEY="admin-key-change-in-prod"

echo "==> Setting up APISIX routes..."

# -----------------------------------------------------------
# Route 1: Auth endpoints (no JWT required)
#   POST /api/auth/register
#   POST /api/auth/login
# -----------------------------------------------------------
curl -s -X PUT "${APISIX_ADMIN}/apisix/admin/routes/1" \
  -H "X-API-KEY: ${API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "auth-routes",
    "uri": "/api/auth/*",
    "methods": ["POST"],
    "plugins": {
      "limit-req": {
        "rate": 5,
        "burst": 10,
        "rejected_code": 429,
        "key_type": "var",
        "key": "remote_addr"
      },
      "cors": {
        "allow_origins": "*",
        "allow_methods": "POST, OPTIONS",
        "allow_headers": "Content-Type, Authorization",
        "max_age": 3600
      }
    },
    "upstream": {
      "type": "roundrobin",
      "nodes": {
        "orchestrator.office-mb.svc.cluster.local:80": 1
      }
    }
  }'

echo ""

# -----------------------------------------------------------
# Route 2: Protected API endpoints (JWT required)
#   All /api/* except /api/auth/*
# -----------------------------------------------------------
curl -s -X PUT "${APISIX_ADMIN}/apisix/admin/routes/2" \
  -H "X-API-KEY: ${API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "protected-routes",
    "uri": "/api/*",
    "methods": ["GET", "POST", "PUT", "DELETE"],
    "priority": 0,
    "plugins": {
      "jwt-auth": {},
      "limit-req": {
        "rate": 20,
        "burst": 40,
        "rejected_code": 429,
        "key_type": "var",
        "key": "remote_addr"
      },
      "cors": {
        "allow_origins": "*",
        "allow_methods": "GET, POST, PUT, DELETE, OPTIONS",
        "allow_headers": "Content-Type, Authorization",
        "max_age": 3600
      }
    },
    "upstream": {
      "type": "roundrobin",
      "nodes": {
        "orchestrator.office-mb.svc.cluster.local:80": 1
      }
    }
  }'

echo ""

# -----------------------------------------------------------
# Create JWT consumer (for APISIX jwt-auth plugin)
# This tells APISIX how to validate JWTs
# -----------------------------------------------------------
curl -s -X PUT "${APISIX_ADMIN}/apisix/admin/consumers" \
  -H "X-API-KEY: ${API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "office-mb-user",
    "plugins": {
      "jwt-auth": {
        "key": "office-mb-jwt",
        "secret": "c2VjdXJlLXJhbmRvbS1rZXktZm9yLWp3dC1zaWduaW5nLXRoaXMtc2hvdWxkLWJlLWxvbmctZW5vdWdo",
        "algorithm": "HS256"
      }
    }
  }'

echo ""
echo "==> APISIX routes configured!"
echo ""
echo "Test auth:      curl -X POST http://<NODE_IP>:30080/api/auth/login -H 'Content-Type: application/json' -d '{\"email\":\"test@test.com\",\"password\":\"pass\"}'"
echo "Test protected: curl http://<NODE_IP>:30080/api/users/profile -H 'Authorization: Bearer <token>'"
