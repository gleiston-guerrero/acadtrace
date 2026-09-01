#!/usr/bin/env python3
"""
Escenario 3: Cierre de Período Académico (Rampa de Estrés)
Parámetros:
  - Rampa escalonada: 0 a 200 usuarios concurrentes
  - Tasa de incremento: Escalones progresivos (20, 50, 100, 150, 200 usuarios)
  - Duración total: 10 minutos (600 segundos)
  - Umbral SLA: Latencia P95 < 500 ms, Tasa de error < 1%
"""

import os
import sys
import subprocess
from locust import LoadTestShape

class RampTo200Shape(LoadTestShape):
    """
    Controlador de forma de carga que incrementa progresivamente los usuarios:
      - 0 a 2 min (0-120s): Rampa a 50 usuarios
      - 2 a 4 min (120-240s): Rampa a 100 usuarios
      - 4 a 6 min (240-360s): Rampa a 150 usuarios
      - 6 a 8 min (360-480s): Rampa a 200 usuarios (pico de estrés)
      - 8 a 10 min (480-600s): Mantenimiento en 200 usuarios
      - > 10 min: Detener prueba
    """
    stages = [
        {"duration": 120, "users": 50, "spawn_rate": 2},
        {"duration": 240, "users": 100, "spawn_rate": 2},
        {"duration": 360, "users": 150, "spawn_rate": 2},
        {"duration": 480, "users": 200, "spawn_rate": 2},
        {"duration": 600, "users": 200, "spawn_rate": 1},
    ]

    def tick(self):
        run_time = self.get_run_time()
        for stage in self.stages:
            if run_time < stage["duration"]:
                tick_data = (stage["users"], stage["spawn_rate"])
                return tick_data
        return None


def run_escenario_3(host="http://localhost:8080"):
    print("=" * 70)
    print("=== EJECUTANDO ESCENARIO 3: CIERRE DE PERÍODO (RAMPA 0-200 x 10 MIN) ===")
    print("=" * 70)
    print(f"Destino (Host): {host}")
    print("Perfil de Carga: Rampa escalonada (0 -> 50 -> 100 -> 150 -> 200 usuarios)")
    print("Duración: 600 segundos (10 minutos)")
    print("-" * 70)

    this_file = os.path.abspath(__file__)
    locustfile_path = os.path.join(os.path.dirname(__file__), "locustfile.py")
    results_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "../../experimentos/resultados"))
    os.makedirs(results_dir, exist_ok=True)
    csv_prefix = os.path.join(results_dir, "locust_esc3")

    cmd = [
        sys.executable, "-m", "locust",
        "-f", locustfile_path,
        "--headless",
        "--host", host,
        "-t", "10m",
        "--csv", csv_prefix,
        "--html", os.path.join(results_dir, "reporte_escenario3.html")
    ]

    print(f"Ejecutando comando: {' '.join(cmd)}")
    result = subprocess.run(cmd)
    
    if result.returncode == 0:
        print("\n[OK] Escenario 3 completado exitosamente.")
        print(f"Reportes guardados en: {results_dir}")
    else:
        print(f"\n[AVISO] Locust finalizó con código {result.returncode}.")

if __name__ == "__main__":
    target_host = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080"
    run_escenario_3(target_host)
