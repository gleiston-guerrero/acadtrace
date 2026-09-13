import json
from datetime import datetime, timezone
from types import SimpleNamespace

from docentes.auditoria.clocks import incrementar_lamport
from docentes.auditoria.hashing import (
    GENESIS_HASH,
    calcular_hash,
    contenido_evento,
    json_canonico,
)
from docentes.auditoria.payloads import payload_instancia
from docentes.auditoria.verifier import (
    verificar_cadena as verificar_cadena_productiva,
    verificar_estado_academico,
)
from experimentos.verificador_cadena import verificar_cadena as verificar_cadena_experimental


def construir_cadena_valida():
    eventos = []
    anterior = GENESIS_HASH
    lamport = 0
    for indice, nota in enumerate(("9.25", "8.50"), start=1):
        lamport = incrementar_lamport(lamport)
        timestamp = datetime(2026, 9, indice, tzinfo=timezone.utc)
        payload = {"id_calificacion": 100 + indice, "nota": nota}
        contenido = contenido_evento(
            tipo_evento="CALIFICACION_ACTUALIZADA", entidad="Calificacion",
            entidad_id=100 + indice, operacion="ACTUALIZAR", actor_id=7,
            timestamp=timestamp, payload=payload, modo="m2",
            reloj_lamport=lamport, reloj_vectorial=None,
            estado_reconciliacion="NO_APLICA",
        )
        actual = calcular_hash(anterior, contenido)
        eventos.append({
            "id_evento": indice, "hash_anterior": anterior,
            "hash_actual": actual, "payload_canonico": json_canonico(payload),
            **{clave: valor for clave, valor in contenido.items() if clave != "payload"},
        })
        anterior = actual
    return eventos


def test_cadena_generada_con_codigo_productivo_es_aceptada():
    resultado = verificar_cadena_productiva(construir_cadena_valida())
    assert resultado.valido is True
    assert resultado.registros_verificados == 2


def test_payload_manipulado_es_rechazado():
    eventos = construir_cadena_valida()
    payload = json.loads(eventos[1]["payload_canonico"])
    payload["nota"] = "10.00"
    eventos[1]["payload_canonico"] = json_canonico(payload)
    resultado = verificar_cadena_productiva(eventos)
    assert resultado.valido is False
    assert resultado.tipo_inconsistencia == "HASH_ACTUAL_INVALIDO"


def test_hash_anterior_incorrecto_es_rechazado():
    eventos = construir_cadena_valida()
    eventos[1]["hash_anterior"] = GENESIS_HASH
    resultado = verificar_cadena_productiva(eventos)
    assert resultado.valido is False
    assert resultado.tipo_inconsistencia == "HASH_ANTERIOR_INVALIDO"


def test_reloj_lamport_no_monotonico_es_rechazado():
    eventos = construir_cadena_valida()
    eventos[1]["reloj_lamport"] = eventos[0]["reloj_lamport"]
    resultado = verificar_cadena_productiva(eventos)
    assert resultado.valido is False
    assert resultado.tipo_inconsistencia == "LAMPORT_NO_MONOTONICO"


def test_modificacion_directa_del_estado_academico_es_detectada():
    nota = SimpleNamespace(id_matricula=20, nota="8.50")
    evidencia = [{
        "id_evento": 9,
        "payload_canonico": json_canonico(payload_instancia(nota)),
    }]
    assert verificar_estado_academico(nota, evidencia).valido is True
    nota.nota = "2.00"
    resultado = verificar_estado_academico(nota, evidencia)
    assert resultado.valido is False
    assert resultado.tipo_inconsistencia == "ESTADO_ACADEMICO_DIVERGENTE"


def test_adaptador_experimental_coincide_con_verificador_productivo():
    eventos = construir_cadena_valida()
    assert verificar_cadena_experimental(eventos) == verificar_cadena_productiva(eventos)
