import json
import logging
from datetime import datetime, timezone
from time import perf_counter


logger = logging.getLogger("micro_docente.http")


class StructuredRequestLoggingMiddleware:
    """Emite un evento JSON por cada respuesta HTTP, sin datos de la petición."""

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        inicio = perf_counter()
        response = self.get_response(request)
        evento = {
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "servicio": "microservicio-docente",
            "metodo": request.method,
            "ruta": request.path,
            "codigo_http": response.status_code,
            "tiempo_respuesta_ms": round((perf_counter() - inicio) * 1000, 2),
        }
        logger.info(json.dumps(evento, ensure_ascii=False))
        return response
