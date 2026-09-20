# E5 — Registro de corridas de carga

## Declaración y alcance

**OFICIAL NOMINAL de Soporte: conjunto A vigente (2026-09-20).** Las métricas oficiales proceden exclusivamente de la fila `Aggregated` del CSV conservado. Es una **prueba de carga reproducible ejecutada en entorno local/contenedorizado**, con usuarios virtuales; no representa tráfico real de producción.

**Corrida oficial de estrés: DISPONIBLE y CUMPLE.** La ejecución local de 200 usuarios máximos registra 98.684 peticiones, 0 fallos, P95=15 ms y P99=23 ms; cumple el criterio de aceptación P95 < 500 ms y 0 fallos. Código ejecutado: `88649f3f`; conservación de evidencia: `b43008e5`. Nominal y estrés son ejecuciones distintas. Las tres capturas nominales están conservadas en `332158e4`; sigue sin estar disponible la evidencia original del perfil nominal. El estado de HikariCP no queda acreditado.

El perfil de [run_locust.py](../../microservicio-soporte/run_locust.py) configura 50 usuarios virtuales, spawn rate de 5 usuarios/s, duración de 5 minutos y host `http://localhost:8083`. El [locustfile.py](../../microservicio-soporte/locustfile.py) envía JWT a los endpoints protegidos y registra como fallos las respuestas HTTP 401 y 500. El conjunto A vigente contiene cero fallos en sus estadísticas. Los auxiliares vacíos no acreditan su procedencia actual.

El **commit de conservación del resultado** identifica una versión de Git que contiene el artefacto; no demuestra qué código estaba desplegado al ejecutarlo. Para el nominal y las corridas históricas, el **commit del código ejecutado** es: **No disponible en la evidencia conservada**. Para el estrés oficial vigente, `perfil.txt` registra `88649f3f`; su evidencia se conserva en `b43008e5`. Tampoco se atribuyen a A el hardware ni las versiones descritas en documentos de otras campañas.

## Tabla de trazabilidad

Las fechas A–F son el primer timestamp de `stats_history`, expresado en UTC. Usuarios significa máximo observado. La duración observada es el intervalo entre primera y última muestra, no la duración exacta del proceso. ND significa literalmente **No disponible en la evidencia conservada**. Los percentiles están en ms.

| Estado | Escenario | Fecha UTC | Commit de conservación | Entorno | Usuarios | Duración | Peticiones | Fallos | P50 | P95 | P99 | Archivo | Motivo de aceptación/descarte |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **A vigente — OFICIAL NOMINAL** | Nominal local Soporte | 2026-09-20 08:46:16 | `332158e4` (capturas nominales) | Entorno local actual | 50 | 5 min configurados según validación manual; 298 s entre muestras | **12.217** | **0** | **4** | **9** | **23** | [A actual stats](../../microservicio-soporte/locust_esc1_stats.csv) | Cumple PI-1 en escenario nominal local; CSV promovidos sin edición. |
| **Estrés vigente — OFICIAL** | Estrés local Soporte | 2026-09-20 21:47:25 | `b43008e5` | localhost:8085; código `88649f3f` | 200 | 10 min configurados; 599 s entre muestras | **98.684** | **0** | **7** | **15** | **23** | [Estrés stats](../../microservicio-soporte/resultados_estres/20260920_164722/locust_estres_200_stats.csv) | P95 < 500 ms y 0 fallos: CUMPLE; incorporación 1 usuario/s. |
| **A anterior — HISTÓRICA** | Nominal Soporte | 2026-09-11 03:59:05 | `956cafcb` (datos); `c5c0e6f5` (normalización posterior) | Local/contenedorizado; localhost:8083 configurado | 50 | 5 min configurados; 299 s entre muestras | **12.994** | **0** | **6** | **440** | **850** | [A stats](../../microservicio-soporte/resultados_historicos/locust_esc1_historico_p99_850ms_stats.csv) | Autenticación instrumentada; 0 fallos, sin 401/500 registrados. Resultado nominal anterior; no cumplía PI-1. |
| B — PRELIMINAR/HISTÓRICA | Nominal anterior | 2026-09-11 03:32:11 | `df0112d3` | ND | 50 | 299 s entre muestras | 12.236 | 0 | 110 | 370 | 460 | [B stats](locust_esc1_stats.csv) | Anterior a A; conservada sin carácter oficial. |
| C — PRELIMINAR/HISTÓRICA | Nominal anterior | 2026-09-05 00:05:03 | `683cc17f` | Local/contenedorizado declarado en documento histórico; host efectivo ND | 50 | 5 min declarados; 298 s entre muestras | 13.606 | 0 | 6 | 340 | 450 | [C stats](../../docs/locust/escenario1_nominal_stats.csv) | Se retira la declaración oficial anterior; no sustituye A. |
| D — FALLIDA/HISTÓRICA | Estrés (escenario2 en docs/locust) | 2026-09-06 21:15:19 | `6a239c99` | Local/contenedorizado declarado en documento histórico; host efectivo ND | 200 | 10 min declarados; 599 s entre muestras | 106.735 | 26 | 7 | 230 | 370 | [D stats](../../docs/locust/escenario2_estres_stats.csv) | 15 HTTP 500 y 11 HTTP 503; no satisface cero fallos. |
| E — FALLIDA/HISTÓRICA | Cierre de período (escenario3) | 2026-09-09 12:28:29 | `46d8a897` | localhost:8080 según reporte HTML | 1 | 599 s entre muestras | 717 | 565 | 5 | 260 | 460 | [E stats](locust_esc3_stats.csv) | 565 HTTP 401; no demuestra una rampa a 200 usuarios. |
| F — PRELIMINAR | Carga corta | 2026-08-31 20:51:20 | `2d125061` | ND | 50 | 59 s entre muestras | 2.419 | 0 | 5 | 440 | 1.100 | [F stats](../../docs/locust/resultados_carga_stats.csv) | Ejecución corta, no satisface el perfil nominal de 5 minutos. |

Los prefijos históricos B–F conservan también `_stats_history.csv`, `_failures.csv` y `_exceptions.csv` en el mismo directorio. A anterior conserva sus estadísticas e historial sin alterar en `resultados_historicos/`. A vigente se acredita exclusivamente con estadísticas e historial promovidos; los auxiliares vacíos no se vinculan a ella. En las estadísticas de A, B, C y F no hay fallos registrados. La ausencia de fallos en una muestra no demuestra disponibilidad de producción.

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
- [Matriz derivada actual](../../docs/experimentos/resultados/matriz_iso25010.csv).

| Campo de Aggregated | Valor oficial |
|---|---|
| Request Count | 12.217 |
| Failure Count | 0 |
| Requests/s | 40,955420 req/s |
| Average Response Time | 10,181439 ms |
| Median Response Time / 50% | 4 ms |
| 95% | 9 ms |
| 99% | 23 ms |
| Max Response Time | 47889,955900 ms |

RPS, promedio y máximo se muestran redondeados a seis decimales; los valores de precisión completa permanecen en el CSV. El historial actual abarca 2026-09-20 08:46:16–08:51:14 UTC (03:46:16–03:51:14 en UTC−05:00): 298 segundos entre muestras. PI-1 exige P99 < 500 ms; cumple en escenario nominal local. No se confunde esta fecha con la del commit.

## Evidencia visual e integridad

La corrida anterior registró media 109,113664 ms, P50=6 ms, P95=440 ms y P99=850 ms: no cumplía PI-1. Sus estadísticas e historial se conservan intactos en `microservicio-soporte/resultados_historicos/`. La diferencia no se atribuye exclusivamente a `JwtParser`: no está demostrada la equivalencia exacta de datasets y entornos.

El máximo actual aislado de 47.889,96 ms pertenece a `/api/soporte/tickets`, con P95=13 ms, P99=32 ms y 0 fallos. Se conserva sin filtrar y no equivale al P99 agregado.

El `locust_esc1_candidato_console.log` y el `locust_esc1_candidato_validation.json` corresponden a una ejecución anterior (07:09 UTC), no a los CSV promovidos (08:46–08:51 UTC). No acreditan temporalmente esta corrida ni se usan como validación retrospectiva. El perfil configurado procede de la validación manual comunicada, con captura pendiente; los CSV acreditan 50 usuarios máximos y 298 segundos entre muestras. Los CSV auxiliares vacíos no permiten acreditar su procedencia.

Se conserva la **descripción HISTÓRICA/COMPLEMENTARIA** de `evidencias/Juliana_Emanuel/backend/pruebas-carga/image.png`, pero no el archivo de imagen versionado. Esa descripción atribuye una salida de terminal de una ejecución cuya cifra final visible es **13.031** peticiones, mientras el CSV histórico anterior contiene **12.994**. La causa de la diferencia **no está demostrada**. Para las métricas oficiales prevalece siempre el CSV, sin alterarlo para coincidir con la captura.

**Capturas nominales disponibles (commit `332158e4`):** [CSV](../../evidencias/Juliana_Emanuel/E48_actual_csv.png), [matriz](../../evidencias/Juliana_Emanuel/E48_actual_matriz.png) e [historial y usuarios](../../evidencias/Juliana_Emanuel/E48_actual_usuarios_historial.png). Documentan los artefactos nominales conservados; no sustituyen la evidencia original del perfil, que sigue no disponible.

Las imágenes [locust_resultados.png](../../Informe-E4_BCEL/locust_resultados.png) y [grafana_hikari_p95.png](../../release/screenshots/grafana_hikari_p95.png) muestran Grafana y son **HISTÓRICAS/COMPLEMENTARIAS**, sin vinculación exacta demostrada con A. Las demás capturas de Grafana y el health-check no acreditan por sí solas las métricas oficiales.

El [certificado anterior de Soporte](../../microservicio-soporte/REPRODUCIBILIDAD.txt) se conserva como histórico: sus hashes no coinciden con los bytes actuales. Git registra normalizaciones de finales de línea posteriores; no se atribuye una causa única sin una comprobación adicional. No se modifica ese certificado ni los CSV.

## Estrés oficial vigente: ejecución separada de A

Ruta: `microservicio-soporte/resultados_estres/20260920_164722/`.
Código ejecutado: `88649f3f`; commit de conservación: `b43008e5`.
Host: `http://localhost:8085`; incorporación: 1 usuario/s; duración configurada:
10 minutos. Timestamps: 1789940845–1789941444, 599 s entre muestras.

| Métrica del estrés oficial | Valor |
|---|---|
| Usuarios máximos | 200 |
| Peticiones | 98.684 |
| Fallos | 0 |
| Requests/s | 164.86740285525565 |
| Average Response Time | 7.86715629893033 ms |
| P50 | 7 ms |
| P95 | 15 ms |
| P99 | 23 ms |
| Max Response Time | 324.8848000075668 ms |
| EXIT_CODE | 0 |
| Criterio de aceptación | P95 < 500 ms y 0 fallos |
| Resultado | CUMPLE |

La carpeta conserva `perfil.txt`, `resumen_validacion.txt`,
`locust_estres_200_stats.csv`, `locust_estres_200_stats_history.csv`,
`locust_estres_200_failures.csv` y `locust_estres_200_exceptions.csv`.
La [captura del resumen](../../evidencias/Juliana_Emanuel/E48_estres_200_resumen.png)
está conservada en el mismo commit. Los contadores corresponden al agregado
completo de la rampa, no exclusivamente al intervalo con 200 usuarios.
No demuestran disponibilidad de producción ni el estado del pool HikariCP.
La corrida incompleta `20260920_163041/` no es oficial ni se usa en estas métricas.
El histórico D (106.735 peticiones, 26 fallos) permanece FALLIDO/HISTÓRICO.

## Otras evidencias y límites

- Los reportes HTML [escenario1](reporte_escenario1.html) y [escenario3](reporte_escenario3.html) son FALLIDOS/HISTÓRICOS: escenario1 conserva 18.479 peticiones y 13.578 fallos (2026-09-10 14:11:05–14:16:04 UTC, localhost:8080), coincidentes con el agregado de `2928d596`; escenario3 conserva 717 peticiones y 565 fallos, coincidentes con E. Ninguno corresponde a A.
- El [log JDBC/Hikari](../../microservicio-soporte/soporte_locust_error.log) es histórico. No prueba por sí solo una corrida de exactamente 10 fallos ni la causa raíz de todas las fallidas.
- La calibración de 212 peticiones está mencionada en el informe previo; CSV correspondiente: **No disponible en la evidencia conservada**. Los prechecks y pruebas visibles en la captura no tienen CSV localizados en el árbol actual.
- `exp1_concurrencia.csv`, `exp3_reconciliacion.csv`, `iso25010.csv` y `boxplot_latencia.png`, incluidos sus duplicados en `docs/experimentos/resultados/`, pertenecen al banco experimental y no constituyen otra corrida oficial Locust de Soporte. Se conservan.
- Las cifras anteriores 12.265 (informe), 12.236 (B), 13.606 (C), 57,4 RPS y P95 285 ms (protocolo) no son métricas oficiales de A. 12.265 no coincide con B; las cifras del protocolo carecen de asociación demostrada a un CSV en este inventario.
- La numeración histórica es ambigua: el protocolo denomina escenario 2 a calificaciones y escenario 3 a cierre/estrés; `docs/locust/escenario2_estres_*` usa escenario 2 para estrés. Se conserva el nombre original de cada archivo y se explicita el perfil.
- [reproducibilidad.sh](../reproducibilidad.sh) ejecuta el banco experimental; no certifica por sí solo A. Los scripts existentes pueden escribir sobre resultados: no se ejecutaron al documentar E5.

Existe la corrida oficial de estrés documentada arriba; no hay un resultado adicional declarado OFICIAL para calificaciones. Los estados PRELIMINAR, FALLIDA y CALIBRACIÓN describen evidencia histórica; DESCARTADA significa excluida de las conclusiones oficiales, nunca eliminada del repositorio.
