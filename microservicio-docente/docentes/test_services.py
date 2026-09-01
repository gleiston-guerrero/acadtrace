from decimal import Decimal
from types import SimpleNamespace
from unittest.mock import MagicMock, patch

import pytest

from docentes import services


@pytest.mark.parametrize(
    ("value", "expected"),
    [("8.125", Decimal("8.13")), (None, Decimal("0.00")), ("4.004", Decimal("4.00"))],
)
def test_quantize_score_usa_round_half_up(value, expected):
    resultado = services.quantize_score(value)
    assert resultado == expected
    assert isinstance(resultado, Decimal)
    assert resultado.as_tuple().exponent == -2


@pytest.mark.parametrize(
    ("nota", "nivel", "expected"),
    [("9", "EGB", "A_MAS"), ("7", "EGB", "B_MAS"), ("4.01", "EGB", "C_MAS"), ("4", "EGB", "D"),
     ("9", "INICIAL", "A_MAS"), ("7", "PREPARATORIA", "B_MAS"), ("4.01", "INICIAL", "C_MAS"), ("4", None, "D")],
)
def test_conversion_cualitativa(nota, nivel, expected):
    resultado = services.convertir_nota_cualitativa(nota, nivel)
    assert resultado == expected
    assert isinstance(resultado, str)
    assert resultado in {"A_MAS", "B_MAS", "C_MAS", "D"}


@patch("docentes.services.Calificacion.objects.filter")
def test_calcular_promedios_por_tipo(mock_filter):
    mock_filter.return_value.aggregate.side_effect = [
        {"promedio": Decimal("8.125")}, {"promedio": None}
    ]
    assert services.calcular_promedio_formativo(1, 2, 3) == Decimal("8.13")
    assert services.calcular_nota_sumativa(1, 2, 3) == Decimal("0.00")
    assert mock_filter.call_args_list[0].kwargs["id_actividad__es_sumativa"] is False
    assert mock_filter.call_args_list[1].kwargs["id_actividad__es_sumativa"] is True


@patch("docentes.services.PromedioTrimestral.objects.update_or_create")
@patch("docentes.services.calcular_nota_sumativa", return_value=Decimal("9.25"))
@patch("docentes.services.calcular_promedio_formativo", return_value=Decimal("8.35"))
def test_promedio_trimestral_real_70_30_y_persistencia(mock_form, mock_sum, mock_upsert):
    persisted = SimpleNamespace(promedio_trimestral=Decimal("8.62"))
    mock_upsert.return_value = (persisted, True)
    assert services.calcular_promedio_trimestral(10, 20, 30) is persisted
    defaults = mock_upsert.call_args.kwargs["defaults"]
    assert defaults == {
        "promedio_formativo": Decimal("8.35"),
        "nota_sumativa": Decimal("9.25"),
        "promedio_trimestral": Decimal("8.62"),
        "nota_cualitativa": "B_MAS",
    }
    assert mock_upsert.call_count == 1


@patch("docentes.services.PromedioTrimestral.objects.update_or_create")
@patch("docentes.services.calcular_nota_sumativa", return_value=Decimal("0.00"))
@patch("docentes.services.calcular_promedio_formativo", return_value=Decimal("0.00"))
def test_promedio_trimestral_sin_calificaciones(mock_form, mock_sum, mock_upsert):
    persisted = SimpleNamespace()
    mock_upsert.return_value = (persisted, False)
    assert services.calcular_promedio_trimestral(1, 2, 3) is persisted
    assert mock_upsert.call_args.kwargs["defaults"]["promedio_trimestral"] == Decimal("0.00")
    assert mock_form.call_count == mock_sum.call_count == 1


@patch("docentes.services.ResumenAsistencia.objects.update_or_create")
@patch("docentes.services.Asistencia.objects.filter")
def test_calcular_resumen_asistencia_persiste_todos_los_estados(mock_filter, mock_upsert):
    mock_filter.return_value.aggregate.return_value = {
        "presentes": 2, "ausentes": 1, "justificados": 1, "atrasos": 3,
    }
    resumen = SimpleNamespace()
    mock_upsert.return_value = (resumen, True)
    assert services.calcular_resumen_asistencia(1, 2, 3) is resumen
    assert mock_upsert.call_args.kwargs["defaults"] == {
        "total_presentes": 2, "total_ausentes": 1,
        "total_justificados": 1, "total_atrasos": 3,
    }
    assert mock_filter.call_count == 1


@patch("docentes.services.PromedioAnualDetalle.objects.bulk_create")
@patch("docentes.services.PromedioAnualDetalle.objects.filter")
@patch("docentes.services.PromedioAnual.objects.update_or_create")
@patch("docentes.services.PromedioTrimestral.objects.filter")
def test_promedio_anual_con_y_sin_trimestres(mock_filter, mock_upsert, mock_detail_filter, mock_bulk):
    from docentes.models import PromedioAnual, PromedioTrimestral
    trimestres = [PromedioTrimestral(promedio_trimestral=Decimal("8.00")), PromedioTrimestral(promedio_trimestral=Decimal("9.00"))]
    mock_filter.side_effect = [trimestres, []]
    promedio = PromedioAnual()
    mock_upsert.return_value = (promedio, True)
    services.calcular_promedio_anual(1, 2, 2026)
    assert mock_upsert.call_args_list[0].kwargs["defaults"]["promedio_anual"] == Decimal("8.50")
    assert len(mock_bulk.call_args_list[0].args[0]) == 2
    services.calcular_promedio_anual(1, 2, 2026)
    assert mock_upsert.call_args_list[1].kwargs["defaults"]["promedio_anual"] == Decimal("0.00")
