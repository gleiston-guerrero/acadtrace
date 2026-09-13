# E5 — Registro de corridas de carga

## Declaración y alcance

**OFICIAL NOMINAL de Soporte: conjunto A.** Las métricas oficiales proceden exclusivamente de la fila `Aggregated` del CSV conservado. Es una **prueba de carga reproducible ejecutada en entorno local/contenedorizado**, con usuarios virtuales; no representa tráfico real de producción.

**Corrida oficial de estrés: NO DISPONIBLE — las evidencias conservadas no satisfacen el criterio** de cero fallos. E5 permanece **PARCIAL** por la ausencia de estrés válido y de una captura exacta del agregado oficial.

El perfil de [run_locust.py](../../microservicio-soporte/run_locust.py) configura 50 usuarios virtuales, spawn rate de 5 usuarios/s, duración de 5 minutos y host `http://localhost:8083`. El [locustfile.py](../../microservicio-soporte/locustfile.py) envía JWT a los endpoints protegidos y registra como fallos las respuestas HTTP 401 y 500. El conjunto A contiene cero fallos y ninguna excepción registrada.

El **commit de conservación del resultado** identifica una versión de Git que contiene el artefacto; no demuestra qué código estaba desplegado al ejecutarlo. Para todas las corridas, el **commit del código ejecutado** es: **No disponible en la evidencia conservada**. Tampoco se atribuyen a A el hardware ni las versiones descritas en documentos de otras campañas.

## Tabla de trazabilidad

Las fechas A–F son el primer timestamp de `stats_history`, expresado en UTC. Usuarios significa máximo observado. La duración observada es el intervalo entre primera y última muestra, no la duración exacta del proceso. ND significa literalmente **No disponible en la evidencia conservada**. Los percentiles están en ms.

| Estado | Escenario | Fecha UTC | Commit de conservación | Entorno | Usuarios | Duración | Peticiones | Fallos | P50 | P95 | P99 | Archivo | Motivo de aceptación/descarte |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **A — OFICIAL NOMINAL** | Nominal Soporte | 2026-09-11 03:59:05 | `956cafcb` (datos); `c5c0e6f5` (normalización posterior) | Local/contenedorizado; localhost:8083 configurado | 50 | 5 min configurados; 299 s entre muestras | **12.994** | **0** | **6** | **440** | **850** | [A stats](../../microservicio-soporte/locust_esc1_stats.csv) | Autenticación instrumentada; 0 fallos, sin 401/500 registrados. Único nominal seleccionado. |
| B — PRELIMINAR/HISTÓRICA | Nominal anterior | 2026-09-11 03:32:11 | `df0112d3` | ND | 50 | 299 s entre muestras | 12.236 | 0 | 110 | 370 | 460 | [B stats](locust_esc1_stats.csv) | Anterior a A; conservada sin carácter oficial. |
| C — PRELIMINAR/HISTÓRICA | Nominal anterior | 2026-09-05 00:05:03 | `683cc17f` | Local/contenedorizado declarado en documento histórico; host efectivo ND | 50 | 5 min declarados; 298 s entre muestras | 13.606 | 0 | 6 | 340 | 450 | [C stats](../../docs/locust/escenario1_nominal_stats.csv) | Se retira la declaración oficial anterior; no sustituye A. |
| D — FALLIDA/HISTÓRICA | Estrés (escenario2 en docs/locust) | 2026-09-06 21:15:19 | `6a239c99` | Local/contenedorizado declarado en documento histórico; host efectivo ND | 200 | 10 min declarados; 599 s entre muestras | 106.735 | 26 | 7 | 230 | 370 | [D stats](../../docs/locust/escenario2_estres_stats.csv) | 15 HTTP 500 y 11 HTTP 503; no satisface cero fallos. |
| E — FALLIDA/HISTÓRICA | Cierre de período (escenario3) | 2026-09-09 12:28:29 | `46d8a897` | localhost:8080 según reporte HTML | 1 | 599 s entre muestras | 717 | 565 | 5 | 260 | 460 | [E stats](locust_esc3_stats.csv) | 565 HTTP 401; no demuestra una rampa a 200 usuarios. |
| F — PRELIMINAR | Carga corta | 2026-08-31 20:51:20 | `2d125061` | ND | 50 | 59 s entre muestras | 2.419 | 0 | 5 | 440 | 1.100 | [F stats](../../docs/locust/resultados_carga_stats.csv) | Ejecución corta, no satisface el perfil nominal de 5 minutos. |

Cada prefijo A–F conserva también `_stats_history.csv`, `_failures.csv` y `_exceptions.csv` en el mismo directorio. En A, B, C y F no hay fallos HTTP registrados. La ausencia de fallos en una muestra no demuestra disponibilidad de producción.

### Versiones históricas conservadas en Git

Estas versiones siguen en Git; no se restauran sobre los archivos actuales. La fecha indicada se acredita en los registros de fallos. Entorno, usuarios y duración no se han acreditado: **No disponible en la evidencia conservada**.

| Estado | Escenario | Fecha UTC | Commit de conservación | Entorno | Usuarios | Duración | Peticiones | Fallos | P50 | P95 | P99 | Archivo en Git | Motivo de descarte |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| FALLIDA/HISTÓRICA | Nominal anterior | 2026-09-09 | `46d8a897` | ND | ND | ND | 18.200 | 14.555 | 5 | 230 | 250 | `46d8a897:experimentos/resultados/locust_esc1_stats.csv` | 14.555 HTTP 401. |
| FALLIDA/HISTÓRICA | Nominal anterior | 2026-09-10 | `2928d596` | ND | ND | ND | 18.479 | 13.578 | 7 | 240 | 260 | `2928d596:experimentos/resultados/locust_esc1_stats.csv` | 4.978 HTTP 401 y 8.600 HTTP 500. |
| FALLIDA/HISTÓRICA | Estrés anterior | 2026-09-04 | `683cc17f` | ND | ND | ND | 12.735 | 12.735 | 4.100 | 4.100 | 4.100 | `683cc17f:docs/locust/escenario2_estres_stats.csv` | Código 0 en los fallos; no debe reinterpretarse como HTTP 500. |

Consulta sin modificar el árbol, por ejemplo: `git show 46d8a897:experimentos/resultados/locust_esc1_stats.csv`. Los CSV de fallos correspondientes se consultan sustituyendo `_stats.csv` por `_failures.csv`.

## Artefactos y métricas oficiales de A

- [Estadísticas oficiales](../../microservicio-soporte/locust_esc1_stats.csv).
- [Historial oficial](../../microservicio-soporte/locust_esc1_stats_history.csv).
- [Fallos oficiales: solo encabezado](../../microservicio-soporte/locust_esc1_failures.csv).
- [Excepciones oficiales: solo encabezado](../../microservicio-soporte/locust_esc1_exceptions.csv).

| Campo de Aggregated | Valor oficial |
|---|---|
| Request Count | 12.994 |
| Failure Count | 0 |
| Requests/s | 43,537580 req/s |
| Average Response Time | 109,113664 ms |
| Median Response Time / 50% | 6 ms |
| 95% | 440 ms |
| 99% | 850 ms |
| Max Response Time | 2.037,104700 ms |

RPS, promedio y máximo se muestran redondeados a seis decimales; los valores de precisión completa permanecen en el CSV. El historial abarca 2026-09-11 03:59:05–04:04:04 UTC (2026-09-10 22:59:05–23:04:04 en UTC−05:00). No se confunde esta fecha con la del commit.

## Evidencia visual e integridad

La [captura de Juliana](../../evidencias/Juliana_Emanuel/backend/pruebas-carga/image.png) se conserva como **HISTÓRICA/COMPLEMENTARIA**. Muestra una salida de terminal de una ejecución cuya cifra final visible es **13.031** peticiones, mientras el CSV oficial conservado contiene **12.994**. La causa de la diferencia **no está demostrada**. Para las métricas oficiales prevalece siempre el CSV, sin alterarlo para coincidir con la captura.

**Captura exacta del CSV oficial: pendiente de toma manual.** Debe mostrar la ruta/nombre `microservicio-soporte/locust_esc1_stats.csv`, la fila `Aggregated`, Request Count = 12994, Failure Count = 0, Requests/s, promedio, P50, P95, P99 y máximo. Puede tomarse del CSV abierto o de su lectura en terminal; debe identificarse como verificación del artefacto conservado, no como nueva ejecución.

Las imágenes [locust_resultados.png](../../Informe-E4_BCEL/locust_resultados.png) y [locust_carga.png](../../release/screenshots/locust_carga.png) muestran Grafana y son **HISTÓRICAS/COMPLEMENTARIAS**, sin vinculación exacta demostrada con A. Las demás capturas de Grafana y el health-check no acreditan por sí solas las métricas oficiales.

El [certificado anterior de Soporte](../../microservicio-soporte/REPRODUCIBILIDAD.txt) se conserva como histórico: sus hashes no coinciden con los bytes actuales. Git registra normalizaciones de finales de línea posteriores; no se atribuye una causa única sin una comprobación adicional. No se modifica ese certificado ni los CSV.

## Otras evidencias y límites

- Los reportes HTML [escenario1](reporte_escenario1.html) y [escenario3](reporte_escenario3.html) son FALLIDOS/HISTÓRICOS: escenario1 conserva 18.479 peticiones y 13.578 fallos (2026-09-10 14:11:05–14:16:04 UTC, localhost:8080), coincidentes con el agregado de `2928d596`; escenario3 conserva 717 peticiones y 565 fallos, coincidentes con E. Ninguno corresponde a A.
- El [log JDBC/Hikari](../../microservicio-soporte/soporte_locust_error.log) es histórico. No prueba por sí solo una corrida de exactamente 10 fallos ni la causa raíz de todas las fallidas.
- La calibración de 212 peticiones está mencionada en el informe previo; CSV correspondiente: **No disponible en la evidencia conservada**. Los prechecks y pruebas visibles en la captura no tienen CSV localizados en el árbol actual.
- `exp1_concurrencia.csv`, `exp3_reconciliacion.csv`, `iso25010.csv` y `boxplot_latencia.png`, incluidos sus duplicados en `docs/experimentos/resultados/`, pertenecen al banco experimental y no constituyen otra corrida oficial Locust de Soporte. Se conservan.
- Las cifras anteriores 12.265 (informe), 12.236 (B), 13.606 (C), 57,4 RPS y P95 285 ms (protocolo) no son métricas oficiales de A. 12.265 no coincide con B; las cifras del protocolo carecen de asociación demostrada a un CSV en este inventario.
- La numeración histórica es ambigua: el protocolo denomina escenario 2 a calificaciones y escenario 3 a cierre/estrés; `docs/locust/escenario2_estres_*` usa escenario 2 para estrés. Se conserva el nombre original de cada archivo y se explicita el perfil.
- [reproducibilidad.sh](../reproducibilidad.sh) ejecuta el banco experimental; no certifica por sí solo A. Los scripts existentes pueden escribir sobre resultados: no se ejecutaron al documentar E5.

No hay un resultado adicional declarado OFICIAL para calificaciones o estrés. Los estados PRELIMINAR, FALLIDA y CALIBRACIÓN describen evidencia histórica; DESCARTADA significa excluida de las conclusiones oficiales, nunca eliminada del repositorio.
