from types import SimpleNamespace
import json
import os
from unittest.mock import MagicMock, patch
from django.test import SimpleTestCase
from docentes import notifications
from docentes.notifications import enqueue_announcement, enqueue_attendance


class NotificationEventTests(SimpleTestCase):
    @patch("docentes.notifications.connection")
    def test_student_name_uses_database_row_and_safe_fallback(self, database_connection):
        cursor_factory = database_connection.cursor
        cursor = cursor_factory.return_value.__enter__.return_value
        cursor.fetchone.return_value = ("Ana", "Paz")

        self.assertEqual(notifications._student_name(10), "Ana Paz")
        cursor.execute.assert_called_once()
        self.assertEqual(cursor.execute.call_args.args[1], [10])

        cursor_factory.reset_mock()
        cursor_factory.return_value.__enter__.side_effect = RuntimeError("db unavailable")
        self.assertEqual(notifications._student_name(10), "El estudiante")

    @patch("docentes.notifications.logger.warning")
    def test_deliver_omits_request_without_internal_token(self, warning):
        with patch.dict(os.environ, {"GRPC_INTERNAL_TOKEN": ""}, clear=False), patch(
            "docentes.notifications.request.urlopen"
        ) as urlopen:
            notifications._deliver({"type": "AUSENTE"})

        urlopen.assert_not_called()
        warning.assert_called_once_with(
            "Notificacion academica omitida: token interno no configurado"
        )

    @patch("docentes.notifications.request.urlopen")
    def test_deliver_posts_json_to_principal(self, urlopen):
        response = MagicMock()
        urlopen.return_value.__enter__.return_value = response
        payload = {"type": "AUSENTE", "attendanceId": 7}

        with patch.dict(
            os.environ,
            {
                "GRPC_INTERNAL_TOKEN": "test-internal-token",
                "SGA_PRINCIPAL_URL": "http://principal:8080/",
            },
            clear=False,
        ):
            notifications._deliver(payload)

        sent_request = urlopen.call_args.args[0]
        self.assertEqual(sent_request.full_url, "http://principal:8080/api/internal/notificaciones/eventos")
        self.assertEqual(sent_request.method, "POST")
        self.assertEqual(json.loads(sent_request.data.decode("utf-8")), payload)
        self.assertEqual(sent_request.headers["X-internal-token"], "test-internal-token")
        self.assertEqual(urlopen.call_args.kwargs["timeout"], 3)

    @patch("docentes.notifications.logger.warning")
    @patch("docentes.notifications.request.urlopen", side_effect=TimeoutError)
    def test_deliver_logs_only_exception_type_on_transport_error(self, _urlopen, warning):
        with patch.dict(
            os.environ,
            {"GRPC_INTERNAL_TOKEN": "test-internal-token"},
            clear=False,
        ):
            notifications._deliver({"type": "ATRASO", "studentName": "Dato privado"})

        warning.assert_called_once_with(
            "No se pudo entregar evento push a Principal: %s", "TimeoutError"
        )

    @patch("docentes.notifications.transaction.on_commit", side_effect=lambda callback: callback())
    @patch("docentes.notifications._student_name", return_value="Ana Paz")
    @patch("docentes.notifications._deliver")
    def test_absent_and_late_notify_present_does_not(self, deliver, _name, _commit):
        base = dict(pk=7, id_matricula=10, id_periodo_id=3, fecha=SimpleNamespace(isoformat=lambda: "2026-09-09"))
        enqueue_attendance(SimpleNamespace(**base, estado="AUSENTE"))
        enqueue_attendance(SimpleNamespace(**base, estado="ATRASO"))
        enqueue_attendance(SimpleNamespace(**base, estado="PRESENTE"))
        self.assertEqual([c.args[0]["type"] for c in deliver.call_args_list], ["AUSENTE", "ATRASO"])
        payload = deliver.call_args_list[0].args[0]
        self.assertEqual(payload["eventKey"], "ASISTENCIA:7:AUSENTE")
        self.assertEqual(payload["studentName"], "Ana Paz")
        self.assertEqual(payload["date"], "2026-09-09")
        self.assertEqual(payload["attendanceId"], 7)

    @patch("docentes.notifications.transaction.on_commit", side_effect=lambda callback: callback())
    @patch("docentes.notifications._deliver")
    def test_announcement_uses_real_content(self, deliver, _commit):
        enqueue_announcement(SimpleNamespace(pk=9, id_asignacion=2, titulo="Reunion real"))
        payload = deliver.call_args.args[0]
        self.assertEqual(payload["eventKey"], "COMUNICADO:9")
        self.assertEqual(payload["body"], "Reunion real")
