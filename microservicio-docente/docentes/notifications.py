import json
import logging
import os
from urllib import request

from django.db import connection, transaction

logger = logging.getLogger("micro_docente.notifications")


def _student_name(id_matricula):
    try:
        with connection.cursor() as cursor:
            cursor.execute(
                "SELECT e.nombres, e.apellidos FROM sga_principal.matriculas m "
                "JOIN sga_principal.estudiantes e ON e.id_estudiante=m.id_estudiante "
                "WHERE m.id_matricula=%s", [id_matricula]
            )
            row = cursor.fetchone()
        return " ".join(row) if row else "El estudiante"
    except Exception:
        return "El estudiante"


def _deliver(payload):
    base = os.environ.get("SGA_PRINCIPAL_URL", "http://localhost:8080").rstrip("/")
    token = os.environ.get("GRPC_INTERNAL_TOKEN", "")
    if not token:
        logger.warning("Notificacion academica omitida: token interno no configurado")
        return
    req = request.Request(
        base + "/api/internal/notificaciones/eventos",
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json", "X-Internal-Token": token},
        method="POST",
    )
    try:
        with request.urlopen(req, timeout=3):
            pass
    except Exception as exc:
        # Nunca se incluye el token ni el payload (contiene datos academicos).
        logger.warning("No se pudo entregar evento push a Principal: %s", type(exc).__name__)


def enqueue_attendance(instance):
    if instance.estado not in ("AUSENTE", "ATRASO"):
        return
    student = _student_name(instance.id_matricula)
    label = "ausencia" if instance.estado == "AUSENTE" else "atraso"
    payload = {
        "eventKey": f"ASISTENCIA:{instance.pk}:{instance.estado}",
        "type": instance.estado,
        "matriculaId": instance.id_matricula,
        "periodId": instance.id_periodo_id,
        "studentName": student,
        "attendanceId": instance.pk,
        "date": instance.fecha.isoformat(),
        "body": f"{student} registro un {label} el {instance.fecha.isoformat()}",
    }
    transaction.on_commit(lambda: _deliver(payload))


def enqueue_announcement(instance):
    payload = {
        "eventKey": f"COMUNICADO:{instance.pk}",
        "type": "COMUNICADO",
        "asignacionId": instance.id_asignacion,
        "announcementId": instance.pk,
        "title": "Nuevo comunicado",
        "body": instance.titulo,
    }
    transaction.on_commit(lambda: _deliver(payload))
