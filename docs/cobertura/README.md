# Cobertura de código

Esta tabla es la fuente central de cobertura documentada para E8. Una cifra marcada como histórica no representa una ejecución contra `HEAD` actual.

| Módulo | Herramienta | Alcance | LINE | INSTRUCTION | BRANCH | Umbral | Fecha | Estado | Comando |
|---|---|---|---:|---:|---:|---|---|---|---|
| `microservicio-soporte/backend` | JaCoCo 0.8.11 | BUNDLE, con exclusiones configuradas en `pom.xml` | 71,61 % (459/641) | 73,05 % (2323/3180) | 59,61 % (121/203) | 70 % LINE | 2026-09-11 21:58:03 UTC | Último reporte local conservado; pendiente de regeneración contra `HEAD` actual | Desde `microservicio-soporte/backend`: `./mvnw clean verify` en Linux/CI; `./mvnw.cmd clean verify` en Windows |
| `sga-principal` | JaCoCo | Alcance global del reporte histórico conservado | 30,31 % | No disponible en esta consolidación | No disponible en esta consolidación | Histórico; no sustituye el umbral actual | Fecha del reporte histórico conservado | Histórico/no regenerado; no es cobertura actual | Reporte histórico conservado |
| `microservicio-secretaria/backend` | JaCoCo | Contador XML global histórico | 34,53 % | No disponible en esta consolidación | No disponible en esta consolidación | Histórico; no sustituye el umbral actual | Fecha del reporte histórico conservado | Histórico/no regenerado. El 34,52 % derivado de suma de CSV por clase no se usa como cifra global | Reporte histórico conservado |
| `microservicio-docente` | coverage.py | HTML de coverage.py histórico | 78 % (evidencia HTML conservada) | No aplica | No aplica | Histórico; no es un umbral JaCoCo | Fecha del HTML conservado | Evidencia histórica conservada/no regenerada. Existe además una cifra histórica de 79,28 % de otra ejecución; la discrepancia no está resuelta | Reporte histórico conservado |

## Alcance oficial de Soporte

El reporte local conservado de Soporte usa JaCoCo 0.8.11 con alcance `BUNDLE` y estas exclusiones configuradas en `microservicio-soporte/backend/pom.xml`:

- `ec/uteq/sga/soporte/grpc/incidencias/**`
- `ec/uteq/sga/soporte/grpc/principal/**`
- `ec/uteq/sga/soporte/infrastructure/election/**`

Por tanto, el 71,61 % de LINE corresponde al alcance configurado del reporte JaCoCo, no a todas las clases sin exclusiones. La cifra todavía debe regenerarse con JDK 17 o 21 antes de declararse cobertura actual.

## Regla de interpretación

El umbral mínimo es **70 % de cobertura de líneas (LINE), medido por JaCoCo, para los módulos Java donde esté configurado**. Es un criterio de aprobación, no un resultado medido. No se mezclan métricas de JaCoCo con coverage.py.
