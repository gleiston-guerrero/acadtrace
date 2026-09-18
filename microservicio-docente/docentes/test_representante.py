from datetime import date
from decimal import Decimal
from types import SimpleNamespace
from unittest.mock import Mock, patch

import jwt
import pytest
from rest_framework.exceptions import AuthenticationFailed, PermissionDenied
from rest_framework.test import APIRequestFactory, force_authenticate

from .representante_security import EsRepresentante, RepresentanteJWTAuthentication
from .representante_views import AsistenciaRepresentadoView, CalificacionesRepresentadoView


def usuario(rol="REPRESENTANTE"):
    return SimpleNamespace(is_authenticated=True, username="madre", roles=[rol])


def test_jwt_representante_valido(monkeypatch):
    monkeypatch.setenv("JWT_SECRET", "clave-segura-de-prueba-1234567890")  # gitleaks:allow
    token = jwt.encode({"sub": "madre", "roles": ["REPRESENTANTE"]}, "clave-segura-de-prueba-1234567890", algorithm="HS384")
    request = APIRequestFactory().get("/", HTTP_AUTHORIZATION=f"Bearer {token}")
    authenticated, claims = RepresentanteJWTAuthentication().authenticate(request)
    assert authenticated.username == "madre"
    assert "REPRESENTANTE" in authenticated.roles
    assert claims["sub"] == "madre"


def test_jwt_invalido_es_401(monkeypatch):
    monkeypatch.setenv("JWT_SECRET", "clave-segura-de-prueba-1234567890")  # gitleaks:allow
    request = APIRequestFactory().get("/", HTTP_AUTHORIZATION="Bearer inválido")
    with pytest.raises(AuthenticationFailed):
        RepresentanteJWTAuthentication().authenticate(request)


def test_permiso_rechaza_docente():
    request = SimpleNamespace(user=usuario("DOCENTE"))
    assert EsRepresentante().has_permission(request, None) is False
    assert request.user.roles == ["DOCENTE"]
    assert request.user.is_authenticated is True


@patch("docentes.representante_views.matriculas_autorizadas", return_value=[21])
@patch("docentes.representante_views.Calificacion.objects")
@patch("docentes.representante_views.PromedioTrimestral.objects")
def test_representante_consulta_calificaciones(promedios, calificaciones, autorizadas):
    periodo = SimpleNamespace(nombre="Primer trimestre")
    actividad = SimpleNamespace(nombre="Tarea 1", id_asignacion=4, id_periodo_id=2, id_periodo=periodo)
    calificacion = SimpleNamespace(id_calificacion=1, id_matricula=21, id_actividad_id=3, id_actividad=actividad, nota=Decimal("9.50"), nota_cualitativa="A_MAS")
    calificaciones.filter.return_value.select_related.return_value = [calificacion]
    promedios.filter.return_value.select_related.return_value = []
    request = APIRequestFactory().get("/")
    force_authenticate(request, user=usuario())
    response = CalificacionesRepresentadoView.as_view()(request, id_estudiante=11)
    assert response.status_code == 200
    assert response.data["calificaciones"][0]["nota"] == Decimal("9.50")
    autorizadas.assert_called_once_with("madre", 11)


@patch("docentes.representante_views.matriculas_autorizadas", return_value=[21])
@patch("docentes.representante_views.Asistencia.objects")
def test_representante_consulta_asistencia(asistencias, autorizadas):
    periodo = SimpleNamespace(nombre="Primer trimestre")
    registro = SimpleNamespace(id_asistencia=1, id_matricula=21, id_asignacion=4, id_periodo_id=2, id_periodo=periodo, fecha=date(2026, 1, 2), estado="PRESENTE")
    asistencias.filter.return_value.select_related.return_value = [registro]
    request = APIRequestFactory().get("/")
    force_authenticate(request, user=usuario())
    response = AsistenciaRepresentadoView.as_view()(request, id_estudiante=11)
    assert response.status_code == 200
    assert response.data["resumen"]["porcentaje_asistencia"] == 100.0
    autorizadas.assert_called_once_with("madre", 11)


@patch("docentes.representante_views.matriculas_autorizadas", side_effect=PermissionDenied("ajeno"))
def test_representante_no_consulta_estudiante_ajeno(autorizadas):
    request = APIRequestFactory().get("/")
    force_authenticate(request, user=usuario())
    response = CalificacionesRepresentadoView.as_view()(request, id_estudiante=99)
    assert response.status_code == 403
    assert response.data["detail"]
    autorizadas.assert_called_once()
