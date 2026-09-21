# Cobertura de código

Esta tabla es la fuente central de cobertura documentada para E8.

Actualización de Soporte (2026-09-20): el resultado actual es LINE 458/640 =
71,5625 %, umbral 70 %, compuerta aprobada.
El [XML actual](soporte/actual/jacoco.xml) es una copia sin edición del reporte
local `microservicio-soporte/backend/target/site/jacoco/jacoco.xml` y es la
fuente que consume `generar_matriz.py`. En la revisión final se ejecutó
`mvn -o verify` con Maven 3.9.9 y Java 21 (compilación release 17): 74 pruebas,
0 fallos, 0 errores y 0 omitidas; `BUILD SUCCESS` y todas las comprobaciones
de cobertura aprobadas. El XML recién generado confirma LINE 458/640;
la copia conservada no se reemplazó. Las cifras 459/641 y los reportes de Soporte enlazados
más abajo se conservan como evidencia histórica anterior.

| Módulo | Herramienta | Alcance | LINE | INSTRUCTION | BRANCH | Umbral | Fecha | Estado | Comando |
|---|---|---|---:|---:|---:|---|---|---|---|
| `microservicio-soporte/backend` | JaCoCo 0.8.11 | BUNDLE, con exclusiones configuradas en `pom.xml` | 71,56 % (458/640) | 73,04 % (2325/3183) | 59,61 % (121/203) | 70 % LINE | 2026-09-20 | Reporte oficial vigente en `docs/cobertura/soporte/jacoco.xml` (74 pruebas, 0 fallos, 0 errores); cumple el umbral de 70 % LINE | Desde `microservicio-soporte/backend`: `./mvnw clean verify` en Linux/CI; `./mvnw.cmd verify` en Windows |
| `app-movil-docente` | JaCoCo 0.8.13 (Android) | BUNDLE, con exclusiones estándar de recursos generados | Regenerar con `jacocoTestReport` | Regenerar con `jacocoTestReport` | No aplica | 10 % INSTRUCTION (compuerta `jacocoCoverageVerification`) | 2026-09-16 | Compuerta encadenada a `check` en `build.gradle.kts` | Desde `app-movil-docente`: `./gradlew clean testDebugUnitTest jacocoTestReport jacocoCoverageVerification` |
| `sga-principal` | JaCoCo 0.8.11 | BUNDLE, con exclusiones configuradas en `pom.xml` | 31,6 % (871 / 2752 líneas cubiertas; 1881 no cubiertas) | 30,9 % (4394 / 14202 instrucciones) | No disponible | 30 % INSTRUCTION | 2026-09-16 | Verificado contra `HEAD` actual con Eclipse Temurin JDK 17; cumple el umbral de 30 % INSTRUCTION | Desde `sga-principal`: `./mvnw clean test jacoco:report "-Dtest=!*ContainerTest,!*ConcurrencyE3Test"` |
| `microservicio-secretaria/backend` | JaCoCo 0.8.11 | BUNDLE, con exclusiones configuradas en `pom.xml` | 74,42 % (2313/3108) | 72,74 % (11272/15497) | 49,67 % (523/1053) | 70 % LINE | 2026-09-18 | Verificado contra `HEAD` actual con Eclipse Temurin JDK 21; cumple el umbral de 70 % LINE (96 pruebas ejecutadas) | Desde `microservicio-secretaria/backend`: `./mvnw test` en Linux/CI; `.\mvnw.cmd test` en Windows |
| `microservicio-docente` | coverage.py | Ejecución sobre el paquete `docentes` con exclusiones declaradas en `.coveragerc` | 72,56 % | No aplica | No aplica | 70 % LINE (fail_under) | 2026-09-17 | Verificado contra `HEAD` actual con Python 3.13.7; cumple el umbral de 70 % LINE | Desde `microservicio-docente`: `python -m pytest --cov=docentes --cov-fail-under=70` |

## Alcance oficial de Docente

La medición oficial del microservicio Docente se ejecuta sobre el paquete `docentes` con `coverage.py` y la configuración versionada en `microservicio-docente/.coveragerc`. El comando reproducible es `python -m pytest --cov=docentes --cov-fail-under=70`. La medición registrada el 2026-09-17 con Python 3.13.7 fue 72,56 %, superando la compuerta mínima de 70 % configurada mediante `fail_under`.

Las exclusiones de `.coveragerc` se interpretan de la siguiente manera:

- `*/migrations/*`: migraciones generadas por Django; no representan lógica de negocio ejecutada por la aplicación.
- `*/test_*.py`, `*/tests.py`, `*/tests_*.py` y `*/tests/*`: código de la propia suite de pruebas; se excluye para evitar que las pruebas incrementen artificialmente su propio denominador de cobertura.
- `*/grpc_services/*_pb2.py` y `*/grpc_services/*_pb2_grpc.py`: artefactos Python generados a partir de las definiciones Protocol Buffers.
- `*/grpc_services/server.py`: adaptador de transporte gRPC (`DocenteServiceServicer`) que recibe llamadas RPC externas, valida tokens de infraestructura gRPC y delega en servicios de dominio; su ejecución requiere contexto de red activo (`context.invocation_metadata()`), por lo que su validación corresponde a pruebas de integración de transporte y E2E fuera del alcance de la suite unitaria aislada de dominio. Por transparencia técnica se declara explícitamente su exclusión del cómputo unitario (incluso incluyéndolo en la medición, la cobertura resultante supera el umbral del 70 %).
- `*/grpc_services/fix_imports.py`: utilidad auxiliar de postprocesamiento de archivos gRPC generados; modifica imports de los módulos `*_pb2_grpc.py` y no forma parte del flujo de negocio en ejecución.
- `*/management/commands/*`: puntos de entrada operativos de Django. En particular, `rungrpcserver.py` realiza el bootstrap del servidor gRPC y mantiene un bucle de ejecución; se considera infraestructura de arranque fuera del alcance de la métrica publicada.

Estas exclusiones no significan que todo el código excluido sea generado ni que sea imposible probarlo. Definen explícitamente el alcance de la cifra publicada y separan la lógica de dominio de los adaptadores de transporte e infraestructura. En particular, `grpc_services/server.py` se declara explícitamente para evitar presentar el 72,56 % como cobertura de la totalidad del código del microservicio.

## Alcance oficial de Secretaría

La cobertura fue regenerada y verificada el 2026-09-18 con Eclipse Temurin JDK 21 ejecutando la suite completa de 96 pruebas unitarias, de integración y gRPC in-process. El reporte actual registra 2313 de 3108 líneas cubiertas (74,42 %) y 11272 de 15497 instrucciones cubiertas (72,74 %), tomados directamente del contador de módulo del `jacoco.xml` generado. Se cumple formalmente con la regla enforceable a nivel de `BUNDLE` de mínimo 70 % LINE configurada en `pom.xml`.

El reporte de Secretaría usa JaCoCo 0.8.11 con alcance `BUNDLE` y las siguientes exclusiones estándar de infraestructura y transporte en `microservicio-secretaria/backend/pom.xml`:

- `**/dto/**`: objetos de transferencia con solo getters y setters generados por Lombok.
- `**/config/**`: clases con anotaciones `@Configuration` y `@Bean`; su comportamiento se prueba integralmente vía `@SpringBootTest`.
- `**/exception/**`: definiciones de excepciones custom, sin flujo condicional.
- `**/payload/**`: modelos de solicitud/respuesta REST, equivalentes a DTOs.
- `**/entity/**`: entidades JPA con solo mapeo `@Entity`, `@Column` y getters/setters.
- `**/grpc/**` (y `**/ec/edu/uteq/sga/grpc/**`): stubs generados por `protoc` a partir de los `.proto`; sin lógica escrita a mano.

Las razones técnicas que justifican cada una de estas exclusiones se detallan a continuación:

- **`dto/**` (Data Transfer Objects):** Clases planas destinadas exclusivamente al acarreo de datos entre capas del microservicio. Contienen únicamente campos privados y métodos de acceso (`getters`, `setters`, constructores y `builder`) generados en tiempo de compilación por la biblioteca Lombok mediante anotaciones como `@Data`, `@Getter`, `@Setter` y `@Builder`. Al no incluir bifurcaciones condicionales, algoritmos de cálculo ni lógica de negocio, no aportan valor en cobertura unitaria y distorsionarían las métricas reales del dominio.
- **`config/**` (Configuración de infraestructura Spring):** Clases anotadas con `@Configuration` y fábricas de beans (`@Bean`) encargadas del ensamblaje del contenedor de inversión de control de Spring Boot (por ejemplo, `SecurityConfig`, `WebConfig`, `DataSourceConfig` y clientes gRPC). La correcta inicialización, vinculación e inyección de dependencias de estas configuraciones se valida de manera integral durante el arranque del contexto en las pruebas de integración (`@SpringBootTest` y pruebas en contenedores), por lo que aislarlas en pruebas unitarias puras crearía pruebas artificiales y redundantes sobre el propio framework.
- **`exception/**` (Definiciones de excepciones de dominio y API):** Clases especializadas de excepciones de la aplicación (como `ResourceNotFoundException`, `BadRequestException`, `UnauthorizedException`, etc.) que extienden directamente de `RuntimeException`. Su estructura se limita a sobrecargar constructores que delegan el mensaje descriptivo o el código de error HTTP a la superclase. Al carecer de estructuras de control de flujo (`if`, `switch`, bucles), su comportamiento se evalúa orgánicamente cuando son arrojadas y capturadas en los flujos de fallo de los servicios y controladores.
- **`payload/**` (Contratos de solicitud y respuesta REST):** Representan los esquemas de entrada y salida de los controladores REST del microservicio, desempeñando un rol idéntico al de los DTOs. Están concebidos únicamente para la serialización y deserialización JSON mediante Jackson. La validación estructural de sus campos se realiza mediante anotaciones declarativas estándar de Jakarta Bean Validation (`@NotNull`, `@NotBlank`, `@Size`), las cuales se auditan y prueban dentro de los tests de integración y validación de endpoints (`@WebMvcTest` o MockMvc).
- **`entity/**` (Entidades de persistencia JPA):** Clases del modelo de persistencia mapeadas a las tablas del esquema relacional `sga_secretaria` mediante anotaciones de JPA/Hibernate (`@Entity`, `@Table`, `@Column`, `@Id`, `@Enumerated`). Al igual que los DTOs, solo contienen campos mapeados y métodos accesores autogenerados por Lombok. El comportamiento transaccional y la persistencia de datos se validan en los repositorios Spring Data y servicios en pruebas con base de datos real (Testcontainers), por lo que excluir los getters/setters planos preserva la rigurosidad de la métrica de cobertura.
- **`grpc/**` (Stubs y clientes generados por protoc):** Artefactos Java producidos automáticamente por el compilador de Protocol Buffers (`protoc`) y el plugin `grpc-java` a partir de los archivos de definición de contratos `.proto` ubicados en `src/main/proto` o compartidos desde `sga-principal`. Al tratarse de código de infraestructura de transporte generado por una herramienta externa y no de código fuente redactado por los desarrolladores, su exclusión es una buena práctica estándar para medir con precisión el código de negocio del microservicio.

El reporte HTML, XML y CSV generado reside de forma unificada en `docs/cobertura/secretaria/`.

## Alcance oficial de Soporte

La cobertura fue regenerada el 2026-09-17 con Maven 3.9.9 y Java 21, y el reporte completo quedó versionado en el commit `a03d0abf`. El contador global `LINE` del reporte XML (alcance `BUNDLE`) registra 182 líneas no cubiertas y 459 cubiertas: 459/641 = 71,61 %, por lo que cumple el umbral mínimo de 70 % LINE. Esta cifra se obtiene del contador global, no de sumar filas por clase del CSV.

Evidencia versionada: [reporte HTML](soporte/index.html), [CSV](soporte/jacoco.csv) y [XML con contadores globales](soporte/jacoco.xml).

El reporte de Soporte usa JaCoCo 0.8.11 con alcance `BUNDLE` y estas exclusiones configuradas en `microservicio-soporte/backend/pom.xml`:

- `ec/uteq/sga/soporte/grpc/incidencias/**` (stubs gRPC generados)
- `ec/uteq/sga/soporte/grpc/principal/**` (stubs gRPC generados)

Por tanto, el 71,61 % de LINE corresponde al alcance configurado del reporte JaCoCo, no a todas las clases sin exclusiones.

## Regla de interpretación

El umbral mínimo es **70 % de cobertura de líneas (LINE), medido por JaCoCo, para los módulos Java donde esté configurado**. Es un criterio de aprobación, no un resultado medido. No se mezclan métricas de JaCoCo con coverage.py.
