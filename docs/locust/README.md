# Pruebas de Carga (Locust) — Declaración Formal de Conjuntos de Datos (E5 / E14)

Este directorio conserva artefactos de las pruebas de rendimiento y carga ejecutadas con **Locust** sobre la arquitectura de **AcadTrace**.

---

## 1. Declaración Formal del Conjunto Nominal Oficial

En cumplimiento del criterio de cierre **E5 / E14 (Item 48)** de la guía rectora del PFC:

> **CONJUNTO OFICIAL NOMINAL (Corrida A):**
> - **Ubicación:** [`microservicio-soporte/locust_esc1_stats.csv`](../../microservicio-soporte/locust_esc1_stats.csv)
> - **Archivos asociados:**
>   - [`microservicio-soporte/locust_esc1_stats_history.csv`](../../microservicio-soporte/locust_esc1_stats_history.csv)
> - **Métricas oficiales (Fila `Aggregated`):**
>   - **Peticiones Totales:** 12,217
>   - **Fallos Totales:** 0 (0.00% tasa de error; sin 401 ni 500)
>   - **Throughput Promedio:** 40.955420 req/s
>   - **Latencia Promedio:** 10.181439 ms
>   - **Percentil 50 (P50 / Mediana):** 4 ms
>   - **Percentil 95 (P95):** 9 ms
>   - **Percentil 99 (P99):** 23 ms
>   - **Latencia Máxima:** 47889.955900 ms
> - **Criterio PI-1:** P99 < 500 ms; cumple en escenario nominal local.
> - **Perfil de ejecución validado manualmente:** 50 usuarios concurrentes, spawn rate 5 usuarios/s, 5 minutos nominales (298 s observados entre primera y última muestra), host `http://localhost:8083` con JWT instrumentado.

---

## 2. Conjuntos No Oficiales, Preliminares y Retirados

La ejecución A anterior, conservada en `microservicio-soporte/resultados_historicos/`, registró 12.994 peticiones, 0 fallos y P99=850 ms: no cumplía PI-1. No se ha demostrado equivalencia exacta de datasets y entornos con la actual, por lo que la diferencia no se atribuye exclusivamente a `JwtParser`.

El máximo actual de 47.889,96 ms es un evento aislado de `/api/soporte/tickets`, con P95=13 ms, P99=32 ms y 0 fallos; permanece sin filtrar. Cero fallos no demuestra disponibilidad de producción. El log y el JSON candidatos son anteriores, no acreditan esta corrida. Los auxiliares vacíos de fallos/excepciones tampoco acreditan su procedencia actual. Véase [E48](../../evidencias/Juliana_Emanuel/E48_actual_README.md).

Los siguientes cinco conjuntos de datos existentes en el repositorio son **NO OFICIALES** y se conservan como evidencia histórica o exploratoria. Pueden citarse con ese alcance explícito, pero **no deben presentarse como resultados oficiales del conjunto A**:

| Identificador | Archivo | Peticiones | Fallos | Estado y Dictamen |
|---|---|---|---|---|
| **B** | `experimentos/resultados/locust_esc1_stats.csv` | 12,236 | 0 | **HISTÓRICO / NO OFICIAL:** Corrida nominal anterior previa a la instrumentación definitiva. Retirada de las métricas oficiales. |
| **C** | `docs/locust/escenario1_nominal_stats.csv` | 13,606 | 0 | **PRELIMINAR / NO OFICIAL:** Corrida exploratoria inicial. Se retira su carácter oficial previo en favor del conjunto A. |
| **D** | `docs/locust/escenario2_estres_stats.csv` | 106,735 | 26 | **FALLIDO / NO OFICIAL:** Prueba de estrés escalonado (hasta 200 usuarios) fallida con 15 HTTP 500 y 11 HTTP 503. No satisface el criterio de cero fallos. Antecedente distinto de la nueva corrida oficial de estrés. |
| **E** | `experimentos/resultados/locust_esc3_stats.csv` | 717 | 565 | **FALLIDO / NO OFICIAL:** Ejecución con HTTP 401; máximo un usuario observado. No acredita estrés oficial válido. |
| **F** | `docs/locust/resultados_carga_stats.csv` | 2,419 | 0 | **PRELIMINAR / NO OFICIAL:** Corrida corta de calibración (59 segundos); no cumple el perfil nominal de 5 minutos. |

---

## Corrida oficial de estrés vigente

**Corrida oficial de estrés: DISPONIBLE y CUMPLE.** La ejecución local de 200 usuarios máximos registra 98.684 peticiones, 0 fallos, P95=15 ms y P99=23 ms; cumple el criterio de aceptación P95 < 500 ms y 0 fallos. Código ejecutado: `88649f3f`; conservación de evidencia: `b43008e5`. Evidencia: `microservicio-soporte/resultados_estres/20260920_164722/`; host `http://localhost:8085`, incorporación de 1 usuario/s, 10 minutos configurados y 599 s entre muestras. Véase [registro E5](../../experimentos/resultados/corridas-e5.md). Las capturas nominales están conservadas en `332158e4` y la captura de estrés en `b43008e5`; solo la evidencia original del perfil nominal sigue no disponible. El estado de HikariCP no está acreditado.

## 3. Firmas Criptográficas (SHA-256)

### 3.1. Conjunto Oficial Nominal (Corrida A)
| Tipo | Archivo | Hash SHA-256 |
|---|---|---|
| **OFICIAL NOMINAL** | `microservicio-soporte/locust_esc1_stats.csv` | `900BED90BFD113B06F9FCA11EE58974ACAD632A6C599FE2C3EFC214217D26A4C` |
| **OFICIAL NOMINAL** | `microservicio-soporte/locust_esc1_stats_history.csv` | `FFB37013E8A01EC99C1F1ED24400999A1B48564A67AE1EBE1DC29288B8678EA3` |
| Histórico anterior | `microservicio-soporte/resultados_historicos/locust_esc1_historico_p99_850ms_stats.csv` | `8A76EEA34413AD186C014FEBC594ACBB0353DA6ABB81D750F1D32A8C621528CB` |
| Histórico anterior | `microservicio-soporte/resultados_historicos/locust_esc1_historico_p99_850ms_stats_history.csv` | `317409E18325BD454248029443E9EBB6039FB41E451502215E410367BF526116` |
| Histórico; no actual | `microservicio-soporte/locust_esc1_failures.csv` | `48EA7DC61427ABBA01680829DD9FB55B50A69604F28AE3139E85D888B289349B` |
| Histórico; no actual | `microservicio-soporte/locust_esc1_exceptions.csv` | `6DBA11106E7EB84C71D85B91CB592276309D8B2D485BA6EA8E82DA18E6ED7663` |

### 3.2. Conjunto Oficial de Estrés Vigente (200 usuarios, corrida 20260920_164722)
| Tipo | Archivo | Hash SHA-256 |
|---|---|---|
| **OFICIAL ESTRÉS** | `microservicio-soporte/resultados_estres/20260920_164722/locust_estres_200_stats.csv` | `C4E286ECD346C593EBFDE080F047519B45CEA00E27E5C5EC909F31E07D8AC089` |
| **OFICIAL ESTRÉS** | `microservicio-soporte/resultados_estres/20260920_164722/locust_estres_200_stats_history.csv` | `226729064FA77A0CCA51BE22C26C91D401869694D8D4926418DB97BB58BFB627` |
| **OFICIAL ESTRÉS** | `microservicio-soporte/resultados_estres/20260920_164722/locust_estres_200_failures.csv` | `48EA7DC61427ABBA01680829DD9FB55B50A69604F28AE3139E85D888B289349B` |
| **OFICIAL ESTRÉS** | `microservicio-soporte/resultados_estres/20260920_164722/locust_estres_200_exceptions.csv` | `6DBA11106E7EB84C71D85B91CB592276309D8B2D485BA6EA8E82DA18E6ED7663` |
| **OFICIAL ESTRÉS** | `microservicio-soporte/resultados_estres/20260920_164722/perfil.txt` | `EC56FA4028F8643227570D24B63F5696042890C09ADD698DF4991770EDD0B586` |
| **OFICIAL ESTRÉS** | `microservicio-soporte/resultados_estres/20260920_164722/resumen_validacion.txt` | `DC5CA0B77E464126EF5AA97664CFB303B3FDDB85CC84E1B6C0432E1F9B604DB8` |

### 3.3. Conjuntos No Oficiales, Preliminares e Históricos
| Tipo | Archivo | Hash SHA-256 |
|---|---|---|
| No oficial (B) | `experimentos/resultados/locust_esc1_stats.csv` | `FC63845A59A397EAEA6E9EA15746BCDEBEA7A479EB09582B4691B016371903EA` |
| No oficial (C) | `docs/locust/escenario1_nominal_stats.csv` | `539C7F827F950FE572178A8CED7E25ACD3976E5C0289066896ECEC870B323B03` |
| No oficial (D) | `docs/locust/escenario2_estres_stats.csv` | `2AD7C788EA7E13DD48424F591DD170E2A8B3C7600C1EA0FAF24BFC25BB06B143` |
| No oficial (F) | `docs/locust/resultados_carga_stats.csv` | `D9C99B33A05DA637B7C1AEB2743FD5E2F98FFF06FAC4CDB8FCC9C15D9B501AE6` |

---

## 4. Reproducibilidad y Validación Automatizada

```bash
python scripts/recalcular_metricas_carga.py --json
python scripts/recalcular_metricas_carga.py --check-latex
python scripts/recalcular_metricas_carga.py --emit-latex-block
```

`--json` informa métricas derivadas de A, auxiliares, endpoints e históricos B y E; no comprueba documentos. `--emit-latex-block` imprime las macros de A sin escribir. `--check-latex` compara las macros almacenadas y las afirmaciones oficiales seleccionadas del manuscrito (incluido el abstract), además de las secciones oficiales de `README.md`, `docs/locust/README.md`, `docs/locust/entorno_medicion.md`, ambos `protocolo-e4.md` y la tabla de métricas de A en `corridas-e5.md`. Termina con error ante discrepancias o métricas requeridas ausentes. No es un parser general: no valida todo el manuscrito, imágenes, fechas ni resultados históricos. Las cifras se derivan de A, admitiendo el redondeo publicado; cero fallos no demuestra disponibilidad de producción.

## Integridad y límites de evidencia (#48)

El manifiesto `docs/locust/manifest_carga.json` fija los SHA-256 de los dos
CSV nominales vigentes y los cuatro CSV del estrés `20260920_164722`, además
de sus métricas declaradas. `scripts/recalcular_metricas_carga.py` verifica
ambos conjuntos antes de emitir resultados o generar macros; `--check-latex`
verifica también los hashes publicados de los archivos referenciados de Soporte,
las afirmaciones nominales seleccionadas y las peticiones, fallos y percentiles
de los bloques oficiales de estrés reconocidos en Markdown y LaTeX. No valida
automáticamente toda la prosa de estrés del informe.
Los hashes de CSV y nueva evidencia cubren bytes exactos, incluidos finales
de línea. Solo los dos TXT históricos derivados de `20260920_164722` se
comparan con LF como sus blobs Git, sin reescribir sus archivos.

El manifiesto histórico conserva n=1 nominal y n=1 estrés. El conjunto
complementario de #48 ya tiene **n=3 nominales y n=3 estrés válidos**:
`nominal_02`, `nominal_03`, `nominal_06`; `estres_01`, `estres_05`, `estres_06`.
Véase [repeticiones #48](repeticiones-48.md). Los IC95 de la media entre
corridas usan bootstrap percentil, 10000 remuestras y semilla 12345.
n=3 ofrece precisión limitada; los percentiles describen peticiones por corrida.

La carpeta oficial de estrés conserva cuatro CSV y dos TXT. `perfil.txt` y
`resumen_validacion.txt` son documentación derivada; la captura relee los CSV.
No se conserva consola original de Locust ni otra evidencia primaria adicional
que acredite independientemente la ejecución. El commit ejecutado, host,
spawn rate, duración configurada y EXIT_CODE son metadatos declarados en esos
TXT, no comprobados por el hash ni por la captura. El nominal tampoco conserva
la evidencia original de configuración. La compuerta acredita integridad y
coherencia de los artefactos declarados, no una validación retrospectiva.
No se usa la corrida incompleta `20260920_163041/` ni los candidatos.
Las nuevas corridas conservan CSV crudos, consola y perfil. Las inválidas se
conservan y se excluyen de todas las estadísticas complementarias.
