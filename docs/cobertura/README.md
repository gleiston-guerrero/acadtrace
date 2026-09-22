# Cobertura de código

Esta tabla es la fuente central de cobertura documentada para E8.

## Fuente oficial de las mediciones

Las cifras oficiales de cobertura de Principal, Secretaría, Soporte y la
aplicación móvil proceden de una misma ejecución de integración continua:
**CI #876**, run `35693153935`, sobre el commit
`6c1f67ab28d569643b4c7ec4f740d7221bd60b0f`.

Los archivos `jacoco.xml` descargados de esa ejecución están versionados en
`docs/cobertura/`. Cada módulo publica **una sola cifra oficial**, correspondiente
a la métrica que utiliza su compuerta de calidad. Los demás contadores permanecen
disponibles dentro del XML para auditoría, pero no se presentan como una segunda
cifra oficial de cobertura.

| Módulo | Herramienta | Alcance | Métrica oficial | Cobertura oficial | Compuerta | Evidencia | Estado | Comando reproducible |
|---|---|---|---|---:|---|---|---|---|
| `sga-principal` | JaCoCo 0.8.11 | Módulo completo (`BUNDLE`) con exclusiones justificadas en `pom.xml` | `INSTRUCTION` | **32,79 % (4668/14237)** | mínimo 30 % `INSTRUCTION` | `docs/cobertura/sga-principal/jacoco.xml`, CI #876 | Cumple | Desde `sga-principal`: `./mvnw clean test -q` |
| `microservicio-secretaria/backend` | JaCoCo 0.8.11 | Módulo completo (`BUNDLE`) con exclusiones justificadas en `pom.xml` | `LINE` | **74,44 % (2315/3110)** | mínimo 70 % `LINE` | `docs/cobertura/secretaria/jacoco.xml`, CI #876 | Cumple | Desde `microservicio-secretaria/backend`: `./mvnw clean test -Dsurefire.useFile=false` |
| `microservicio-soporte/backend` | JaCoCo 0.8.11 | Módulo completo (`BUNDLE`) con exclusiones justificadas en `pom.xml` | `LINE` | **71,56 % (458/640)** | mínimo 70 % `LINE` | `docs/cobertura/soporte/jacoco.xml`, CI #876 | Cumple | Desde `microservicio-soporte/backend`: `./mvnw clean verify` |
| `app-movil-docente` | JaCoCo 0.8.13 | Módulo completo (`BUNDLE`) con exclusiones estándar de código generado | `INSTRUCTION` | **10,21 % (7611/74556)** | mínimo 10 % `INSTRUCTION` | `docs/cobertura/movil/jacoco.xml`, CI #876 | Cumple | Desde `app-movil-docente`: `./gradlew testDebugUnitTest jacocoTestReport jacocoCoverageVerification lintDebug --no-daemon` |
| `microservicio-docente` | coverage.py | Paquete `docentes` con exclusiones declaradas en `.coveragerc` | `LINE` | **72,56 %** | mínimo 70 % `LINE` | Reporte `docs/cobertura/docente/` | Cumple | Desde `microservicio-docente`: `python -m pytest --cov=docentes --cov-fail-under=70` |

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

La cifra oficial de Secretaría es **74,44 % de cobertura de líneas**
(2115 de 2882 líneas). Se obtiene directamente del contador global `LINE`
del `jacoco.xml` generado por el CI #876, run `35693153935`, sobre el commit
`6c1f67ab28d569643b4c7ec4f740d7221bd60b0f`. La regla de calidad se aplica al módulo completo (`BUNDLE`) y exige
un mínimo de 70 % de cobertura de líneas.

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

La cifra oficial de Soporte es **71,56 % de cobertura de líneas**
(458 de 640 líneas). Se obtiene directamente del contador global `LINE`
del único `jacoco.xml` canónico, generado por el CI #876, run `35693153935`,
sobre el commit `6c1f67ab28d569643b4c7ec4f740d7221bd60b0f`. La compuerta exige un mínimo de 70 % de cobertura
de líneas a nivel de módulo completo (`BUNDLE`).

Evidencia versionada: [reporte HTML](soporte/index.html), [CSV](soporte/jacoco.csv) y [XML con contadores globales](soporte/jacoco.xml).

El reporte de Soporte usa JaCoCo 0.8.11 con alcance `BUNDLE` y estas exclusiones configuradas en `microservicio-soporte/backend/pom.xml`:

- `ec/uteq/sga/soporte/grpc/incidencias/**` (stubs gRPC generados)
- `ec/uteq/sga/soporte/grpc/principal/**` (stubs gRPC generados)

Por tanto, la cifra oficial de Soporte es 71,56 % de cobertura de líneas, calculada sobre el alcance configurado del módulo completo (`BUNDLE`).

## Regla de interpretación

Las compuertas se interpretan según la métrica oficial de cada módulo:
Principal exige un mínimo de 30 % de instrucciones (`INSTRUCTION`);
Secretaría y Soporte exigen un mínimo de 70 % de líneas (`LINE`);
la aplicación móvil utiliza una compuerta mínima de no regresión de 10 % de
instrucciones; y Docente exige 70 % de líneas mediante `coverage.py`.
El umbral es una regla de aprobación y no debe confundirse con la cifra
de cobertura efectivamente medida.
