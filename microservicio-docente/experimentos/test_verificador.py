from pathlib import Path
from types import SimpleNamespace
import importlib.util
import json


REPO_ROOT = Path(__file__).resolve().parents[2]
VERIFICADOR_PATH = (
    REPO_ROOT
    / "experimentos"
    / "verificador_cadena.py"
)

spec = importlib.util.spec_from_file_location(
    "verificador_experimental",
    VERIFICADOR_PATH,
)

verificador = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verificador)


GENESIS_HASH = verificador.GENESIS_HASH
calcular_hash_canonico = verificador.calcular_hash_canonico
contenido_evento = verificador.contenido_evento
json_canonico = verificador.json_canonico
verificar_cadena_global = verificador.verificar_cadena_global
verificar_estado_academico = verificador.verificar_estado_academico


def construir_evento(
    *,
    id_evento,
    hash_anterior,
    reloj_lamport,
    estudiante,
    nota,
):
    payload = {
        "est_id": estudiante,
        "nota_final": nota,
    }

    timestamp = (
        f"2026-09-12T10:00:0{id_evento}"
    )

    contenido = contenido_evento(
        tipo_evento="CALIFICACION_ACTUALIZADA",
        entidad="Calificacion",
        entidad_id=estudiante,
        operacion="ACTUALIZAR",
        actor_id=14,
        timestamp=timestamp,
        payload=payload,
        modo="m2",
        reloj_lamport=reloj_lamport,
        reloj_vectorial=None,
        estado_reconciliacion="NO_APLICA",
    )

    contenido_canonico = json_canonico(contenido)

    hash_actual = calcular_hash_canonico(
        hash_anterior,
        contenido_canonico,
    )

    return {
        # Cadena institucional v1
        "id_auditoria": id_evento,
        "version_canonica": "v1",
        "contenido_canonico": contenido_canonico,
        "hash_anterior": hash_anterior,
        "hash_actual": hash_actual,
        "reloj_lamport": reloj_lamport,

        # Compatibilidad con el verificador de estado académico
        "id_evento": id_evento,
        "tipo_evento": "CALIFICACION_ACTUALIZADA",
        "entidad": "Calificacion",
        "entidad_id": str(estudiante),
        "operacion": "ACTUALIZAR",
        "actor_id": 14,
        "timestamp": timestamp,
        "payload_canonico": json_canonico(payload),
        "modo": "m2",
        "reloj_vectorial": None,
        "estado_reconciliacion": "NO_APLICA",
    }


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


def test_verificador_acepta_cadena_global_v1_valida():
    cadena = construir_cadena_valida()

    resultado = verificar_cadena_global(cadena)

    assert resultado.valido is True
    assert resultado.registros_verificados == 2


def test_verificador_rechaza_contenido_canonico_manipulado():
    cadena = construir_cadena_valida()

    contenido = json.loads(
        cadena[1]["contenido_canonico"]
    )
    contenido["payload"]["nota_final"] = "10.00"

    cadena[1]["contenido_canonico"] = json_canonico(
        contenido
    )

    resultado = verificar_cadena_global(cadena)

    assert resultado.valido is False
    assert resultado.primer_eslabon_roto == 2
    assert (
        resultado.tipo_inconsistencia
        == "HASH_ACTUAL_INVALIDO"
    )


def test_verificador_rechaza_hash_anterior_roto():
    cadena = construir_cadena_valida()

    cadena[1]["hash_anterior"] = GENESIS_HASH

    resultado = verificar_cadena_global(cadena)

    assert resultado.valido is False
    assert resultado.primer_eslabon_roto == 2
    assert (
        resultado.tipo_inconsistencia
        == "HASH_ANTERIOR_INVALIDO"
    )


def test_verificador_rechaza_lamport_no_monotonico():
    cadena = construir_cadena_valida()

    cadena[1]["reloj_lamport"] = 1

    resultado = verificar_cadena_global(cadena)

    assert resultado.valido is False
    assert resultado.primer_eslabon_roto == 2
    assert (
        resultado.tipo_inconsistencia
        == "LAMPORT_NO_MONOTONICO"
    )


def test_detecta_edicion_directa_del_estado_academico():
    cadena = construir_cadena_valida()
    evidencia = cadena[0]

    estado_alterado = SimpleNamespace(
        est_id=7,
        nota_final="10.00",
    )

    resultado = verificar_estado_academico(
        estado_alterado,
        [evidencia],
    )

    assert resultado.valido is False


def test_adaptador_es_exactamente_produccion():
    from docentes.auditoria import hashing as prod_hashing
    from docentes.auditoria import verifier as prod_verifier

    assert (
        verificador.verificar_cadena_global
        is prod_verifier.verificar_cadena_global
    )

    assert (
        verificador.calcular_hash_canonico
        is prod_hashing.calcular_hash_canonico
    )

    assert (
        verificador.contenido_evento
        is prod_hashing.contenido_evento
    )

    assert (
        verificador.json_canonico
        is prod_hashing.json_canonico
    )