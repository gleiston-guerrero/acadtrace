#!/usr/bin/env python3
"""
Adaptador experimental del verificador de auditoría de Docente.

E2:
- No reimplementa SHA-256.
- No mantiene un algoritmo de verificación paralelo.
- Reutiliza directamente hashing.py y verifier.py de producción.
- La evidencia de cadena real utiliza el contrato global canónico v1.
"""

import os
from pathlib import Path
import sys


REPO_ROOT = Path(__file__).resolve().parents[1]
DOCENTE_DIR = REPO_ROOT / "microservicio-docente"

if str(DOCENTE_DIR) not in sys.path:
    sys.path.insert(0, str(DOCENTE_DIR))

# Configuración de Django para ejecución independiente desde CLI
if not os.environ.get("DJANGO_SETTINGS_MODULE"):
    try:
        from django.conf import settings
        if not settings.configured:
            db_engine = (
                "django.db.backends.postgresql"
                if (os.environ.get("DB_NAME") or os.environ.get("DB_HOST"))
                else "django.db.backends.sqlite3"
            )
            if db_engine == "django.db.backends.postgresql":
                db_conf = {
                    "ENGINE": "django.db.backends.postgresql",
                    "NAME": os.environ.get("DB_NAME", "sga"),
                    "USER": os.environ.get("DB_USER", "postgres"),
                    "PASSWORD": os.environ.get("DB_PASSWORD", ""),
                    "HOST": os.environ.get("DB_HOST", "localhost"),
                    "PORT": os.environ.get("DB_PORT", "5432"),
                    "OPTIONS": {"options": "-c search_path=sga_docente,sga_principal,public"},
                }
            else:
                db_conf = {"ENGINE": "django.db.backends.sqlite3", "NAME": ":memory:"}

            settings.configure(
                SECRET_KEY="temporary-secret-for-standalone-verifier",
                INSTALLED_APPS=[
                    "django.contrib.contenttypes",
                    "django.contrib.auth",
                    "docentes",
                ],
                DATABASES={"default": db_conf},
            )
            import django
            django.setup()
    except Exception:
        pass


from docentes.auditoria.hashing import (  # noqa: E402
    GENESIS_HASH,
    calcular_hash,
    calcular_hash_canonico,
    contenido_evento,
    json_canonico,
    normalizar,
)

from docentes.auditoria.verifier import (  # noqa: E402
    ResultadoVerificacion,
    verificar_cadena,
    verificar_cadena_global,
    verificar_estado_academico,
)


__all__ = [
    "GENESIS_HASH",
    "ResultadoVerificacion",
    "calcular_hash",
    "calcular_hash_canonico",
    "contenido_evento",
    "json_canonico",
    "normalizar",
    "verificar_cadena",
    "verificar_cadena_global",
    "verificar_estado_academico",
]


def main():
    print("=== AcadTrace: Verificador de Cadena de Auditoría (Criterio E2) ===")
    print("[OK] Funciones criptográficas y verificador de producción cargados correctamente.")
    print(f"[OK] Bloque génesis configurado: {GENESIS_HASH[:16]}... (longitud: {len(GENESIS_HASH)})")

    # E2 / Punto 43:
    # El verificador debe leer la cadena real persistida en la base de datos
    # de producción en lugar de fabricar un eslabón sintético en memoria.
    print("[INFO] Leyendo cadena de auditoría real desde la base de datos de producción...")
    try:
        resultado = verificar_cadena_global()
        print(f"[INFO] Eslabones leídos y comprobados: {resultado.registros_verificados}")
        if resultado.valido:
            print(f"[OK] Verificación completada: {resultado.registros_verificados} eslabón(es) validado(s).")
            print(f"=== Resultado: Cadena global íntegra ({resultado.registros_verificados} eslabones confirmados) ===")
            return 0
        else:
            print(f"[ERROR] Inconsistencia detectada en eslabón {resultado.primer_eslabon_roto}: {resultado.tipo_inconsistencia}")
            print(f"=== Resultado: Cadena inválida ({resultado.registros_verificados} eslabones válidos antes de la ruptura) ===")
            return 2
    except Exception as exc:
        try:
            from docentes.models import EstadoCadenaAuditoria, EventoAuditoria
            estado = EstadoCadenaAuditoria.objects.filter(id_estado=1).first()
            eventos = list(EventoAuditoria.objects.filter(modo__in=["m2", "m3"]).order_by("id_evento"))
            resultado = verificar_cadena(
                eventos,
                hash_cabeza=estado.ultimo_hash if estado else None,
                lamport_cabeza=estado.ultimo_lamport if estado else None,
            )
            print(f"[INFO] Eslabones locales leídos de base de datos: {resultado.registros_verificados}")
            if resultado.valido:
                print(f"[OK] Verificación completada: {resultado.registros_verificados} eslabón(es) locales validado(s).")
                print(f"=== Resultado: Cadena local íntegra ({resultado.registros_verificados} eslabones confirmados) ===")
                return 0
            else:
                print(f"[ERROR] Inconsistencia en eslabón {resultado.primer_eslabon_roto}: {resultado.tipo_inconsistencia}")
                print(f"=== Resultado: Cadena local inválida ({resultado.registros_verificados} eslabones válidos) ===")
                return 2
        except Exception as local_exc:
            print(f"[ERROR] Error al consultar la cadena de auditoría en la base de datos: {exc} (local: {local_exc})")
            return 1


if __name__ == "__main__":
    sys.exit(main())
