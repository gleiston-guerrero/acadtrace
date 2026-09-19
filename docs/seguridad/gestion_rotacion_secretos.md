# Gestión de secretos fuera del árbol versionado — Tarea 46

**Fecha de actualización:** 2026-09-18
**Rama:** `Juliana-Emanuel`
**HEAD de referencia del saneamiento inicial:** `5ef594c1`; baselines saneados en `8c07513b`.
**Estado:** PARCIAL. Saneamiento del alcance identificado verificado; revocación de credenciales históricas pendiente de verificación externa.

## Alcance y límites

Se revisaron archivos de texto tracked del HEAD mediante patrones de contraseñas, secretos, tokens, claves API, JWT, Gemini, SMTP y claves privadas. Los resultados se clasifican sin reproducir valores. Se excluyeron E10, Playwright/E2E y workflows. No se certifica el contenido de imágenes, binarios, servicios desplegados ni almacenes de secretos externos. Esta revisión no equivale a un informe de Gitleaks ni acredita una externalización total.

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

- `docker-compose.yml`: eliminado el valor de reserva de la IP publica
  del servidor en `DB_HOST` (postgres-exporter). Ahora exige la variable
  via `${DB_HOST:?...}`.
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
- Detector de CI: el job `secret-scan` ejecuta ahora tambien un
  escaneo del arbol completo con `gitleaks detect --no-git`, cuyo
  reporte queda versionado como artefacto para auditoria.

### Historial

El historial conserva valores retirados del árbol vivo. Su rotación o
revocación permanece pendiente de verificación externa; retirarlos del
repositorio actual no demuestra que hayan dejado de ser válidos. La reescritura del historial con
`git filter-repo` no se ejecuta en esta entrega para no romper el tag
`v1.0.1`, las etiquetas de release moviles ni los clones de los
integrantes durante el plazo. Se documenta como deuda tecnica: ejecutar
la reescritura tras el cierre del PFC coordinando con el equipo.

## Corrección acotada de #46 (2026-09-18)

Comprobable mediante los archivos del repositorio:

- Principal obtiene `SGA_APP_PASSWORD` y los dos tokens internos basados en `GRPC_INTERNAL_TOKEN` sin credenciales literales de respaldo en `application.properties`.
- El Compose raíz transmite `SGA_APP_PASSWORD` al contenedor de Principal y rechaza su ausencia o valor vacío.
- `scripts/populate_all_docentes_full.py` exige `SGA_ADMIN_USERNAME` y `SGA_ADMIN_PASSWORD` antes de construir la petición. No imprime sus valores ni detalles de excepciones de autenticación o actualización. No se ejecutó contra AWS ni contra otro servidor durante esta corrección.
- `8c07513b` retiró `sga-principal/src/main/resources/db/init/baseline_completo.sql` y la antigua ruta `db/baseline/baseline_flyway_v8.sql`. Los baselines V8 actuales en `sga-principal/src/main/resources/db/migration/` y `microservicio-secretaria/backend/src/test/resources/db/migration/` contienen esquema, sin cargas de usuarios ni hashes de contraseñas identificados. El baseline V0 de pruebas no constituye por sí mismo una exposición de secretos. No se modificaron estas migraciones ni baselines en esta corrección.

Requiere comprobación externa: confirmar el suministro de variables en cada despliegue y acreditar la revocación o rotación de las credenciales históricas, incluidas las del script operativo. Esta corrección no demuestra que las cuentas existentes hayan cambiado de contraseña, no valida servicios desplegados y no aporta evidencia de rotación. El cierre externo continúa pendiente.
