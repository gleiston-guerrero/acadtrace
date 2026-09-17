# E10 - Pruebas de extremo a extremo sobre navegador

## Evidencia de ejecución

La evidencia de este directorio fue generada automáticamente por la suite E2E de Playwright durante una ejecución satisfactoria del flujo de integración continua.

El job `E10 - Playwright Frontend Docente` construye y levanta los servicios necesarios para E10 antes de ejecutar los recorridos de navegador. Playwright ejecuta las pruebas contra ese sistema levantado por el propio flujo, en lugar de depender de un entorno E2E desplegado externamente.

No se utilizan capturas elaboradas manualmente ni capturas de la configuración de GitHub Actions como evidencia de los recorridos.

La suite ejecutó seis pruebas sobre el frontend Docente:

1. Autenticación del docente y acceso al dashboard.
2. Consulta de cursos mediante la interfaz.
3. Consulta de asistencia de un curso.
4. Registro de una calificación y restauración del estado original.
5. Cierre de sesión y retorno al Login.
6. Protección del acceso sin autenticación.

## Resultado registrado

El archivo `ejecucion-verde/e10-junit.xml` fue producido por el reporter JUnit de Playwright.

Resultado de la ejecución conservada:

- Pruebas ejecutadas: 6
- Fallos: 0
- Omitidas: 0
- Errores: 0
- Tiempo registrado: 45.952967 segundos

## Capturas generadas por Playwright

Las siguientes imágenes fueron generadas automáticamente por Playwright mediante la configuración `screenshot: "on"`:

- `01-acceso-sin-autenticacion.png`
- `02-login-dashboard.png`
- `03-cierre-sesion-login.png`
- `04-consulta-cursos.png`
- `05-consulta-asistencia.png`
- `06-registro-calificacion-restaurado.png`

Las capturas corresponden a los estados finales observados por el navegador durante los seis recorridos E2E.

## Informe Playwright versionado

El informe HTML generado por Playwright durante la ejecución satisfactoria se conserva en:

`ejecucion-verde/playwright-report/index.html`

De esta forma, el informe de la ejecución en verde queda almacenado en el árbol del repositorio junto con el resultado JUnit y las capturas producidas por la suite.

## Generación en CI

El job `E10 - Playwright Frontend Docente`:

1. instala Playwright y Chromium;
2. construye los servicios requeridos para E10;
3. levanta los backends Principal y Docente;
4. levanta los frontends Principal y Docente;
5. espera a que el sistema esté disponible;
6. ejecuta los seis recorridos E2E sobre ese sistema;
7. conserva `playwright-report-docente` y `playwright-test-results-docente`.

La evidencia almacenada en `ejecucion-verde/` procede de una ejecución satisfactoria de esa suite y conserva en el árbol del repositorio los artefactos producidos por Playwright.