#!/usr/bin/env python3
"""
Arnes ejecutable de conformidad canonica v1 (12 vectores).

Compila e invoca las TRES implementaciones reales del repositorio sobre los
mismos 12 vectores de entrada (experimentos/vectores_canonicos_v1.json):
  - AuditHashService de sga-principal (Java), via CanonicoRunner + subprocess.
  - AuditHashService de secretaria (Java), via CanonicoRunner + subprocess.
  - docentes/auditoria/hashing.py (Python), importado directo por ruta.

No simula ningun lenguaje. Todos los porcentajes se calculan
aritmeticamente (coincidentes / total * 100) sobre la salida medida.
Las aserciones finales hacen fallar el arnes (exit 1) si Java Principal
deja de coincidir con Java Secretaria en algun vector, o si el conteo
Java == Python cambia respecto a la evidencia congelada EXPECTED_PYP.
"""

import importlib.util
import json
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
HARNESS_BUILD = REPO_ROOT / "experimentos" / "java_harness" / "build"
VECTORES_PATH = REPO_ROOT / "experimentos" / "vectores_canonicos_v1.json"
HASHING_PATH = (
    REPO_ROOT / "microservicio-docente" / "docentes" / "auditoria" / "hashing.py"
)

FQN_PRINCIPAL = "ec.edu.uteq.sga.application.service.AuditHashService"
FQN_SECRETARIA = "ec.uteq.sga.secretaria.application.service.AuditHashService"

# Evidencia congelada: coincidencias Java == Python medidas en la ejecucion
# de referencia. Si el codigo Java o Python cambia y este conteo varia, el
# arnes falla con exit 1 (deteccion de regresion/divergencia nueva).
EXPECTED_PYP = 8


def cargar_hashing():
    """Carga el modulo REAL hashing.py por ruta, sin reimplementarlo."""
    spec = importlib.util.spec_from_file_location("hashing_real", HASHING_PATH)
    modulo = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(modulo)
    return modulo


def localizar_jars():
    """Resuelve el classpath minimo (Jackson + spring-context) desde ~/.m2."""
    m2 = Path.home() / ".m2" / "repository"
    piezas = [
        "com/fasterxml/jackson/core/jackson-core/2.15.4/jackson-core-2.15.4.jar",
        "com/fasterxml/jackson/core/jackson-annotations/2.15.4"
        "/jackson-annotations-2.15.4.jar",
        "com/fasterxml/jackson/core/jackson-databind/2.15.4"
        "/jackson-databind-2.15.4.jar",
        "com/fasterxml/jackson/datatype/jackson-datatype-jsr310/2.15.4"
        "/jackson-datatype-jsr310-2.15.4.jar",
        "org/springframework/spring-context/6.1.6/spring-context-6.1.6.jar",
    ]
    jars = []
    for pieza in piezas:
        ruta = m2 / pieza
        if not ruta.is_file():
            raise SystemExit(f"ERROR: jar no encontrado en ~/.m2: {pieza}")
        jars.append(str(ruta))
    import os

    return os.pathsep.join([str(HARNESS_BUILD)] + jars)


def decodificar(valor):
    """Decodifica marcadores tipados al valor nativo de Python."""
    if isinstance(valor, dict):
        if set(valor.keys()) == {"$nan"} and valor["$nan"] is True:
            return float("nan")
        return {str(k): decodificar(v) for k, v in valor.items()}
    if isinstance(valor, list):
        return [decodificar(v) for v in valor]
    return valor


def ejecutar_java(vectores_doc, classpath):
    """Invoca CanonicoRunner (clases Java reales) y devuelve su stdout."""
    carga = json.dumps(vectores_doc, ensure_ascii=False).encode("utf-8")
    proc = subprocess.run(
        ["java", "-cp", classpath, "CanonicoRunner"],
        input=carga,
        capture_output=True,
        check=True,
    )
    return json.loads(proc.stdout.decode("utf-8"))


def main():
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass
    vectores_doc = json.loads(VECTORES_PATH.read_text(encoding="utf-8"))
    vectores = vectores_doc["vectores"]
    total = len(vectores)

    hashing = cargar_hashing()
    classpath = localizar_jars()
    print(f"Runner Java: {FQN_PRINCIPAL} + {FQN_SECRETARIA}")
    salida_java = ejecutar_java(vectores_doc, classpath)
    canon_principal = salida_java["principal"]
    canon_secretaria = salida_java["secretaria"]
    assert len(canon_principal) == total and len(canon_secretaria) == total

    coincidentes_ps = 0
    coincidentes_pyp = 0
    divergentes = []

    print()
    encabezado_id = "ID"
    encabezado_vec = "Vector"
    encabezado_ps = "P = S"
    encabezado_trip = "P = S = Py"
    encabezado_est = "Estado"
    print(
        f"{encabezado_id:<3} | {encabezado_vec:<38} | "
        f"{encabezado_ps:<7} | {encabezado_trip:<10} | {encabezado_est:<12}"
    )
    print("-" * 78)

    for vector, canon_p, canon_s in zip(vectores, canon_principal, canon_secretaria):
        vid = vector["id"]
        nombre = vector["nombre"][:38]
        canon_py = hashing.json_canonico(decodificar(vector["contenido"]))
        igual_ps = canon_p == canon_s
        igual_trip = igual_ps and canon_p == canon_py
        if igual_ps:
            coincidentes_ps += 1
        if igual_trip:
            coincidentes_pyp += 1
            estado = "COINCIDE"
        else:
            estado = "DIVERGE"
            if not igual_ps:
                divergentes.append((vid, "P-S", canon_p, canon_s))
            else:
                divergentes.append((vid, "P-S-Py", canon_p, canon_py))
        marca_ps = "SI" if igual_ps else "NO"
        marca_trip = "SI" if igual_trip else "NO"
        print(f"{vid:<3} | {nombre:<38} | {marca_ps:<7} | {marca_trip:<10} | {estado:<12}")

    print("-" * 78)
    pct_ps = coincidentes_ps / total * 100
    pct_pyp = coincidentes_pyp / total * 100
    print("Resumen de evaluacion de equivalencia canonica (calculado):")
    print(f"  * Principal == Secretaria (Java == Java): {coincidentes_ps}/{total} ({pct_ps:.1f}%)")
    print(f"  * Principal == Secretaria == Python:      {coincidentes_pyp}/{total} ({pct_pyp:.1f}%)")
    print(f"  * Vectores con alguna divergencia:        {total - coincidentes_pyp}/{total}")

    for vid, clase, sal_a, sal_b in divergentes:
        print(f"  - Vector {vid} [{clase}]:")
        print(f"      A: {sal_a}")
        print(f"      B: {sal_b}")

    assert coincidentes_ps == total, (
        f"regresion: Java Principal != Java Secretaria "
        f"en {total - coincidentes_ps} vectores"
    )
    assert coincidentes_pyp == EXPECTED_PYP, (
        f"regresion: Java == Python cambio de {EXPECTED_PYP} "
        f"a {coincidentes_pyp}"
    )
    print("Aserciones OK: sin regresion respecto a la evidencia congelada.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
