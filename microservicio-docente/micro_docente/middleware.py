import json
import logging
from datetime import datetime, timezone
from time import perf_counter

from prometheus_client import Counter, Gauge


logger = logging.getLogger("micro_docente.http")

calificaciones_total = Counter(
    "docente_calificaciones_total",
    "Total de notas registradas exitosamente.",
)
asistencias_total = Counter(
    "docente_asistencias_total",
    "Total de asistencias tomadas o registradas exitosamente.",
)
active_requests = Gauge(
    "docente_active_requests",
    "Cantidad actual de peticiones HTTP concurrentes activas en microservicio-docente.",
)


def registrar_calificacion_exitosa(cantidad=1):
    """Cuenta operaciones exitosas sin exponer datos de la calificacion."""
    calificaciones_total.inc(cantidad)


def registrar_asistencias_exitosas(cantidad=1):
    """Cuenta filas de asistencia persistidas exitosamente."""
    asistencias_total.inc(cantidad)


def _registrar_metricas_http(request, response):
    """Instrumenta las rutas REST directas; las entradas gRPC se cuentan allí."""
    if request.method != "POST" or not 200 <= response.status_code < 300:
        return

    ruta = request.path.rstrip("/") + "/"
    if ruta == "/api/docente/calificaciones/":
        registrar_calificacion_exitosa()
    elif ruta == "/api/docente/asistencias/":
        registrar_asistencias_exitosas()


class StructuredRequestLoggingMiddleware:
    """Emite un evento JSON por cada respuesta HTTP, sin datos de la petición."""

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        inicio = perf_counter()
        active_requests.inc()
        try:
            response = self.get_response(request)
            _registrar_metricas_http(request, response)
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
        finally:
            active_requests.dec()
