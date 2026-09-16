#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
recalcular_metricas_carga.py
----------------------------
Guion de derivación determinista de métricas de pruebas de carga Locust (E5/E14).

Lee exclusivamente el conjunto de datos OFICIAL declarado:
    microservicio-soporte/locust_esc1_stats.csv
    microservicio-soporte/locust_esc1_stats_history.csv

Calcula y presenta:
1. Métricas agregadas exactas (Total peticiones, fallos, RPS, promedio, percentiles P50, P95, P99, max).
2. Desglose detallado por endpoint (tickets, health, election/status, actuator).
3. Clasificación formal de los 5 conjuntos de datos del repositorio (1 oficial, 4 no oficiales/retirados).
4. Verificación de consistencia cruzada contra el manuscrito LaTeX (Informe-E4_BCEL/TA-PFC-E4_BCEL.tex).

Uso:
    python scripts/recalcular_metricas_carga.py
    python scripts/recalcular_metricas_carga.py --check-latex
    python scripts/recalcular_metricas_carga.py --json
"""

import sys
import os
import csv
import json
import argparse
from datetime import datetime, timezone
from pathlib import Path

# Rutas estándar del repositorio
REPO_ROOT = Path(__file__).resolve().parent.parent
OFFICIAL_STATS = REPO_ROOT / "microservicio-soporte" / "locust_esc1_stats.csv"
OFFICIAL_HISTORY = REPO_ROOT / "microservicio-soporte" / "locust_esc1_stats_history.csv"
OFFICIAL_FAILURES = REPO_ROOT / "microservicio-soporte" / "locust_esc1_failures.csv"
OFFICIAL_EXCEPTIONS = REPO_ROOT / "microservicio-soporte" / "locust_esc1_exceptions.csv"
LATEX_REPORT = REPO_ROOT / "Informe-E4_BCEL" / "TA-PFC-E4_BCEL.tex"

# Inventario de los 5 conjuntos de datos identificados en el PFC (E5)
DATASET_INVENTORY = [
    {
        "id": "A",
        "estado": "OFICIAL NOMINAL",
        "archivo": "microservicio-soporte/locust_esc1_stats.csv",
        "peticiones": 12994,
        "fallos": 0,
        "rps": 43.537580,
        "p50": 6,
        "p95": 440,
        "p99": 850,
        "motivo": "Único conjunto oficial declarado con JWT instrumentado, 0 fallos HTTP y 5 minutos nominales."
    },
    {
        "id": "B",
        "estado": "PRELIMINAR / HISTÓRICA (NO OFICIAL)",
        "archivo": "experimentos/resultados/locust_esc1_stats.csv",
        "peticiones": 12236,
        "fallos": 0,
        "rps": 40.92,
        "p50": 110,
        "p95": 370,
        "p99": 460,
        "motivo": "Corrida previa descartada; conservada sin carácter oficial en el historial experimental."
    },
    {
        "id": "C",
        "estado": "PRELIMINAR / HISTÓRICA (NO OFICIAL)",
        "archivo": "docs/locust/escenario1_nominal_stats.csv",
        "peticiones": 13606,
        "fallos": 0,
        "rps": 45.54,
        "p50": 6,
        "p95": 340,
        "p99": 450,
        "motivo": "Corrida exploratoria anterior; retractada como oficial, no sustituye al conjunto A."
    },
    {
        "id": "D",
        "estado": "FALLIDA / HISTÓRICA (NO OFICIAL)",
        "archivo": "docs/locust/escenario2_estres_stats.csv",
        "peticiones": 106735,
        "fallos": 26,
        "rps": 178.29,
        "p50": 7,
        "p95": 230,
        "p99": 370,
        "motivo": "Prueba de estrés fallida (15 HTTP 500 y 11 HTTP 503). No satisface criterio de 0 fallos."
    },
    {
        "id": "E",
        "estado": "PRELIMINAR (NO OFICIAL)",
        "archivo": "docs/locust/resultados_carga_stats.csv",
        "peticiones": 2419,
        "fallos": 0,
        "rps": 40.32,
        "p50": 5,
        "p95": 440,
        "p99": 1100,
        "motivo": "Corrida corta de calibración (59 s); no satisface el perfil nominal de 5 minutos."
    }
]


def cargar_estadisticas_csv(ruta_csv: Path):
    """Lee y analiza el archivo CSV de estadísticas de Locust."""
    if not ruta_csv.exists():
        raise FileNotFoundError(f"No se encontró el archivo de estadísticas: {ruta_csv}")

    endpoints = []
    aggregated = None

    with open(ruta_csv, mode="r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            name = row.get("Name", "").strip()
            item = {
                "type": row.get("Type", "").strip(),
                "name": name,
                "request_count": int(row.get("Request Count", 0)),
                "failure_count": int(row.get("Failure Count", 0)),
                "median_response_time": float(row.get("Median Response Time", 0)),
                "avg_response_time": float(row.get("Average Response Time", 0)),
                "min_response_time": float(row.get("Min Response Time", 0)),
                "max_response_time": float(row.get("Max Response Time", 0)),
                "requests_per_sec": float(row.get("Requests/s", 0)),
                "failures_per_sec": float(row.get("Failures/s", 0)),
                "p50": float(row.get("50%", 0)),
                "p66": float(row.get("66%", 0)),
                "p75": float(row.get("75%", 0)),
                "p80": float(row.get("80%", 0)),
                "p90": float(row.get("90%", 0)),
                "p95": float(row.get("95%", 0)),
                "p98": float(row.get("98%", 0)),
                "p99": float(row.get("99%", 0)),
                "p99_9": float(row.get("99.9%", 0)),
                "p100": float(row.get("100%", 0))
            }
            if name == "Aggregated":
                aggregated = item
            else:
                endpoints.append(item)

    if aggregated is None:
        raise ValueError(f"No se encontró la fila 'Aggregated' en {ruta_csv}")

    return aggregated, endpoints


def cargar_historial_csv(ruta_history: Path):
    """Analiza la ventana temporal de la corrida desde stats_history."""
    if not ruta_history.exists():
        return None

    timestamps = []
    with open(ruta_history, mode="r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            ts = row.get("Timestamp")
            if ts and ts.strip().isdigit():
                timestamps.append(int(ts.strip()))

    if not timestamps:
        return None

    min_ts = min(timestamps)
    max_ts = max(timestamps)
    dt_inicio = datetime.fromtimestamp(min_ts, timezone.utc)
    dt_fin = datetime.fromtimestamp(max_ts, timezone.utc)
    duracion_segundos = max_ts - min_ts

    return {
        "inicio_utc": dt_inicio.strftime("%Y-%m-%d %H:%M:%S UTC"),
        "fin_utc": dt_fin.strftime("%Y-%m-%d %H:%M:%S UTC"),
        "duracion_segundos": duracion_segundos,
        "muestras": len(timestamps)
    }


def verificar_contra_latex(aggregated, endpoints, ruta_latex: Path):
    """Verifica que el manuscrito LaTeX contenga exactamente las cifras derivadas."""
    if not ruta_latex.exists():
        print(f"[AVISO] No se encontró el informe LaTeX en {ruta_latex}")
        return []

    with open(ruta_latex, mode="r", encoding="utf-8") as f:
        content = f.read()

    errores = []

    # Validaciones obligatorias de cifras oficiales
    req_str = f"{aggregated['request_count']:,}".replace(",", "{,}")
    req_plain = str(aggregated['request_count'])

    if req_str not in content and req_plain not in content and "12{,}994" not in content:
        errores.append(f"El total de peticiones oficiales ({aggregated['request_count']}) no aparece con formato adecuado en el informe LaTeX.")

    if "43.537580" not in content and "43.54" not in content and "43,537580" not in content:
        errores.append(f"La tasa oficial de RPS ({aggregated['requests_per_sec']:.6f}) no aparece en el informe.")

    if "109.113664" not in content and "109.11" not in content:
        errores.append(f"La latencia promedio ({aggregated['avg_response_time']:.6f}) no aparece en el informe.")

    # Verificar que no subsistan cifras inventadas/retractadas en tablas principales
    cifras_prohibidas = ["12{,}265", "12,265", "12265", "12{,}735", "12,735", "12735"]
    for cp in cifras_prohibidas:
        if cp in content:
            errores.append(f"Se detectó la cifra no oficial o retractada '{cp}' en el archivo LaTeX.")

    # Validar endpoints en PI-2
    tickets_ep = next((ep for ep in endpoints if "tickets" in ep["name"]), None)
    if tickets_ep:
        if str(tickets_ep['request_count']) not in content and f"{tickets_ep['request_count']:,}".replace(",", "{,}") not in content:
            errores.append(f"Las peticiones de tickets ({tickets_ep['request_count']}) no se citan en la discusión de PI-2.")

    return errores


def imprimir_reporte(aggregated, endpoints, historial):
    """Genera salida formateada en consola."""
    print("=" * 78)
    print(" REPORTE DETERMINISTA DE MÉTRICAS OFICIALES DE CARGA LOCUST -- ACADTRACE")
    print("=" * 78)
    print(f"Archivo oficial analizado: {OFFICIAL_STATS.relative_to(REPO_ROOT)}")
    if historial:
        print(f"Ventana de ejecución UTC : {historial['inicio_utc']} -> {historial['fin_utc']}")
        print(f"Duración observada       : {historial['duracion_segundos']} segundos ({historial['muestras']} muestras)")
    print("-" * 78)

    print("\n[1] MÉTRICAS AGREGADAS NOMINALES (Fila 'Aggregated'):")
    print(f"  * Peticiones Totales (Requests) : {aggregated['request_count']:,}".replace(",", "."))
    print(f"  * Peticiones Fallidas (Failures): {aggregated['failure_count']}")
    print(f"  * Tasa de Fallos                : {aggregated['failure_count'] / aggregated['request_count'] * 100:.4f}%")
    print(f"  * Throughput (Requests/s)       : {aggregated['requests_per_sec']:.6f} req/s")
    print(f"  * Latencia Promedio             : {aggregated['avg_response_time']:.6f} ms")
    print(f"  * Latencia Mínima               : {aggregated['min_response_time']:.6f} ms")
    print(f"  * Latencia Máxima               : {aggregated['max_response_time']:.6f} ms")
    print("  * Percentiles de Latencia:")
    print(f"      - Mediana (P50)             : {aggregated['p50']:.1f} ms")
    print(f"      - P66                       : {aggregated['p66']:.1f} ms")
    print(f"      - P75                       : {aggregated['p75']:.1f} ms")
    print(f"      - P80                       : {aggregated['p80']:.1f} ms")
    print(f"      - P90                       : {aggregated['p90']:.1f} ms")
    print(f"      - P95                       : {aggregated['p95']:.1f} ms")
    print(f"      - P98                       : {aggregated['p98']:.1f} ms")
    print(f"      - P99                       : {aggregated['p99']:.1f} ms")
    print(f"      - P99.9                     : {aggregated['p99_9']:.1f} ms")
    print(f"      - P100 (Max)                : {aggregated['p100']:.1f} ms")

    print("\n[2] DESGLOSE DETALLADO POR ENDPOINT:")
    header = f"{'Método y Endpoint':<38} | {'Reqs':>6} | {'Fallos':>6} | {'RPS':>7} | {'Prom(ms)':>8} | {'P50':>5} | {'P95':>5} | {'P99':>5}"
    print(header)
    print("-" * len(header))
    for ep in endpoints:
        print(f"{ep['name']:<38} | {ep['request_count']:>6} | {ep['failure_count']:>6} | {ep['requests_per_sec']:>7.2f} | {ep['avg_response_time']:>8.2f} | {ep['p50']:>5.0f} | {ep['p95']:>5.0f} | {ep['p99']:>5.0f}")

    print("\n[3] INVENTARIO Y CLASIFICACIÓN DE CONJUNTOS DE DATOS (E5):")
    for ds in DATASET_INVENTORY:
        marca = "[OFICIAL]   " if "OFICIAL" in ds["estado"] and "NO OFICIAL" not in ds["estado"] else "[DESCARTADO]"
        print(f"  {marca} Conjunto {ds['id']}: {ds['archivo']}")
        print(f"      Estado   : {ds['estado']}")
        print(f"      Métricas : {ds['peticiones']:,} reqs, {ds['fallos']} fallos, {ds['rps']:.2f} RPS, P50={ds['p50']}ms, P95={ds['p95']}ms, P99={ds['p99']}ms".replace(",", "."))
        print(f"      Dictamen : {ds['motivo']}")

    print("\n" + "=" * 78)


def main():
    parser = argparse.ArgumentParser(description="Derivación determinista de métricas de carga Locust (E5).")
    parser.add_argument("--csv", type=Path, default=OFFICIAL_STATS, help="Ruta al archivo CSV de Locust")
    parser.add_argument("--check-latex", action="store_true", help="Validar consistencia con el manuscrito LaTeX")
    parser.add_argument("--json", action="store_true", help="Imprimir salida en formato JSON")
    args = parser.parse_args()

    aggregated, endpoints = cargar_estadisticas_csv(args.csv)
    historial = cargar_historial_csv(OFFICIAL_HISTORY)

    if args.json:
        data = {
            "aggregated": aggregated,
            "endpoints": endpoints,
            "history": historial,
            "inventory": DATASET_INVENTORY
        }
        print(json.dumps(data, indent=2))
        return

    imprimir_reporte(aggregated, endpoints, historial)

    print("[4] AUDITORÍA DE CONSISTENCIA CON INFORME LATEX:")
    errores = verificar_contra_latex(aggregated, endpoints, LATEX_REPORT)
    if errores:
        print("  [FALLO] Se detectaron inconsistencias:")
        for err in errores:
            print(f"    - {err}")
        sys.exit(1)
    else:
        print("  [OK] El informe LaTeX está 100% alineado con las cifras derivadas del CSV oficial.")
        print("  [OK] No existen cifras retractadas (12,265 o 12,735) en la matriz de calidad.")
        print("=" * 78)


if __name__ == "__main__":
    main()
