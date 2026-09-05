# Entorno de Medición y Metadatos de Pruebas de Carga (Locust) — Entrega 4

Este documento registra formalmente los metadatos de ejecución, especificaciones del entorno de pruebas y las firmas criptográficas SHA-256 de los conjuntos de datos obtenidos durante la evaluación de rendimiento y resiliencia del sistema **AcadTrace**.

---

## 1. Especificaciones del Entorno de Medición

| Parámetro | Valor Registrado |
|---|---|
| **Fecha y Hora de Medición (UTC)** | 2026-09-05 00:15:40 UTC |
| **Commit Hash del Repositorio** | `b132230b59852b557c0ffdf470aebfa6c20803bd` |
| **Rama Git** | `Juliana-Emanuel` |
| **Sistema Operativo** | Microsoft Windows 11 Pro (x86_64) |
| **Procesador (CPU)** | AMD Ryzen 7 7730U with Radeon Graphics (8 núcleos, 16 hilos @ 2.0 GHz base / 4.5 GHz boost) |
| **Memoria RAM** | 15.34 GB DDR4 |
| **Motor de Contenedores** | Docker Desktop 4.28 / Docker Engine v25.0.3 en WSL2 |
| **Versión de Locust** | Locust 2.24.0 (Python 3.11.8) |
| **Herramienta de Observabilidad** | Prometheus v2.51.0 + Grafana 10.4.0 + cAdvisor v0.49.1 |

---

## 2. Escenarios Ejecutados y Comandos de Prueba

### Escenario 1: Carga Nominal Sostenida
- **Descripción:** 50 usuarios concurrentes simulando navegación, registro y consulta durante 5 minutos.
- **Tasa de aparición (Spawn Rate):** 5 usuarios/segundo.
- **Duración:** 300 segundos (5 minutos).
- **Comando Ejecutado:**
  ```bash
  locust -f tests/load/locustfile.py --headless --host http://localhost:8080 -u 50 -r 5 -t 5m --csv=docs/locust/escenario1_nominal
  ```

### Escenario 2: Prueba de Estrés (Rampa Escalonada de Cierre de Período)
- **Descripción:** Rampa progresiva de 0 a 200 usuarios concurrentes en etapas escalonadas (50, 100, 150, 200 usuarios) simulando saturación por cierre de ciclo lectivo.
- **Duración:** 600 segundos (10 minutos).
- **Comando Ejecutado:**
  ```bash
  locust -f tests/load/locustfile.py -f tests/load/escenario3_cierre_periodo.py --headless --host http://localhost:8080 -t 10m --csv=docs/locust/escenario2_estres
  ```

---

## 3. Firmas Criptográficas (SHA-256) de los Resultados CSV

| Archivo | Hash SHA-256 |
|---|---|
| `docs/locust/escenario1_nominal_stats.csv` | `D78900791C05745794BED0787AF8B3AE3EC4B0544453B96ABB8578B92666D9B1` |
| `docs/locust/escenario2_estres_stats.csv` | `FE394DA7D3EC3F2172F97AEE02FF4699350F97CE091D27A65BF59D28F18B4CAC` |

---

## 4. Síntesis de Resultados Empíricos

- **Escenario 1 (Carga Nominal - 50 usuarios, 5 min):**
  - **Peticiones Totales:** 13,606 peticiones procesadas.
  - **Throughput Promedio:** 45.54 RPS.
  - **Tasa de Errores:** 0.0% (0 fallos).
  - **Latencia Mediana (P50):** 6 ms.
  - **Latencia P95:** 440 ms.
  - **Latencia P99:** 470 ms.

- **Escenario 2 (Estrés - Rampa 0 a 200 usuarios, 10 min):**
  - **Peticiones Totales:** 12,735 peticiones procesadas.
  - **Comportamiento ante saturación:** Degradación controlada por agotamiento de conexiones en endpoints de alta carga transaccional con contención de base de datos, evidenciando el límite de capacidad del pool HikariCP bajo saturación extrema.
