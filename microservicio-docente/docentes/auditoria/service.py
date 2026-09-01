from .config import obtener_modo_auditoria
from .strategies import STRATEGIES


def auditar_evento(**evento):
    modo = obtener_modo_auditoria()
    return STRATEGIES[modo]().registrar(**evento)
