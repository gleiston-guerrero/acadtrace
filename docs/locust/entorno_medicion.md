# Entorno de Medición y Metadatos de Pruebas de Carga (Locust) — Entrega 4

Este documento registra formalmente los metadatos de ejecución, especificaciones del entorno de pruebas y las firmas criptográficas SHA-256 de los conjuntos de datos obtenidos durante la evaluación de rendimiento y resiliencia del sistema **AcadTrace**.

---

## 1. Especificaciones del Entorno de Medición

| Parámetro | Valor Registrado |
|---|---|
| **Sistema Operativo del Host** | Microsoft Windows 11 Pro 64-bit |
| **Procesador (CPU)** | AMD Ryzen 5 7520U with Radeon Graphics (4 núcleos, 8 hilos, 2.80 GHz base) |
| **Memoria RAM del Host** | 16.0 GB LPDDR5 (15.24 GB utilizable) |
| **Almacenamiento** | SSD NVMe PCIe M.2 512 GB |
| **Versión de Python** | 3.14.6 (entorno local de carga) / 3.12.3 (contenedor Docente) |
| **Versión de Locust** | 2.46.4 |
| **Fecha de Medición Oficial** | 2026-09-06T15:30:00-05:00 |
| **Topología Desplegada** | Clúster contenerizado Docker Compose (HAProxy 2.9, SGA Principal, Soporte, Docente, Secretaría, etcd, PostgreSQL 16) |

---

## 2. Parámetros de los Escenarios de Carga

1. **Escenario 1 (Carga Nominal):**
   - **Usuarios Concurrentes:** 50 usuarios simultáneos.
   - **Tasa de Concurrencia (Spawn Rate):** 5 usuarios/segundo.
   - **Duración de la Corrida:** 5 minutos (300 segundos).
   - **Comando:** `locust -f tests/load/locustfile.py --headless -u 50 -r 5 -t 5m --csv=docs/locust/escenario1_nominal`

2. **Escenario 2 (Estrés Escalonado):**
   - **Usuarios Concurrentes:** Rampa progresiva hasta 200 usuarios concurrentes.
   - **Tasa de Concurrencia (Spawn Rate):** 10 usuarios/segundo.
   - **Duración de la Corrida:** 10 minutos (600 segundos).
   - **Comando:** `locust -f tests/load/locustfile.py --headless -u 200 -r 10 -t 10m --csv=docs/locust/escenario2_estres`

---

## 3. Firmas Criptográficas (SHA-256) de los Resultados CSV

| Archivo | Hash SHA-256 |
|---|---|
| `docs/locust/escenario1_nominal_stats.csv` | `D78900791C05745794BED0787AF8B3AE3EC4B0544453B96ABB8578B92666D9B1` |
| `docs/locust/escenario2_estres_stats.csv` | `DF099D3AEB6030FB9730D0853594262F3FFCC318783B5EB3270B1092BE9CE8FA` |

---

## 4. Síntesis de Resultados Empíricos Medidos

- **Escenario 1 (Carga Nominal - 50 usuarios, 5 min):**
  - **Peticiones Totales:** 13,606 peticiones procesadas.
  - **Throughput Promedio:** 45.54 RPS.
  - **Tasa de Errores:** 0.0% (0 fallos).
  - **Latencia Mediana (P50):** 6 ms.
  - **Latencia P95:** 340 ms.
  - **Latencia P99:** 450 ms.
  - **Disponibilidad:** 100.0%.

- **Escenario 2 (Estrés Oficial - Rampa 0 a 200 usuarios, 10 min):**
  - **Peticiones Totales:** 106,735 peticiones procesadas.
  - **Throughput Promedio:** 178.29 RPS.
  - **Tasa de Éxito / Disponibilidad:** 99.98% (solo 26 fallos registrados bajo saturación pico, tasa de error 0.024%).
  - **Latencia Mediana (P50):** 7 ms.
  - **Latencia P95:** 230 ms.
  - **Latencia P99:** 370 ms.
  - **Hallazgo Metodológico:** Una ejecución preliminar ejecutada el 2026-09-05 experimentó fallos HTTP debido a la detención anómala del contenedor de soporte (no por degradación intrínseca de red). Tras estabilizar el contenedor y asegurar el pool HikariCP, la re-ejecución oficial del 6 de septiembre procesó 106,735 peticiones demostrando alta resiliencia.
