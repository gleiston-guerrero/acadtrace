#!/usr/bin/env bash
# Compatibilidad Bash: generación y certificado E7 implementados en Python.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PYTHON_CMD="${PYTHON_CMD:-python3}"
if ! command -v "$PYTHON_CMD" >/dev/null 2>&1; then
    echo "ERROR: instala Python 3 y configura PATH o PYTHON_CMD." >&2
    exit 1
fi
exec "$PYTHON_CMD" "$SCRIPT_DIR/run_experimentos.py" "$@"
