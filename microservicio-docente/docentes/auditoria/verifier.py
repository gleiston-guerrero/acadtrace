import json
from dataclasses import dataclass

from .hashing import GENESIS_HASH, calcular_hash, contenido_evento
from .hashing import json_canonico
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
