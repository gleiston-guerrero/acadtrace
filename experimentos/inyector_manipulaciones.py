import argparse
import os
from datetime import timedelta
from pathlib import Path
import sys


BASE_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BASE_DIR))
os.environ.setdefault("DJANGO_SETTINGS_MODULE", "micro_docente.settings")


def inyectar(tipo):
    if os.environ.get("ALLOW_AUDIT_EXPERIMENTS") != "1":
        raise RuntimeError("Operación bloqueada: define ALLOW_AUDIT_EXPERIMENTS=1 en una BD experimental")
    if os.environ.get("DJANGO_DEBUG", "False").lower() != "true":
        raise RuntimeError("Operación bloqueada: DJANGO_DEBUG=True es obligatorio")

    import django
    django.setup()
    from django.db import transaction
    from docentes.models import EventoAuditoria

    with transaction.atomic():
        eventos = list(EventoAuditoria.objects.select_for_update().filter(modo__in=["m2", "m3"]).order_by("id_evento"))
        if not eventos:
            raise RuntimeError("No existen eventos m2/m3 para manipular")
        if tipo == "T1":
            evento = eventos[-1]; evento.payload_canonico = '{"nota":"0.00"}'; evento.save(update_fields=["payload_canonico"])
        elif tipo == "T2":
            eventos[len(eventos) // 2].delete()
        elif tipo == "T3":
            if len(eventos) < 2:
                raise RuntimeError("T3 requiere al menos dos eventos")
            primero, segundo = eventos[:2]
            primero.hash_actual, segundo.hash_actual = segundo.hash_actual, primero.hash_actual
            primero.save(update_fields=["hash_actual"]); segundo.save(update_fields=["hash_actual"])
        elif tipo == "T4":
            evento = eventos[0]; evento.payload_canonico = '{"retroactivo":true}'; evento.save(update_fields=["payload_canonico"])
        elif tipo == "T5":
            evento = eventos[0]; evento.timestamp += timedelta(days=1); evento.save(update_fields=["timestamp"])


def main():
    parser = argparse.ArgumentParser(description="Inyecta manipulaciones solo en una BD experimental.")
    parser.add_argument("--tipo", required=True, choices=["T1", "T2", "T3", "T4", "T5"])
    args = parser.parse_args()
    inyectar(args.tipo)
    print(f"Manipulación {args.tipo} aplicada")


if __name__ == "__main__":
    main()
