from types import SimpleNamespace
from unittest.mock import patch
from django.test import SimpleTestCase
from docentes.notifications import enqueue_announcement, enqueue_attendance


class NotificationEventTests(SimpleTestCase):
    @patch("docentes.notifications.transaction.on_commit", side_effect=lambda callback: callback())
    @patch("docentes.notifications._student_name", return_value="Ana Paz")
    @patch("docentes.notifications._deliver")
    def test_absent_and_late_notify_present_does_not(self, deliver, _name, _commit):
        base = dict(pk=7, id_matricula=10, id_periodo_id=3, fecha=SimpleNamespace(isoformat=lambda: "2026-09-09"))
        enqueue_attendance(SimpleNamespace(**base, estado="AUSENTE"))
        enqueue_attendance(SimpleNamespace(**base, estado="ATRASO"))
        enqueue_attendance(SimpleNamespace(**base, estado="PRESENTE"))
        self.assertEqual([c.args[0]["type"] for c in deliver.call_args_list], ["AUSENTE", "ATRASO"])
        self.assertEqual(deliver.call_args_list[0].args[0]["eventKey"], "ASISTENCIA:7:AUSENTE")

    @patch("docentes.notifications.transaction.on_commit", side_effect=lambda callback: callback())
    @patch("docentes.notifications._deliver")
    def test_announcement_uses_real_content(self, deliver, _commit):
        enqueue_announcement(SimpleNamespace(pk=9, id_asignacion=2, titulo="Reunion real"))
        payload = deliver.call_args.args[0]
        self.assertEqual(payload["eventKey"], "COMUNICADO:9")
        self.assertEqual(payload["body"], "Reunion real")
