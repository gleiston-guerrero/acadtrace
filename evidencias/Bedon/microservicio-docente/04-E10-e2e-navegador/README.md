# E10 — Pruebas de extremo a extremo sobre navegador

## Evidencia de ejecución y trazabilidad

La evidencia versionada en este directorio (`ejecucion-verde/`) fue generada automáticamente por la suite E2E de Playwright durante una ejecución en verde del flujo de integración continua (CI) de GitHub Actions.

### Metadatos de la corrida de CI

- **Workflow Run ID:** `35248870498`
- **Job ID:** `105295925033` (`E10 - Playwright Frontend Docente`)
- **Rama:** `devBedon`
- **Commit de origen:** `c9cc6135`
- **Resultado global:** `success` (6 pruebas pasadas de 6, 0 fallos, 0 omitidas)
- **Nota de armonización documental:** Los artefactos versionados (`playwright-report/index.html`, `e10-junit.xml` y las 6 capturas PNG) fueron generados por el runner de GitHub Actions en dicho run y preservados en el repositorio. El commit histórico `c2f81128` que los incorporó utilizó el término "local" de forma informal para indicar la extracción del artefacto al árbol de trabajo, pero la procedencia oficial de la suite y sus metadatos corresponde fehacientemente a la ejecución en CI citada.

## Arquitectura de ejecución aislada y efímera

A diferencia de configuraciones iniciales que dependían de un servidor externo persistente, la suite E10 se ejecuta sobre una infraestructura completamente efímera levantada en el runner:

1. **Base de datos efímera en contenedor (`postgres-e10`):** Se instancia en la red Docker privada `e10-docente`.
2. **Esquema canónico:** Se inicializa aplicando el DDL oficial `V8__baseline_completo.sql`.
3. **Población con datos sintéticos:** Se inyectan registros controlados mediante `microservicio-docente/experimentos/seed_e10.sql` y el comando de shell de Django, preparando la asignación docente, período de evaluación y matrícula requeridos para el flujo.
4. **Servicios backend:** Se levantan `sga-principal` (puerto 8080) y `microservicio-docente` (puerto 8081) comunicados por red interna Docker y gRPC interno (:9091).
5. **Servicios frontend:** Se levantan `sga-frontend` (puerto 5173) y `docente-frontend` (puerto 3000) mediante contenedores Node.
6. **Sondas de disponibilidad:** El flujo espera activamente a que los 4 endpoints respondan HTTP 200 antes de lanzar Playwright.

## Privacidad y datos sintéticos

En estricto cumplimiento de los principios éticos y de privacidad de la información:
- Los registros de prueba (nombres, matrículas, asignaciones y calificaciones visibles en las capturas como `06-registro-calificacion-restaurado.png`) son datos **100 % sintéticos**.
- Utilizan identificadores en el rango reservado de pruebas (`900001+`), generados por el script de seed para E10.
- No se utilizan datos de estudiantes reales ni credenciales de entornos de producción.

## Resultado de las pruebas

El archivo `ejecucion-verde/e10-junit.xml` certifica los resultados emitidos por el reporter JUnit de Playwright:

- **Total de pruebas:** 6
- **Aprobadas:** 6 (100 %)
- **Fallos:** 0
- **Omitidas:** 0
- **Tiempo de ejecución:** 45.95 segundos

### Casos de prueba verificados

1. `01-acceso-sin-autenticacion`: Redirección y protección ante accesos sin credenciales.
2. `02-login-dashboard`: Autenticación del docente y carga del panel principal.
3. `03-cierre-sesion-login`: Cierre de sesión seguro y retorno a la pantalla de login.
4. `04-consulta-cursos`: Visualización de asignaciones y cursos a cargo del docente.
5. `05-consulta-asistencia`: Interfaz de registro y consulta de asistencia estudiantil.
6. `06-registro-calificacion-restaurado`: Registro de calificación numérica y confirmación en la interfaz.

## Artefactos generados por Playwright

- **Informe HTML interactivo:** `ejecucion-verde/playwright-report/index.html` (conserva trazas y reportes completos).
- **Capturas visuales finales (`screenshot: "on"`):**
  - `01-acceso-sin-autenticacion.png`
  - `02-login-dashboard.png`
  - `03-cierre-sesion-login.png`
  - `04-consulta-cursos.png`
  - `05-consulta-asistencia.png`
  - `06-registro-calificacion-restaurado.png`