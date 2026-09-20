import os
import sys
from pathlib import Path

if not os.environ.get("DJANGO_SETTINGS_MODULE"):
    from django.conf import settings
    if not settings.configured:
        settings.configure(
            SECRET_KEY="secret-key-canonical-test",
            INSTALLED_APPS=[
                "django.contrib.contenttypes",
                "django.contrib.auth",
                "docentes",
            ],
            DATABASES={"default": {"ENGINE": "django.db.backends.sqlite3", "NAME": ":memory:"}},
        )
        import django
        django.setup()

from docentes.auditoria.hashing import (
    GENESIS_HASH,
    calcular_hash,
    contenido_evento,
    json_canonico,
)


def test_contrato_canonico_v1_coincide_con_java():
    payload = {
        "descripcion": "Ajuste",
        "resultado": "EXITO",
        "schema_origen": "PRINCIPAL",
        "trace_id": "11111111-1111-1111-1111-111111111111",
    }

    vector = {
        "docente": 2,
        "principal": 5,
        "secretaria": 1,
    }

    evento = contenido_evento(
        tipo_evento="AUDITORIA",
        entidad="calificacion",
        entidad_id="456",
        operacion="EDITAR",
        actor_id="123",
        timestamp="2026-09-13T08:30:00Z",
        payload=payload,
        modo="m3",
        reloj_lamport=42,
        reloj_vectorial=vector,
        estado_reconciliacion="APLICADO",
    )

    esperado = (
        '{"actor_id":"123",'
        '"entidad":"calificacion",'
        '"entidad_id":"456",'
        '"estado_reconciliacion":"APLICADO",'
        '"modo":"m3",'
        '"operacion":"EDITAR",'
        '"payload":{"descripcion":"Ajuste",'
        '"resultado":"EXITO",'
        '"schema_origen":"PRINCIPAL",'
        '"trace_id":"11111111-1111-1111-1111-111111111111"},'
        '"reloj_lamport":42,'
        '"reloj_vectorial":{"docente":2,'
        '"principal":5,"secretaria":1},'
        '"timestamp":"2026-09-13T08:30:00Z",'
        '"tipo_evento":"AUDITORIA"}'
    )

    assert json_canonico(evento) == esperado

    assert calcular_hash(
        GENESIS_HASH,
        evento,
    ) == (
        "4d440be187e77bc07dd34422dea07ccdc94f07c6deb4318b3c10196de81a88b6"
    )


def test_contrato_canonico_v1_excluye_material_sensible():
    evento = contenido_evento(
        tipo_evento="AUDITORIA",
        entidad="usuario",
        entidad_id="1",
        operacion="EDITAR",
        actor_id="actor",
        timestamp="2026-09-13T08:30:00Z",
        payload={
            "dato": "permitido",
            "password": "VALOR-FICTICIO",
            "token": "VALOR-FICTICIO",
        },
        modo="m2",
        reloj_lamport=1,
        reloj_vectorial=None,
        estado_reconciliacion="NO_APLICA",
    )

    canonico = json_canonico(evento)

    assert "password" not in canonico
    assert "token" not in canonico
    assert "VALOR-FICTICIO" not in canonico
