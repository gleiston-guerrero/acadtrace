# Entorno de Medición y Metadatos de Pruebas de Carga (Locust) — Entrega 4

> **E5 vigente:** la corrida OFICIAL NOMINAL de Soporte es `microservicio-soporte/locust_esc1_stats.csv` y `microservicio-soporte/locust_esc1_stats_history.csv`. Ver [registro, métricas y trazabilidad E5](../../experimentos/resultados/corridas-e5.md).
>
> CSV oficial: **12.217 peticiones, 0 fallos, 40,955420 req/s, promedio 10,181439 ms, P50 4 ms, P95 9 ms, P99 23 ms y máximo 47889,955900 ms**. Prueba de carga reproducible ejecutada en entorno local/contenedorizado; perfil configurado: 50 usuarios virtuales, 5 usuarios/s, 5 minutos, localhost:8083. Historial actual: 2026-09-20 08:46:16–08:51:14 UTC (03:46:16–03:51:14 UTC−05:00), 298 segundos entre muestras. Perfil validado manualmente; 5 minutos configurados. PI-1: P99 < 500 ms, cumple en escenario nominal local. Capturas nominales conservadas en `332158e4`; commit del código ejecutado: **No disponible en la evidencia conservada**.
>
> **Estrés oficial: DISPONIBLE y CUMPLE.** Corrida `20260920_164722`: 200 usuarios máximos, 98.684 peticiones, 0 fallos y P95=15 ms < 500 ms; código `88649f3f`, evidencia `b43008e5`. Las secciones siguientes son antecedentes HISTÓRICOS: C es PRELIMINAR y D es FALLIDA (26 fallos). Sus metadatos y hashes históricos no certifican A. La fecha histórica declarada abajo no coincide con el inicio de C en CSV: 2026-09-05 00:05:03 UTC.
>
> Se conserva la descripción histórica de la captura de Juliana con 13.031 peticiones, pero no el archivo de imagen versionado; no coincide exactamente con el CSV histórico anterior de 12.994 y la causa no está demostrada. Las capturas nominales actuales de CSV, matriz e historial están conservadas en `332158e4`; falta únicamente la evidencia original de configuración del perfil nominal.

Este documento registra formalmente los metadatos de ejecución, especificaciones del entorno de pruebas y las firmas criptográficas SHA-256 de los conjuntos de datos obtenidos durante la evaluación de rendimiento y resiliencia del sistema **AcadTrace**.

---

La corrida anterior registró P99=850 ms y no cumplía PI-1; se conserva en [el registro E5](../../experimentos/resultados/corridas-e5.md). No está demostrada la equivalencia exacta de datasets y entornos, ni causalidad exclusiva de `JwtParser`. El máximo actual de 47.889,96 ms corresponde a tickets (P95=13 ms, P99=32 ms, 0 fallos), conservado sin filtrar. No se demuestra disponibilidad de producción. El `console.log` y el `validation.json` candidatos pertenecen a una corrida anterior y no acreditan la actual; los auxiliares vacíos tampoco acreditan su procedencia.

## Entorno de la corrida oficial de estrés vigente

Ruta: `microservicio-soporte/resultados_estres/20260920_164722/`. El `perfil.txt` registra host `http://localhost:8085`, código `88649f3f`, 200 usuarios máximos, spawn rate de 1 usuario/s, 10 minutos configurados y `EXIT_CODE=0`. El historial abarca timestamps 1789940845–1789941444 (599 s). La evidencia se conservó en `b43008e5`, incluida `E48_estres_200_resumen.png`. Las métricas completas están en [el registro E5](../../experimentos/resultados/corridas-e5.md). No se atribuyen a esta corrida las especificaciones históricas siguientes ni se acredita el estado de HikariCP.

## 1. Especificaciones históricas del Entorno de Medición

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

## 2. Parámetros históricos de los Escenarios de Carga

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

### 3.1. Conjunto Oficial Declarado (Corrida A)
| Archivo | Hash SHA-256 |
|---|---|
| `microservicio-soporte/locust_esc1_stats.csv` | `9EF6F59D131AA9D4FAB42A9542A18BD25AEC2E4861AA4BF361A942E59AC16A95` |
| `microservicio-soporte/resultados_historicos/locust_esc1_historico_p99_850ms_stats.csv` | `8A76EEA34413AD186C014FEBC594ACBB0353DA6ABB81D750F1D32A8C621528CB` |
| `microservicio-soporte/locust_esc1_stats_history.csv` | `2E07E6788BC9BE8FDBC7E3743670CFA33AA7A6BF3C9B748C489718358E6D1E34` |
| `microservicio-soporte/resultados_historicos/locust_esc1_historico_p99_850ms_stats_history.csv` | `317409E18325BD454248029443E9EBB6039FB41E451502215E410367BF526116` |
| Histórico, no actual: `microservicio-soporte/locust_esc1_failures.csv` | `48EA7DC61427ABBA01680829DD9FB55B50A69604F28AE3139E85D888B289349B` |
| Histórico, no actual: `microservicio-soporte/locust_esc1_exceptions.csv` | `6DBA11106E7EB84C71D85B91CB592276309D8B2D485BA6EA8E82DA18E6ED7663` |

### 3.2. Hashes de conjuntos no oficiales B, C, D y F (E se identifica en el registro E5)
| Archivo | Hash SHA-256 |
|---|---|
| `experimentos/resultados/locust_esc1_stats.csv` | `FC63845A59A397EAEA6E9EA15746BCDEBEA7A479EB09582B4691B016371903EA` |
| `docs/locust/escenario1_nominal_stats.csv` | `539C7F827F950FE572178A8CED7E25ACD3976E5C0289066896ECEC870B323B03` |
| `docs/locust/escenario2_estres_stats.csv` | `2AD7C788EA7E13DD48424F591DD170E2A8B3C7600C1EA0FAF24BFC25BB06B143` |
| `docs/locust/resultados_carga_stats.csv` | `D9C99B33A05DA637B7C1AEB2743FD5E2F98FFF06FAC4CDB8FCC9C15D9B501AE6` |

> Todas las cifras oficiales pueden reproducirse deterministamente con el guion `scripts/recalcular_metricas_carga.py`.

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
  - **Clasificación E5:** FALLIDA: 15 HTTP 500 y 11 HTTP 503. No satisface cero fallos. La corrida anterior conservada en `683cc17f` registró 12.735 fallos con código 0 el 2026-09-04; Locust por sí solo no demuestra la causa raíz. Esta corrida D permanece fallida; la corrida oficial vigente es una ejecución distinta, conservada en `b43008e5`.
