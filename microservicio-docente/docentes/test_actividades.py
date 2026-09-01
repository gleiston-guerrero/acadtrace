from decimal import Decimal
from contextlib import nullcontext
from types import SimpleNamespace
from unittest.mock import MagicMock, patch

import grpc
import pytest
from rest_framework import serializers

from docentes.grpc_services.actividades_service import ActividadServiceServicer
from docentes.grpc_services import actividades_pb2
from docentes.serializers import ActividadSerializer


class DummyContext:
    def __init__(self, metadata=()):
        self.metadata, self.code, self.details = metadata, None, None

    def invocation_metadata(self):
        return self.metadata

    def abort(self, code, details):
        self.code, self.details = code, details
        raise grpc.RpcError(details)


@pytest.mark.parametrize(("sumativa", "actual", "nueva"), [(False, 60, 10), (True, 20, 10)])
@patch("docentes.grpc_services.actividades_service.Actividad.objects.filter")
def test_ponderacion_grpc_acepta_limite(mock_filter, sumativa, actual, nueva):
    mock_filter.return_value.aggregate.return_value = {"total": Decimal(actual)}
    ActividadServiceServicer()._validar_ponderacion(1, 2, sumativa, Decimal(nueva))
    assert mock_filter.call_count == 1
    assert mock_filter.call_args.kwargs["es_sumativa"] is sumativa
    assert mock_filter.return_value.aggregate.call_count == 1


@pytest.mark.parametrize(("sumativa", "actual", "nueva", "limite"), [(False, 65, 6, 70), (True, 25, 6, 30)])
@patch("docentes.grpc_services.actividades_service.Actividad.objects.filter")
def test_ponderacion_grpc_rechaza_exceso(mock_filter, sumativa, actual, nueva, limite):
    mock_filter.return_value.aggregate.return_value = {"total": Decimal(actual)}
    with pytest.raises(ValueError, match=str(limite)) as exc:
        ActividadServiceServicer()._validar_ponderacion(1, 2, sumativa, Decimal(nueva))
    assert str(limite) in str(exc.value)
    assert mock_filter.call_count == 1
    assert mock_filter.return_value.aggregate.call_count == 1


@patch("docentes.grpc_services.actividades_service.Actividad.objects.filter")
def test_ponderacion_edicion_excluye_actividad_actual(mock_filter):
    mock_filter.return_value.exclude.return_value.aggregate.return_value = {"total": Decimal("60")}
    ActividadServiceServicer()._validar_ponderacion(1, 2, False, Decimal("10"), excluir_id=99)
    assert mock_filter.call_count == 1
    mock_filter.return_value.exclude.assert_called_once_with(id_actividad=99)
    assert mock_filter.return_value.exclude.call_count == 1
    assert mock_filter.return_value.exclude.return_value.aggregate.call_count == 1


@patch("docentes.grpc_services.actividades_service.Actividad.objects.filter")
def test_ponderacion_negativa_grpc(mock_filter):
    with pytest.raises(ValueError, match="negativa") as exc:
        ActividadServiceServicer()._validar_ponderacion(1, 2, False, Decimal("-1"))
    assert isinstance(exc.value, ValueError)
    assert "negativa" in str(exc.value)
    assert not mock_filter.called


@pytest.mark.parametrize(("metadata", "validation", "code"), [
    ((("docente_id", "10"), ("internal_token", "malo")), {"is_valid": True}, grpc.StatusCode.UNAUTHENTICATED),
    ((("internal_token", "***REMOVED***"),), {"is_valid": True}, grpc.StatusCode.UNAUTHENTICATED),
    ((("docente_id", "10"), ("internal_token", "***REMOVED***")), {"is_valid": False}, grpc.StatusCode.PERMISSION_DENIED),
])
@patch("docentes.grpc_services.actividades_service.validate_teacher_assignment")
def test_autorizacion_actividad_rechaza_acceso(mock_validate, metadata, validation, code):
    mock_validate.return_value = validation
    context = DummyContext(metadata)
    with pytest.raises(grpc.RpcError) as exc:
        ActividadServiceServicer()._validate_auth(context, 50)
    assert context.code == code
    assert isinstance(exc.value, grpc.RpcError)
    assert context.details in str(exc.value)


@patch("docentes.grpc_services.actividades_service.validate_teacher_assignment", return_value={"is_valid": True})
def test_autorizacion_actividad_valida(mock_validate):
    context = DummyContext((("docente_id", "10"), ("internal_token", "***REMOVED***")))
    assert ActividadServiceServicer()._validate_auth(context, 50) is True
    assert context.code is None
    assert mock_validate.call_args.args == (10, 50)


@patch.object(ActividadServiceServicer, "_validar_ponderacion")
@patch.object(ActividadServiceServicer, "_validate_auth")
@patch("docentes.grpc_services.actividades_service.Actividad.objects.create")
@patch("docentes.grpc_services.actividades_service.PeriodoEvaluacion.objects.get")
@patch("docentes.grpc_services.actividades_service.transaction.atomic", return_value=nullcontext())
def test_crear_actividad_retorna_dto(mock_atomic, mock_periodo, mock_create, mock_auth, mock_ponderacion):
    periodo = SimpleNamespace()
    mock_periodo.return_value = periodo
    mock_create.return_value = SimpleNamespace(id_actividad=9, id_asignacion=50, id_periodo_id=3,
        tipo="TAREA", nombre="Ensayo", descripcion=None, fecha_entrega="2026-09-01",
        ponderacion=Decimal("10"), nota_maxima=Decimal("10"), es_sumativa=False)
    request = actividades_pb2.CrearActividadRequest(id_asignacion=50, id_periodo=3, tipo="TAREA",
        nombre="Ensayo", fecha_entrega="2026-09-01", ponderacion=10, nota_maxima=10, es_sumativa=False)
    response = ActividadServiceServicer().CrearActividad(request, DummyContext())
    assert response.exitoso and response.actividad.id_actividad == 9
    assert response.mensaje == "Actividad creada exitosamente"
    mock_create.assert_called_once()
    assert mock_ponderacion.call_args.args[:3] == (50, 3, False)
    assert mock_atomic.call_count == 1


@patch.object(ActividadServiceServicer, "_validate_auth")
@patch("docentes.grpc_services.actividades_service.Actividad.objects.filter")
def test_listar_actividades_filtra_periodo(mock_filter, mock_auth):
    actividad = SimpleNamespace(id_actividad=9, id_asignacion=50, id_periodo_id=3, tipo="TAREA",
        nombre="Ensayo", descripcion="Texto", fecha_entrega="2026-09-01",
        ponderacion=Decimal("10"), nota_maxima=Decimal("10"), es_sumativa=False)
    query = MagicMock(); query.filter.return_value = [actividad]; mock_filter.return_value = query
    request = actividades_pb2.ListarActividadesRequest(id_asignacion=50, id_periodo=3)
    response = ActividadServiceServicer().ListarActividades(request, DummyContext())
    assert response.exitoso and len(response.actividades) == 1
    assert response.mensaje == "1 actividades encontradas"
    assert mock_auth.call_args.args[1] == 50


@patch("docentes.serializers.Actividad.objects.filter")
def test_serializer_edicion_no_cuenta_la_actividad(mock_filter):
    instance = SimpleNamespace(pk=9, id_asignacion=1, id_periodo=SimpleNamespace(pk=2), es_sumativa=False, ponderacion=Decimal("10"))
    mock_filter.return_value.exclude.return_value.aggregate.return_value = {"total": Decimal("60")}
    attrs = {"ponderacion": Decimal("10")}
    assert ActividadSerializer(instance=instance).validate(attrs) == attrs
    assert mock_filter.call_count == 1
    mock_filter.return_value.exclude.assert_called_once_with(pk=9)
    assert mock_filter.return_value.exclude.return_value.aggregate.call_count == 1
