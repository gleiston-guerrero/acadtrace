#!/usr/bin/env python3
"""Adaptador experimental del verificador productivo de auditoría."""

import os
import sys
from pathlib import Path

MICROSERVICIO_DIR = Path(__file__).resolve().parents[1] / "microservicio-docente"
if str(MICROSERVICIO_DIR) not in sys.path:
    sys.path.insert(0, str(MICROSERVICIO_DIR))
os.environ.setdefault("DJANGO_SETTINGS_MODULE", "micro_docente.settings")

import django
django.setup()

from docentes.auditoria.verifier import (
    ResultadoVerificacion,
    verificar_cadena as verificar_cadena_productiva,
    verificar_estado_academico,
)


def verificar_cadena(eventos, hash_cabeza=None, lamport_cabeza=None):
    return verificar_cadena_productiva(
        eventos, hash_cabeza=hash_cabeza, lamport_cabeza=lamport_cabeza
    )


def verificar_registros(eventos, hash_cabeza=None, lamport_cabeza=None):
    return verificar_cadena(
        eventos, hash_cabeza=hash_cabeza, lamport_cabeza=lamport_cabeza
    ).valido


__all__ = [
    "ResultadoVerificacion",
    "verificar_cadena",
    "verificar_estado_academico",
    "verificar_registros",
]
