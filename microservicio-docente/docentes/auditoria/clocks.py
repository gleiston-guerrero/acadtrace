from enum import StrEnum


class RelacionVectorial(StrEnum):
    ANTES = "ANTES"
    DESPUES = "DESPUES"
    IGUAL = "IGUAL"
    CONCURRENTE = "CONCURRENTE"


def incrementar_vector(reloj, nodo):
    resultado = dict(reloj or {})
    resultado[nodo] = int(resultado.get(nodo, 0)) + 1
    return resultado


def combinar_vectores(izquierdo, derecho):
    izquierdo, derecho = izquierdo or {}, derecho or {}
    return {
        nodo: max(int(izquierdo.get(nodo, 0)), int(derecho.get(nodo, 0)))
        for nodo in set(izquierdo) | set(derecho)
    }


def comparar_vectores(izquierdo, derecho):
    izquierdo, derecho = izquierdo or {}, derecho or {}
    nodos = set(izquierdo) | set(derecho)
    menor = any(int(izquierdo.get(n, 0)) < int(derecho.get(n, 0)) for n in nodos)
    mayor = any(int(izquierdo.get(n, 0)) > int(derecho.get(n, 0)) for n in nodos)
    if menor and mayor:
        return RelacionVectorial.CONCURRENTE
    if menor:
        return RelacionVectorial.ANTES
    if mayor:
        return RelacionVectorial.DESPUES
    return RelacionVectorial.IGUAL


def reconciliar_vectores(version_actual, version_recibida):
    relacion = comparar_vectores(version_actual, version_recibida)
    return {
        "relacion": relacion,
        "estado": "CONFLICTO" if relacion == RelacionVectorial.CONCURRENTE else "APLICADO",
        "reloj_combinado": combinar_vectores(version_actual, version_recibida),
        "politica": "PRESERVAR_AMBAS_Y_RESOLVER_MANUALMENTE"
        if relacion == RelacionVectorial.CONCURRENTE else "APLICAR_VERSION_CAUSAL",
    }
