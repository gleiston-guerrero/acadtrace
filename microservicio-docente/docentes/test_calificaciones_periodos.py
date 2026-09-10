from types import SimpleNamespace
from django.test import SimpleTestCase
from docentes.grpc_services.representante_academico_service import annual_grades_visible


class AnnualVisibilityTest(SimpleTestCase):
    def periods(self, active_type=None):
        return [SimpleNamespace(tipo=t, activo=t == active_type) for t in (
            "PRIMER_TRIMESTRE", "SEGUNDO_TRIMESTRE", "TERCER_TRIMESTRE")]

    def test_year_in_progress_hides_annual_grades(self):
        self.assertFalse(annual_grades_visible(self.periods("SEGUNDO_TRIMESTRE"), [object()]))

    def test_three_closed_periods_with_annual_grades_are_visible(self):
        self.assertTrue(annual_grades_visible(self.periods(), [object()]))

    def test_missing_period_hides_annual_grades(self):
        self.assertFalse(annual_grades_visible(self.periods()[:2], [object()]))
