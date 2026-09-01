#!/usr/bin/env python3
"""
Escenario 1: Carga Nominal Sostenida
Parámetros:
  - Usuarios concurrentes: 50
  - Tasa de aparición (Spawn rate): 5 usuarios/s
  - Duración: 5 minutos (300 segundos)
  - Umbral SLA: Latencia P95 < 500 ms, Tasa de error < 1%
"""

import os
import sys
import subprocess

def run_escenario_1(host="http://localhost:8080"):
    print("=" * 70)
    print("=== EJECUTANDO ESCENARIO 1: CARGA NOMINAL (50 USUARIOS x 5 MIN) ===")
    print("=" * 70)
    print(f"Destino (Host): {host}")
    print("Concurrencia: 50 usuarios virtuales")
    print("Tasa de spawn: 5 usuarios/segundo")
    print("Duración: 300 segundos (5 minutos)")
    print("-" * 70)

    locustfile_path = os.path.join(os.path.dirname(__file__), "locustfile.py")
    results_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "../../experimentos/resultados"))
    os.makedirs(results_dir, exist_ok=True)
    csv_prefix = os.path.join(results_dir, "locust_esc1")

    cmd = [
        sys.executable, "-m", "locust",
        "-f", locustfile_path,
        "--headless",
        "--host", host,
        "-u", "50",
        "-r", "5",
        "-t", "5m",
        "--csv", csv_prefix,
        "--html", os.path.join(results_dir, "reporte_escenario1.html")
    ]

    print(f"Ejecutando comando: {' '.join(cmd)}")
    result = subprocess.run(cmd)
    
    if result.returncode == 0:
        print("\n[OK] Escenario 1 completado exitosamente.")
        print(f"Reportes guardados en: {results_dir}")
    else:
        print(f"\n[AVISO] Locust finalizó con código {result.returncode}.")

if __name__ == "__main__":
    target_host = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080"
    run_escenario_1(target_host)
