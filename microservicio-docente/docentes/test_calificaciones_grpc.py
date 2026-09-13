from contextlib import nullcontext
from decimal import Decimal
from types import SimpleNamespace
from unittest.mock import patch

import grpc
import pytest

from docentes.grpc_services import docente_pb2
from docentes.grpc_services.server import DocenteServiceServicer


class Abortado(Exception):
    pass


class Contexto:
    def invocation_metadata(self):
        return (("internal_token", "test-internal-token"), ("docente_id", "7"))

    def abort(self, code, detail):
        raise Abortado(code, detail)


@pytest.mark.parametrize(("creada", "mensaje"), [(True, "guardada"), (False, "actualizada")])
@patch("docentes.grpc_services.server.registrar_calificacion_exitosa")
@patch("docentes.grpc_services.server.auditar_evento")
@patch("docentes.grpc_services.server._usuario_de_persona", return_value=17)
@patch("docentes.grpc_services.server.validate_teacher_assignment", return_value={"is_valid": True})
@patch("docentes.grpc_services.server.transaction.atomic", return_value=nullcontext())
@patch("docentes.grpc_services.server.Calificacion.objects.update_or_create")
@patch("docentes.grpc_services.server.Actividad.objects.get")
def test_registro_valido_y_upsert_de_calificacion(
    actividad_get, upsert, _atomic, _validate, _usuario, auditar, metrica, creada, mensaje
):
    actividad = SimpleNamespace(id_asignacion=4, nota_maxima=Decimal("10.00"))
    nota = SimpleNamespace(id_calificacion=31, registrado_por=17)
    actividad_get.return_value = actividad
    upsert.return_value = (nota, creada)
    request = docente_pb2.RegistrarCalificacionRequest(id_matricula=21, id_actividad=3, nota=9.5)

    response = DocenteServiceServicer().RegistrarCalificacion(request, Contexto())

    assert response.exitoso is True and mensaje in response.mensaje.lower()
    assert upsert.call_args.kwargs["defaults"]["nota"] == Decimal("9.5")
    assert auditar.call_args.kwargs["operacion"] == ("CREAR" if creada else "ACTUALIZAR")
    metrica.assert_called_once_with()


@patch("docentes.grpc_services.server.validate_teacher_assignment", return_value={"is_valid": True})
@patch("docentes.grpc_services.server.Actividad.objects.get")
def test_registro_rechaza_nota_superior_al_maximo(actividad_get, _validate):
    actividad_get.return_value = SimpleNamespace(id_asignacion=4, nota_maxima=Decimal("10.00"))
    request = docente_pb2.RegistrarCalificacionRequest(id_matricula=21, id_actividad=3, nota=11)
    with pytest.raises(Abortado) as error:
        DocenteServiceServicer().RegistrarCalificacion(request, Contexto())
    assert error.value.args[0] == grpc.StatusCode.INVALID_ARGUMENT
