from .hashing import normalizar


def payload_instancia(instancia):
    if not hasattr(instancia, "_meta"):
        return normalizar({
            clave: valor for clave, valor in vars(instancia).items()
            if not clave.startswith("_") and not callable(valor)
        })
    datos = {}
    for campo in instancia._meta.concrete_fields:
        if campo.primary_key:
            continue
        datos[campo.name] = getattr(instancia, campo.attname)
    return normalizar(datos)
