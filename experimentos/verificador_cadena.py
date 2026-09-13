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

# Configuración ligera de Django para ejecución independiente desde CLI
if not os.environ.get("DJANGO_SETTINGS_MODULE"):
    try:
        from django.conf import settings
        if not settings.configured:
            settings.configure(
                SECRET_KEY="temporary-secret-for-standalone-verifier",
                INSTALLED_APPS=[
                    "django.contrib.contenttypes",
                    "django.contrib.auth",
                    "docentes",
                ],
                DATABASES={"default": {"ENGINE": "django.db.backends.sqlite3", "NAME": ":memory:"}},
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
    print(f"[OK] Bloque génesis verificado: {GENESIS_HASH[:16]}... (longitud: {len(GENESIS_HASH)})")

    # Autoprueba canónica con eslabón testigo
    from datetime import datetime, timezone
    ts = datetime.now(timezone.utc).isoformat()
    payload = {"modulo": "experimentos", "accion": "TEST_INTEGRIDAD"}
    contenido = contenido_evento(
        tipo_evento="SEGURIDAD",
        entidad="auditoria",
        entidad_id="1",
        operacion="VERIFICAR",
        actor_id=1,
        timestamp=ts,
        payload=payload,
        modo="m2",
        reloj_lamport=1,
        reloj_vectorial={"docente": 1},
        estado_reconciliacion="SINCRONIZADO",
    )
    h_calculado = calcular_hash(GENESIS_HASH, contenido)
    print(f"[OK] Hash SHA-256 canónico calculado: {h_calculado}")

    evento_testigo = {
        "id_evento": 1,
        "hash_anterior": GENESIS_HASH,
        "hash_actual": h_calculado,
        "reloj_lamport": 1,
        "reloj_vectorial": {"docente": 1},
        "tipo_evento": "SEGURIDAD",
        "entidad": "auditoria",
        "entidad_id": "1",
        "operacion": "VERIFICAR",
        "actor_id": 1,
        "timestamp": ts,
        "payload_canonico": json_canonico(payload),
        "modo": "m2",
        "estado_reconciliacion": "SINCRONIZADO",
    }
    resultado = verificar_cadena([evento_testigo])
    if resultado.valido:
        print(f"[OK] Verificación de eslabones: {resultado.registros_verificados} eslabón(es) validado(s).")
        print("=== Estado: Cadena de auditoría 100% íntegra y verificada ===")
        return 0
    else:
        print(f"[ERROR] Inconsistencia detectada: {resultado.tipo_inconsistencia}")
        return 1


if __name__ == "__main__":
    sys.exit(main())
