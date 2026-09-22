# E10 — Pruebas E2E de navegador del Frontend Docente

## Resultado de la ejecución

Esta evidencia corresponde a la ejecución de E10 realizada sobre `main`
después del merge del PR #187.

| Dato | Valor |
| --- | --- |
| Workflow | CI/CD Pipeline - AcadTrace (Entrega 4) |
| Workflow run | #856 |
| Run ID | `35674548611` |
| SHA ejecutado en `main` | `7e7408bd2dd3ccdde41a97660023c62926857f56` |
| Job | E10 - Playwright Frontend Docente |
| Job ID | `106578153060` |
| Resultado E10 | **success** |
| Pruebas | **6** |
| Fallos | **0** |
| Errores | **0** |
| Omitidas | **0** |
| Tiempo JUnit | **15.361 s** |
| Commit de artefactos | `0489155b906c2f3c454f0c6ebd6a744775e5b303` |

Run de GitHub Actions:

https://github.com/gleiston-guerrero/acadtrace/actions/runs/35674548611

Job E10:

https://github.com/gleiston-guerrero/acadtrace/actions/runs/35674548611/job/106578153060

## Aclaración sobre el estado global del workflow

El workflow #856 terminó globalmente con estado `failure`.

El fallo ocurrió posteriormente en:

`7. Integración y Despliegue en AWS EC2`

específicamente durante el despliegue por SSH.

El job evaluado para E10 no falló. El job
`E10 - Playwright Frontend Docente` terminó con resultado `success`.

Por tanto, esta evidencia no presenta el workflow completo como exitoso:
documenta específicamente el resultado verificable del job E10 asociado a la
entrega #36.

## Entorno E10

Las pruebas utilizan un entorno efímero creado por GitHub Actions.

La preparación incluye:

1. PostgreSQL efímero.
2. Aplicación de las migraciones oficiales requeridas.
3. Configuración del rol de aplicación y permisos.
4. Carga de datos sintéticos de E10.
5. Inicialización del esquema del microservicio Docente.
6. Levantamiento de los servicios requeridos.
7. Ejecución de Playwright contra las interfaces reales.

Entre los datos sintéticos utilizados se encuentran:

- usuario: `docente.e10@acadtrace.test`
- grado: `Decimo ano EGB E10`
- curso: `Matematica E10`
- actividad: `Tarea de ecuaciones lineales E10`

La asignación académica requerida por las pruebas se valida antes de iniciar
Playwright.

## Casos ejecutados

El archivo `e10-junit.xml` registra seis pruebas:

1. Login del docente y visualización del dashboard.
2. Consulta de cursos mediante la interfaz.
3. Consulta de asistencia.
4. Registro de calificación y restauración del estado original.
5. Cierre de sesión y retorno al login.
6. Rechazo o redirección de acceso no autenticado a Asistencia.

Resultado certificado por JUnit:

**6 pruebas / 0 fallos / 0 errores / 0 omitidas.**

## Evidencia almacenada

La carpeta `ejecucion-verde/` contiene:

- `01-acceso-sin-autenticacion.png`
- `02-login-dashboard.png`
- `03-cierre-sesion-login.png`
- `04-consulta-cursos.png`
- `05-consulta-asistencia.png`
- `06-registro-calificacion-restaurado.png`
- `e10-junit.xml`
- `playwright-report/`

Las capturas proceden del artefacto
`playwright-test-results-docente` del run #856.

El contenido de `playwright-report/` procede del artefacto
`playwright-report-docente` del mismo run.

Las capturas fueron renombradas al incorporarlas al repositorio para identificar
el caso representado. No se modificó su contenido gráfico.

`01-acceso-sin-autenticacion.png` y `03-cierre-sesion-login.png` tienen el mismo
SHA-256 porque ambos escenarios finalizan mostrando la misma pantalla de login.

## Integridad SHA-256

| Artefacto | Bytes | SHA-256 |
| --- | ---: | --- |
| `01-acceso-sin-autenticacion.png` | 58974 | `01CD16BBD4FEBA1978873876237C5AA35F7BF29E787D2FF50E517846F53A8AD9` |
| `02-login-dashboard.png` | 174372 | `EB3422759625891923E803BA119313DD007C4FAB09215C518E98F2C1B21D2D47` |
| `03-cierre-sesion-login.png` | 58974 | `01CD16BBD4FEBA1978873876237C5AA35F7BF29E787D2FF50E517846F53A8AD9` |
| `04-consulta-cursos.png` | 70105 | `5FDD019E56459270108D059931950D5FF0CB1FEB28B832EFCFD9311885BB0C55` |
| `05-consulta-asistencia.png` | 89323 | `912119A54AE69B6FB42D3E637B6209D5444876DCBDEA94136EB66FFF83B31853` |
| `06-registro-calificacion-restaurado.png` | 88135 | `ADC0323356EE4BD15CF265BF49DA5B3B4274C93398124DCB43F9EB455629D613` |
| `e10-junit.xml` | 2132 | `CD51C46190E2D2E2B2B03D7D8615270C0100431426FF0A80732C37FF89CA4296` |
| `playwright-report/data/11e52593caf0509c4054c9aa105294d3562519d3.png` | 88135 | `ADC0323356EE4BD15CF265BF49DA5B3B4274C93398124DCB43F9EB455629D613` |
| `playwright-report/data/2af3ed9364e9a4bcd6087e15d3d0b7cf442660c2.png` | 70105 | `5FDD019E56459270108D059931950D5FF0CB1FEB28B832EFCFD9311885BB0C55` |
| `playwright-report/data/6eb3d0f3c4d153087b68e74c479a150cb2c12324.png` | 58974 | `01CD16BBD4FEBA1978873876237C5AA35F7BF29E787D2FF50E517846F53A8AD9` |
| `playwright-report/data/be702267b7c87717d5aee7838c597041dbe0342e.png` | 174372 | `EB3422759625891923E803BA119313DD007C4FAB09215C518E98F2C1B21D2D47` |
| `playwright-report/data/c5d7b69bb3ab32a34698bec3218606100592dc5e.png` | 89323 | `912119A54AE69B6FB42D3E637B6209D5444876DCBDEA94136EB66FFF83B31853` |
| `playwright-report/index.html` | 532431 | `3AA3272204DB8FA9EFF929FDC71FBF3F3F41FAE69C5EB8AE6B3D7B1A4FB2134F` |

## Trazabilidad

La cadena de trazabilidad de esta evidencia es:

`main 7e7408bd2dd3ccdde41a97660023c62926857f56`

→ workflow `#856` / run `35674548611`

→ job E10 `106578153060` / resultado `success`

→ JUnit `6/6`

→ artefactos Playwright descargados de ese mismo run

→ commit `0489155b906c2f3c454f0c6ebd6a744775e5b303`.

Esta evidencia sustituye la ejecución anterior de E10 y vincula la entrega #36
con una ejecución posterior al merge del PR #187.
