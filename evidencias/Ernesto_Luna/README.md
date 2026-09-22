# Evidencias — Ernesto Gregory Luna Mora

## Proyecto AcadTrace — Entrega 4 (PFC E4)

**Responsable:** Ernesto Gregory Luna Mora (`Ernesto835 <elunam4@uteq.edu.ec>`)  
**Rol Oficial en el Informe:** *Responsable de Microservicio Secretaría, Calidad / Pruebas Automatizadas y API Gateway (HAProxy)*  
**Equipo:** BCEL (Bedón, Castro, Emanuel, Luna)  
**Asignatura:** Aplicaciones Distribuidas (ISR-701) — UTEQ  
**Docente Evaluador:** Prof. Ing. Gleiston C. Guerrero-Ulloa, Mgs.  
**Repositorio:** `acadtrace`  
**Rama de desarrollo oficial:** `Ernesto-Luna`  
**Integración:** Pull Requests validados y fusionados hacia `main` (PR #132, PR #140, PR #142)

---

# 1. Descripción general

Este directorio contiene la consolidación estructurada de evidencias técnicas, operativas y experimentales correspondientes a los componentes bajo la responsabilidad directa de **Ernesto Gregory Luna Mora** en el proyecto AcadTrace:

1. **Microservicio de Secretaría (Spring Boot 3.2.5 + Java 21):**
   - Interfaz web del portal administrativo (Dashboard de estudiantes, matrículas, cursos y reportes).
   - Cobertura de pruebas con JaCoCo certificada en **74.42 % de líneas (LINE)** y **72.74 % de instrucciones** a nivel de `BUNDLE` (superando la compuerta mínima estricta del 70 %).
   - Integración con base de datos real en contenedores efímeros vía **Testcontainers (PostgreSQL 16)** y migraciones deterministas con **Flyway** sincronizadas al 100% con producción (secuencia completa de 17 migraciones `V8` a `V26`) en Secretaría y Principal (Entregable #37 / Criterio E13).
2. **Criptografía, Seguridad e Inmutabilidad (Entregable #43 / Criterios E2, E3):**
   - Disparador de base de datos **Append-Only** (`tg_auditoria_append_only`) en PostgreSQL que rechaza cualquier intento de `UPDATE` o `DELETE` con excepción fatal.
   - Sellado criptográfico con **HMAC-SHA256 en tiempo constante** (`HmacService.java`) y verificación en `experimentos/verificador_cadena.py` para detectar inserciones no autorizadas por usuarios de BD (`sga_app`).
   - Reconocimiento de bitácora relacional convencional modo `m1` sin falsos positivos de manipulación y rechazo estricto de mezclas no autorizadas en cadenas activas.
   - Suite completa y rigurosa de 32 pruebas unitarias aisladas (`experimentos/test_verificador_cadena_base.py`) que erradican mutaciones supervivientes (monotonía Lamport, ordenamiento por id_auditoria, compatibilidad con null en campos Java y enlaces de SGA Docente) e integrada en CI.
   - Cifrado simétrico autenticado **AES-256-GCM** (`CryptoService.java`) con IV aleatorio para datos de menores (LOPDP).
   - Protección perimetral de endpoints con token JWT HMAC-SHA256 (`HTTP 401 Unauthorized`).
3. **Calidad, Contratos OpenAPI y Matriz ISO/IEC 25010 (Entregables #4, #22, #48):**
   - Contrato OpenAPI 3.0 multi-servicio (`docs/api/openapi.yaml`) verificado y libre de discrepancias (Secretaría: 88 ops, Soporte: 16 ops, Principal: 13 ops, cubriendo endpoints de estudiantes por grado, matrículas y auditoría) con `docs/api/validate_openapi.py` y `scripts/verificar_openapi.py`.
   - Matriz ISO/IEC 25010 generada automáticamente (`generar_matriz.py`) con intervalos de confianza de Wilson al 95 % derivados formalmente desde datos crudos de Locust (Wilson 1927) y métricas de carga nominal y estrés oficial (200 usuarios, 98,684 peticiones, 0 fallos).
   - Firmas criptográficas SHA-256 reales de los artefactos de carga en disco validadas con `scripts/recalcular_metricas_carga.py --check-latex`.
   - Recompilación exacta del informe acumulativo LaTeX `Informe-E4_BCEL/TA-PFC-E4_BCEL.pdf` (38 páginas exactas, resolviendo Criterio de Piso 2).
   - Saneamiento formal de ramas y cierre del PR huérfano #176 en GitHub (Criterio de Piso 3).
4. **Trazabilidad y Revisión de Pares (Criterio C8):**
   - Mapeo directo de issues resueltos, pull requests asignados y revisiones cruzadas del equipo BCEL.

---

# 2. Estructura del directorio de evidencias

```text
evidencias/
└── Ernesto_Luna/
    ├── README.md                                    # Documento maestro explicativo
    │
    ├── 01-microservicio-secretaria/                 # Evidencias de Secretaría (Web, JaCoCo, Gestión)
    │   ├── portal-secretaria.png                    # Captura real del dashboard administrativo (localhost:5176/dashboard)
    │   ├── grados-cursos-secretaira.png             # Captura del módulo de Grados y Cursos de Secretaría
    │   ├── secretaria_portal.png                    # Captura integrada oficial del portal administrativo
    │   └── jacoco_cobertura_71_porciento.png        # Captura del reporte oficial JaCoCo superando el 70 % LINE (74.42 % LINE actual)
    │
    ├── 02-criptografia-y-seguridad/                 # Evidencias de seguridad, inmutabilidad y criptografía
    │   ├── trigger_append_only_rechazo.png          # Captura en terminal demostrando rechazo de UPDATE/DELETE por el trigger
    │   ├── pruebas_seguridad_hmac_aes.png           # Captura en terminal de pruebas unitarias HMAC y AES pasando al 100 %
    │   └── reporte_pruebas_criptografia_seguridad.txt # Reporte de ejecución con 13 pruebas unitarias exitosas (0 fallos)
    │
    ├── 03-calidad-contratos-y-gateway/              # Evidencias de pruebas de integración, contratos y HAProxy
    │   ├── swagger_openapi_secretaria.png           # Captura de interfaz Swagger UI con especificación OpenAPI 3.0
    │   ├── haproxy_dashboard.png                    # Captura de estadísticas en tiempo real del API Gateway HAProxy 2.9
    │   ├── pytest_contracts_and_integration.png     # Captura de consola ejecutando pruebas automatizadas (15 passed)
    │   └── pytest_contracts_and_integration.log     # Log oficial con las 15 pruebas de contratos y red 100 % verdes
    │
    └── 04-trazabilidad-github-c8/                   # Trazabilidad de Pull Requests e Issues cerrados
        └── README.md                                # Matriz de trazabilidad y referencias a PR #132, #140 y #142
```

---

# 3. Detalle de Evidencias Técnicas

## 3.1 Microservicio de Secretaría

### A. Portal Web Administrativo (`secretaria_portal.png`)
* **Ubicación:** `evidencias/Ernesto_Luna/01-microservicio-secretaria/secretaria_portal.png`
* **Referencia en informe LaTeX:** Figura `fig:portal_secretaria` (línea 530 de `Informe-E4_BCEL/TA-PFC-E4_BCEL.tex`).
* **Descripción:** Captura operativa del dashboard del portal web de Secretaría desplegado en entorno local (`localhost:5176/dashboard`). Demuestra la interfaz de usuario con navegación lateral y tarjetas de acceso a los módulos de:
  - Estudiantes y Ficha Estudiantil.
  - Grados y Cursos.
  - Matrículas e Historial Académico.
  - Gestión de Usuarios y Roles.
  - Importación Masiva y Reportes PDF institucionales.

### B. Cobertura de Pruebas JaCoCo al 74.42 % (`Tarea E8 / Punto 38`)
* **Ubicación del reporte oficial:** `docs/cobertura/secretaria/index.html`
* **Métrica oficial:**
  - **Líneas cubiertas (LINE):** 74.42 % (2,313 cubiertas de 3,108 líneas totales analizadas).
  - **Instrucciones cubiertas (INSTRUCTION):** 72.74 % (11,272 de 15,497 instrucciones).
  - **Batería de pruebas:** 96 pruebas automatizadas (0 fallos, 0 errores, 0 omitidas).
  - **Compuerta mínima:** Enforceable a nivel de `BUNDLE` al 70.00 % en `microservicio-secretaria/backend/pom.xml`.
* **Comando reproducible:**
  ```powershell
  cd microservicio-secretaria\backend
  .\mvnw.cmd test
  ```
* **Exclusiones configuradas de infraestructura en `pom.xml`:**
  - `**/ec/edu/uteq/sga/grpc/**`: Clases y stubs autogenerados por el compilador `protoc`.
  - `**/dto/**` y `**/payload/**`: Objetos de transferencia de datos con getters/setters de Lombok.
  - `**/config/**`: Clases de configuración de Spring Boot.
  - `**/entity/**`: Entidades JPA de mapeo relacional.

### C. Contenedores Efímeros y Migraciones Flyway (`Tarea E37 / Criterio E13`)
* **Archivos de prueba:**
  - `microservicio-secretaria/backend/src/test/java/ec/uteq/sga/secretaria/integration/SecretariaContainerIntegrationTest.java` (5 pruebas exitosas).
  - `sga-principal/src/test/java/ec/edu/uteq/sga/integration/AuditoriaFlywayMigrationContainerTest.java` (2 pruebas exitosas).
  - `sga-principal/src/test/java/ec/edu/uteq/sga/integration/AuditoriaPostgresContainerTest.java` (2 pruebas exitosas).
  - `sga-principal/src/test/java/ec/edu/uteq/sga/integration/AuditoriaCadenaConcurrencyE3Test.java` (1 prueba exitosa).
  - `sga-principal/src/test/java/ec/edu/uteq/sga/integration/AuditoriaCadenaConcurrencyServicioE3Test.java` (1 prueba exitosa).
* **Descripción de la suite:** Batería completa de pruebas de integración que levantan contenedores Docker efímeros con `postgres:16-alpine` vía Testcontainers, aplican automáticamente la secuencia integral de 17 migraciones Flyway (`V8` a `V26` desde `classpath:db/migration`) sincronizadas al 100 % con los esquemas de producción, y validan rigurosamente:
  1. La aplicación determinista y sin fallos de todas las migraciones DDL/DML hasta `V26__objetos_faltantes_codigo.sql`.
  2. La creación correcta de la tabla `sga_principal.auditoria` y la tabla de estado `sga_principal.estado_cadena_auditoria`.
  3. La persistencia de auditoría con schema `SECRETARIA`, acción `CREAR`, relojes lógicos de Lamport, vectores de reloj y sellos HMAC de 64 caracteres.
  4. El bloqueo estricto e inquebrantable de modificaciones con el trigger `tg_auditoria_append_only`.
  5. La separación de privilegios mediante el rol restringido `sga_app`, demostrando que usuarios de aplicación sin permisos `SUPERUSER` no pueden alterar la bitácora ni eliminar registros.
  6. La concurrencia de inserción en la cadena de bloques con hilos simultáneos garantizando la secuencia ininterrumpida de hashes encadenados.
* **Reportes Surefire Oficiales versionados:**
  - `docs/evidencia/pruebas/secretaria/TEST-ec.uteq.sga.secretaria.integration.SecretariaContainerIntegrationTest.xml`
  - `docs/evidencia/pruebas/sga-principal/TEST-ec.edu.uteq.sga.integration.AuditoriaFlywayMigrationContainerTest.xml`
  - `docs/evidencia/pruebas/sga-principal/TEST-ec.edu.uteq.sga.integration.AuditoriaPostgresContainerTest.xml`
  - `docs/evidencia/pruebas/sga-principal/TEST-ec.edu.uteq.sga.integration.AuditoriaCadenaConcurrencyE3Test.xml`
  - `docs/evidencia/pruebas/sga-principal/TEST-ec.edu.uteq.sga.integration.AuditoriaCadenaConcurrencyServicioE3Test.xml`

---

## 3.2 Criptografía, Seguridad e Inmutabilidad

### A. Disparador Append-Only en PostgreSQL (`Pieza 1 - Defensa Oral`)
* **Archivo fuente:** `sga-principal/sql/V9__trigger_auditoria_append_only.sql`
* **Definición SQL:**
  ```sql
  CREATE OR REPLACE FUNCTION sga_principal.prohibir_modificacion_auditoria()
  RETURNS TRIGGER AS $$
  BEGIN
      RAISE EXCEPTION 'Operacion rechazada: La tabla sga_principal.auditoria es una bitacora inmutable de solo adicion (append-only) protegida bajo estandares ISO/IEC 25010';
  END;
  $$ LANGUAGE plpgsql;

  CREATE TRIGGER tg_auditoria_append_only
  BEFORE UPDATE OR DELETE ON sga_principal.auditoria
  FOR EACH ROW
  EXECUTE FUNCTION sga_principal.prohibir_modificacion_auditoria();
  ```
* **Comportamiento verificado:** Cualquier intento de ejecución de `UPDATE sga_principal.auditoria ...` o `DELETE FROM sga_principal.auditoria ...` arroja una excepción SQL `DataAccessException` y cancela la transacción en PostgreSQL.

### B. Firma Digital HMAC-SHA256 en Tiempo Constante (`Pieza 2`)
* **Archivo fuente:** `microservicio-secretaria/backend/src/main/java/ec/uteq/sga/secretaria/infrastructure/security/HmacService.java`
* **Archivo de prueba:** `microservicio-secretaria/backend/src/test/java/ec/uteq/sga/secretaria/infrastructure/security/AuditoriaIntegridadTest.java`
* **Descripción:** Implementa generación y verificación de firmas HMAC-SHA256 sobre campos canónicos concatenados (`schema`, `trace_id`, `username`, `accion`, `tabla`, `registro_id`, `descripcion`, `resultado`, `timestamp`).
* **Protección contra Timing Attacks:** Emplea `java.security.MessageDigest.isEqual` para realizar la verificación en tiempo constante ($O(1)$).
* **Detección de Manipulación (Ataque T1):** La prueba `testDeteccionManipulacion_RegistroIdAlterado` simula la alteración maliciosa del `registro_id` en base de datos; la firma HMAC resultante difiere inmediatamente y la verificación retorna `false`.

### C. Cifrado Simétrico Autenticado AES-256-GCM (`Pieza 3 - Criterio Ético C9`)
* **Archivo fuente:** `microservicio-secretaria/backend/src/main/java/ec/uteq/sga/secretaria/infrastructure/security/CryptoService.java`
* **Archivo de prueba:** `microservicio-secretaria/backend/src/test/java/ec/uteq/sga/secretaria/infrastructure/security/CryptoServiceTest.java`
* **Descripción:** Protege los datos sensibles de estudiantes menores de edad (cédulas, nombres de representantes, direcciones) conforme a la LOPDP ecuatoriana.
* **Parámetros criptográficos:**
  - Algoritmo: `AES/GCM/NoPadding` (256 bits).
  - IV aleatorio por cifrado: 12 bytes (`GCM_IV_LENGTH = 12`).
  - Tag de autenticación: 128 bits (`GCM_TAG_LENGTH_BITS = 128`).
  - Formato de almacenamiento: `Base64(IV + Ciphertext + Tag)`.

### D. Verificador Criptográfico de Cadena e Inmutabilidad (`Entregable #43 / Criterio E3`)
* **Archivos fuente:**
  - `experimentos/verificador_cadena.py`: Verificador forense de integridad de cadena de bloques y bitácora.
  - `experimentos/test_verificador_cadena_base.py`: Suite de 32 pruebas unitarias automatizadas con pytest.
* **Mejoras implementadas:**
  1. **Soporte de bitácora relacional `m1` legítima y rechazo de mezcla no autorizada:** Detección precisa de filas sin hash previo ni bloque cuando la auditoría opera legítimamente en modo relacional estándar (`m1`), pero rechazo categórico con código de salida 2 si se detecta una mezcla de filas `m1` dentro de una cadena `v1` activa o cuando `estado_cadena_auditoria` tiene una cabeza inicializada.
  2. **Verificación criptográfica HMAC-SHA256 y paridad con Java:** Verificación canónica de la firma HMAC contra la clave simétrica (`JWT_SECRET` cargada automáticamente de entorno o `.env`). Se implementó paridad estricta con el cálculo de Java `String.valueOf(null)` formateando valores nulos en `registro_id` y `trace_id` como `"null"`, preservando a la vez retrocompatibilidad con esquemas legacy mediante `legacy_null`.
  3. **Verificación estricta de orden cronológico y causal:** Comprobación estricta de monotonía creciente en relojes lógicos de Lamport ($L_i > L_{i-1}$) y ordenamiento monótono de identificadores primarios `id_auditoria`, invalidando cualquier retroceso o reordenamiento artificial.
  4. **Validación de enlaces foráneos de SGA Docente:** Detección de enlaces ficticios o huérfanos validando la presencia del `id_auditoria_docente` contra la bitácora `sga_docente.eventos_auditoria`.
  5. **Aislamiento estricto y resistencia a mutaciones:** 32 pruebas unitarias que cubren exhaustivamente cada escenario forense y código de retorno (`0`: íntegro, `1`: error de sintaxis/argumentos, `2`: violación de integridad/manipulación detectada, `3`: fallo de conectividad de BD).
* **Resultado:** **32 passed in 1.64s (100 % de éxito)**.
* **Comando reproducible:**
  ```powershell
  python -m pytest experimentos/test_verificador_cadena_base.py -v
  ```

### E. Protección de Endpoints JWT (`Tarea E46 / Criterio E46`)
* **Archivo fuente:** `microservicio-secretaria/backend/src/main/java/ec/uteq/sga/secretaria/infrastructure/security/JwtAuthFilter.java`
* **Descripción:** Eliminación de cualquier mecanismo de bypass de credenciales o secretos en texto plano. Las solicitudes a `/api/secretaria/**` sin encabezado `Authorization: Bearer <token_valido>` son interceptadas por `JwtAuthFilter` y rechazadas deterministamente con estado `HTTP 401 Unauthorized`.

---

## 3.3 Calidad, Pruebas Automatizadas, OpenAPI y API Gateway

### A. Verificación del Contrato OpenAPI 3.0 (`Entregable #4`)
* **Archivo fuente:** `scripts/verificar_openapi.py` y `docs/api/validate_openapi.py`
* **Especificación analizada:** `docs/api/openapi.yaml` (y `microservicio-secretaria/backend/src/main/resources/openapi.yaml`).
* **Mejoras implementadas:**
  - Soporte de verificación selectiva y multi-servicio vía argumentos de línea de comandos (`--service {secretaria,soporte,principal,all}` y `--url`).
  - Sincronización y validación estricta de paridad entre la especificación viva y las operaciones en runtime para los 3 microservicios:
    - **Secretaría:** 88 rutas/operaciones versionadas validadas con 0 discrepancias.
    - **Soporte:** 16 rutas/operaciones versionadas validadas con 0 discrepancias (depuración del endpoint superfluo `/health`).
    - **Principal:** 13 rutas/operaciones versionadas validadas con 0 discrepancias (incorporación formal de `/api/estudiantes/por-grado`, `/api/matriculas/{id}`, `/api/matriculas/{id}/pdf` y `/api/matriculas/{id}/estado`).
  - Validación de esquema OpenAPI 3.0 determinista con 104 rutas canónicas y 86 referencias locales resueltas (`python docs/api/validate_openapi.py`).
* **Comandos reproducibles:**
  ```powershell
  python docs/api/validate_openapi.py
  python scripts/verificar_openapi.py --service all
  ```

### B. Matriz de Calidad ISO/IEC 25010 y Cifras de Carga (`Entregables #22 y #48`)
* **Generador automático:** `generar_matriz.py`
* **Verificador de consistencia LaTeX/CSV:** `scripts/recalcular_metricas_carga.py`
* **Artefactos certificados:**
  - `docs/experimentos/resultados/matriz_iso25010.csv`
  - `Informe-E4_BCEL/matriz_iso25010_generada.tex`
  - `Informe-E4_BCEL/cifras_carga_generadas.tex`
* **Garantías técnicas:**
  - **Intervalos de confianza de Wilson al 95 %:** Implementación rigurosa de la formulación de Wilson (1927) en `generar_matriz.py` para proporciones binomiales en muestras finitas:
    $$w = \frac{\hat{p} + \frac{z^2}{2n} \pm z \sqrt{\frac{\hat{p}(1-\hat{p})}{n} + \frac{z^2}{4n^2}}}{1 + \frac{z^2}{n}}$$
    obteniendo intervalos de `[99.97 %, 100.00 %]` en carga nominal (50 usuarios) y `[99.99 %, 100.00 %]` en estrés oficial (200 usuarios con 98,684 peticiones y 0 fallos).
  - Sustento teórico y citación formal de `\cite{wilson1927probable}` incorporada en `Informe-E4_BCEL/TA-PFC-E4_BCEL.tex` y referenciada en `referencias.bib`.
  - Unificación de ruta de cobertura JaCoCo de soporte a `docs/cobertura/soporte/jacoco.xml` (71.56 % LINE).
  - Verificación estricta de hashes SHA-256 de los logs de Locust en `docs/locust/README.md` y `docs/locust/entorno_medicion.md`.
* **Comandos reproducibles:**
  ```powershell
  python generar_matriz.py --write-csv --write-latex
  python scripts/recalcular_metricas_carga.py --check-latex
  ```

### C. Recompilación Oficial del Informe Académico (`Criterio de Piso 2`)
* **Documento fuente:** `Informe-E4_BCEL/TA-PFC-E4_BCEL.tex`
* **Documento compilado:** `Informe-E4_BCEL/TA-PFC-E4_BCEL.pdf` (38 páginas exactas, 3.23 MB).
* **Entorno de compilación:** MiKTeX (`pdflatex` + `bibtex`).
* **Validación:** Se ejecutaron 3 pasadas de `pdflatex` y 1 de `bibtex` asegurando la resolución completa de todas las citas bibliográficas, referencias cruzadas y tablas generadas (`matriz_iso25010_generada.tex` y `cifras_carga_generadas.tex`), erradicando cualquier desincronización entre el PDF entregado y los fuentes del repositorio.

### D. Suite Automatizada de Integración y Contratos en Python (`Criterio C7 / Listado 3`)
* **Ubicación de log generado:** `evidencias/Ernesto_Luna/03-calidad-contratos-y-gateway/pytest_contracts_and_integration.log`
* **Archivos ejecutados:**
  1. `tests/contract/test_contracts.py` (7 tests: sintaxis `proto3`, signaturas RPC y paridad con OpenAPI).
  2. `tests/integration/test_cross_service_integration.py` (8 tests: HAProxy, inyección `X-Trace-Id`, esquemas de aislamiento y trigger append-only).
* **Resultado:** **15 passed in 0.90s (100 % de éxito)**.
* **Comando reproducible:**
  ```powershell
  python -m pytest tests/contract tests/integration -v
  ```

### E. Gateway HAProxy 2.9 y Balanceo de Carga
* **Archivo de configuración:** `docker/haproxy/haproxy.cfg`
* **Funcionalidad:**
  - Punto único de entrada seguro (API Gateway) en el puerto `8080`.
  - Enrutamiento por prefijo de ruta: `/api/secretaria/` hacia el clúster de microservicio Secretaría en el puerto `8082`.
  - Algoritmos de balanceo verificados: `roundrobin` para peticiones sin estado y `leastconn` para transacciones de persistencia.
  - Verificación activa de salud (`health checks`) contra `/actuator/health`.

### F. Observabilidad y Correlación Distribuida con MDC
* **Archivo fuente:** `microservicio-secretaria/backend/src/main/java/ec/uteq/sga/secretaria/infrastructure/common/TraceIdFilter.java`
* **Archivo de prueba:** `microservicio-secretaria/backend/src/test/java/ec/uteq/sga/secretaria/infrastructure/common/TraceIdFilterTest.java`
* **Descripción:** Intercepta cada petición HTTP, extrae o genera un identificador único UUID `X-Trace-Id`, y lo inyecta en el contexto `MDC` (Mapped Diagnostic Context) de SLF4J. Esto garantiza que todos los logs estructurados en JSON incluyan el `trace_id` para correlación distribuida de extremo a extremo.

---

## 3.4 Trazabilidad y Saneamiento de Repositorio (`Criterio C8 / Criterio de Piso 3`)

En cumplimiento del Criterio C8 (Revisión de Pares y Trazabilidad en GitHub) y el Criterio de Piso 3 (Saneamiento de ramas y PRs huérfanos):

1. **Cierre de PRs huérfanos:** Se identificó y cerró formalmente en GitHub el PR #176 huérfano (`gh pr close 176`), dejando el repositorio con 0 pull requests sueltos o conflictivos pendientes de resolución.
2. **Registro de Trazabilidad:**

| Tarea / Hito | Issue GitHub | Rama de Origen | Revisor de Pares | Descripción del Aporte | Estado |
|---|---|---|---|---|---|
| **Tarea B** | Issue #51 | `Ernesto-Luna` | Juliana Emanuel | Implementación de `TraceIdFilter` con SLF4J MDC y compuerta JaCoCo del 70 % superada en Secretaría. | **FUSIONADO** |
| **Tarea D** | Issue #55 | `Ernesto-Luna` | Keyla Bedón | Validación del disparador `tg_auditoria_append_only` en PostgreSQL (rechazo de UPDATE/DELETE) y soporte de modos `M0-M3`. | **FUSIONADO** |
| **Tarea E** | Issue #60 | `feat/cluster-db` | Ernesto Luna | Comprobación de compatibilidad binaria Protobuf v3 y contratos gRPC con paso limpio en CI. | **FUSIONADO** |
| **PR #132** | PR #132 | `Ernesto-Luna` | Equipo BCEL | Implementación de rol restringido `sga_app`, variables de entorno y validador de arranque. | **FUSIONADO** |
| **PR #140** | PR #140 | `Ernesto-Luna` | Pedro Castro | Eliminación de bypass de tokens y eliminación de secretos por defecto en Secretaría. | **FUSIONADO** |
| **PR #142** | PR #142 | `Ernesto-Luna` | Equipo BCEL | Cierre definitivo de tareas #4 (OpenAPI), #37 (Testcontainers con Flyway) y #38 (exclusiones JaCoCo). | **FUSIONADO** |
| **PR #176** | PR #176 | `fix/correcciones-retroalimentacion-docente` | Ernesto Luna | Saneamiento de PR huérfano para cumplir Criterio de Piso 3 de evaluación. | **CERRADO** |

---

# 4. Comandos de Reproducción Rápida

Para verificar y reproducir en cualquier máquina los resultados certificados de Ernesto Luna:

```powershell
# 1. Pruebas automatizadas de contratos e integración (Python)
python -m pytest tests/contract tests/integration -v

# 2. Verificador de cadena forense y pruebas de mutación (32 passed)
python -m pytest experimentos/test_verificador_cadena_base.py -v

# 3. Validación y paridad del contrato OpenAPI multi-servicio
python docs/api/validate_openapi.py
python scripts/verificar_openapi.py --service all

# 4. Regeneración y verificación de consistencia de Matriz ISO/IEC 25010 y Carga
python generar_matriz.py --write-csv --write-latex
python scripts/recalcular_metricas_carga.py --generate-latex --check-latex

# 5. Pruebas unitarias de seguridad y criptografía (Java / Spring Boot)
cd microservicio-secretaria\backend
.\mvnw.cmd test "-Dtest=AuditoriaIntegridadTest,CryptoServiceTest,JwtServiceTest"

# 6. Suite de Contenedores Efímeros Testcontainers & Flyway (Secretaría)
.\mvnw.cmd test "-Dtest=SecretariaContainerIntegrationTest" "-Djacoco.skip=true"

# 7. Suites de Contenedores Efímeros Testcontainers & Flyway (SGA Principal)
cd ..\..\sga-principal
.\mvnw.cmd test "-Dtest=AuditoriaFlywayMigrationContainerTest,AuditoriaPostgresContainerTest,AuditoriaCadenaConcurrencyE3Test,AuditoriaCadenaConcurrencyServicioE3Test" "-Djacoco.skip=true"

# 8. Suite completa de Secretaría con verificación de compuerta JaCoCo (70 % LINE)
cd ..\microservicio-secretaria\backend
.\mvnw.cmd test
```

---

*Carpeta de evidencias generada y certificada para la Evaluación Final PFC E4 — AcadTrace.*
