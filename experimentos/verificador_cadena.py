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
    except Exception as exc:
        raise SystemExit(f"[ERROR] No se pudo configurar Django: {exc}")


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


import json

def cotejar_columnas_visibles(fila):
    """Devuelve la lista de columnas cuyo valor visible difiere del canonico.

    contenido_canonico es un JSON con los campos reales del evento. Si el
    canonico dice descripcion='X' pero la columna descripcion dice 'Y',
    alguien alteró la fila visible sin recalcular el hash: manipulacion.
    """
    try:
        canonico = json.loads(fila["contenido_canonico"])
    except (TypeError, ValueError):
        return ["contenido_canonico"]

    payload = canonico.get("payload", {}) if isinstance(canonico, dict) else {}
    discrepancias = []

    # Descripcion vive en payload segun el esquema v1
    desc_canonico = payload.get("descripcion") if isinstance(payload, dict) else None
    if desc_canonico is not None and fila.get("descripcion") != desc_canonico:
        discrepancias.append("descripcion")

    # Fecha del canonico es timestamp del evento
    ts_canonico = canonico.get("timestamp") if isinstance(canonico, dict) else None
    if ts_canonico is not None and fila.get("fecha") is not None:
        # Comparar en ISO string sin milisegundos para tolerar formato
        try:
            fecha_col = fila["fecha"].isoformat().split(".")[0]
            fecha_can = str(ts_canonico).split(".")[0].split("+")[0].rstrip("Z")
            if fecha_col != fecha_can:
                discrepancias.append("fecha")
        except AttributeError:
            pass

    # Actor y entidad_id
    actor_canonico = canonico.get("actor_id") if isinstance(canonico, dict) else None
    if actor_canonico is not None and fila.get("username") != actor_canonico:
        discrepancias.append("username")

    entidad_id_canonico = canonico.get("entidad_id") if isinstance(canonico, dict) else None
    if entidad_id_canonico is not None and fila.get("registro_id") != entidad_id_canonico:
        # entidad_id puede venir como str; comparar por texto
        if str(fila.get("registro_id")) != str(entidad_id_canonico):
            discrepancias.append("registro_id")

    # Reloj lamport
    lamport_canonico = canonico.get("reloj_lamport") if isinstance(canonico, dict) else None
    if lamport_canonico is not None and fila.get("reloj_lamport") != lamport_canonico:
        discrepancias.append("reloj_lamport")

    return discrepancias

def main():
    print("=== AcadTrace: Verificador de Cadena de Auditoria (Criterio E2) ===")
    print("[OK] Funciones criptograficas y verificador de produccion cargados correctamente.")
    print(f"[OK] Bloque genesis configurado: {GENESIS_HASH[:16]}... (longitud: {len(GENESIS_HASH)})")

    print("[INFO] Leyendo cadena de auditoria real desde la base de datos de produccion...")
    from django.db import connection
    try:
        with connection.cursor() as cur:
            cur.execute("SELECT COUNT(*) FROM sga_principal.auditoria")
            total_filas = cur.fetchone()[0]

            cur.execute("""
                SELECT
                    id_auditoria,
                    descripcion,
                    fecha,
                    username,
                    registro_id,
                    reloj_lamport,
                    contenido_canonico,
                    hash_anterior,
                    hash_actual,
                    version_canonica
                FROM sga_principal.auditoria
                ORDER BY reloj_lamport ASC, id_auditoria ASC
            """)
            columnas = [col[0] for col in cur.description]
            todas_las_filas = [dict(zip(columnas, fila)) for fila in cur.fetchall()]

        filas_v1_con_hash = [f for f in todas_las_filas if f.get("version_canonica") == "v1" and f.get("hash_actual")]
        filas_fuera_alcance = total_filas - len(filas_v1_con_hash)

        if filas_fuera_alcance > 0:
            print(f"[AVISO] Hay {filas_fuera_alcance} fila(s) fuera del alcance del verificador "
                  "(sin hash o con version_canonica != 'v1'). No se garantiza su integridad.")

        # Deteccion de INSERT falso sin hash con rol restringido (criterio E43).
        # Una fila con version_canonica='v1' pero hash_actual NULL indica que
        # alguien inserto una fila sin sellarla criptograficamente. Con el rol
        # sga_app esto es posible sin desactivar el trigger, por lo que se
        # trata como ERROR fuerte y no como aviso.
        filas_v1_sin_hash = [
            f for f in todas_las_filas
            if f.get("version_canonica") == "v1" and not f.get("hash_actual")
        ]
        if filas_v1_sin_hash:
            ids_sospechosos = [f.get("id_auditoria") for f in filas_v1_sin_hash]
            print(f"[ERROR] Se detectaron {len(filas_v1_sin_hash)} fila(s) con "
                  f"version_canonica='v1' pero sin hash_actual. IDs sospechosos: "
                  f"{ids_sospechosos}. Estas filas pueden ser INSERTs falsos "
                  "producidos por el rol restringido sga_app sin sello "
                  "criptografico.")
            return 2

        GENESIS = "0" * 64
        if filas_v1_con_hash:
            primer_hash_anterior = filas_v1_con_hash[0].get("hash_anterior")
            if primer_hash_anterior != GENESIS:
                print(f"[ERROR] Primer eslabon (id={filas_v1_con_hash[0]['id_auditoria']}) tiene "
                      f"hash_anterior={primer_hash_anterior!r} en lugar del GENESIS. "
                      "Alguien borro los eslabones anteriores.")
                return 2

        # Deteccion de retroceso de cabeza (criterio E43).
        # estado_cadena_auditoria.ultimo_hash debe coincidir con el hash_actual
        # del ultimo eslabon v1 con hash. Si un atacante borra el ultimo eslabon
        # y retrocede la cabeza para que "cuadre", esta comprobacion lo detecta.
        if filas_v1_con_hash:
            ultimo_hash_esperado = filas_v1_con_hash[-1].get("hash_actual")
            try:
                with connection.cursor() as cur_cabeza:
                    cur_cabeza.execute(
                        "SELECT ultimo_hash FROM sga_principal.estado_cadena_auditoria "
                        "WHERE id_estado = 1"
                    )
                    fila_cabeza = cur_cabeza.fetchone()
                    if fila_cabeza is None:
                        print("[ERROR] No existe fila singleton en "
                              "sga_principal.estado_cadena_auditoria (id_estado=1). "
                              "La cabeza de cadena no puede verificarse.")
                        return 2
                    cabeza_almacenada = fila_cabeza[0]
                    if cabeza_almacenada != ultimo_hash_esperado:
                        print(f"[ERROR] Retroceso de cabeza detectado. La cabeza "
                              f"almacenada es {cabeza_almacenada[:16]!r}... pero el "
                              f"ultimo eslabon v1 tiene hash "
                              f"{ultimo_hash_esperado[:16]!r}... "
                              "Alguien borro eslabones y ajusto la cabeza.")
                        return 2
            except Exception as exc_cabeza:
                print(f"[ERROR] No se pudo consultar la cabeza de cadena en "
                      f"estado_cadena_auditoria: {exc_cabeza}")
                return 2

        for fila in filas_v1_con_hash:
            discrepancias = cotejar_columnas_visibles(fila)
            if discrepancias:
                print(f"[ERROR] Eslabon id={fila['id_auditoria']}: columnas alteradas fuera del hash: {discrepancias}")
                return 2

        resultado = verificar_cadena_global()
        print(f"[INFO] Eslabones leidos y comprobados: {resultado.registros_verificados}")
        if resultado.valido:
            print(f"[OK] Verificacion completada: {len(filas_v1_con_hash)} eslabon(es) validado(s), "
                  f"{filas_fuera_alcance} fila(s) fuera del alcance.")
            if filas_fuera_alcance == 0:
                print(f"=== Resultado: Cadena global integra sobre {len(filas_v1_con_hash)} eslabones (sin filas fuera del alcance) ===")
            else:
                print(f"=== Resultado: Cadena global integra sobre {len(filas_v1_con_hash)} eslabones; "
                      f"{filas_fuera_alcance} fila(s) fuera del alcance no verificadas ===")
            return 0
        else:
            print(f"[ERROR] Inconsistencia detectada en eslabon {resultado.primer_eslabon_roto}: {resultado.tipo_inconsistencia}")
            print(f"=== Resultado: Cadena invalida ({resultado.registros_verificados} eslabones validos antes de la ruptura) ===")
            return 2

    except Exception as exc:
        print(f"[ERROR] Fallo la consulta a sga_principal.auditoria: {exc}")
        print("[ERROR] NO se recurre a la cadena local de Docente como fallback silencioso.")
        print("[ERROR] Revisa las variables DB_HOST/DB_PORT/DB_USER/DB_PASSWORD y vuelve a ejecutar.")
        return 1

if __name__ == "__main__":
    sys.exit(main())
