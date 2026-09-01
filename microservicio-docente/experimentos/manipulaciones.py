from copy import deepcopy
from datetime import timedelta


def aplicar_manipulacion(eventos, tipo):
    resultado = deepcopy(eventos)
    if not resultado:
        raise ValueError("Se requiere al menos un evento")
    if tipo == "T1":
        resultado[-1]["payload_canonico"] = '{"nota":"0.00"}'
    elif tipo == "T2":
        resultado.pop(0 if len(resultado) == 1 else len(resultado) // 2)
    elif tipo == "T3":
        if len(resultado) < 2:
            raise ValueError("T3 requiere al menos dos eventos")
        resultado[0], resultado[1] = resultado[1], resultado[0]
    elif tipo == "T4":
        resultado[0]["payload_canonico"] = '{"retroactivo":true}'
    elif tipo == "T5":
        resultado[0]["timestamp"] = resultado[0]["timestamp"] + timedelta(days=1)
    else:
        raise ValueError(f"Tipo de manipulación inválido: {tipo}")
    return resultado
