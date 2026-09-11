import os
from pathlib import Path
import sys


BASE_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BASE_DIR))
os.environ.setdefault("DJANGO_SETTINGS_MODULE", "micro_docente.settings")

from docentes.auditoria.hashing import calcular_hash


def verificar_registros(registros):
    hash_anterior = "0" * 64
    for registro in registros:
        if registro["hash_anterior"] != hash_anterior:
            return False
        if calcular_hash(hash_anterior, registro["contenido"]) != registro["hash"]:
            return False
        hash_anterior = registro["hash"]
    return True


def main():
    import django
    django.setup()
    from docentes.auditoria.verifier import verificar_cadena
    from docentes.models import EstadoCadenaAuditoria, EventoAuditoria

    estado = EstadoCadenaAuditoria.objects.get(id_estado=1)
    eventos = EventoAuditoria.objects.filter(modo__in=["m2", "m3"]).order_by("id_evento")
    resultado = verificar_cadena(
        eventos, hash_cabeza=estado.ultimo_hash or "0" * 64,
        lamport_cabeza=estado.ultimo_lamport,
    )
    if resultado.valido:
        print("CADENA VALIDA")
        print(f"Registros verificados: {resultado.registros_verificados}")
        return 0
    print("CADENA INVALIDA")
    print(f"Primer eslabon roto: {resultado.primer_eslabon_roto or 'CABEZA'}")
    print(f"Tipo de inconsistencia: {resultado.tipo_inconsistencia}")
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
