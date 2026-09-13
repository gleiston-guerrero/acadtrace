import hashlib
import os
import subprocess
import sys
from pathlib import Path


BASE_DIR = Path(__file__).resolve().parent
PREFIX = "locust_esc1"
CERTIFICADO = BASE_DIR / "REPRODUCIBILIDAD.txt"


def ejecutar_locust():
    if not os.getenv("JWT_SECRET"):
        print("ERROR: JWT_SECRET no esta definido en el entorno.")
        sys.exit(1)

    comando = [
        "locust",
        "-f", str(BASE_DIR / "locustfile.py"),
        "--host", "http://localhost:8083",
        "--headless",
        "-u", "50",
        "-r", "5",
        "--run-time", "5m",
        "--csv", str(BASE_DIR / PREFIX),
    ]

    print("Ejecutando prueba oficial Locust:")
    print("50 usuarios - 5 minutos")
    print("JWT_SECRET detectado correctamente.")
    print()

    resultado = subprocess.run(comando)

    if resultado.returncode != 0:
        print("\nERROR: Locust termino con fallos.")
        sys.exit(resultado.returncode)


def generar_certificado():
    archivos = sorted(BASE_DIR.glob(f"{PREFIX}*.csv"))

    if not archivos:
        print("ERROR: No se encontraron archivos CSV.")
        sys.exit(1)

    with CERTIFICADO.open("w", encoding="utf-8", newline="\n") as salida:
        for archivo in archivos:
            sha256 = hashlib.sha256(archivo.read_bytes()).hexdigest()
            salida.write(f"{sha256}  {archivo.name}\n")

    print()
    print("Certificado generado automaticamente:")
    print(CERTIFICADO)

    print()
    print(CERTIFICADO.read_text(encoding="utf-8"))


if __name__ == "__main__":
    ejecutar_locust()
    generar_certificado()