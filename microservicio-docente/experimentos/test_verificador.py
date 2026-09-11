from experimentos.verificador_cadena import verificar_registros
from docentes.auditoria.hashing import calcular_hash


def construir_cadena_valida():
    registros = []
    hash_anterior = "0" * 64
    for contenido in (
        {"id_calificacion": 101, "estudiante": 7, "nota": "9.25"},
        {"id_calificacion": 102, "estudiante": 8, "nota": "8.50"},
    ):
        hash_actual = calcular_hash(hash_anterior, contenido)
        registros.append(
            {
                "hash_anterior": hash_anterior,
                "contenido": contenido,
                "hash": hash_actual,
            }
        )
        hash_anterior = hash_actual
    return registros


def test_detecta_nota_manipulada_sin_recalcular_hash():
    cadena = construir_cadena_valida()
    assert verificar_registros(cadena) is True

    cadena[1]["contenido"]["nota"] = "10.00"

    assert verificar_registros(cadena) is False
