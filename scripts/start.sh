#!/usr/bin/env bash
# =============================================================================
# scripts/start.sh - Arrancador del stack completo AcadTrace (Criterio E16)
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

echo "=== AcadTrace: Iniciando stack completo ==="

# Validar archivo .env
if [ ! -f ".env" ]; then
    if [ -f ".env.example" ]; then
        echo "Aviso: Creando .env a partir de .env.example..."
        cp .env.example .env
    fi
fi

# Levantar contenedores
docker compose up -d

echo "=== Stack desplegado exitosamente ==="
echo "Frontend SGA: http://localhost:5173"
echo "API Gateway HAProxy: http://localhost:8080 (Stats: http://localhost:8404)"
echo "Prometheus: http://localhost:9090"
echo "Grafana: http://localhost:3001"
