import os

from django.core.exceptions import ImproperlyConfigured


MODOS_VALIDOS = frozenset({"m0", "m1", "m2", "m3"})


def obtener_modo_auditoria():
    modo = os.environ.get("AUDIT", "m0").strip().lower()
    if modo not in MODOS_VALIDOS:
        raise ImproperlyConfigured(
            f"AUDIT debe ser uno de {', '.join(sorted(MODOS_VALIDOS))}; recibido: {modo!r}"
        )
    return modo
