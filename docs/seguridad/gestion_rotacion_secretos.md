# Gestión de secretos fuera del árbol versionado — Tarea 46

**Fecha de actualización:** 2026-09-21
**Rama:** `Juliana-Emanuel`
**HEAD de referencia del saneamiento inicial:** `5ef594c1`; baselines saneados en `8c07513b`.
**Estado:** PARCIAL. Saneamiento del alcance identificado verificado; revocación de credenciales históricas pendiente de verificación externa.

## Alcance y límites

La revisión inicial examinó archivos de texto tracked del HEAD mediante patrones de contraseñas, secretos, tokens, claves API, JWT, Gemini, SMTP y claves privadas; excluyó E10, Playwright/E2E y workflows. La comprobación del 2026-09-21 descrita abajo incorpora todos los archivos versionados actuales al escaneo con Gitleaks, incluidos los workflows, sin nuevas exclusiones. Los resultados se clasifican sin reproducir valores. No se certifica el contenido visual de imágenes, binarios, servicios desplegados ni almacenes de secretos externos; tampoco se acredita una externalización total.

## Suministro externo y plantillas

- Principal y Soporte reciben secretos mediante variables/configuración externa. Secretaría exige `DB_PASSWORD`, `JWT_SECRET`, `AES_SECRET_KEY` y `GRPC_INTERNAL_TOKEN` sin valores de respaldo en `application.properties`.
- IA obtiene `GEMINI_API_KEY` del entorno. El generador de carga de Soporte exige `JWT_SECRET`; la firma móvil utiliza configuración externa.
- Los archivos `.env` reales no están tracked y están excluidos por `.gitignore`. Si se usan localmente, deben permanecer fuera del árbol versionado; preferir suministro del entorno o almacenamiento externo con acceso restringido.
- `.env.example`, `microservicio-docente/.env.example` y `microservicio-secretaria/.env.example` son plantillas con placeholders, no credenciales reales. La plantilla raíz incluye `MAIL_PASSWORD`, `FIREBASE_CREDENTIALS_PATH` y `GRAFANA_ADMIN_PASSWORD`, alineados con Compose. Los placeholders deben sustituirse externamente; no son valores para producción.
- Firebase requiere un archivo externo de credenciales, indicado por `FIREBASE_CREDENTIALS_PATH`. No debe incorporarse al repositorio.
- No se afirma fallo inmediato global: una interpolación `${VARIABLE}` en Compose no es equivalente a una validación obligatoria `${VARIABLE:?mensaje}`. Tampoco se verificó aquí el arranque de todos los servicios sin secretos.

## Comprobaciones del saneamiento

| Cambio | Evidencia versionada |
|---|---|
| Cuatro secretos obligatorios de Secretaría sin fallback | `28e8c27c` |
| Comentario de HmacService y dos copias HTML saneados, sin cambiar lógica | `cc84077e` |
| Contraseña retirada de ejemplos SQL y README; `PGPASSWORD` externa | `2a351196` |
| Plantilla y exclusiones reforzadas | `6f32ddc2` |
| Cinco logs operativos retirados del índice | `d76ddbaa` |
| Dos dumps sensibles retirados del índice y excluidos específicamente | `5ef594c1` |

Se comprobó la ausencia de los valores previamente identificados en los tres archivos HmacService y en los dos scripts SQL y su README, sin mostrarlos. La inyección existente de HmacService permanece intacta.

No están tracked los siguientes archivos:

- `microservicio-docente/server.err.log`
- `microservicio-docente/server.out.log`
- `sga-principal/backend.log`
- `sga-principal/build.log`
- `sga-principal/maven-debug.log`
- `scripts/backups/backup_pre_seed_20260806_135036.sql`
- `sga-principal/sql/supabase_dump_completo.sql`

Su retirada se realizó con `git rm --cached`: las copias locales se conservaron, sin sanear su contenido. Esto no demuestra que están almacenadas físicamente fuera del directorio de trabajo ni que tengan permisos adecuados. Los respaldos sensibles deben custodiarse fuera del repositorio; para reproducibilidad se deben usar esquema, migraciones y datos sintéticos apropiados. No se ignoran todos los archivos SQL.

Se conserva `Informe-E4_BCEL/evidencias/tolerancia/evidencia_tolerancia_20260811_081422.log` como excepción académica preventiva. No se encontró referencia explícita que pruebe su obligatoriedad; su contenido no queda certificado por esta excepción.

## Clasificación y asuntos pendientes

- **Variables de entorno:** referencias a configuración externa, sin confundir el nombre de una variable con un secreto.
- **Placeholders/examples:** plantillas `.env.example` y ejemplos de Prometheus; no acreditan credenciales válidas.
- **Desarrollo/pruebas:** los fixtures ficticios no acreditan credenciales reales. El Compose de Principal ya exige una contraseña externa y Django genera un secreto aleatorio si falta su variable. El script operativo `scripts/populate_all_docentes_full.py` obtiene ahora sus credenciales del entorno; no se clasifica como fixture ni se ha comprobado la vigencia de sus credenciales anteriores.
- **Falsos positivos:** DTO, formularios, traducciones, validadores y variables que reciben tokens en ejecución.
- **Secretos reales:** no se identificaron valores reales adicionales confirmados en el alcance textual revisado. La ausencia de coincidencias no certifica todos los formatos ni la configuración desplegada.

## Exposición histórica y revocación

El historial no fue reescrito. Hay indicios históricos de contraseñas y claves en `microservicio-soporte/.env` y de una contraseña de correo en revisiones anteriores de la configuración de Principal. Los blobs históricos de dumps y logs también permanecen accesibles: eliminarlos del HEAD no elimina su historia.

Toda credencial que haya estado versionada debe considerarse comprometida y revocarse o rotarse en el proveedor o sistema correspondiente. No se afirma que la rotación está completada: las afirmaciones anteriores de rotación global y sesiones invalidadas no están respaldadas por evidencia verificable en esta revisión.

Para cerrar esta limitación se necesita evidencia redactada de la revocación o rotación efectiva, con fecha, sistema, responsable y resultado, sin incluir valores. Deben verificarse las cuentas de BD y correo, claves de firma, tokens internos y cualquier otra credencial expuesta. La rotación de claves de cifrado requiere preservar la recuperación de datos y planificar su migración; no debe sustituirse una clave sin ese control.

La revocación efectiva permanece **PENDIENTE DE VERIFICACIÓN** en los proveedores/sistemas. No se ejecutaron cambios en esos sistemas, reescrituras de historial ni force push durante este cierre documental.

## Estado post-cierre PFC (2026-09-18)

Cambios aplicados al arbol vivo antes del cierre del PFC:

- `docker-compose.yml`, `microservicio-secretaria/docker-compose.yml`,
  `application.properties` (sga-principal y microservicio-secretaria),
  `DataSourceConfig.java`, `micro_docente/settings.py` y `.env.example`:
  sustituido el valor de reserva de la IP publica del servidor en `DB_HOST`
  por `localhost` (y fail-fast `${DB_HOST:?...}` en postgres-exporter).
- `docker-compose.yml`: `GRAFANA_ADMIN_PASSWORD` ya usaba fail-fast
  `${GRAFANA_ADMIN_PASSWORD:?...}`.
- `sga-principal/docker-compose.yml`: eliminado el literal
  `POSTGRES_PASSWORD: postgres`. Ahora exige la variable via
  `${POSTGRES_PASSWORD:?...}`.
- `microservicio-docente/micro_docente/settings.py`: sustituido el
  fallback literal de `DJANGO_SECRET_KEY` por `secrets.token_urlsafe(64)`,
  que genera un secreto aleatorio en memoria si la variable no esta
  definida.
- Retirado el HTML obsoleto de cobertura
  `docs/cobertura/secretaria/ec.uteq.sga.secretaria.infrastructure.config/DataSourceConfig.java.html`,
  que conservaba un literal antiguo de `db.password` que ya no existe
  en el fuente actual.
- Anotacion `gitleaks:allow` aplicada a 5 constantes de prueba de
  alta entropia en `CryptoServiceTest.java`, `GrpcTracePropagationTest.java`,
  `SecurityTest.java` y `test_representante.py` (2 apariciones).
- Detector inicial de CI: el escaneo `gitleaks detect --no-git` usaba
  `--exit-code 0`, por lo que no constituía una compuerta efectiva.
  El reporte se subía como artefacto, no como archivo versionado.
  Esta configuración queda sustituida por la comprobación descrita abajo.

### Historial

El historial conserva valores retirados del árbol vivo. Su rotación o
revocación permanece pendiente de verificación externa; retirarlos del
repositorio actual no demuestra que hayan dejado de ser válidos. La reescritura del historial con
herramientas de reescritura no se ejecuta en esta entrega para no romper el tag
`v1.0.1`, las etiquetas de release moviles ni los clones de los
integrantes durante el plazo. Cualquier posible reescritura futura requiere
una decisión independiente y coordinada; no forma parte de esta tarea.

## Corrección acotada de #46 (2026-09-18)

Comprobable mediante los archivos del repositorio:

- Principal obtiene `SGA_APP_PASSWORD` y los dos tokens internos basados en `GRPC_INTERNAL_TOKEN` sin credenciales literales de respaldo en `application.properties`.
- El Compose raíz transmite `SGA_APP_PASSWORD` al contenedor de Principal y rechaza su ausencia o valor vacío.
- `scripts/populate_all_docentes_full.py` exige `SGA_ADMIN_USERNAME` y `SGA_ADMIN_PASSWORD` antes de construir la petición. No imprime sus valores ni detalles de excepciones de autenticación o actualización. No se ejecutó contra AWS ni contra otro servidor durante esta corrección.
- `8c07513b` retiró `sga-principal/src/main/resources/db/init/baseline_completo.sql` y la antigua ruta `db/baseline/baseline_flyway_v8.sql`. Los baselines V8 actuales en `sga-principal/src/main/resources/db/migration/` y `microservicio-secretaria/backend/src/test/resources/db/migration/` contienen esquema, sin cargas de usuarios ni hashes de contraseñas identificados. El baseline V0 de pruebas no constituye por sí mismo una exposición de secretos. No se modificaron estas migraciones ni baselines en esta corrección.

Requiere comprobación externa: confirmar el suministro de variables en cada despliegue y acreditar la revocación o rotación de las credenciales históricas, incluidas las del script operativo. Esta corrección no demuestra que las cuentas existentes hayan cambiado de contraseña, no valida servicios desplegados y no aporta evidencia de rotación. El cierre externo continúa pendiente.

## Compuertas y corrección no destructiva (2026-09-21)

El workflow `ci-cd.yml` instala Gitleaks **8.18.0** y ejecuta
`node scripts/scan-secrets.cjs tree`, `history`, `self-test` y `self-test-history`.
El árbol es una compuerta bloqueante: propaga el código **2** cuando hay
hallazgos. El historial conserva el escaneo completo y compara sus resultados
con los **11 hallazgos históricos pendientes** documentados en
`gitleaks-history-baseline.json`: devuelve **0** si solo encuentra deuda conocida
y **2** si aparece cualquier ocurrencia nueva. Ambos fallan con **1** ante errores
de análisis o configuración. No se usa `continue-on-error`. El éxito de esta
comparación no significa historial limpio ni resuelve #46.

Los comandos efectivos de Gitleaks son:

```sh
gitleaks detect --source <copia-temporal-del-arbol-versionado> --config <repo>/.gitleaks.toml --redact --exit-code 2 --report-format json --report-path <temporal>/report.json --no-git
gitleaks detect --source <repo> --config <repo>/.gitleaks.toml --redact --exit-code 2 --report-format json --report-path <temporal>/report.json --log-opts="--all --full-history -m"
```

- **Árbol:** copia los archivos enumerados por `git ls-files`, con su contenido
  actual, a una carpeta temporal. No incluye ni modifica los untracked locales.
  Incluye el propio helper y la línea base durante su revisión antes de incorporarlos al índice.
  Se conserva `.gitleaks.toml` con las reglas predeterminadas y las cinco
  anotaciones existentes para constantes ficticias de pruebas; no se añaden exclusiones.
- **Historial:** checkout con `fetch-depth: 0`; `--all` examina las referencias
  disponibles (ramas, referencias remotas y tags, además de HEAD),
  `--full-history` evita simplificación y `-m` incluye diferencias de merges
  contra sus padres. Se rechazan clones shallow. No se limita al rango empujado,
  ni a `first-parent`. No incluye objetos inalcanzables, reflogs, forks ni
  referencias que el servidor no suministre al checkout. La ejecución local
  cubre las referencias ya disponibles en el clon, sin afirmar sincronización remota.
- **Salida segura:** stdout/stderr del scanner se descartan; el reporte temporal
  usa `--redact` y se elimina al finalizar. El único artefacto publicado contiene
  `path` y `type` para el árbol; para historial añade identificadores derivados
  exclusivamente de metadata, clasificación y recuentos de conocidos/nuevos/ausentes.
  No contiene coincidencias, secretos, hashes de secretos, mensajes de commits,
  autores ni correos. La consola muestra recuentos, estado pendiente y códigos de salida.
- **Mutación controlada:** `self-test` crea una credencial sintética en la copia
  temporal del árbol, exige código 2 y un hallazgo en el archivo de prueba, y
  elimina la copia. El éxito del autotest acredita el rechazo de la mutación,
  no transforma en éxito los pasos de árbol o historial.

### Línea base de deuda histórica pendiente

`gitleaks-history-baseline.json` registra las 11 ocurrencias conocidas con
Gitleaks 8.18.0. Cada entrada contiene ruta, tipo e identificador SHA-256
de la representación JSON de `[Commit, File, RuleID, StartLine, EndLine,
StartColumn, EndColumn]`, normalizando separadores de ruta a `/`.
El identificador **no se calcula a partir del secreto ni de la coincidencia**.
El commit y la ubicación permiten detectar otra aparición incluso si conserva
la misma ruta y tipo. La comparación conserva multiplicidades: una ocurrencia
adicional tampoco pasa por compartir identificador con una conocida.

Esta línea base **representa deuda pendiente, no una exclusión de seguridad**:
no se entrega a Gitleaks como ignore/baseline, no elimina hallazgos del escaneo
y no acredita rotación, revocación ni saneamiento histórico. El scanner sigue
devolviendo 2 ante los 11 hallazgos; la comprobación de CI devuelve 0 únicamente
porque no hay ocurrencias nuevas. Los conocidos se publican como `known-pending`
y los nuevos como `new`. Una entrada ausente se informa sin declararla resuelta.
La línea base no se regenera automáticamente y cualquier cambio requiere revisión
explícita; no debe ampliarse para aceptar nuevas exposiciones.

`self-test-history` verifica la misma comparación usada por el escaneo con
metadata sintética: 11 conocidos pasan; una aparición nueva, una sustitución
manteniendo el total en 11 y una ocurrencia adicional duplicada producen código 2.
También verifica que cambiar el commit cambia la identidad. Esta prueba no crea
commits ni modifica historia: simula resultados sanitizados del scanner.

La IP identificada en la evaluación aparecía una vez en cada uno de
`ci-cd.yml`, los dos `protocolo-e4.md`, `scripts/README.md`,
`scripts/create_eventos_academicos.sql` y `scripts/create_notificaciones.sql`.
Se sustituye por `secrets.DB_HOST` en CI, configuración externa en los
protocolos y `${DB_HOST:?Configure DB_HOST}` en los ejemplos de psql.
Quedan **0 coincidencias textuales** de ese literal en archivos versionados
actuales. No se certifica su ausencia visual en capturas ni en el historial.
Debe configurarse el secreto `DB_HOST` en GitHub; su existencia y valor no
se verificaron externamente. El diagnóstico Flyway deja de imprimir el host
directamente y solicita su enmascaramiento para los mensajes posteriores.

### Estado de retirada y rotación

| Categoría | Estado acreditado / pendiente |
|---|---|
| Secretos retirados del árbol actual | Retiradas y externalizaciones enumeradas en las secciones anteriores; no equivalen a revocación. |
| Secretos históricos | Persisten en revisiones antiguas, incluidos archivos eliminados del árbol; el escaneo histórico también registra hallazgos, sin certificar que todos sean credenciales reales. |
| Rotaciones verificadas | **Ninguna acreditada** por evidencia verificable revisada en el repositorio. |
| BD y cuentas del script operativo | Pendiente acreditar cambio/revocación de credenciales anteriores y comprobar acceso con la configuración nueva. |
| Correo/SMTP | Pendiente acreditar revocación o rotación en el proveedor. |
| JWT, firma y tokens internos | Pendiente acreditar rotación y, cuando corresponda, invalidación de sesiones/tokens anteriores. |
| Claves de cifrado | Pendiente inventario y rotación controlada, preservando recuperación y migración de datos. |
| API, Firebase y otras credenciales expuestas | Pendiente completar inventario histórico y aportar evidencia del proveedor para cada credencial aplicable. |
| Historial compartido | **No reescrito**; no se modifican commits, SHAs, mensajes, autores ni correos. |

Para acreditar cada rotación se necesita un registro sin valores secretos:
sistema/cuenta o identificador no sensible, responsable, fecha, evidencia de
revocación del valor anterior y resultado de la verificación. La corrección
de CI y la ausencia de hallazgos del árbol no sustituyen esa evidencia.

### Verificación local del 2026-09-21

- Gitleaks 8.18.0, árbol versionado actual: **0 hallazgos, código 0**.
- Historial completo disponible en el clon (1.079 commits alcanzables):
  **11 hallazgos pendientes, código 2 del scanner y código 0 de la comparación
  contra la línea base** (0 nuevos). Son coincidencias de `generic-api-key` en
  revisiones de `.github/workflows/ci-cd.yml`,
  `tests/browser/test_sga_navegador.py` y
  `microservicio-soporte/backend/src/test/java/ec/uteq/sga/soporte/security/SecurityTest.java`.
  Pueden incluir fixtures o falsos positivos; no se ha acreditado su validez
  ni se han añadido exclusiones al detector. La línea base permite informar
  esta deuda sin que por sí sola mantenga todo el workflow en rojo.
  El detector no identifica necesariamente todas las contraseñas históricas;
  este recuento no sustituye el inventario y la verificación de rotación.
- Mutación sintética temporal: **detectada, compuerta con código 2**;
  autotest satisfactorio, sin introducir la mutación en el árbol de trabajo.
- Comparación histórica: pruebas de aparición nueva, sustitución y duplicado
  adicional satisfactorias. Una prueba local del CLI con salida sintética del
  scanner confirmó **código de proceso 2** ante un hallazgo nuevo. No se creó
  ningún commit para probarlo ni se modificó el historial compartido.
- `git diff --check`: sin errores.

Estos resultados corresponden a ejecución local, no a una ejecución publicada
de GitHub Actions. No se hizo commit, push ni reescritura del historial.
