import json
from dataclasses import dataclass

from django.db import connection

from .hashing import (
    GENESIS_HASH,
    calcular_hash,
    calcular_hash_canonico,
    contenido_evento,
    json_canonico,
)
from .payloads import payload_instancia


@dataclass(frozen=True)
class ResultadoVerificacion:
    valido: bool
    registros_verificados: int
    primer_eslabon_roto: int | None = None
    tipo_inconsistencia: str | None = None


def _valor(evento, campo):
    return evento[campo] if isinstance(evento, dict) else getattr(evento, campo)


def verificar_cadena(eventos, *, hash_cabeza=None, lamport_cabeza=None):
    anterior = GENESIS_HASH
    lamport_anterior = 0
    verificados = 0
    for evento in eventos:
        identificador = int(_valor(evento, "id_evento"))
        if _valor(evento, "hash_anterior") != anterior:
            return ResultadoVerificacion(False, verificados, identificador, "HASH_ANTERIOR_INVALIDO")
        lamport = int(_valor(evento, "reloj_lamport"))
        if lamport <= lamport_anterior:
            return ResultadoVerificacion(False, verificados, identificador, "LAMPORT_NO_MONOTONICO")
        payload = json.loads(_valor(evento, "payload_canonico"))
        contenido = contenido_evento(
            tipo_evento=_valor(evento, "tipo_evento"), entidad=_valor(evento, "entidad"),
            entidad_id=_valor(evento, "entidad_id"), operacion=_valor(evento, "operacion"),
            actor_id=_valor(evento, "actor_id"), timestamp=_valor(evento, "timestamp"),
            payload=payload, modo=_valor(evento, "modo"), reloj_lamport=lamport,
            reloj_vectorial=_valor(evento, "reloj_vectorial"),
            estado_reconciliacion=_valor(evento, "estado_reconciliacion"),
        )
        calculado = calcular_hash(anterior, contenido)
        if _valor(evento, "hash_actual") != calculado:
            return ResultadoVerificacion(False, verificados, identificador, "HASH_ACTUAL_INVALIDO")
        anterior = calculado
        lamport_anterior = lamport
        verificados += 1
    if hash_cabeza is not None and anterior != hash_cabeza:
        return ResultadoVerificacion(False, verificados, None, "CABEZA_CADENA_INVALIDA")
    if lamport_cabeza is not None and lamport_anterior != int(lamport_cabeza):
        return ResultadoVerificacion(False, verificados, None, "CABEZA_LAMPORT_INVALIDA")
    return ResultadoVerificacion(True, verificados)



def _valor_global(evento, campo):
    if isinstance(evento, dict):
        return evento.get(campo)

    return getattr(evento, campo)


def _leer_cadena_global():
    """
    Lee exclusivamente los eslabones E3 versionados como v1.

    M0/M1 y filas historicas anteriores al contrato v1 no forman parte
    del segmento criptografico E3.
    """
    with connection.cursor() as cursor:
        cursor.execute(
            """
            SELECT
                id_auditoria,
                hash_anterior,
                hash_actual,
                reloj_lamport,
                contenido_canonico,
                version_canonica
            FROM sga_principal.auditoria
            WHERE version_canonica = 'v1'
              AND hash_anterior IS NOT NULL
              AND hash_actual IS NOT NULL
              AND reloj_lamport IS NOT NULL
              AND contenido_canonico IS NOT NULL
            ORDER BY reloj_lamport ASC, id_auditoria ASC
            """
        )

        columnas = [
            columna[0]
            for columna in cursor.description
        ]

        eventos = [
            dict(zip(columnas, fila))
            for fila in cursor.fetchall()
        ]

        cursor.execute(
            """
            SELECT
                ultimo_hash,
                ultimo_lamport
            FROM sga_principal.estado_cadena_auditoria
            WHERE id_estado = 1
            """
        )

        cabeza = cursor.fetchone()

    if cabeza is None:
        raise RuntimeError(
            "No existe la cabeza global "
            "sga_principal.estado_cadena_auditoria"
        )

    return eventos, cabeza[0], int(cabeza[1])


def verificar_cadena_global(
    eventos=None,
    *,
    hash_cabeza=None,
    lamport_cabeza=None,
):
    """
    Verificador unico E3 para Principal, Secretaria y Docente.

    No reconstruye mapas particulares de ningun microservicio.
    Verifica directamente el contenido_canonico v1 persistido por todos.

    Esto permite verificar con exactamente la misma formula una cadena:

        Principal -> Secretaria -> Docente -> Principal -> ...

    Para instalaciones actualizadas desde una cadena anterior a v1, el
    hash_anterior del primer evento v1 actua como ancla historica.
    """

    if eventos is None:
        (
            eventos,
            hash_bd,
            lamport_bd,
        ) = _leer_cadena_global()

        if hash_cabeza is None:
            hash_cabeza = hash_bd

        if lamport_cabeza is None:
            lamport_cabeza = lamport_bd

    eventos = list(eventos)

    if not eventos:
        if (
            hash_cabeza is not None
            and hash_cabeza != GENESIS_HASH
        ):
            return ResultadoVerificacion(
                False,
                0,
                None,
                "CABEZA_CADENA_INVALIDA",
            )

        if (
            lamport_cabeza is not None
            and int(lamport_cabeza) != 0
        ):
            return ResultadoVerificacion(
                False,
                0,
                None,
                "CABEZA_LAMPORT_INVALIDA",
            )

        return ResultadoVerificacion(True, 0)

    primero = eventos[0]

    anterior = _valor_global(
        primero,
        "hash_anterior",
    )

    if not anterior:
        return ResultadoVerificacion(
            False,
            0,
            int(_valor_global(primero, "id_auditoria")),
            "HASH_ANTERIOR_INVALIDO",
        )

    lamport_anterior = None
    verificados = 0

    for indice, evento in enumerate(eventos):
        identificador = int(
            _valor_global(evento, "id_auditoria")
        )

        version = _valor_global(
            evento,
            "version_canonica",
        )

        if version != "v1":
            return ResultadoVerificacion(
                False,
                verificados,
                identificador,
                "VERSION_CANONICA_INVALIDA",
            )

        hash_anterior = _valor_global(
            evento,
            "hash_anterior",
        )

        if indice > 0 and hash_anterior != anterior:
            return ResultadoVerificacion(
                False,
                verificados,
                identificador,
                "HASH_ANTERIOR_INVALIDO",
            )

        lamport = int(
            _valor_global(
                evento,
                "reloj_lamport",
            )
        )

        if (
            lamport_anterior is not None
            and lamport <= lamport_anterior
        ):
            return ResultadoVerificacion(
                False,
                verificados,
                identificador,
                "LAMPORT_NO_MONOTONICO",
            )

        contenido_canonico = _valor_global(
            evento,
            "contenido_canonico",
        )

        if not isinstance(contenido_canonico, str) or not contenido_canonico:
            return ResultadoVerificacion(
                False,
                verificados,
                identificador,
                "CONTENIDO_CANONICO_AUSENTE",
            )

        calculado = calcular_hash_canonico(
            hash_anterior,
            contenido_canonico,
        )

        hash_actual = _valor_global(
            evento,
            "hash_actual",
        )

        if hash_actual != calculado:
            return ResultadoVerificacion(
                False,
                verificados,
                identificador,
                "HASH_ACTUAL_INVALIDO",
            )

        anterior = hash_actual
        lamport_anterior = lamport
        verificados += 1

    if (
        hash_cabeza is not None
        and anterior != hash_cabeza
    ):
        return ResultadoVerificacion(
            False,
            verificados,
            None,
            "CABEZA_CADENA_INVALIDA",
        )

    if (
        lamport_cabeza is not None
        and lamport_anterior != int(lamport_cabeza)
    ):
        return ResultadoVerificacion(
            False,
            verificados,
            None,
            "CABEZA_LAMPORT_INVALIDA",
        )

    return ResultadoVerificacion(
        True,
        verificados,
    )


def verificar_estado_academico(instancia, eventos=None):
    """Compara el estado persistido con la última evidencia legítima auditada.

    Esto detecta T1 aunque la cadena criptográfica permanezca intacta, porque la
    escritura directa cambia la tabla académica pero no el payload auditado.
    """
    if eventos is None:
        from docentes.models import EventoAuditoria

        eventos = EventoAuditoria.objects.filter(
            entidad=instancia.__class__.__name__, entidad_id=str(instancia.pk)
        ).exclude(operacion="ELIMINAR").order_by("-id_evento")
    ultimo = next(iter(eventos), None)
    if ultimo is None:
        return ResultadoVerificacion(False, 0, None, "EVIDENCIA_AUDITORIA_AUSENTE")
    esperado = _valor(ultimo, "payload_canonico")
    actual = json_canonico(payload_instancia(instancia))
    if actual != esperado:
        return ResultadoVerificacion(False, 0, int(_valor(ultimo, "id_evento")), "ESTADO_ACADEMICO_DIVERGENTE")
    return ResultadoVerificacion(True, 1)
