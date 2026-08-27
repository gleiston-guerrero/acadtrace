import json
from unittest.mock import patch

from django.http import HttpResponse
from django.test import Client, RequestFactory

from micro_docente.middleware import StructuredRequestLoggingMiddleware


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
    assert client.get("/health").status_code == 200
    metrics = client.get("/metrics")
    assert metrics.status_code == 200
    assert b"django_http" in metrics.content or b"python_info" in metrics.content
