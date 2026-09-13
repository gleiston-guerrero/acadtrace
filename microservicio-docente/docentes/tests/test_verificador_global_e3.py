from copy import deepcopy

from docentes.auditoria.hashing import (
    calcular_hash,
    calcular_hash_canonico,
    contenido_evento,
    json_canonico,
)
from docentes.auditoria.verifier import verificar_cadena_global


ANCLA_HISTORICA = "a" * 64


def _evento_global(
    *,
    id_auditoria,
    schema_origen,
    hash_anterior,
    lamport,
):
    contenido = contenido_evento(
        tipo_evento="AUDITORIA",
        entidad="calificacion",
        entidad_id=str(id_auditoria),
        operacion="EDITAR",
        actor_id="123",
        timestamp="2026-09-13T08:30:00Z",
        payload={
            "descripcion": "Evento E3",
            "resultado": "EXITO",
            "schema_origen": schema_origen,
            "trace_id": (
                f"11111111-1111-1111-1111-"
                f"{id_auditoria:012d}"
            ),
        },
        modo="m3",
        reloj_lamport=lamport,
        reloj_vectorial={
            "docente": 1 if schema_origen == "DOCENTE" else 0,
            "principal": 1 if schema_origen == "PRINCIPAL" else 0,
            "secretaria": 1 if schema_origen == "SECRETARIA" else 0,
        },
        estado_reconciliacion="APLICADO",
    )

    canonico = json_canonico(contenido)

    actual = calcular_hash(
        hash_anterior,
        contenido,
    )

    return {
        "id_auditoria": id_auditoria,
        "hash_anterior": hash_anterior,
        "hash_actual": actual,
        "reloj_lamport": lamport,
        "contenido_canonico": canonico,
        "version_canonica": "v1",
    }


def _cadena_tres_servicios():
    principal = _evento_global(
        id_auditoria=1,
        schema_origen="PRINCIPAL",
        hash_anterior=ANCLA_HISTORICA,
        lamport=41,
    )

    secretaria = _evento_global(
        id_auditoria=2,
        schema_origen="SECRETARIA",
        hash_anterior=principal["hash_actual"],
        lamport=42,
    )

    docente = _evento_global(
        id_auditoria=3,
        schema_origen="DOCENTE",
        hash_anterior=secretaria["hash_actual"],
        lamport=43,
    )

    return [
        principal,
        secretaria,
        docente,
    ]


def test_un_mismo_verificador_valida_principal_secretaria_y_docente():
    eventos = _cadena_tres_servicios()

    resultado = verificar_cadena_global(
        eventos,
        hash_cabeza=eventos[-1]["hash_actual"],
        lamport_cabeza=43,
    )

    assert resultado.valido is True
    assert resultado.registros_verificados == 3
    assert resultado.primer_eslabon_roto is None


def test_verificador_global_detecta_manipulacion_canonica():
    eventos = deepcopy(
        _cadena_tres_servicios()
    )

    eventos[1]["contenido_canonico"] = (
        eventos[1]["contenido_canonico"]
        .replace(
            '"SECRETARIA"',
            '"ALTERADO"',
        )
    )

    resultado = verificar_cadena_global(
        eventos,
        hash_cabeza=eventos[-1]["hash_actual"],
        lamport_cabeza=43,
    )

    assert resultado.valido is False
    assert resultado.primer_eslabon_roto == 2
    assert resultado.tipo_inconsistencia == "HASH_ACTUAL_INVALIDO"


def test_hash_desde_objeto_y_desde_canonico_es_identico():
    evento = _cadena_tres_servicios()[0]

    calculado = calcular_hash_canonico(
        evento["hash_anterior"],
        evento["contenido_canonico"],
    )

    assert calculado == evento["hash_actual"]
