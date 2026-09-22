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


import hashlib
import hmac
import json


def calcular_hmac(secret: str, fila: dict) -> str:
    """Calcula firma HMAC-SHA256 para una fila de auditoria segun HmacService.

    Campos canonicos: schema_origen | trace_id | username | accion | tabla_afectada |
                      registro_id | descripcion | resultado | fecha_ms
    """
    schema_origen = fila.get("schema_origen") or ""
    trace_id = fila.get("trace_id") or ""
    username = fila.get("username") or ""
    accion = fila.get("accion") or ""
    tabla_afectada = fila.get("tabla_afectada") or ""
    registro_id = fila.get("registro_id")
    reg_id_str = str(registro_id) if registro_id is not None else ""
    descripcion = fila.get("descripcion") or ""
    resultado = fila.get("resultado") or ""

    fecha = fila.get("fecha")
    if hasattr(fecha, "timestamp"):
        fecha_ms = str(int(fecha.timestamp() * 1000))
    elif isinstance(fecha, (int, float)):
        fecha_ms = str(int(fecha))
    elif isinstance(fecha, str):
        try:
            from datetime import datetime
            dt = datetime.fromisoformat(fecha.replace("Z", "+00:00"))
            fecha_ms = str(int(dt.timestamp() * 1000))
        except Exception:
            fecha_ms = str(fecha)
    else:
        fecha_ms = ""

    campos = [
        str(schema_origen),
        str(trace_id),
        str(username),
        str(accion),
        str(tabla_afectada),
        reg_id_str,
        str(descripcion),
        str(resultado),
        fecha_ms,
    ]
    canonical = "|".join(campos)
    return hmac.new(secret.encode("utf-8"), canonical.encode("utf-8"), hashlib.sha256).hexdigest()

def _normalizar_ts(val):
    if val is None:
        return ""
    if hasattr(val, "isoformat"):
        s = val.isoformat()
    else:
        s = str(val)
    s = s.replace(" ", "T")
    if "." in s:
        s = s.split(".")[0]
    if "+" in s:
        s = s.split("+")[0]
    s = s.rstrip("Z")
    return s


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

    if not isinstance(canonico, dict):
        return ["contenido_canonico"]

    payload = canonico.get("payload", {}) if isinstance(canonico.get("payload"), dict) else {}
    discrepancias = []

    # 1. Descripcion:
    # En Principal/Secretaria viaja en payload.get("descripcion").
    # En Docente, central_ledger escribe tipo_evento en la columna descripcion si payload no tiene descripcion.
    desc_canonico = payload.get("descripcion")
    if desc_canonico is None:
        desc_canonico = canonico.get("tipo_evento")

    if desc_canonico is not None:
        val_desc = fila.get("descripcion")
        if val_desc is None or str(val_desc) != str(desc_canonico):
            discrepancias.append("descripcion")

    # 2. Fecha:
    # Comparar normalizando a segundos (evita falso positivo cuando Instant de Java
    # tiene 0 ms, p.ej. ...:01Z vs Postgres ...:01+00:00).
    ts_canonico = canonico.get("timestamp")
    if ts_canonico is not None and fila.get("fecha") is not None:
        f_col = _normalizar_ts(fila.get("fecha"))
        f_can = _normalizar_ts(ts_canonico)
        if f_col != f_can:
            discrepancias.append("fecha")

    # 3. Actor (username):
    # Comparar como texto para evitar falso positivo con actor_id entero de Docente (p.ej. 7 vs "7")
    actor_canonico = canonico.get("actor_id")
    if actor_canonico is not None:
        user_col = fila.get("username")
        if user_col is None or str(user_col) != str(actor_canonico):
            discrepancias.append("username")

    # 4. Registro ID (entidad_id):
    entidad_id_canonico = canonico.get("entidad_id")
    if entidad_id_canonico is not None:
        reg_col = fila.get("registro_id")
        if reg_col is not None and str(reg_col) != str(entidad_id_canonico):
            discrepancias.append("registro_id")

    # 5. Reloj lamport:
    lamport_canonico = canonico.get("reloj_lamport")
    if lamport_canonico is not None and fila.get("reloj_lamport") is not None:
        try:
            if int(fila.get("reloj_lamport")) != int(lamport_canonico):
                discrepancias.append("reloj_lamport")
        except (ValueError, TypeError):
            discrepancias.append("reloj_lamport")

    # 6. Schema origen:
    schema_can = payload.get("schema_origen")
    if schema_can is None and canonico.get("reloj_vectorial") and "docente" in str(canonico.get("reloj_vectorial")).lower():
        schema_can = "DOCENTE"
    if schema_can and "schema_origen" in fila and fila.get("schema_origen") is not None:
        if str(fila.get("schema_origen")).upper() != str(schema_can).upper():
            discrepancias.append("schema_origen")

    # 7. Tabla afectada (entidad):
    entidad_can = canonico.get("entidad")
    if entidad_can and "tabla_afectada" in fila and fila.get("tabla_afectada") is not None:
        if str(fila.get("tabla_afectada")).lower() != str(entidad_can).lower():
            discrepancias.append("tabla_afectada")

    # 8. Accion (operacion):
    operacion_can = canonico.get("operacion")
    if "accion" in fila and fila.get("accion") is not None:
        val_accion = str(fila.get("accion")).upper()
        if str(fila.get("schema_origen", "")).upper() == "DOCENTE":
            if val_accion != "AUDITAR":
                discrepancias.append("accion")
        elif operacion_can and val_accion != str(operacion_can).upper():
            discrepancias.append("accion")

    # 9. Resultado:
    res_can = payload.get("resultado") or "EXITO"
    if "resultado" in fila and fila.get("resultado") is not None:
        if str(fila.get("resultado")).upper() != str(res_can).upper():
            discrepancias.append("resultado")

    # 10. IP address:
    ip_can = payload.get("ip_address")
    if ip_can and "ip_address" in fila and fila.get("ip_address") is not None:
        if str(fila.get("ip_address")) != str(ip_can):
            discrepancias.append("ip_address")

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
                    version_canonica,
                    schema_origen,
                    tabla_afectada,
                    accion,
                    resultado,
                    ip_address,
                    trace_id,
                    hmac
                FROM sga_principal.auditoria
                ORDER BY reloj_lamport ASC, id_auditoria ASC
            """)
            columnas = [col[0] for col in cur.description]
            todas_las_filas = [dict(zip(columnas, fila)) for fila in cur.fetchall()]

        # Clasificación rigurosa de filas:
        # 1. Filas m1 legítimas: bitácora relacional convencional (sin versión v1, sin hashes, sin reloj Lamport)
        # 2. Filas v1 legítimas: selladas criptográficamente con version_canonica='v1'
        # 3. Filas inválidas: registros rotos, alterados o a medio versionar
        filas_m1 = []
        filas_v1 = []
        filas_invalidas = []

        for f in todas_las_filas:
            vc = f.get("version_canonica")
            ha = f.get("hash_actual")
            hant = f.get("hash_anterior")
            cc = f.get("contenido_canonico")
            lp = f.get("reloj_lamport")

            # Fila legitima m1: sin encadenamiento criptografico
            if vc is None and not ha and not hant and not cc and not lp:
                filas_m1.append(f)
            # Fila candidata v1: debe tener todos los metadatos criptograficos requeridos
            elif vc == "v1" and ha and hant and cc and lp is not None:
                filas_v1.append(f)
            else:
                filas_invalidas.append(f)

        if filas_invalidas:
            ids_invalidos = [f.get("id_auditoria") for f in filas_invalidas]
            print(f"[ERROR] Se detectaron {len(filas_invalidas)} fila(s) invalidas o fuera del alcance "
                  f"(sin hash_actual, sin contenido_canonico o con version_canonica != 'v1'). "
                  f"IDs sospechosos: {ids_invalidos}. Manipulacion detectada: "
                  "eslabones no sellados criptograficamente o desversionados.")
            return 2

        if filas_m1:
            print(f"[INFO] {len(filas_m1)} fila(s) legitimas en modo m1 (bitacora relacional sin encadenamiento) identificadas.")

        # Si el despliegue es 100% modo m1 (sin filas v1)
        if not filas_v1 and filas_m1:
            print(f"[OK] Bitacora relacional convencional en modo m1 validada ({len(filas_m1)} eventos sin manipulaciones estructurales).")
            print(f"=== Resultado: Bitacora m1 integra sobre {len(filas_m1)} registros ===")
            return 0

        # Verificacion de HMAC para eslabones v1
        jwt_secret = os.environ.get("JWT_SECRET")
        for f in filas_v1:
            schema = (f.get("schema_origen") or "").upper()
            hmac_stored = f.get("hmac")
            # En Principal y Secretaria la firma HMAC institucional es obligatoria
            if schema in ("PRINCIPAL", "SECRETARIA"):
                if not hmac_stored or len(str(hmac_stored)) != 64:
                    print(f"[ERROR] Eslabon id={f.get('id_auditoria')}: falta firma HMAC requerida para {schema} "
                          f"(hmac={hmac_stored!r}). Manipulacion detectada: insercion directa sin pasar por el servicio de aplicacion.")
                    return 2

            # Verificacion criptografica del HMAC si disponemos del secreto
            if hmac_stored and jwt_secret:
                hmac_calc = calcular_hmac(jwt_secret, f)
                if not hmac.compare_digest(str(hmac_stored).lower(), hmac_calc.lower()):
                    print(f"[ERROR] Eslabon id={f.get('id_auditoria')}: firma HMAC invalida. "
                          f"Manipulacion detectada: registro alterado o insertado sin el secreto institucional.")
                    return 2

        # Verificacion del eslabon inicial respecto al GENESIS
        GENESIS = "0" * 64
        if filas_v1:
            primer_hash_anterior = filas_v1[0].get("hash_anterior")
            if primer_hash_anterior != GENESIS:
                print(f"[ERROR] Primer eslabon (id={filas_v1[0]['id_auditoria']}) tiene "
                      f"hash_anterior={primer_hash_anterior!r} en lugar del GENESIS. "
                      "Alguien borro los eslabones anteriores.")
                return 2

        # Deteccion de truncamiento final / borrado de ultimos eslabones:
        # 1. Comprobar la secuencia de PostgreSQL para verificar que no falte la cola
        try:
            with connection.cursor() as cur_seq:
                cur_seq.execute(
                    "SELECT last_value, is_called FROM sga_principal.auditoria_id_auditoria_seq"
                )
                seq_row = cur_seq.fetchone()
                if seq_row and seq_row[1]:  # si ya fue llamada (is_called = true)
                    seq_val = seq_row[0]
                    max_id = max((f.get("id_auditoria") or 0) for f in todas_las_filas) if todas_las_filas else 0
                    if max_id < seq_val:
                        print(f"[ERROR] Truncamiento final detectado: la secuencia auditoria_id_auditoria_seq esta en {seq_val} "
                              f"pero el ultimo registro es id={max_id}. Se eliminaron los ultimos eslabones y se retrocedio la cabeza.")
                        return 2
        except Exception:
            pass

        # 2. Comprobar monotonicidad estricta y orden de IDs y reloj Lamport en la cadena v1
        for i in range(len(filas_v1) - 1):
            cur_f = filas_v1[i]
            next_f = filas_v1[i + 1]
            if int(cur_f["id_auditoria"]) >= int(next_f["id_auditoria"]):
                print(f"[ERROR] Inconsistencia en orden de IDs: {cur_f['id_auditoria']} >= {next_f['id_auditoria']}")
                return 2
            if int(cur_f["reloj_lamport"]) >= int(next_f["reloj_lamport"]):
                print(f"[ERROR] Lamport no monotonico: {cur_f['reloj_lamport']} >= {next_f['reloj_lamport']}")
                return 2

        # Deteccion de retroceso o divergencia de cabeza
        cabeza_hash = None
        cabeza_lamport = None
        if filas_v1:
            ultimo_hash_esperado = filas_v1[-1].get("hash_actual")
            ultimo_lamport_esperado = int(filas_v1[-1].get("reloj_lamport") or 0)
            try:
                with connection.cursor() as cur_cabeza:
                    cur_cabeza.execute(
                        "SELECT ultimo_hash, ultimo_lamport FROM sga_principal.estado_cadena_auditoria "
                        "WHERE id_estado = 1"
                    )
                    fila_cabeza = cur_cabeza.fetchone()
                    if fila_cabeza is None:
                        print("[ERROR] No existe fila singleton en "
                              "sga_principal.estado_cadena_auditoria (id_estado=1). "
                              "La cabeza de cadena no puede verificarse.")
                        return 2
                    cabeza_hash = fila_cabeza[0]
                    cabeza_lamport = int(fila_cabeza[1] or 0)
                    if cabeza_hash != ultimo_hash_esperado or cabeza_lamport != ultimo_lamport_esperado:
                        print(f"[ERROR] Divergencia de cabeza detectada. La cabeza "
                              f"almacenada es hash={cabeza_hash[:16]!r}..., lamport={cabeza_lamport} pero el "
                              f"ultimo eslabon v1 tiene hash={ultimo_hash_esperado[:16]!r}..., lamport={ultimo_lamport_esperado}. "
                              "Alguien altero eslabones o ajusto la cabeza fraudulentamente.")
                        return 2
            except Exception as exc_cabeza:
                print(f"[ERROR] No se pudo consultar la cabeza de cadena en "
                      f"estado_cadena_auditoria: {exc_cabeza}")
                return 2

        for fila in filas_v1:
            discrepancias = cotejar_columnas_visibles(fila)
            if discrepancias:
                print(f"[ERROR] Eslabon id={fila['id_auditoria']}: columnas alteradas fuera del hash: {discrepancias}")
                return 2

        resultado = verificar_cadena_global(filas_v1)
        print(f"[INFO] Eslabones leidos y comprobados: {resultado.registros_verificados}")
        if resultado.valido:
            print(f"[OK] Verificacion completada: {len(filas_v1)} eslabon(es) validado(s), "
                  f"0 fila(s) fuera del alcance.")
            print(f"=== Resultado: Cadena global integra sobre {len(filas_v1)} eslabones (sin filas fuera del alcance) ===")
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
