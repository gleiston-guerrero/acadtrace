#!/usr/bin/env python3
"""
Adaptador experimental del verificador de auditoría de Docente.

E2:
- No reimplementa SHA-256.
- No mantiene un algoritmo de verificación paralelo.
- Reutiliza directamente hashing.py y verifier.py de producción.
"""

from pathlib import Path
import sys


REPO_ROOT = Path(__file__).resolve().parents[1]
DOCENTE_DIR = REPO_ROOT / "microservicio-docente"

if str(DOCENTE_DIR) not in sys.path:
    sys.path.insert(0, str(DOCENTE_DIR))


from docentes.auditoria.hashing import (  # noqa: E402
    GENESIS_HASH,
    calcular_hash,
    contenido_evento,
    json_canonico,
    normalizar,
)
from docentes.auditoria.verifier import (  # noqa: E402
    ResultadoVerificacion,
    verificar_cadena,
    verificar_estado_academico,
)


__all__ = [
    "GENESIS_HASH",
    "ResultadoVerificacion",
    "calcular_hash",
    "contenido_evento",
    "json_canonico",
    "normalizar",
    "verificar_cadena",
    "verificar_estado_academico",
]