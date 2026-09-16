# Cobertura de código

Esta tabla es la fuente central de cobertura documentada para E8. Una cifra marcada como histórica no representa una ejecución contra `HEAD` actual.

| Módulo | Herramienta | Alcance | LINE | INSTRUCTION | BRANCH | Umbral | Fecha | Estado | Comando |
|---|---|---|---:|---:|---:|---|---|---|---|
| `microservicio-soporte/backend` | JaCoCo 0.8.11 | BUNDLE, con exclusiones configuradas en `pom.xml` | 71,61 % (459/641) | 73,05 % (2323/3180) | 59,61 % (121/203) | 70 % LINE | 2026-09-12 | Verificado contra `HEAD` actual con Eclipse Temurin JDK 21; cumple el umbral de 70 % LINE | Desde `microservicio-soporte/backend`: `./mvnw clean verify` en Linux/CI; `./mvnw.cmd verify` en Windows |
| `sga-principal` | JaCoCo | Alcance global del reporte histórico conservado | 30,31 % | No disponible en esta consolidación | No disponible en esta consolidación | Histórico; no sustituye el umbral actual | Fecha del reporte histórico conservado | Histórico/no regenerado; no es cobertura actual | Reporte histórico conservado |
| `microservicio-secretaria/backend` | JaCoCo 0.8.11 | BUNDLE, con exclusiones configuradas en `pom.xml` | 71,43 % (2220/3108) | 71,34 % (11048/15487) | 49,38 % (518/1049) | 70 % LINE | 2026-09-16 | Verificado contra `HEAD` actual con Eclipse Temurin JDK 21; cumple el umbral de 70 % LINE (94 pruebas ejecutadas) | Desde `microservicio-secretaria/backend`: `./mvnw test` en Linux/CI; `.\mvnw.cmd test` en Windows |
| `microservicio-docente` | coverage.py | HTML de coverage.py histórico | 78 % (evidencia HTML conservada) | No aplica | No aplica | Histórico; no es un umbral JaCoCo | Fecha del HTML conservado | Evidencia histórica conservada/no regenerada. Existe además una cifra histórica de 79,28 % de otra ejecución; la discrepancia no está resuelta | Reporte histórico conservado |

## Alcance oficial de Secretaría

La cobertura fue regenerada y verificada el 2026-09-16 con Eclipse Temurin JDK 21 ejecutando la suite completa de 94 pruebas unitarias, de integración y gRPC in-process. El reporte actual registra 2220 de 3108 líneas cubiertas (71,43 %) y 11048 de 15487 instrucciones cubiertas (71,34 %), cumpliendo formalmente con la regla enforceable a nivel de `BUNDLE` de mínimo 70 % LINE configurada en `pom.xml`.

El reporte de Secretaría usa JaCoCo 0.8.11 con alcance `BUNDLE` y las siguientes exclusiones estándar de infraestructura y transporte en `microservicio-secretaria/backend/pom.xml`:

- `**/ec/edu/uteq/sga/grpc/**` (stubs de gRPC generados)
- `**/dto/**` (objetos planos de transferencia de datos)
- `**/config/**` (clases de configuración de beans de Spring)
- `**/exception/**` (definición de excepciones de dominio)
- `**/payload/**` (modelos de solicitud/respuesta)
- `**/entity/**` (entidades JPA puras sin lógica de negocio)

El reporte HTML, XML y CSV generado reside de forma unificada en `docs/cobertura/secretaria/`.

## Alcance oficial de Soporte

La cobertura fue regenerada y verificada el 2026-09-12 con Eclipse Temurin JDK 21. El reporte actual registra 459 de 641 líneas cubiertas (71,61 %), por lo que cumple el umbral mínimo de 70 % LINE.

El reporte de Soporte usa JaCoCo 0.8.11 con alcance `BUNDLE` y estas exclusiones configuradas en `microservicio-soporte/backend/pom.xml`:

- `ec/uteq/sga/soporte/grpc/incidencias/**`
- `ec/uteq/sga/soporte/grpc/principal/**`
- `ec/uteq/sga/soporte/infrastructure/election/**`

Por tanto, el 71,61 % de LINE corresponde al alcance configurado del reporte JaCoCo, no a todas las clases sin exclusiones.

## Regla de interpretación

El umbral mínimo es **70 % de cobertura de líneas (LINE), medido por JaCoCo, para los módulos Java donde esté configurado**. Es un criterio de aprobación, no un resultado medido. No se mezclan métricas de JaCoCo con coverage.py.
