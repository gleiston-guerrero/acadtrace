# Entorno de Medición y Metadatos de Pruebas de Carga (Locust) — Entrega 4

> **E5 vigente:** la única corrida OFICIAL NOMINAL de Soporte es `microservicio-soporte/locust_esc1_stats.csv` y sus tres CSV asociados. Ver [registro, métricas y trazabilidad E5](../../experimentos/resultados/corridas-e5.md).
>
> CSV oficial: **12.994 peticiones, 0 fallos, 43,537580 req/s, promedio 109,113664 ms, P50 6 ms, P95 440 ms, P99 850 ms y máximo 2.037,104700 ms**. Prueba de carga reproducible ejecutada en entorno local/contenedorizado; perfil configurado: 50 usuarios virtuales, 5 usuarios/s, 5 minutos, localhost:8083. Inicio registrado: 2026-09-11 03:59:05 UTC. Commit de conservación `956cafcb` (normalización posterior `c5c0e6f5`); commit del código ejecutado: **No disponible en la evidencia conservada**.
>
> **Estrés oficial: NO DISPONIBLE — las evidencias conservadas no satisfacen el criterio.** Las secciones siguientes son antecedentes HISTÓRICOS: C es PRELIMINAR y D es FALLIDA (26 fallos). Sus metadatos y hashes históricos no certifican A. La fecha histórica declarada abajo no coincide con el inicio de C en CSV: 2026-09-05 00:05:03 UTC.
>
> La captura de Juliana con 13.031 peticiones es complementaria; no coincide exactamente con el CSV oficial de 12.994 y la causa no está demostrada. Falta una captura manual de la fila Aggregated del CSV oficial.

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
| **Fecha histórica declarada (no certifica A ni fecha de C)** | 2026-09-06T15:30:00-05:00 |
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

## 4. Resultados históricos, no oficiales E5

- **C — PRELIMINAR/HISTÓRICA (nominal - 50 usuarios, 5 min declarados):**
  - **Peticiones Totales:** 13,606 peticiones procesadas.
  - **Throughput Promedio:** 45.54 RPS.
  - **Tasa de Errores:** 0.0% (0 fallos).
  - **Latencia Mediana (P50):** 6 ms.
  - **Latencia P95:** 340 ms.
  - **Latencia P99:** 450 ms.
  - **Éxito de peticiones en la muestra histórica:** 100.0%; no mide disponibilidad de producción.

- **D — FALLIDA/HISTÓRICA (estrés - hasta 200 usuarios, 10 min declarados):**
  - **Peticiones Totales:** 106,735 peticiones procesadas.
  - **Throughput Promedio:** 178.29 RPS.
  - **Tasa de éxito de peticiones históricas (no disponibilidad de producción):** 99.98% (solo 26 fallos registrados bajo saturación pico, tasa de error 0.024%).
  - **Latencia Mediana (P50):** 7 ms.
  - **Latencia P95:** 230 ms.
  - **Latencia P99:** 370 ms.
  - **Clasificación E5:** FALLIDA: 15 HTTP 500 y 11 HTTP 503. No satisface cero fallos. La corrida anterior conservada en `683cc17f` registró 12.735 fallos con código 0 el 2026-09-04; Locust por sí solo no demuestra la causa raíz. No se declara una reejecución oficial exitosa.
