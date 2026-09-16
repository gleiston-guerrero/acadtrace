# Pruebas de Carga (Locust) — Declaración Formal de Conjuntos de Datos (E5 / E14)

Este directorio conserva artefactos de las pruebas de rendimiento y carga ejecutadas con **Locust** sobre la arquitectura de **AcadTrace**.

---

## 1. Declaración Formal del Conjunto Único Oficial

En cumplimiento del criterio de cierre **E5 / E14 (Item 48)** de la guía rectora del PFC:

> **ÚNICO CONJUNTO OFICIAL DECLARADO (Corrida A):**
> - **Ubicación:** [`microservicio-soporte/locust_esc1_stats.csv`](../../microservicio-soporte/locust_esc1_stats.csv)
> - **Archivos asociados:**
>   - [`microservicio-soporte/locust_esc1_stats_history.csv`](../../microservicio-soporte/locust_esc1_stats_history.csv)
>   - [`microservicio-soporte/locust_esc1_failures.csv`](../../microservicio-soporte/locust_esc1_failures.csv)
>   - [`microservicio-soporte/locust_esc1_exceptions.csv`](../../microservicio-soporte/locust_esc1_exceptions.csv)
> - **Métricas oficiales (Fila `Aggregated`):**
>   - **Peticiones Totales:** 12,994
>   - **Fallos Totales:** 0 (0.00% tasa de error; sin 401 ni 500)
>   - **Throughput Promedio:** 43.537580 req/s
>   - **Latencia Promedio:** 109.113664 ms
>   - **Percentil 50 (P50 / Mediana):** 6 ms
>   - **Percentil 95 (P95):** 440 ms
>   - **Percentil 99 (P99):** 850 ms
>   - **Latencia Máxima:** 2,037.104700 ms
> - **Perfil de ejecución:** 50 usuarios concurrentes, spawn rate 5 usuarios/s, 5 minutos nominales (299 s observados entre primera y última muestra), host `http://localhost:8083` con JWT instrumentado.

---

## 2. Conjuntos No Oficiales, Preliminares y Retirados

Los siguientes cuatro conjuntos de datos existentes en el repositorio son **NO OFICIALES** y se conservan exclusivamente como evidencia histórica o exploratoria. **No deben ser utilizados como métricas del sistema en el informe ni en publicaciones**:

| Identificador | Archivo | Peticiones | Fallos | Estado y Dictamen |
|---|---|---|---|---|
| **B** | `experimentos/resultados/locust_esc1_stats.csv` | 12,236 | 0 | **HISTÓRICO / NO OFICIAL:** Corrida nominal anterior previa a la instrumentación definitiva. Retirada de las métricas oficiales. |
| **C** | `docs/locust/escenario1_nominal_stats.csv` | 13,606 | 0 | **PRELIMINAR / NO OFICIAL:** Corrida exploratoria inicial. Se retira su carácter oficial previo en favor del conjunto A. |
| **D** | `docs/locust/escenario2_estres_stats.csv` | 106,735 | 26 | **FALLIDO / NO OFICIAL:** Prueba de estrés escalonado (hasta 200 usuarios) fallida con 15 HTTP 500 y 11 HTTP 503. No satisface el criterio de cero fallos. **No existe estrés oficial válido.** |
| **E** | `docs/locust/resultados_carga_stats.csv` | 2,419 | 0 | **PRELIMINAR / NO OFICIAL:** Corrida corta de calibración (59 segundos); no cumple el perfil nominal de 5 minutos. |

---

## 3. Firmas Criptográficas (SHA-256)

| Tipo | Archivo | Hash SHA-256 |
|---|---|---|
| **OFICIAL** | `microservicio-soporte/locust_esc1_stats.csv` | `8A76EEA34413AD186C014FEBC594ACBB0353DA6ABB81D750F1D32A8C621528CB` |
| **OFICIAL** | `microservicio-soporte/locust_esc1_stats_history.csv` | `317409E18325BD454248029443E9EBB6039FB41E451502215E410367BF526116` |
| **OFICIAL** | `microservicio-soporte/locust_esc1_failures.csv` | `48EA7DC61427ABBA01680829DD9FB55B50A69604F28AE3139E85D888B289349B` |
| **OFICIAL** | `microservicio-soporte/locust_esc1_exceptions.csv` | `6DBA11106E7EB84C71D85B91CB592276309D8B2D485BA6EA8E82DA18E6ED7663` |
| No oficial (B) | `experimentos/resultados/locust_esc1_stats.csv` | `FC63845A59A397EAEA6E9EA15746BCDEBEA7A479EB09582B4691B016371903EA` |
| No oficial (C) | `docs/locust/escenario1_nominal_stats.csv` | `539C7F827F950FE572178A8CED7E25ACD3976E5C0289066896ECEC870B323B03` |
| No oficial (D) | `docs/locust/escenario2_estres_stats.csv` | `2AD7C788EA7E13DD48424F591DD170E2A8B3C7600C1EA0FAF24BFC25BB06B143` |
| No oficial (E) | `docs/locust/resultados_carga_stats.csv` | `D9C99B33A05DA637B7C1AEB2743FD5E2F98FFF06FAC4CDB8FCC9C15D9B501AE6` |

---

## 4. Reproducibilidad y Validación Automatizada

Para reproducir y derivar deterministamente cada una de las cifras publicadas en el informe técnico a partir del conjunto de datos oficial:

```bash
python scripts/recalcular_metricas_carga.py
```

El guion valida automáticamente que:
1. Los valores agregados y por endpoint coincidan de manera exacta con el CSV crudo.
2. El informe técnico `Informe-E4_BCEL/TA-PFC-E4_BCEL.tex` cite únicamente los valores del conjunto oficial (12,994 reqs, 43.537580 req/s, P50=6ms, P95=440ms, P99=850ms).
3. No existan cifras inventadas ni retractadas (como 12,265 o 12,735) en la matriz de evaluación ISO/IEC 25010 ni en las tablas principales.
