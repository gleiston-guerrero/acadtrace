import json
import uuid
from dataclasses import dataclass

from django.db import connection

from .hashing import GENESIS_HASH, json_canonico


@dataclass
class EstadoCadenaGlobal:
    ultimo_hash: str
    ultimo_lamport: int
    reloj_vectorial: dict


def _parsear_vector(valor):
    if valor is None:
        return {}

    if isinstance(valor, dict):
        return dict(valor)

    if isinstance(valor, str):
        if not valor.strip():
            return {}
        parsed = json.loads(valor)
        if not isinstance(parsed, dict):
            raise ValueError("vector_reloj global no es un objeto JSON")
        return {
            str(k): int(v)
            for k, v in parsed.items()
        }

    raise ValueError(
        f"Tipo inesperado para vector_reloj global: {type(valor).__name__}"
    )


def bloquear_estado_global():
    """
    Lee y bloquea la unica cabeza institucional.

    El SELECT FOR UPDATE se mantiene hasta finalizar transaction.atomic()
    en la estrategia que invoca esta funcion.
    """
    with connection.cursor() as cursor:
        cursor.execute(
            """
            SELECT
                ultimo_hash,
                ultimo_lamport,
                vector_reloj
            FROM sga_principal.estado_cadena_auditoria
            WHERE id_estado = 1
            FOR UPDATE
            """
        )

        fila = cursor.fetchone()

    if fila is None:
        raise RuntimeError(
            "No existe sga_principal.estado_cadena_auditoria id_estado=1. "
            "Debe aplicarse la migracion V16 de sga-principal."
        )

    return EstadoCadenaGlobal(
        ultimo_hash=fila[0] or GENESIS_HASH,
        ultimo_lamport=int(fila[1] or 0),
        reloj_vectorial=_parsear_vector(fila[2]),
    )


def _registro_id_numerico(valor):
    if valor is None:
        return None

    try:
        return int(valor)
    except (TypeError, ValueError):
        return None


def insertar_evento_global(
    *,
    evento,
    instante,
    hash_anterior,
    hash_actual,
    reloj_lamport,
    reloj_vectorial,
    contenido_canonico,
):
    """
    Persiste el evento Docente en la bitacora institucional.

    La columna accion usa AUDITAR como accion tecnica institucional.
    La operacion de dominio real permanece dentro del contenido canonico v1.
    """
    trace_id = uuid.uuid4()

    actor = evento.get("actor_id")
    username = None if actor is None else str(actor)[:50]

    entidad = str(evento.get("entidad") or "")[:50]
    tipo_evento = str(evento.get("tipo_evento") or "")

    vector_json = (
        json_canonico(reloj_vectorial)
        if reloj_vectorial is not None
        else None
    )

    with connection.cursor() as cursor:
        cursor.execute(
            """
            INSERT INTO sga_principal.auditoria
                (
                    schema_origen,
                    username,
                    accion,
                    tabla_afectada,
                    registro_id,
                    descripcion,
                    trace_id,
                    resultado,
                    hash_anterior,
                    hash_actual,
                    reloj_lamport,
                    vector_reloj,
                    contenido_canonico,
                    version_canonica,
                    fecha
                )
            VALUES
                (
                    'DOCENTE',
                    %s,
                    'AUDITAR'::sga_principal.accion_auditoria_t,
                    %s,
                    %s,
                    %s,
                    %s,
                    'EXITO',
                    %s,
                    %s,
                    %s,
                    %s,
                    %s,
                    'v1',
                    %s
                )
            RETURNING id_auditoria
            """,
            [
                username,
                entidad,
                _registro_id_numerico(evento.get("entidad_id")),
                tipo_evento,
                str(trace_id),
                hash_anterior,
                hash_actual,
                reloj_lamport,
                vector_json,
                contenido_canonico,
                instante,
            ],
        )

        fila = cursor.fetchone()

    return fila[0] if fila else None


def actualizar_estado_global(
    *,
    hash_actual,
    reloj_lamport,
    reloj_vectorial,
):
    vector_json = (
        json_canonico(reloj_vectorial)
        if reloj_vectorial is not None
        else None
    )

    with connection.cursor() as cursor:
        cursor.execute(
            """
            UPDATE sga_principal.estado_cadena_auditoria
            SET
                ultimo_hash = %s,
                ultimo_lamport = %s,
                vector_reloj =
                    CASE
                        WHEN %s IS NULL
                            THEN vector_reloj
                        ELSE %s
                    END
            WHERE id_estado = 1
            """,
            [
                hash_actual,
                reloj_lamport,
                vector_json,
                vector_json,
            ],
        )

        if cursor.rowcount != 1:
            raise RuntimeError(
                "No se pudo actualizar la cabeza global de auditoria"
            )
