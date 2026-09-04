import json
from contextlib import nullcontext
from datetime import datetime, timezone
from types import SimpleNamespace
from unittest.mock import patch

import pytest
from django.core.exceptions import ImproperlyConfigured

from docentes.auditoria.clocks import (
    RelacionVectorial,
    combinar_vectores,
    comparar_vectores,
    incrementar_vector,
    reconciliar_vectores,
)
from docentes.auditoria.config import obtener_modo_auditoria
from docentes.auditoria.hashing import GENESIS_HASH, calcular_hash, contenido_evento, json_canonico
from docentes.auditoria.service import auditar_evento
from docentes.auditoria.verifier import verificar_cadena, verificar_estado_academico
from docentes.auditoria.payloads import payload_instancia
from docentes.models import EstadoCadenaAuditoria, EventoAuditoria
from experimentos.generador_sintetico import SEED, generar_dataset
from experimentos.manipulaciones import aplicar_manipulacion


@pytest.mark.parametrize("modo", ["m0", "m1", "m2", "m3"])
def test_selector_acepta_cada_modo(monkeypatch, modo):
    monkeypatch.setenv("AUDIT", modo)
    assert obtener_modo_auditoria() == modo
    assert modo in {"m0", "m1", "m2", "m3"}
    assert len(modo) == 2


def test_selector_usa_m0_y_rechaza_valor_invalido(monkeypatch):
    monkeypatch.delenv("AUDIT", raising=False)
    assert obtener_modo_auditoria() == "m0"
    monkeypatch.setenv("AUDIT", "desconocido")
    with pytest.raises(ImproperlyConfigured) as exc:
        obtener_modo_auditoria()
    assert "AUDIT debe ser uno de" in str(exc.value)
    assert "desconocido" in str(exc.value)


@patch("docentes.auditoria.strategies.EventoAuditoria.objects.create")
@patch("docentes.auditoria.strategies.EstadoCadenaAuditoria.objects")
def test_m0_no_inserta_ni_calcula_cadena(mock_estado, mock_create, monkeypatch):
    monkeypatch.setenv("AUDIT", "m0")
    resultado = auditar_evento(
        tipo_evento="PRUEBA", entidad="Calificacion", entidad_id=1,
        operacion="CREAR", actor_id=7, payload={"nota": "9.00"},
    )
    assert resultado is None
    assert mock_create.call_count == 0
    assert mock_estado.mock_calls == []


@patch("docentes.auditoria.strategies.EventoAuditoria.objects.create")
def test_m1_crea_bitacora_plana_sin_hash(mock_create, monkeypatch):
    monkeypatch.setenv("AUDIT", "m1")
    mock_create.side_effect = lambda **kwargs: SimpleNamespace(**kwargs)
    evento = auditar_evento(
        tipo_evento="CALIFICACION_CREADA", entidad="Calificacion", entidad_id=15,
        operacion="CREAR", actor_id=7,
        payload={"nota": "9.00", "internal_token": "no-guardar"},
    )
    assert evento.operacion == "CREAR" and evento.entidad == "Calificacion"
    assert json.loads(evento.payload_canonico) == {"nota": "9.00"}
    assert "hash_anterior" not in mock_create.call_args.kwargs
    assert "hash_actual" not in mock_create.call_args.kwargs


@patch("docentes.auditoria.strategies.transaction.atomic", return_value=nullcontext())
@patch("docentes.auditoria.strategies.EventoAuditoria.objects.create")
@patch("docentes.auditoria.strategies.EstadoCadenaAuditoria.objects")
def test_m2_encadena_hash_y_lamport_monotonico(mock_manager, mock_create, _mock_atomic, monkeypatch):
    monkeypatch.setenv("AUDIT", "m2")
    estado = SimpleNamespace(ultimo_hash=None, ultimo_lamport=0, reloj_vectorial={}, save=lambda **_kwargs: None)
    mock_manager.select_for_update.return_value.get_or_create.return_value = (estado, False)
    mock_create.side_effect = lambda **kwargs: SimpleNamespace(**kwargs)
    primero = auditar_evento(
        tipo_evento="ASISTENCIA_REGISTRADA", entidad="Asistencia", entidad_id=1,
        operacion="CREAR", actor_id=2, payload={"estado": "PRESENTE"},
    )
    segundo = auditar_evento(
        tipo_evento="ASISTENCIA_ACTUALIZADA", entidad="Asistencia", entidad_id=1,
        operacion="ACTUALIZAR", actor_id=2, payload={"estado": "ATRASO"},
        lamport_recibido=0,
    )
    assert primero.hash_anterior == GENESIS_HASH and segundo.hash_anterior == primero.hash_actual
    assert (primero.reloj_lamport, segundo.reloj_lamport) == (1, 2)
    assert len(primero.hash_actual) == len(segundo.hash_actual) == 64
    assert mock_manager.select_for_update.call_count == 2


def test_vector_clock_incrementa_combina_y_compara():
    incrementado = incrementar_vector({"docente-1": 2}, "docente-1")
    combinado = combinar_vectores({"a": 2, "b": 0}, {"a": 1, "b": 1})
    assert incrementado == {"docente-1": 3}
    assert combinado == {"a": 2, "b": 1}
    assert comparar_vectores({"a": 2}, {"a": 3}) == RelacionVectorial.ANTES
    assert comparar_vectores({"a": 3}, {"a": 2}) == RelacionVectorial.DESPUES
    assert comparar_vectores({"a": 2}, {"a": 2}) == RelacionVectorial.IGUAL
    assert comparar_vectores({"a": 2, "b": 0}, {"a": 1, "b": 1}) == RelacionVectorial.CONCURRENTE


def test_reconciliacion_concurrente_preserva_ambas_versiones():
    resultado = reconciliar_vectores({"docente-1": 2}, {"docente-1": 1, "docente-2": 1})
    assert resultado["relacion"] == RelacionVectorial.CONCURRENTE
    assert resultado["estado"] == "CONFLICTO"
    assert resultado["politica"] == "PRESERVAR_AMBAS_Y_RESOLVER_MANUALMENTE"
    assert resultado["reloj_combinado"] == {"docente-1": 2, "docente-2": 1}


@patch("docentes.auditoria.strategies.transaction.atomic", return_value=nullcontext())
@patch("docentes.auditoria.strategies.EventoAuditoria.objects.create")
@patch("docentes.auditoria.strategies.EstadoCadenaAuditoria.objects")
def test_m3_marca_conflicto_y_avanza_vector(mock_manager, mock_create, _mock_atomic, monkeypatch):
    monkeypatch.setenv("AUDIT", "m3")
    estado = SimpleNamespace(ultimo_hash=None, ultimo_lamport=0, reloj_vectorial={}, save=lambda **_kwargs: None)
    mock_manager.select_for_update.return_value.get_or_create.return_value = (estado, False)
    mock_create.side_effect = lambda **kwargs: SimpleNamespace(**kwargs)
    primero = auditar_evento(
        tipo_evento="CALIFICACION_ACTUALIZADA", entidad="Calificacion", entidad_id=9,
        operacion="ACTUALIZAR", actor_id=1, payload={"nota": "8"},
        nodo="docente-1", reloj_vectorial_recibido={"remoto": 1},
    )
    segundo = auditar_evento(
        tipo_evento="CALIFICACION_ACTUALIZADA", entidad="Calificacion", entidad_id=9,
        operacion="ACTUALIZAR", actor_id=1, payload={"nota": "9"},
        nodo="docente-1", reloj_vectorial_recibido={"remoto": 2},
    )
    assert primero.reloj_vectorial == {"remoto": 1, "docente-1": 1}
    assert segundo.estado_reconciliacion == "CONFLICTO"
    assert segundo.reloj_vectorial == {"remoto": 2, "docente-1": 2}
    assert mock_manager.select_for_update.call_count == 2


def _cadena_prueba(cantidad=3):
    anterior, eventos = GENESIS_HASH, []
    for indice in range(1, cantidad + 1):
        timestamp = datetime(2026, 1, indice, tzinfo=timezone.utc)
        payload = {"valor": indice}
        base = dict(
            tipo_evento="PRUEBA", entidad="Entidad", entidad_id=str(indice),
            operacion="ACTUALIZAR", actor_id=1, timestamp=timestamp, payload=payload,
            modo="m2", reloj_lamport=indice, reloj_vectorial=None,
            estado_reconciliacion="NO_APLICA",
        )
        actual = calcular_hash(anterior, contenido_evento(**base))
        eventos.append({
            "id_evento": indice, "hash_anterior": anterior, "hash_actual": actual,
            "payload_canonico": json_canonico(payload), **{k: v for k, v in base.items() if k != "payload"},
        })
        anterior = actual
    return eventos, anterior


def test_verificador_acepta_cadena_intacta():
    eventos, cabeza = _cadena_prueba()
    resultado = verificar_cadena(eventos, hash_cabeza=cabeza, lamport_cabeza=3)
    assert resultado.valido is True
    assert resultado.registros_verificados == 3
    assert resultado.primer_eslabon_roto is None


@pytest.mark.parametrize("tipo", ["T1", "T2", "T3", "T4", "T5"])
def test_verificador_detecta_todas_las_manipulaciones(tipo):
    eventos, cabeza = _cadena_prueba()
    manipulados = aplicar_manipulacion(eventos, tipo)
    resultado = verificar_cadena(manipulados, hash_cabeza=cabeza, lamport_cabeza=3)
    assert resultado.valido is False
    assert resultado.tipo_inconsistencia in {
        "HASH_ANTERIOR_INVALIDO", "HASH_ACTUAL_INVALIDO", "CABEZA_CADENA_INVALIDA",
        "CABEZA_LAMPORT_INVALIDA", "LAMPORT_NO_MONOTONICO",
    }
    assert resultado.registros_verificados < 3


def test_generador_es_determinista_y_respeta_propiedad_de_datos():
    primero = generar_dataset()
    segundo = generar_dataset(SEED)
    assert primero == segundo
    assert len(primero["estudiantes"]) == 344
    assert len(primero["docentes"]) == 14
    assert primero["seed"] == 701


def test_t1_detecta_cambio_directo_en_nota_contra_evidencia(monkeypatch):
    nota = SimpleNamespace(pk=31, id_matricula=20, nota="8.50")
    evidencia = [{
        "id_evento": 9,
        "payload_canonico": json_canonico(payload_instancia(nota)),
    }]
    assert verificar_estado_academico(nota, evidencia).valido is True

    # Representa el estado recargado tras un UPDATE SQL que elude la aplicación.
    nota.nota = "2.00"
    resultado = verificar_estado_academico(nota, evidencia)
    assert resultado.valido is False
    assert resultado.tipo_inconsistencia == "ESTADO_ACADEMICO_DIVERGENTE"
