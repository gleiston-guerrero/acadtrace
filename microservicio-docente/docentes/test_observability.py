import json
from unittest.mock import patch

import pytest
from django.http import HttpResponse
from django.test import Client, RequestFactory

from micro_docente.middleware import (
    StructuredRequestLoggingMiddleware,
    active_requests,
    asistencias_total,
    calificaciones_total,
)


def test_middleware_emite_json_con_campos_requeridos():
    request = RequestFactory().get("/health/")
    middleware = StructuredRequestLoggingMiddleware(lambda _request: HttpResponse(status=204))
    with patch("micro_docente.middleware.logger.info") as mock_info:
        response = middleware(request)
    evento = json.loads(mock_info.call_args.args[0])
    assert response.status_code == 204
    assert evento["servicio"] == "microservicio-docente"
    assert evento["metodo"] == "GET"
    assert evento["ruta"] == "/health/"
    assert evento["codigo_http"] == 204
    assert evento["timestamp"]
    assert isinstance(evento["tiempo_respuesta_ms"], float)


def test_health_y_metrics_estan_expuestos():
    client = Client()
    health = client.get("/health")
    assert health.status_code == 200
    assert health.json()["status"] == "UP"
    assert health.json()["service"] == "Microservicio Docente"
    metrics = client.get("/metrics")
    assert metrics.status_code == 200
    assert b"django_http" in metrics.content or b"python_info" in metrics.content
    assert b"docente_calificaciones_total" in metrics.content
    assert b"docente_asistencias_total" in metrics.content
    assert b"docente_active_requests" in metrics.content


def test_active_requests_incrementa_durante_peticion_y_restaura_valor():
    before = active_requests._value.get()
    observed = []
    middleware = StructuredRequestLoggingMiddleware(
        lambda _request: observed.append(active_requests._value.get()) or HttpResponse(status=200)
    )
    response = middleware(RequestFactory().get("/health"))
    assert observed == [before + 1]
    assert active_requests._value.get() == before
    assert response.status_code == 200


def test_active_requests_decrementa_si_la_vista_lanza_excepcion():
    before = active_requests._value.get()
    observed = []

    def failing_view(_request):
        observed.append(active_requests._value.get())
        raise RuntimeError("fallo controlado")

    middleware = StructuredRequestLoggingMiddleware(failing_view)
    with pytest.raises(RuntimeError) as exc:
        middleware(RequestFactory().get("/falla"))
    assert observed == [before + 1]
    assert active_requests._value.get() == before
    assert str(exc.value) == "fallo controlado"


def test_calificaciones_counter_solo_incrementa_en_post_exitoso():
    before = calificaciones_total._value.get()
    factory = RequestFactory()
    success = StructuredRequestLoggingMiddleware(lambda _request: HttpResponse(status=201))
    failure = StructuredRequestLoggingMiddleware(lambda _request: HttpResponse(status=400))
    assert success(factory.post("/api/docente/calificaciones/", data={})).status_code == 201
    assert calificaciones_total._value.get() == before + 1
    assert success(factory.get("/api/docente/calificaciones/")).status_code == 201
    assert calificaciones_total._value.get() == before + 1
    assert failure(factory.post("/api/docente/calificaciones/", data={})).status_code == 400
    assert calificaciones_total._value.get() == before + 1


def test_asistencias_counter_solo_incrementa_en_post_exitoso():
    before = asistencias_total._value.get()
    factory = RequestFactory()
    success = StructuredRequestLoggingMiddleware(lambda _request: HttpResponse(status=201))
    failure = StructuredRequestLoggingMiddleware(lambda _request: HttpResponse(status=422))
    assert success(factory.post("/api/docente/asistencias/", data={})).status_code == 201
    assert asistencias_total._value.get() == before + 1
    assert success(factory.get("/api/docente/asistencias/")).status_code == 201
    assert asistencias_total._value.get() == before + 1
    assert failure(factory.post("/api/docente/asistencias/", data={})).status_code == 422
    assert asistencias_total._value.get() == before + 1
