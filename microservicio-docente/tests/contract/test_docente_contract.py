import json
from decimal import Decimal
from pathlib import Path
from threading import Thread
from urllib.request import urlopen
from unittest.mock import patch
from wsgiref.simple_server import make_server

from django.core.wsgi import get_wsgi_application
from pact import Pact, Verifier, match

from docentes.models import Calificacion
from docentes.views import CalificacionViewSet


CONSUMER = "portal-docente"
PROVIDER = "microservicio-docente"
PACT_DIR = Path(__file__).resolve().parent / "pacts"


def test_consulta_calificaciones_genera_contrato_pact_real():
    pact = Pact(CONSUMER, PROVIDER).with_specification("V4")
    (
        pact.upon_receiving("consultar calificaciones de una actividad")
        .given("la actividad 9 tiene una calificación")
        .with_request("GET", "/api/docente/calificaciones/")
        .with_query_parameter("id_actividad", "9")
        .will_respond_with(200)
        .with_header("Content-Type", match.regex("application/json", regex=r"^application/json"))
        .with_body([
            {
                "id_calificacion": match.integer(1),
                "id_matricula": match.integer(101),
                "nota": match.regex("9.00", regex=r"^\d{1,2}\.\d{2}$"),
                "id_actividad": match.integer(9),
            }
        ])
    )
    with pact.serve() as servidor:
        with urlopen(f"{servidor.url}/api/docente/calificaciones/?id_actividad=9") as respuesta:
            cuerpo = json.load(respuesta)
            assert respuesta.status == 200
            assert cuerpo[0]["id_actividad"] == 9
            assert cuerpo[0]["id_matricula"] == 101
    pact.write_file(PACT_DIR, overwrite=True)
    contrato = PACT_DIR / "portal-docente-microservicio-docente.json"
    contenido = json.loads(contrato.read_text(encoding="utf-8"))
    assert contrato.exists()
    assert contenido["consumer"]["name"] == CONSUMER
    assert contenido["provider"]["name"] == PROVIDER

    calificacion = Calificacion(
        id_calificacion=1, id_actividad_id=9, id_matricula=101,
        nota=Decimal("9.00"), nota_cualitativa="A_MAS",
    )
    with patch.object(CalificacionViewSet, "get_queryset", return_value=[calificacion]):
        servidor_django = make_server("localhost", 0, get_wsgi_application())
        hilo = Thread(target=servidor_django.serve_forever, daemon=True)
        hilo.start()
        try:
            verificador = (
                Verifier(PROVIDER)
                .add_transport(url=f"http://localhost:{servidor_django.server_port}")
                .add_source(contrato)
            )
            assert verificador.verify() is verificador
            assert verificador.results["errors"] == []
            assert "consultar calificaciones" in verificador.output()
        finally:
            servidor_django.shutdown()
            hilo.join(timeout=5)
