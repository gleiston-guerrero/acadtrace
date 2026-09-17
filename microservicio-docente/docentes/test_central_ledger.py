from unittest.mock import patch

import pytest

from docentes.auditoria import central_ledger
from docentes.auditoria.hashing import GENESIS_HASH


def test_parsear_vector_accepts_supported_representations():
    original = {"docente": 2}

    assert central_ledger._parsear_vector(None) == {}
    assert central_ledger._parsear_vector(original) == original
    assert central_ledger._parsear_vector(original) is not original
    assert central_ledger._parsear_vector("") == {}
    assert central_ledger._parsear_vector('{"docente": "3"}') == {"docente": 3}


@pytest.mark.parametrize("value", ("[]", 7))
def test_parsear_vector_rejects_unsupported_representations(value):
    with pytest.raises(ValueError):
        central_ledger._parsear_vector(value)


def test_registro_id_numerico_handles_valid_and_invalid_values():
    assert central_ledger._registro_id_numerico(None) is None
    assert central_ledger._registro_id_numerico("17") == 17
    assert central_ledger._registro_id_numerico("no-numeric") is None


@patch("docentes.auditoria.central_ledger.connection.cursor")
def test_bloquear_estado_global_maps_database_row(cursor_factory):
    cursor = cursor_factory.return_value.__enter__.return_value
    cursor.fetchone.return_value = (None, None, '{"docente": 4}')

    state = central_ledger.bloquear_estado_global()

    assert state.ultimo_hash == GENESIS_HASH
    assert state.ultimo_lamport == 0
    assert state.reloj_vectorial == {"docente": 4}
    assert "FOR UPDATE" in cursor.execute.call_args.args[0]


@patch("docentes.auditoria.central_ledger.connection.cursor")
def test_bloquear_estado_global_requires_single_head(cursor_factory):
    cursor_factory.return_value.__enter__.return_value.fetchone.return_value = None

    with pytest.raises(RuntimeError, match="estado_cadena_auditoria"):
        central_ledger.bloquear_estado_global()


@patch("docentes.auditoria.central_ledger.uuid.uuid4", return_value="trace-id")
@patch("docentes.auditoria.central_ledger.connection.cursor")
def test_insertar_evento_global_uses_canonical_values(cursor_factory, _uuid):
    cursor = cursor_factory.return_value.__enter__.return_value
    cursor.fetchone.return_value = (91,)

    result = central_ledger.insertar_evento_global(
        evento={
            "actor_id": 17,
            "entidad": "Calificacion",
            "entidad_id": "23",
            "tipo_evento": "CALIFICACION_CREADA",
        },
        instante="2026-09-15T10:00:00Z",
        hash_anterior="a" * 64,
        hash_actual="b" * 64,
        reloj_lamport=8,
        reloj_vectorial={"docente": 8},
        contenido_canonico='{"nota":"9.00"}',
    )

    assert result == 91
    sql, params = cursor.execute.call_args.args
    assert "INSERT INTO sga_principal.auditoria" in sql
    assert params[0] == "17"
    assert params[1] == "Calificacion"
    assert params[2] == 23
    assert params[4] == "trace-id"
    assert params[8] == '{"docente":8}'


@patch("docentes.auditoria.central_ledger.connection.cursor")
def test_actualizar_estado_global_updates_single_head(cursor_factory):
    cursor = cursor_factory.return_value.__enter__.return_value
    cursor.rowcount = 1

    central_ledger.actualizar_estado_global(
        hash_actual="b" * 64,
        reloj_lamport=9,
        reloj_vectorial={"docente": 9},
    )

    sql, params = cursor.execute.call_args.args
    assert "UPDATE sga_principal.estado_cadena_auditoria" in sql
    assert params == ["b" * 64, 9, '{"docente":9}', '{"docente":9}']


@patch("docentes.auditoria.central_ledger.connection.cursor")
def test_actualizar_estado_global_rejects_missing_head(cursor_factory):
    cursor_factory.return_value.__enter__.return_value.rowcount = 0

    with pytest.raises(RuntimeError, match="cabeza global"):
        central_ledger.actualizar_estado_global(
            hash_actual="b" * 64,
            reloj_lamport=9,
            reloj_vectorial=None,
        )
