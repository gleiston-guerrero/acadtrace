import hashlib
import json
from datetime import date, datetime
from decimal import Decimal


GENESIS_HASH = "0" * 64
SECRET_KEYS = {
    "authorization",
    "jwt",
    "password",
    "contraseña",
    "internal_token",
    "token",
    "secret",
}


def normalizar(valor):
    if isinstance(valor, dict):
        return {
            str(clave): normalizar(contenido)
            for clave, contenido in valor.items()
            if str(clave).lower() not in SECRET_KEYS
        }
    if isinstance(valor, (list, tuple)):
        return [normalizar(elemento) for elemento in valor]
    if isinstance(valor, (datetime, date)):
        return valor.isoformat()
    if isinstance(valor, Decimal):
        return str(valor)
    return valor


def json_canonico(contenido):
    return json.dumps(
        normalizar(contenido), sort_keys=True, separators=(",", ":"), ensure_ascii=False
    )


def contenido_evento(
    *, tipo_evento, entidad, entidad_id, operacion, actor_id, timestamp,
    payload, modo, reloj_lamport, reloj_vectorial, estado_reconciliacion,
):
    return {
        "actor_id": actor_id,
        "entidad": entidad,
        "entidad_id": str(entidad_id),
        "estado_reconciliacion": estado_reconciliacion,
        "modo": modo,
        "operacion": operacion,
        "payload": normalizar(payload),
        "reloj_lamport": reloj_lamport,
        "reloj_vectorial": normalizar(reloj_vectorial),
        "timestamp": normalizar(timestamp),
        "tipo_evento": tipo_evento,
    }


def calcular_hash(hash_anterior, contenido):
    material = (hash_anterior + json_canonico(contenido)).encode("utf-8")
    return hashlib.sha256(material).hexdigest()
