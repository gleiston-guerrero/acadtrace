#!/usr/bin/env python3
"""Verifica los seis artefactos E7 sin dependencias externas ni acceso a red."""

import argparse
import hashlib
from pathlib import Path
import re
import sys


ARTIFACTS = (
    "deteccion.csv", "manipulaciones.csv", "exp1_concurrencia.csv",
    "exp3_reconciliacion.csv", "iso25010.csv", "boxplot_latencia.png",
)
CERTIFICATE = "REPRODUCIBILIDAD.txt"
DEFAULT_DIRECTORY = Path(__file__).resolve().parent / "resultados"


def artifact_hash(path):
    if not path.is_file() or path.stat().st_size == 0:
        raise ValueError("archivo ausente o vacío")
    data = path.read_bytes()
    if path.suffix == ".csv" and b"\r" in data:
        raise ValueError("CSV contiene CR; se requiere LF antes de certificar")
    return hashlib.sha256(data).hexdigest()


def write_certificate(directory):
    """Valida todos los archivos antes de escribir exactamente seis entradas."""
    lines = [f"{artifact_hash(directory / name)}  {name}\n" for name in ARTIFACTS]
    with (directory / CERTIFICATE).open("w", encoding="utf-8", newline="\n") as out:
        out.writelines(lines)


def verify(directory):
    errors = []
    entries = {}
    try:
        lines = (directory / CERTIFICATE).read_text(encoding="utf-8").splitlines()
    except (OSError, UnicodeError) as exc:
        print(f"ERROR {CERTIFICATE}: {exc}")
        return 1
    for number, line in enumerate(lines, 1):
        if not line.strip():
            continue
        match = re.fullmatch(r"([0-9a-fA-F]{64})  (\S+)", line)
        if not match:
            errors.append(f"{CERTIFICATE}:{number}: formato inválido")
            continue
        digest, name = match.groups()
        if name not in ARTIFACTS:
            errors.append(f"{name}: artefacto no permitido")
        elif name in entries:
            errors.append(f"{name}: nombre duplicado")
        else:
            entries[name] = digest.lower()
    for name in ARTIFACTS:
        if name not in entries:
            errors.append(f"{name}: ausente del certificado")
            continue
        try:
            actual = artifact_hash(directory / name)
            if actual != entries[name]:
                errors.append(f"{name}: SHA-256 incorrecto; esperado={entries[name]}, actual={actual}")
            else:
                print(f"OK {name}: {actual}")
        except (OSError, ValueError) as exc:
            errors.append(f"{name}: {exc}")
    for error in errors:
        print(f"ERROR {error}")
    return 1 if errors else 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--directorio", type=Path, default=DEFAULT_DIRECTORY)
    args = parser.parse_args()
    return verify(args.directorio)


if __name__ == "__main__":
    sys.exit(main())
