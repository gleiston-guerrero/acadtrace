# Cobertura de código

Esta tabla es la fuente central de cobertura documentada para E8. Una cifra marcada como histórica no representa una ejecución contra `HEAD` actual.

| Módulo | Herramienta | Alcance | LINE | INSTRUCTION | BRANCH | Umbral | Fecha | Estado | Comando |
|---|---|---|---:|---:|---:|---|---|---|---|
| `microservicio-soporte/backend` | JaCoCo 0.8.11 | BUNDLE, con exclusiones configuradas en `pom.xml` | 71,61 % (459/641) | 73,03 % (2323/3181) | 59,61 % (121/203) | 70 % LINE | 2026-09-17 | Reporte versionado en `a03d0abf`, generado con Maven 3.9.9 y Java 21; cumple el umbral de 70 % LINE | Desde `microservicio-soporte/backend`: `./mvnw clean verify` en Linux/CI; `./mvnw.cmd verify` en Windows |
| `sga-principal` | JaCoCo | Alcance global del reporte histórico conservado | 30,31 % | No disponible en esta consolidación | No disponible en esta consolidación | Histórico; no sustituye el umbral actual | Fecha del reporte histórico conservado | Histórico/no regenerado; no es cobertura actual | Reporte histórico conservado |
| `microservicio-secretaria/backend` | JaCoCo | Contador XML global histórico | 34,53 % | No disponible en esta consolidación | No disponible en esta consolidación | Histórico; no sustituye el umbral actual | Fecha del reporte histórico conservado | Histórico/no regenerado. El 34,52 % derivado de suma de CSV por clase no se usa como cifra global | Reporte histórico conservado |
| `microservicio-docente` | coverage.py | HTML de coverage.py histórico | 78 % (evidencia HTML conservada) | No aplica | No aplica | Histórico; no es un umbral JaCoCo | Fecha del HTML conservado | Evidencia histórica conservada/no regenerada. Existe además una cifra histórica de 79,28 % de otra ejecución; la discrepancia no está resuelta | Reporte histórico conservado |

## Alcance oficial de Soporte

La cobertura fue regenerada el 2026-09-17 con Maven 3.9.9 y Java 21, y el reporte completo quedó versionado en el commit `a03d0abf`. El contador global `LINE` del reporte XML (alcance `BUNDLE`) registra 182 líneas no cubiertas y 459 cubiertas: 459/641 = 71,61 %, por lo que cumple el umbral mínimo de 70 % LINE. Esta cifra se obtiene del contador global, no de sumar filas por clase del CSV.

Evidencia versionada: [reporte HTML](soporte/index.html), [CSV](soporte/jacoco.csv) y [XML con contadores globales](soporte/jacoco.xml).

El reporte de Soporte usa JaCoCo 0.8.11 con alcance `BUNDLE` y estas exclusiones configuradas en `microservicio-soporte/backend/pom.xml`:

- `ec/uteq/sga/soporte/grpc/incidencias/**`
- `ec/uteq/sga/soporte/grpc/principal/**`

Por tanto, el 71,61 % de LINE corresponde al alcance configurado del reporte JaCoCo, no a todas las clases sin exclusiones.

## Regla de interpretación

El umbral mínimo es **70 % de cobertura de líneas (LINE), medido por JaCoCo, para los módulos Java donde esté configurado**. Es un criterio de aprobación, no un resultado medido. No se mezclan métricas de JaCoCo con coverage.py.
