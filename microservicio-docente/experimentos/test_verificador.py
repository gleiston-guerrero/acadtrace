from pathlib import Path
import importlib.util
import json


REPO_ROOT = Path(__file__).resolve().parents[2]
VERIFICADOR_PATH = REPO_ROOT / "experimentos" / "verificador_cadena.py"

spec = importlib.util.spec_from_file_location(
    "verificador_experimental",
    VERIFICADOR_PATH,
)
verificador = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verificador)

GENESIS_HASH = verificador.GENESIS_HASH
calcular_hash = verificador.calcular_hash
contenido_evento = verificador.contenido_evento
json_canonico = verificador.json_canonico
verificar_cadena = verificador.verificar_cadena


def construir_evento(
    *,
    id_evento,
    hash_anterior,
    reloj_lamport,
    estudiante,
    nota,
):
    payload = {
        "id_calificacion": id_evento,
        "estudiante": estudiante,
        "nota": nota,
    }

    timestamp = f"2026-09-12T10:00:0{id_evento}"

    contenido = contenido_evento(
        tipo_evento="CALIFICACION_ACTUALIZADA",
        entidad="Calificacion",
        entidad_id=id_evento,
        operacion="ACTUALIZAR",
        actor_id=14,
        timestamp=timestamp,
        payload=payload,
        modo="m2",
        reloj_lamport=reloj_lamport,
        reloj_vectorial=None,
        estado_reconciliacion="NO_APLICA",
    )

    hash_actual = calcular_hash(hash_anterior, contenido)

    evento = {
        "id_evento": id_evento,
        "tipo_evento": "CALIFICACION_ACTUALIZADA",
        "entidad": "Calificacion",
        "entidad_id": str(id_evento),
        "operacion": "ACTUALIZAR",
        "actor_id": 14,
        "timestamp": timestamp,
        "payload_canonico": json_canonico(payload),
        "modo": "m2",
        "hash_anterior": hash_anterior,
        "hash_actual": hash_actual,
        "reloj_lamport": reloj_lamport,
        "reloj_vectorial": None,
        "estado_reconciliacion": "NO_APLICA",
    }

    return evento


def construir_cadena_valida():
    primero = construir_evento(
        id_evento=1,
        hash_anterior=GENESIS_HASH,
        reloj_lamport=1,
        estudiante=7,
        nota="9.25",
    )

    segundo = construir_evento(
        id_evento=2,
        hash_anterior=primero["hash_actual"],
        reloj_lamport=2,
        estudiante=8,
        nota="8.50",
    )

    return [primero, segundo]


def test_verificador_acepta_cadena_generada_con_hash_productivo():
    cadena = construir_cadena_valida()

    resultado = verificar_cadena(cadena)

    assert resultado.valido is True
    assert resultado.registros_verificados == 2


def test_verificador_rechaza_payload_manipulado():
    cadena = construir_cadena_valida()

    payload_manipulado = json.loads(cadena[1]["payload_canonico"])
    payload_manipulado["nota"] = "10.00"
    cadena[1]["payload_canonico"] = json_canonico(payload_manipulado)

    resultado = verificar_cadena(cadena)

    assert resultado.valido is False
    assert resultado.primer_eslabon_roto == 2
    assert resultado.tipo_inconsistencia == "HASH_ACTUAL_INVALIDO"