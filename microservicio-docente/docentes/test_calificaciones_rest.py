from types import SimpleNamespace
from unittest.mock import MagicMock, patch

from docentes.views import CalificacionViewSet


@patch("docentes.views.connection.cursor")
def test_consulta_calificaciones_filtra_por_paralelo(cursor):
    db_cursor = MagicMock()
    db_cursor.__enter__.return_value = db_cursor
    db_cursor.fetchall.return_value = [(21,), (22,)]
    cursor.return_value = db_cursor
    base = MagicMock()
    view = CalificacionViewSet()
    view.request = SimpleNamespace(query_params={"id_paralelo": "8"})
    view.queryset = base

    resultado = view.get_queryset()

    db_cursor.execute.assert_called_once_with(
        "SELECT id_matricula FROM sga_principal.matriculas WHERE id_paralelo = %s", ["8"]
    )
    assert resultado is not None
