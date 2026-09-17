# E10 - Pruebas de extremo a extremo sobre navegador

## Evidencia de ejecución

La evidencia de este directorio fue generada automáticamente por la suite E2E de Playwright durante una ejecución satisfactoria del flujo de integración continua.

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

## Capturas generadas por Playwright

Las siguientes imágenes fueron generadas automáticamente por Playwright mediante la configuración `screenshot: "on"`:

- `01-acceso-sin-autenticacion.png`
- `02-login-dashboard.png`
- `03-cierre-sesion-login.png`
- `04-consulta-cursos.png`
- `05-consulta-asistencia.png`
- `06-registro-calificacion-restaurado.png`

Las capturas corresponden a los estados finales observados por el navegador durante los seis recorridos E2E.

## Generación en CI

El job `E10 - Playwright Frontend Docente` genera el reporte HTML y los resultados de prueba. El flujo conserva `playwright-report-docente` y `playwright-test-results-docente` incluso cuando la ejecución finaliza correctamente.

La evidencia almacenada en `ejecucion-verde/` procede de una ejecución satisfactoria de esa suite y permite conservar en el árbol del repositorio el resultado verificable de E10.
