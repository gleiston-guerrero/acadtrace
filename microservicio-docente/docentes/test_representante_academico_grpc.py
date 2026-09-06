from datetime import date, datetime, timezone
from types import SimpleNamespace
from unittest.mock import patch
import grpc
import pytest

from django.test import override_settings

from docentes.grpc_services import representante_academico_pb2 as pb2
from docentes.grpc_services.representante_academico_service import RepresentanteAcademicoServiceServicer


class Contexto:
    def invocation_metadata(self):
        return (("internal_token", "test-token"),)

    def abort(self, code, detail):
        raise RuntimeError(code, detail)


@override_settings(GRPC_INTERNAL_TOKEN="test-token")
def test_rechaza_autenticacion_interna_invalida():
    class ContextoInvalido(Contexto):
        def invocation_metadata(self):
            return (("internal_token", "incorrecto"),)
    with pytest.raises(RuntimeError) as error:
        RepresentanteAcademicoServiceServicer().ConsultarComunicados(
            pb2.AsignacionesRequest(id_asignaciones=[4]), ContextoInvalido())
    assert error.value.args[0] == grpc.StatusCode.UNAUTHENTICATED


@override_settings(GRPC_INTERNAL_TOKEN="test-token")
@patch("docentes.grpc_services.representante_academico_service.PromedioTrimestral.objects.filter")
@patch("docentes.grpc_services.representante_academico_service.Calificacion.objects.filter")
def test_calificaciones_existentes(notas_filter, promedios_filter):
    periodo = SimpleNamespace(id_periodo=2, nombre="Primer trimestre")
    actividad = SimpleNamespace(id_actividad=3, id_asignacion=4, id_periodo_id=2, id_periodo=periodo, nombre="Tarea")
    notas_filter.return_value.select_related.return_value = [SimpleNamespace(
        id_calificacion=1, id_matricula=21, id_actividad_id=3, id_actividad=actividad,
        nota=9.5, nota_cualitativa="A_MAS")]
    promedios_filter.return_value.select_related.return_value = []
    response = RepresentanteAcademicoServiceServicer().ConsultarCalificaciones(
        pb2.MatriculasRequest(id_matriculas=[21]), Contexto())
    assert response.calificaciones[0].actividad == "Tarea"


@override_settings(GRPC_INTERNAL_TOKEN="test-token")
@patch("docentes.grpc_services.representante_academico_service.PromedioTrimestral.objects.filter")
@patch("docentes.grpc_services.representante_academico_service.Calificacion.objects.filter")
def test_calificaciones_vacias(notas_filter, promedios_filter):
    notas_filter.return_value.select_related.return_value = []
    promedios_filter.return_value.select_related.return_value = []
    response = RepresentanteAcademicoServiceServicer().ConsultarCalificaciones(
        pb2.MatriculasRequest(id_matriculas=[21]), Contexto())
    assert list(response.calificaciones) == [] and list(response.promedios) == []


@override_settings(GRPC_INTERNAL_TOKEN="test-token")
@patch("docentes.grpc_services.representante_academico_service.Asistencia.objects.filter")
def test_asistencia_existente_y_resumen(asistencia_filter):
    periodo = SimpleNamespace(nombre="Primer trimestre")
    asistencia_filter.return_value.select_related.return_value = [SimpleNamespace(
        id_asistencia=1, id_matricula=21, id_asignacion=4, id_periodo_id=2,
        id_periodo=periodo, fecha=date(2026, 9, 4), estado="PRESENTE")]
    response = RepresentanteAcademicoServiceServicer().ConsultarAsistencia(
        pb2.MatriculasRequest(id_matriculas=[21]), Contexto())
    assert response.resumen.total == 1 and response.resumen.porcentaje_asistencia == 100.0


@override_settings(GRPC_INTERNAL_TOKEN="test-token")
@patch("docentes.grpc_services.representante_academico_service.Anuncio.objects.filter")
def test_comunicados_solo_de_asignaciones_autorizadas(anuncios_filter):
    anuncios_filter.return_value = [SimpleNamespace(
        id_anuncio=8, titulo="Reunión", contenido="Viernes", fijado=True,
        fecha=datetime(2026, 9, 4, tzinfo=timezone.utc))]
    response = RepresentanteAcademicoServiceServicer().ConsultarComunicados(
        pb2.AsignacionesRequest(id_asignaciones=[4]), Contexto())
    anuncios_filter.assert_called_once_with(id_asignacion__in=[4])
    assert response.comunicados[0].titulo == "Reunión"
