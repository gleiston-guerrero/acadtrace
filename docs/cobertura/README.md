# Cobertura de código

Esta tabla es la fuente central de cobertura documentada para E8. Una cifra marcada como histórica no representa una ejecución contra `HEAD` actual.

| Módulo | Herramienta | Alcance | LINE | INSTRUCTION | BRANCH | Umbral | Fecha | Estado | Comando |
|---|---|---|---:|---:|---:|---|---|---|---|
| `microservicio-soporte/backend` | JaCoCo 0.8.11 | BUNDLE, con exclusiones configuradas en `pom.xml` | 71,61 % (459/641) | 73,05 % (2323/3180) | 59,61 % (121/203) | 70 % LINE | 2026-09-12 | Verificado contra `HEAD` actual con Eclipse Temurin JDK 21; cumple el umbral de 70 % LINE | Desde `microservicio-soporte/backend`: `./mvnw clean verify` en Linux/CI; `./mvnw.cmd verify` en Windows |
| `app-movil-docente` | JaCoCo 0.8.13 (Android) | BUNDLE, con exclusiones estándar de recursos generados | Regenerar con `jacocoTestReport` | Regenerar con `jacocoTestReport` | No aplica | 10 % INSTRUCTION (compuerta `jacocoCoverageVerification`) | 2026-09-16 | Compuerta encadenada a `check` en `build.gradle.kts` | Desde `app-movil-docente`: `./gradlew clean testDebugUnitTest jacocoTestReport jacocoCoverageVerification` |
| `sga-principal` | JaCoCo 0.8.11 | BUNDLE, con exclusiones configuradas en `pom.xml` | 31,6 % (1881 / 2752 líneas) | 30,9 % (4394 / 14202 instrucciones) | No disponible | 30 % INSTRUCTION | 2026-09-16 | Verificado contra `HEAD` actual con Eclipse Temurin JDK 17; cumple el umbral de 30 % INSTRUCTION | Desde `sga-principal`: `./mvnw clean test jacoco:report "-Dtest=!*ContainerTest,!*ConcurrencyE3Test"` |
| `microservicio-secretaria/backend` | JaCoCo 0.8.11 | BUNDLE, con exclusiones configuradas en `pom.xml` | 71,42 % (2217/3104) | 71,34 % (11048/15487) | 49,38 % (518/1049) | 70 % LINE | 2026-09-16 | Verificado contra `HEAD` actual con Eclipse Temurin JDK 21; cumple el umbral de 70 % LINE (94 pruebas ejecutadas) | Desde `microservicio-secretaria/backend`: `./mvnw test` en Linux/CI; `.\mvnw.cmd test` en Windows |
| `microservicio-docente` | coverage.py | Ejecución sobre el paquete `docentes` con exclusiones declaradas en `.coveragerc` | 72,56 % | No aplica | No aplica | 70 % LINE (fail_under) | 2026-09-16 | Verificado contra `HEAD` actual con Python 3.12; cumple el umbral de 70 % LINE | Desde `microservicio-docente`: `python -m pytest --cov=docentes --cov-fail-under=70` |

## Alcance oficial de Secretaría

La cobertura fue regenerada y verificada el 2026-09-16 con Eclipse Temurin JDK 21 ejecutando la suite completa de 94 pruebas unitarias, de integración y gRPC in-process. El reporte actual registra 2217 de 3104 líneas cubiertas (71,42 %) y 11048 de 15487 instrucciones cubiertas (71,34 %), tomados directamente del contador de módulo del `jacoco.xml` generado. Se cumple formalmente con la regla enforceable a nivel de `BUNDLE` de mínimo 70 % LINE configurada en `pom.xml`.

El reporte de Secretaría usa JaCoCo 0.8.11 con alcance `BUNDLE` y las siguientes exclusiones estándar de infraestructura y transporte en `microservicio-secretaria/backend/pom.xml`:

- `**/ec/edu/uteq/sga/grpc/**`: stubs generados por `protoc` a partir de los `.proto`; sin lógica escrita a mano.
- `**/dto/**`: objetos de transferencia con solo getters y setters generados por Lombok.
- `**/config/**`: clases con anotaciones `@Configuration` y `@Bean`; su comportamiento se prueba integralmente vía `@SpringBootTest`.
- `**/exception/**`: definiciones de excepciones custom, sin flujo condicional.
- `**/payload/**`: modelos de solicitud/respuesta REST, equivalentes a DTOs.
- `**/entity/**`: entidades JPA con solo mapeo `@Entity`, `@Column` y getters/setters.

El reporte HTML, XML y CSV generado reside de forma unificada en `docs/cobertura/secretaria/`.

## Alcance oficial de Soporte

La cobertura fue regenerada y verificada el 2026-09-12 con Eclipse Temurin JDK 21. El reporte actual registra 459 de 641 líneas cubiertas (71,61 %), por lo que cumple el umbral mínimo de 70 % LINE.

El reporte de Soporte usa JaCoCo 0.8.11 con alcance `BUNDLE` y estas exclusiones configuradas en `microservicio-soporte/backend/pom.xml`:

- `ec/uteq/sga/soporte/grpc/incidencias/**` (stubs gRPC generados)
- `ec/uteq/sga/soporte/grpc/principal/**` (stubs gRPC generados)

Por tanto, el 71,61 % de LINE corresponde al alcance configurado del reporte JaCoCo, no a todas las clases sin exclusiones.

## Regla de interpretación

El umbral mínimo es **70 % de cobertura de líneas (LINE), medido por JaCoCo, para los módulos Java donde esté configurado**. Es un criterio de aprobación, no un resultado medido. No se mezclan métricas de JaCoCo con coverage.py.
