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
   - Integración con base de datos real en contenedores efímeros vía **Testcontainers (PostgreSQL 16)** y migraciones deterministas con **Flyway** (Criterios E13, E6 y E37).
2. **Criptografía, Seguridad e Inmutabilidad (Criterios C9, C10, P5):**
   - Disparador de base de datos **Append-Only** (`tg_auditoria_append_only`) en PostgreSQL que rechaza cualquier intento de `UPDATE` o `DELETE` con excepción fatal.
   - Sellado criptográfico con **HMAC-SHA256 en tiempo constante** (`HmacService.java`) y suite unitaria de detección de manipulaciones directas en BD (Ataque T1) y borrado en cadena (Ataque T2).
   - Cifrado simétrico autenticado **AES-256-GCM** (`CryptoService.java`) con vector de inicialización (IV) de 12 bytes aleatorio para resguardar la privacidad de datos de menores (LOPDP).
   - Protección perimetral de endpoints con token JWT HMAC-SHA256 y rechazo estricto `HTTP 401 Unauthorized` ante solicitudes sin credencial (Criterio E46).
3. **Calidad, Pruebas Automatizadas y API Gateway (Criterios C7 - Listado 3, C10):**
   - Batería de pruebas automatizadas en Python (`tests/contract/` e `tests/integration/`) para verificación de contratos binarios Protobuf v3 y OpenAPI 3.0.
   - Enrutamiento perimetral y balanceo de carga en capa 7 mediante **HAProxy 2.9** con algoritmos `roundrobin` y `leastconn`.
   - Propagación de trazas distribuidas e inyección de encabezados de correlación `X-Trace-Id` utilizando el filtro `TraceIdFilter` con el patrón MDC de SLF4J.
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
* **Archivo de prueba:** `microservicio-secretaria/backend/src/test/java/ec/uteq/sga/secretaria/integration/SecretariaContainerIntegrationTest.java`
* **Descripción:** Prueba de integración que levanta un contenedor Docker efímero con `postgres:16-alpine`, ejecuta automáticamente las migraciones Flyway (`classpath:db/migration`), y valida:
  1. La creación correcta de la tabla `sga_principal.auditoria`.
  2. La persistencia de auditoría con schema `SECRETARIA`, acción `CREAR` y firma HMAC de 64 caracteres.
  3. El bloqueo estricto de modificaciones con el trigger `tg_auditoria_append_only`.
  4. La separación de privilegios mediante el rol `sga_app` demostrando que usuarios sin privilegios no pueden alterar la tabla incluso con el trigger temporalmente deshabilitado.

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

### D. Protección de Endpoints JWT (`Tarea E46 / Criterio E46`)
* **Archivo fuente:** `microservicio-secretaria/backend/src/main/java/ec/uteq/sga/secretaria/infrastructure/security/JwtAuthFilter.java`
* **Descripción:** Eliminación de cualquier mecanismo de bypass de credenciales o secretos en texto plano. Las solicitudes a `/api/secretaria/**` sin encabezado `Authorization: Bearer <token_valido>` son interceptadas por `JwtAuthFilter` y rechazadas deterministamente con estado `HTTP 401 Unauthorized`.

---

## 3.3 Calidad, Pruebas Automatizadas y API Gateway

### A. Suite Automatizada en Python (`Criterio C7 / Listado 3`)
* **Ubicación de log generado:** `evidencias/Ernesto_Luna/03-calidad-contratos-y-gateway/pytest_contracts_and_integration.log`
* **Archivos ejecutados:**
  1. `tests/contract/test_contracts.py` (7 tests: sintaxis `proto3`, signaturas RPC y paridad con OpenAPI).
  2. `tests/integration/test_cross_service_integration.py` (8 tests: HAProxy, inyección `X-Trace-Id`, esquemas de aislamiento y trigger append-only).
* **Resultado:** **15 passed in 0.90s (100 % de éxito)**.
* **Comando reproducible:**
  ```powershell
  python -m pytest tests/contract tests/integration -v
  ```

### B. Gateway HAProxy 2.9 y Balanceo de Carga
* **Archivo de configuración:** `docker/haproxy/haproxy.cfg`
* **Funcionalidad:**
  - Punto único de entrada seguro (API Gateway) en el puerto `8080`.
  - Enrutamiento por prefijo de ruta: `/api/secretaria/` hacia el clúster de microservicio Secretaría en el puerto `8082`.
  - Algoritmos de balanceo verificados: `roundrobin` para peticiones sin estado y `leastconn` para transacciones de persistencia.
  - Verificación activa de salud (`health checks`) contra `/actuator/health`.

### C. Observabilidad y Correlación Distribuida con MDC
* **Archivo fuente:** `microservicio-secretaria/backend/src/main/java/ec/uteq/sga/secretaria/infrastructure/common/TraceIdFilter.java`
* **Archivo de prueba:** `microservicio-secretaria/backend/src/test/java/ec/uteq/sga/secretaria/infrastructure/common/TraceIdFilterTest.java`
* **Descripción:** Intercepta cada petición HTTP, extrae o genera un identificador único UUID `X-Trace-Id`, y lo inyecta en el contexto `MDC` (Mapped Diagnostic Context) de SLF4J. Esto garantiza que todos los logs estructurados en JSON incluyan el `trace_id` para correlación distribuida de extremo a extremo.

---

## 3.4 Trazabilidad y Gestión de Issues en GitHub (`Criterio C8`)

En cumplimiento del Criterio C8 (Revisión de Pares y Trazabilidad en GitHub), se registran los siguientes hitos de desarrollo e integración liderados por Ernesto Luna:

| Tarea / Hito | Issue GitHub | Rama de Origen | Revisor de Pares | Descripción del Aporte | Estado |
|---|---|---|---|---|---|
| **Tarea B** | Issue #51 | `Ernesto-Luna` | Juliana Emanuel | Implementación de `TraceIdFilter` con SLF4J MDC y compuerta JaCoCo del 70 % superada en Secretaría. | **FUSIONADO** |
| **Tarea D** | Issue #55 | `Ernesto-Luna` | Keyla Bedón | Validación del disparador `tg_auditoria_append_only` en PostgreSQL (rechazo de UPDATE/DELETE) y soporte de modos `M0-M3`. | **FUSIONADO** |
| **Tarea E** | Issue #60 | `feat/cluster-db` | Ernesto Luna | Comprobación de compatibilidad binaria Protobuf v3 y contratos gRPC con paso limpio en CI. | **FUSIONADO** |
| **PR #132** | PR #132 | `Ernesto-Luna` | Equipo BCEL | Implementación de rol restringido `sga_app`, variables de entorno y validador de arranque. | **FUSIONADO** |
| **PR #140** | PR #140 | `Ernesto-Luna` | Pedro Castro | Eliminación de bypass de tokens y eliminación de secretos por defecto en Secretaría. | **FUSIONADO** |
| **PR #142** | PR #142 | `Ernesto-Luna` | Equipo BCEL | Cierre definitivo de tareas #4 (OpenAPI), #37 (Testcontainers con Flyway) y #38 (exclusiones JaCoCo). | **FUSIONADO** |

---

# 4. Comandos de Reproducción Rápida

Para verificar y reproducir en cualquier máquina los resultados documentados en esta carpeta:

```powershell
# 1. Pruebas automatizadas de contratos e integración (Python)
python -m pytest tests/contract tests/integration -v

# 2. Pruebas unitarias de seguridad y criptografía (Java / Spring Boot)
cd microservicio-secretaria\backend
.\mvnw.cmd test "-Dtest=AuditoriaIntegridadTest,CryptoServiceTest,JwtServiceTest"

# 3. Suite completa de Secretaría con verificación de compuerta JaCoCo (70 % LINE)
.\mvnw.cmd test
```

---

*Carpeta de evidencias generada y certificada para la Evaluación Final PFC E4 — AcadTrace.*
