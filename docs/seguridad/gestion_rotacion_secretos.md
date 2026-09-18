# Gestión de secretos fuera del árbol versionado — Tarea 46

**Fecha de verificación:** 2026-09-16
**Rama:** `Juliana-Emanuel`
**HEAD de referencia del saneamiento:** `5ef594c1`
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
- **Desarrollo/pruebas:** se mantienen valores predeterminados o de demostración en `sga-principal/docker-compose.yml`, `scripts/populate_all_docentes_full.py` y `microservicio-docente/micro_docente/settings.py`, además de fixtures de pruebas. No se ha acreditado su vigencia como credenciales reales ni una restricción técnica que impida utilizarlos fuera de desarrollo. Deben revisarse antes de declarar externalización total.
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

El historial de main, tags y ramas remotas conserva valores de reserva
ya rotados fuera del arbol vivo. La reescritura del historial con
`git filter-repo` no se ejecuta en esta entrega para no romper el tag
`v1.0.1`, las etiquetas de release moviles ni los clones de los
integrantes durante el plazo. Se documenta como deuda tecnica: ejecutar
la reescritura tras el cierre del PFC coordinando con el equipo.
