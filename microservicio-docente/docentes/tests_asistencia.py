from contextlib import nullcontext
from types import SimpleNamespace
from unittest.mock import MagicMock, patch

import grpc
import pytest

from docentes.grpc_services import asistencia_pb2
from docentes.grpc_services.asistencia_service import AsistenciaServiceServicer
from micro_docente.middleware import asistencias_total


class DummyContext:
    def __init__(self, metadata=()):
        self.metadata, self.code, self.details = metadata, None, None

    def invocation_metadata(self):
        return self.metadata

    def abort(self, code, details):
        self.code, self.details = code, details
        raise grpc.RpcError(details)


def auth_context(token="***REMOVED***", docente="10"):
    return DummyContext((("docente_id", docente), ("internal_token", token)))


@patch("docentes.grpc_services.asistencia_service.validate_teacher_assignment", return_value={"is_valid": True})
def test_autenticacion_valida(mock_validate):
    context = auth_context()
    assert AsistenciaServiceServicer()._validate_auth(context, 50) == 10
    assert context.code is None
    assert mock_validate.call_args.args == (10, 50)


@pytest.mark.parametrize("context", [auth_context(token="malo"), DummyContext((("internal_token", "***REMOVED***"),))])
def test_autenticacion_rechaza_credenciales_invalidas(context):
    with pytest.raises(grpc.RpcError) as exc:
        AsistenciaServiceServicer()._validate_auth(context, 50)
    assert context.code == grpc.StatusCode.UNAUTHENTICATED
    assert isinstance(exc.value, grpc.RpcError)
    assert context.details in str(exc.value)


@patch("docentes.grpc_services.asistencia_service.validate_teacher_assignment", return_value={"is_valid": False})
def test_docente_sin_acceso(mock_validate):
    context = auth_context()
    with pytest.raises(grpc.RpcError) as exc:
        AsistenciaServiceServicer()._validate_auth(context, 50)
    assert context.code == grpc.StatusCode.PERMISSION_DENIED
    assert isinstance(exc.value, grpc.RpcError)
    assert mock_validate.call_args.args == (10, 50)


@patch("docentes.grpc_services.asistencia_service.transaction.atomic", return_value=nullcontext())
@patch("docentes.grpc_services.asistencia_service._usuario_de_persona", return_value=77)
@patch("docentes.grpc_services.asistencia_service._asegurar_asignacion")
@patch("docentes.grpc_services.asistencia_service.get_students_by_assignment")
@patch.object(AsistenciaServiceServicer, "_validate_auth", return_value=10)
@patch.object(AsistenciaServiceServicer, "_recalcular_resumen_bulk")
@patch("django.db.connection.cursor")
@patch("docentes.grpc_services.asistencia_service.Asistencia.objects")
@patch("docentes.grpc_services.asistencia_service.PeriodoEvaluacion.objects.get")
def test_registro_grupal_todos_estados_y_reemplazo(mock_periodo, mock_objects, mock_connection,
        mock_resumen, mock_auth, mock_students, mock_assignment, mock_user, mock_atomic):
    metric_before = asistencias_total._value.get()
    periodo = SimpleNamespace(id_periodo=3)
    mock_periodo.return_value = periodo
    mock_students.return_value = [{"id_matricula": value} for value in range(101, 105)]
    existentes = MagicMock()
    creadas = [SimpleNamespace(id_asistencia=i, id_matricula=100+i, id_asignacion=50,
        id_periodo_id=3, fecha="2026-07-15", estado=estado, justificacion=None)
        for i, estado in enumerate(("PRESENTE", "AUSENTE", "JUSTIFICADO", "ATRASO"), 1)]
    creadas_query = MagicMock()
    creadas_query.order_by.return_value = creadas
    mock_objects.filter.side_effect = [existentes, creadas_query]
    cursor = mock_connection.return_value.__enter__.return_value
    cursor.fetchone.side_effect = [(1,), (2,), (3,), (4,)]
    request = asistencia_pb2.RegistrarAsistenciaGrupalRequest(id_asignacion=50, id_periodo=3,
        fecha="2026-07-15", asistencias=[
            asistencia_pb2.AsistenciaItemRequest(id_matricula=101+i, estado=e)
            for i, e in enumerate(("PRESENTE", "AUSENTE", "JUSTIFICADO", "ATRASO"))])
    response = AsistenciaServiceServicer().RegistrarAsistenciaGrupal(request, auth_context())
    assert response.success and len(response.asistencias) == 4
    existentes.delete.assert_called_once()
    assert cursor.execute.call_count == 4
    mock_resumen.assert_called_once_with(50, periodo, [101, 102, 103, 104])
    assert asistencias_total._value.get() == metric_before + 4


@pytest.mark.parametrize(("students", "item"), [
    ([{"id_matricula": 1}], asistencia_pb2.AsistenciaItemRequest(id_matricula=2, estado="PRESENTE")),
    ([{"id_matricula": 1}], asistencia_pb2.AsistenciaItemRequest(id_matricula=1, estado="ATRASADO")),
])
@patch("docentes.grpc_services.asistencia_service._asegurar_asignacion")
@patch.object(AsistenciaServiceServicer, "_validate_auth", return_value=10)
@patch("docentes.grpc_services.asistencia_service.PeriodoEvaluacion.objects.get", return_value=SimpleNamespace(id_periodo=3))
def test_registro_valida_matricula_y_estado(mock_periodo, mock_auth, mock_assignment, students, item):
    context = auth_context()
    request = asistencia_pb2.RegistrarAsistenciaGrupalRequest(id_asignacion=50, id_periodo=3,
        fecha="2026-07-15", asistencias=[item])
    with patch("docentes.grpc_services.asistencia_service.get_students_by_assignment", return_value=students):
        with pytest.raises(grpc.RpcError) as exc:
            AsistenciaServiceServicer().RegistrarAsistenciaGrupal(request, context)
    assert context.code == grpc.StatusCode.INVALID_ARGUMENT
    assert isinstance(exc.value, grpc.RpcError)
    assert context.details in str(exc.value)


@patch("docentes.grpc_services.asistencia_service.transaction.atomic", return_value=nullcontext())
@patch.object(AsistenciaServiceServicer, "_validate_auth", return_value=10)
@patch.object(AsistenciaServiceServicer, "_actualizar_resumen")
@patch("docentes.grpc_services.asistencia_service.Asistencia.objects.get")
def test_actualiza_asistencia_y_resumen(mock_get, mock_resumen, mock_auth, mock_atomic):
    asistencia = SimpleNamespace(id_asistencia=1, id_matricula=101, id_asignacion=50,
        id_periodo=SimpleNamespace(), id_periodo_id=3, fecha="2026-07-15", estado="PRESENTE",
        justificacion=None, registrado_por=77, save=MagicMock())
    mock_get.return_value = asistencia
    request = asistencia_pb2.ActualizarAsistenciaRequest(id_asistencia=1, estado="ATRASO", justificacion="Tarde")
    response = AsistenciaServiceServicer().ActualizarAsistencia(request, auth_context())
    assert response.success and response.asistencia.estado == "ATRASO"
    asistencia.save.assert_called_once()
    mock_resumen.assert_called_once()
    assert asistencia.save.call_count == 1
    assert mock_resumen.call_args.args == (101, 50, asistencia.id_periodo)


@patch.object(AsistenciaServiceServicer, "_validate_auth", return_value=10)
@patch("docentes.grpc_services.asistencia_service.Asistencia.objects.filter")
def test_consulta_asistencia_aplica_filtros(mock_filter, mock_auth):
    row = SimpleNamespace(id_asistencia=1, id_matricula=101, id_asignacion=50,
        id_periodo_id=3, fecha="2026-07-15", estado="PRESENTE", justificacion=None)
    query = MagicMock(); query.filter.return_value = query; query.__iter__.return_value = iter([row])
    mock_filter.return_value = query
    request = asistencia_pb2.ConsultarAsistenciaRequest(id_asignacion=50, fecha="2026-07-15", id_periodo=3, id_matricula=101)
    response = AsistenciaServiceServicer().ConsultarAsistencia(request, auth_context())
    assert response.success and len(response.asistencias) == 1 and query.filter.call_count == 3
    assert response.message == "1 registros encontrados"
    assert mock_auth.call_args.args[1] == 50


@patch("docentes.grpc_services.asistencia_service.get_students_by_assignment", return_value=[{"id_matricula": 101}])
@patch.object(AsistenciaServiceServicer, "_validate_auth", return_value=10)
@patch("docentes.grpc_services.asistencia_service.ResumenAsistencia.objects.filter")
def test_consulta_resumen_calcula_porcentaje(mock_filter, mock_auth, mock_students):
    row = SimpleNamespace(id_resumen=1, id_matricula=101, id_asignacion=50, id_periodo_id=3,
        total_presentes=1, total_ausentes=1, total_justificados=1, total_atrasos=1)
    query = MagicMock(); query.filter.return_value = query; query.__iter__.return_value = iter([row])
    mock_filter.return_value = query
    request = asistencia_pb2.ConsultarResumenRequest(id_asignacion=50, id_periodo=3, id_matricula=101)
    response = AsistenciaServiceServicer().ConsultarResumenAsistencia(request, auth_context())
    assert response.resumenes[0].porcentaje_asistencia == 75.0
    assert response.success is True
    assert response.message == "1 resúmenes encontrados"


@patch("docentes.grpc_services.asistencia_service.ResumenAsistencia.objects")
@patch("docentes.grpc_services.asistencia_service.Asistencia.objects.filter")
def test_actualizar_resumen_cuenta_cada_estado(mock_filter, mock_resumen):
    mock_filter.return_value = [SimpleNamespace(estado=e) for e in ("PRESENTE", "AUSENTE", "JUSTIFICADO", "ATRASO")]
    resumen = SimpleNamespace(save=MagicMock())
    mock_resumen.get_or_create.return_value = (resumen, False)
    AsistenciaServiceServicer()._actualizar_resumen(1, 2, 3)
    assert (resumen.total_presentes, resumen.total_ausentes, resumen.total_justificados, resumen.total_atrasos) == (1, 1, 1, 1)
    assert resumen.save.call_count == 1
    assert mock_resumen.get_or_create.call_count == 1


@patch("docentes.grpc_services.asistencia_service.ResumenAsistencia.objects")
@patch("docentes.grpc_services.asistencia_service.Asistencia.objects")
def test_recalculo_bulk_reemplaza_resumenes(mock_asistencia, mock_resumen):
    from docentes.models import PeriodoEvaluacion
    conteos = [{"id_matricula": 1, "estado": "PRESENTE", "n": 2}, {"id_matricula": 2, "estado": "ATRASO", "n": 1}]
    mock_asistencia.filter.return_value.values.return_value.annotate.return_value = conteos
    AsistenciaServiceServicer()._recalcular_resumen_bulk(50, PeriodoEvaluacion(), [1, 2])
    creados = mock_resumen.bulk_create.call_args.args[0]
    assert creados[0].total_presentes == 2 and creados[1].total_atrasos == 1
    assert mock_resumen.filter.return_value.delete.call_count == 1
    assert len(creados) == 2
