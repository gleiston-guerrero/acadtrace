# E10 — Pruebas de extremo a extremo sobre navegador

## Evidencia de ejecución y trazabilidad

El directorio `ejecucion-verde/` conserva los artefactos generados automáticamente por la suite E2E de Playwright durante una ejecución en verde de GitHub Actions.

Los archivos fueron descargados directamente de la corrida documentada y posteriormente versionados en el repositorio. Las capturas no fueron elaboradas ni modificadas manualmente.

### Ejecución de CI documentada

- **Workflow:** `CI/CD Pipeline - AcadTrace (Entrega 4)`
- **Workflow Run ID:** `35665200470`
- **Job:** `E10 - Playwright Frontend Docente`
- **Job ID:** `106549280440`
- **Rama:** `devBedon`
- **Commit ejecutado:** `693949c317dfd9c320b04f404a4d874740422664`
- **Inicio del job:** `2026-09-21T22:56:42Z`
- **Fin del job:** `2026-09-21T22:59:43Z`
- **Conclusión:** `success`
- **Commit que versionó los artefactos:** `648f9bda`

El commit `648f9bda` conserva en el árbol los artefactos producidos por la corrida anterior y no modifica el escenario que fue sometido a prueba.

## Arquitectura de ejecución aislada y efímera

La suite E10 se ejecuta contra un sistema levantado dentro del propio flujo de CI y no depende de la antigua base PostgreSQL compartida.

El entorno se compone de:

1. PostgreSQL efímero en el contenedor `postgres-e10`;
2. red Docker privada `e10-docente`;
3. esquema y migraciones requeridas por el sistema;
4. datos sintéticos cargados mediante `microservicio-docente/experimentos/seed_e10.sql`;
5. backend `sga-principal`;
6. backend `microservicio-docente`;
7. frontend principal;
8. frontend Docente;
9. Playwright, ejecutado después de las sondas de disponibilidad.

Las contraseñas de base de datos, la credencial del docente y los secretos internos se generan exclusivamente para la ejecución y no corresponden a credenciales de producción.

## Escenario académico sintético

Los registros utilizados en la prueba son controlados y exclusivos de E10:

- **Docente:** `Andrea Prueba E10`
- **Usuario:** `docente.e10@acadtrace.test`
- **ID docente:** `900001`
- **Cédula sintética docente:** `9999000001`
- **Estudiante:** `Mateo Prueba E10`
- **ID estudiante:** `900001`
- **ID matrícula:** `900001`
- **Cédula sintética estudiante:** `9999000002`
- **Año lectivo:** `2026 - 2027`
- **Nivel:** `Educacion General Basica E10`
- **Grado:** `Decimo ano EGB E10`
- **Paralelo:** `A`
- **Asignatura:** `Matematica E10`
- **Actividad:** `Tarea de ecuaciones lineales E10`
- **Calificación inicial:** `8.50 / 10`

Los identificadores `900001`, las cédulas de prueba `9999000001` y `9999000002`, los nombres y el dominio `.test` forman parte de un escenario deliberadamente sintético.

La corrida no utiliza nombres, cédulas, asignaciones ni calificaciones procedentes de la antigua base compartida.

## Resultado certificado por JUnit

El archivo `ejecucion-verde/e10-junit.xml` fue producido por el reporter JUnit de Playwright durante la corrida documentada.

- **Total:** 6
- **Aprobadas:** 6
- **Fallos:** 0
- **Errores:** 0
- **Omitidas:** 0
- **Tiempo acumulado:** `13.098575` segundos

Los seis casos cubren:

1. acceso sin autenticación;
2. autenticación del docente y carga del dashboard;
3. cierre de sesión;
4. consulta de cursos;
5. consulta de asistencia;
6. registro de calificación y restauración del estado original.

## Artefactos conservados

- `ejecucion-verde/e10-junit.xml`
- `ejecucion-verde/playwright-report/index.html`
- `ejecucion-verde/01-acceso-sin-autenticacion.png`
- `ejecucion-verde/02-login-dashboard.png`
- `ejecucion-verde/03-cierre-sesion-login.png`
- `ejecucion-verde/04-consulta-cursos.png`
- `ejecucion-verde/05-consulta-asistencia.png`
- `ejecucion-verde/06-registro-calificacion-restaurado.png`

Las capturas `01` y `03` representan la misma pantalla de login y por ello pueden ser idénticas byte a byte. Playwright conserva cinco adjuntos PNG únicos para las seis capturas finales.

## Integridad SHA-256

| Artefacto | SHA-256 |
| --- | --- |
| `e10-junit.xml` | `2ccd59ec542f834802a14cce09142e8b2d4a2e3f121db7ce439c70a4eac0ac38` |
| `playwright-report/index.html` | `0dfd328c09c5ace7df2e8feef1c956fe4d660a039273c78b1c3cd09358776ce9` |
| `01-acceso-sin-autenticacion.png` | `01cd16bbd4feba1978873876237c5aa35f7bf29e787d2ff50e517846f53a8ad9` |
| `02-login-dashboard.png` | `1e090b93d816af6bb0207b07313ff57ec483df9094c828df9b33739eaa43abc7` |
| `03-cierre-sesion-login.png` | `01cd16bbd4feba1978873876237c5aa35f7bf29e787d2ff50e517846f53a8ad9` |
| `04-consulta-cursos.png` | `5fdd019e56459270108d059931950d5ff0cb1feb28b832efcfd9311885bb0c55` |
| `05-consulta-asistencia.png` | `912119a54ae69b6fb42d3e637b6209d5444876dcbdea94136eb66fff83b31853` |
| `06-registro-calificacion-restaurado.png` | `adc0323356ee4bd15cf265bf49da5b3b4274c93398124dcb43f9eb455629d613` |

Los hashes anteriores se calculan directamente sobre los archivos versionados en `ejecucion-verde/`.
