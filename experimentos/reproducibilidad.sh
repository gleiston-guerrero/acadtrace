#!/usr/bin/env bash
# ==============================================================================
# E7 — Certificado y Verificación de Reproducibilidad Criptográfica
# Proyecto AcadTrace - SGA Escuela (Entrega 4)
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTADOS_DIR="$SCRIPT_DIR/resultados"
CERT_FILE="REPRODUCIBILIDAD.txt"

# Seleccionar ejecutable de Python disponible (python3 o python)
if [ -z "${PYTHON_CMD:-}" ]; then
  if command -v python3 >/dev/null 2>&1; then
    PYTHON_CMD="python3"
  elif command -v python >/dev/null 2>&1; then
    PYTHON_CMD="python"
  else
    PYTHON_CMD="python3"
  fi
fi

# Los 6 artefactos oficiales de E7
ARTIFACTS=(
  "deteccion.csv"
  "manipulaciones.csv"
  "exp1_concurrencia.csv"
  "exp3_reconciliacion.csv"
  "iso25010.csv"
  "boxplot_latencia.png"
)

# Detectar herramienta de verificación de SHA-256
SHA_CMD=""
if command -v sha256sum >/dev/null 2>&1; then
  SHA_CMD="sha256sum"
elif command -v shasum >/dev/null 2>&1; then
  SHA_CMD="shasum -a 256"
fi

if [ -z "$SHA_CMD" ]; then
  echo "ERROR: sha256sum (o shasum) no encontrado en PATH." >&2
  exit 1
fi

REGENERATE=false

# Si el usuario pasa banderas de generación o modo, regenerar
for arg in "$@"; do
  case "$arg" in
    --generate|-g|--regenerate|--mode|--force)
      REGENERATE=true
      ;;
  esac
done

# Si el certificado o alguno de los 6 artefactos oficiales no existe, regenerar
if [ ! -f "$RESULTADOS_DIR/$CERT_FILE" ]; then
  REGENERATE=true
else
  for art in "${ARTIFACTS[@]}"; do
    if [ ! -f "$RESULTADOS_DIR/$art" ]; then
      REGENERATE=true
      break
    fi
  done
fi

if [ "$REGENERATE" = true ]; then
  echo "==> Generando / regenerando artefactos experimentales E7..."
  if ! command -v "$PYTHON_CMD" >/dev/null 2>&1; then
    echo "ERROR: instala Python 3 y configura PATH o PYTHON_CMD." >&2
    exit 1
  fi
  "$PYTHON_CMD" "$SCRIPT_DIR/run_experimentos.py" "$@"
fi

# Verificación criptográfica obligatoria con sha256sum -c
echo "==> Verificando hashes SHA-256 de los 6 artefactos oficiales..."
cd "$RESULTADOS_DIR"

if [ ! -f "$CERT_FILE" ]; then
  echo "ERROR: Certificado $CERT_FILE no encontrado en $RESULTADOS_DIR" >&2
  exit 1
fi

# Ejecución de sha256sum -c
# Se comprueba directamente el certificado oficial REPRODUCIBILIDAD.txt
set +e
$SHA_CMD -c "$CERT_FILE"
VERIFY_STATUS=$?
set -e

if [ $VERIFY_STATUS -ne 0 ]; then
  echo "ERROR: La verificación con $SHA_CMD -c FALLÓ (exit code $VERIFY_STATUS)." >&2
  exit $VERIFY_STATUS
fi

echo "OK: Los 6 artefactos oficiales coinciden con el certificado de reproducibilidad."
exit 0
