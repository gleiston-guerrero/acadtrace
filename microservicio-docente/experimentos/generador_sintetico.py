import argparse
import json
import random
from pathlib import Path


SEED = 701
TOTAL_ESTUDIANTES = 344
TOTAL_DOCENTES = 14


def generar_dataset(seed=SEED):
    randomizer = random.Random(seed)
    docentes = [
        {"id_docente": indice, "id_persona": 1000 + indice, "codigo": f"DOC-{indice:03d}"}
        for indice in range(1, TOTAL_DOCENTES + 1)
    ]
    estudiantes = [
        {
            "id_estudiante": indice,
            "id_matricula": 10000 + indice,
            "codigo": f"EST-{indice:04d}",
            "docente_referencia": randomizer.choice(docentes)["id_docente"],
        }
        for indice in range(1, TOTAL_ESTUDIANTES + 1)
    ]
    return {"seed": seed, "docentes": docentes, "estudiantes": estudiantes}


def main():
    parser = argparse.ArgumentParser(description="Genera IDs/payloads sintéticos, sin duplicar modelos de Principal.")
    parser.add_argument("--salida", type=Path, help="Archivo JSON; stdout si se omite.")
    parser.add_argument("--semilla", type=int, default=SEED)
    args = parser.parse_args()
    contenido = json.dumps(generar_dataset(args.semilla), ensure_ascii=False, sort_keys=True, indent=2)
    if args.salida:
        args.salida.write_text(contenido + "\n", encoding="utf-8")
    else:
        print(contenido)


if __name__ == "__main__":
    main()
