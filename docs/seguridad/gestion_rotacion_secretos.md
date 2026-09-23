# Gestión de secretos fuera del árbol versionado — E46

Revisión: 2026-09-23. Rama `Juliana-Emanuel`; commit de referencia de la auditoría inicial `9c49a4b7`.
**Estado: PARCIAL. E46 no está cerrado. JWT_SECRET, la credencial PostgreSQL del rol `sga_app`, SMTP, la credencial usada por el script operativo histórico y los hashes de autenticación vigentes fueron rotados o invalidados y verificados. Los assets móviles históricos de v1.0.2 también fueron retirados. Permanece pendiente el saneamiento real del historial remoto y su verificación posterior.**
Los cambios E46 documentados se han versionado en la rama `Juliana-Emanuel`. No se ha ejecutado una reescritura real del historial remoto. El release móvil v1.0.3 fue publicado y verificado.

## HEAD ACTUAL / árbol de trabajo

El commit de referencia de la auditoría inicial conservaba las 15 apariciones evaluadas en seis archivos. Las correcciones E46 de esos archivos se han versionado:

- Android obtiene `SGA_GATEWAY_URL` y `SGA_DOCENTE_URL` del entorno mediante BuildConfig, con valores locales para emulador. Constants y SettingsScreen comparten esa configuración. Las preferencias persistidas por instalaciones anteriores no se borran automáticamente; puede ser necesario restablecer endpoints en Ajustes.
- Los dos Calificaciones.jsx importan `IA_API_BASE_URL` desde el módulo de configuración existente. Usan `VITE_IA_API_URL`, con fallback al hostname actual y puerto del servicio IA.
- El script de docentes usa `SGA_API_BASE_URL`, `SGA_ADMIN_USERNAME` y `SGA_ADMIN_PASSWORD`, exige credenciales no vacías y rechaza URLs con credenciales embebidas. Llama a la API HTTP, no a PostgreSQL: añadir DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD no tendría efecto; cambiarlo a acceso directo alteraría su funcionamiento.
- El HTML móvil se regeneró mediante `:app:jacocoTestReport`, sin editarlo manualmente. Tras clasificar las 1.094 diferencias, se restauraron 1.093 salidas: 1.087 cambios de formato regional (idioma, porcentajes, millares y orden de filas), cuatro páginas con cambios de cinco instrucciones de SettingsScreen, el XML y la página de sesiones. Se conserva únicamente `ec.edu.uteq.sga.representante.ui.screens.settings/SettingsScreen.kt.html`, tal como lo produjo JaCoCo, porque era la única página que conservaba la dirección pública. Las métricas e índices canónicos siguen siendo la evidencia de CI #876 documentada en `docs/cobertura/movil/README.md`; esta sustitución mínima de fuente no representa una nueva medición integral de cobertura.

La búsqueda completa con `scripts/auditar_referencias_e46.py` examina los archivos de `git ls-files`, incluidos binarios como bytes: **0 apariciones de ambas direcciones de producción; 0 IPv4 públicas sin clasificar**. Los 40 candidatos numéricos en cinco XML son versiones de JDK/bibliotecas, no endpoints; se conservan. El detalle regenerable por ruta y URLs HTTP se conserva fuera del árbol versionable, en la carpeta temporal `acadtrace-e46-tools`. Puede regenerarse con `python scripts/auditar_referencias_e46.py --output <ruta-externa.json>`. Las referencias HTTP documentales no son secretos por su protocolo. No se certifica ausencia visual en imágenes ni configuración desplegada.

Se conservan las plantillas, exclusiones de archivos de entorno reales y configuración externa de Firebase/firma. Retirar un archivo de Git no acredita custodia externa ni revocación de su contenido.

## HISTORIAL

### Resultado posterior a la reescritura publicada ? 2026-09-23

La limpieza hist?rica se ejecut? sobre un espejo aislado con `git-filter-repo 2.47.0` y posteriormente se public? sobre las refs remotas controladas.

Resultado verificado desde un clon nuevo obtenido directamente de GitHub:

- ramas publicadas reescritas: 6
- tags publicados reescritos: 5
- `tree`: 0 hallazgos; gate exit 0
- `history`: 0 known pending; 0 new; 0 replaced; 0 extra
- scanner exit: 0
- gate exit: 0
- `self-test`: PASS
- `self-test-history`: PASS
- `git diff --check`: sin errores
- las seis rutas hist?ricas sensibles retiradas presentan 0 commits alcanzables en las refs reescritas

Las referencias internas `refs/pull/*` de GitHub no pueden actualizarse mediante push normal. `git-filter-repo` inform? 200 refs de pull request afectadas por el cambio de SHAs; esto no equivale a 200 PR con secretos, sino a PR cuyas referencias dependen de historia reescrita. Se solicit? purga a GitHub Support mediante el ticket **#4787781**. El ticket fue cerrado autom?ticamente porque la cuenta solicitante no incluye soporte t?cnico. Esta limitaci?n de infraestructura queda documentada y no invalida el resultado de los escaneos sobre las ramas y tags publicados.


**COMPUERTA ACTIVA — estado previo a la reescritura real:**
- known = 11
- new = 0
- replaced = 0
- extra = 0
- scannerExit = 2
- gateExit = 0
- processExit = 0

La evidencia oficial reproducible actual es la configuración por omisión de Gitleaks (`useDefault = true`) y su deuda histórica clasificada. `scannerExit = 2` indica hallazgos; `known` representa deuda histórica ya clasificada. `gateExit = 2` solamente cuando existen `new`, `replaced` o `extra`. Si solo existen hallazgos `known`, `gateExit = 0` y el proceso puede terminar correctamente. El baseline no significa que el historial esté saneado: E46 sigue pendiente mientras exista deuda histórica, aunque la compuerta de regresión no bloquee CI por hallazgos `known`.

**AUDITORÍA AMPLIADA LOCAL/EXPERIMENTAL PREVIA — no es evidencia oficial actual:**
- baseline occurrences = 11
- additional occurrences = 91
- total observed occurrences = 102
- rules e46-* active in CI = false

Las 102 ocurrencias corresponden a una auditoría ampliada local/experimental previa, ejecutada con reglas `e46-*` no versionadas/no activas en CI; el resultado no es reproducible desde el árbol actual y no forma parte de la compuerta oficial. No se presenta esta cifra como resultado de la configuración activa ni de CI.

El [inventario](e46_inventario_historico.md) y [metadata por ocurrencia](e46_historial_metadata.json) conservan el contexto de esa auditoría previa, con commits y refs alcanzables en aquel momento. Incluyen los tres archivos señalados y otras ubicaciones antiguas de dumps/configuraciones; no sustituyen la evidencia oficial reproducible actual. El detector no certifica haber encontrado todos los secretos ni su vigencia.

Se identificaron cuatro mensajes históricos con direcciones. Las refs locales incluyen main, ramas remotas, tags y stash; no se afirma sincronización completa con el servidor. La limpieza histórica es necesaria para E46 y sigue pendiente conforme al [plan de reescritura](e46_plan_reescritura.md).

## ROTACIÓN

**JWT_SECRET, la credencial PostgreSQL del rol `sga_app`, SMTP, la cuenta usada históricamente por el script operativo y los hashes de usuarios que seguían vigentes están ROTADOS/INVALIDADOS Y VERIFICADOS.**

## Estado de rotación PostgreSQL — `sga_app`

**Producción / AWS EC2:**
- Estado: ROTADO Y VERIFICADO OPERATIVAMENTE.
- Fecha: 2026-09-23.
- Se generó una nueva credencial mediante CSPRNG sin mostrarla ni versionarla.
- La rotación se ejecutó mediante `scripts/rotar_password_sga_app.sh`.
- La nueva credencial autenticó correctamente como `sga_app`.
- La credencial anterior fue rechazada antes de completar el procedimiento.
- El rol verificado conserva `superuser=false`.
- Se actualizaron `SGA_APP_PASSWORD` y `DB_PASSWORD` en el `.env` externo de producción.
- Se recrearon los consumidores PostgreSQL.
- Las dos instancias de `sga-principal` quedaron `running`, con `exit=0`.
- En ambas instancias `RestriccionBitacoraValidator` verificó que `sga_app` no puede modificar la bitácora.
- Ambas instancias registraron `Started SgaPrincipalApplication`.
- `/actuator/health` respondió `status=UP` y el componente PostgreSQL `db` respondió `status=UP`.

No se almacena en el repositorio el valor anterior ni el nuevo de la contraseña.

## Estado de rotación SMTP

**Producción / Gmail SMTP:**
- Estado: ROTADO Y VERIFICADO OPERATIVAMENTE.
- Fecha: 2026-09-23.
- La credencial SMTP desplegada antes de la rotación coincidía con uno de los valores históricos identificados.
- La autenticación con esa credencial fue rechazada por Gmail con código `535 5.7.8`.
- Se generó una nueva contraseña de aplicación del proveedor.
- La nueva credencial fue validada antes de desplegarse: `SMTP_AUTH=OK`.
- `MAIL_PASSWORD` fue actualizado en el `.env` externo de producción.
- Las instancias consumidoras de `sga-principal` fueron recreadas.
- La autenticación usando el valor almacenado en producción resultó satisfactoria: `SMTP_PRODUCCION=OK`.
- Se realizó un envío real de prueba mediante `smtp.gmail.com:587` con STARTTLS.
- El mensaje de prueba fue recibido correctamente.
- Evidencia visual: `evidencias/Juliana_Emanuel/e46_smtp_prueba_2026-09-23.png`.

No se almacena ni documenta el valor anterior ni el nuevo de la contraseña SMTP.

## Estado de rotación JWT_SECRET

**CI / GitHub Actions:**
- Estado: ROTADO Y VERIFICADO EN CI

**Evidencia:**
- JWT_SECRET fue actualizado en GitHub Actions Secrets.
- Workflow manual #960.
- Rama: Juliana-Emanuel.
- Commit: 3ab0a368.
- Resultado: SUCCESS.
- El valor del secret no se almacena ni se documenta.

**Producción / AWS EC2:**
- Estado: ROTADO Y VERIFICADO OPERATIVAMENTE

**Evidencia:**
- El archivo .env de producción existe.
- .env no está versionado por Git.
- permisos del .env: 600, propietario ubuntu.
- JWT_SECRET estaba configurado.
- Se generó un nuevo valor directamente en EC2 usando un CSPRNG mediante: `openssl rand -hex 32`
- El valor nunca fue mostrado ni almacenado en documentación.
- Se recrearon únicamente los servicios consumidores:
  - sga‑principal
  - microservicio‑secretaria
  - microservicio‑soporte
- microservicio‑secretaria: status=running, health=healthy, exit=0
- microservicio‑soporte: status=running, sin healthcheck, exit=0
- sga‑principal instancia 1: status=running, sin healthcheck, exit=0
- sga‑principal instancia 2: status=running, sin healthcheck, exit=0

El [registro](registro_rotacion_e46.md) separa PostgreSQL, JWT, gRPC, SMTP, cuenta administrativa, hashes de usuarios, APIs, Firebase y cifrado. Para JWT_SECRET, el estado actualizado es el acreditado en la evidencia anterior de CI y producción. Las demás categorías permanecen pendientes de validación o rotación según corresponda; algunas requieren confirmar exposición. Juliana debe aportar evidencia externa verificable para los casos restantes aplicables. El registro histórico no se modifica en esta actualización documental.

## Estado de la cuenta usada por el script operativo histórico

- Fecha: 2026-09-23.
- La cuenta identificada en `scripts/populate_all_docentes_full.py` continúa existiendo en producción.
- La verificación actual mostró `ROLE_REPRESENTANTE`; no se acredita que actualmente sea una cuenta administrativa.
- La contraseña histórica fue invalidada sustituyendo el hash por uno generado desde una credencial aleatoria que no se mostró ni se almacenó.
- Se estableció `primer_ingreso=true`, `intentos_fallidos=0` y `bloqueado_hasta=NULL`.
- El rol y el estado de la cuenta no fueron modificados.
- La nueva credencial aleatoria no se conserva; si la cuenta vuelve a utilizarse deberá ejecutarse un reset autorizado.

## Estado de hashes históricos de usuarios

- Fecha: 2026-09-23.
- Se identificaron 21 hashes bcrypt históricos únicos en dumps y baselines alcanzables.
- Se revisaron 36 cuentas actuales sin imprimir usernames, correos, contraseñas ni hashes.
- 19 cuentas conservaban un hash exactamente igual a alguno de los hashes históricos.
- Las 19 estaban activas, disponían de correo y ninguna registraba acceso previo.
- Distribución: 3 con `ROLE_REPRESENTANTE` y 16 sin rol.
- Las 19 coincidencias fueron sustituidas por hashes bcrypt nuevos derivados de credenciales aleatorias independientes.
- Los valores planos no fueron mostrados ni almacenados.
- Se estableció `primer_ingreso=true`, `intentos_fallidos=0` y `bloqueado_hasta=NULL`.
- Resultado posterior: **19 cuentas rotadas y 0 hashes históricos vigentes en producción**.
- Evidencia sanitizada: `docs/seguridad/e46_rotacion_usuarios_2026-09-23.md`.

## APK

### APK v1.0.2 — evidencia histórica

La copia local del APK v1.0.2 coincide con el digest del asset consultado mediante la API de GitHub. Tiene **1 aparición en classes.dex**. Evidencia: `e46_apk_anterior.json`.

SHA-256 del APK anterior: `cfd6b03995b292cba2a9926fc9adde3c6e9ebc075e2e034fee68b117d476e27c`.

### APK local de revisión anterior — evidencia histórica local

Ubicación utilizada para aquel APK de revisión: `app-movil-docente/app/build/outputs/apk/release/app-release.apk`. Esta ruta de salida puede reutilizarse; el hash siguiente identifica el artefacto histórico local, no el release oficial actual.

SHA-256: `ff5bda44157d60407d085ef4d9b1ae67a6aaca856ce651b528c5dd29d4945f7a`.

Se generó con wrapper oficial, endpoints locales y keystore sintético temporal. Inspección de todas las entradas ZIP, incluidos DEX/recursos, como ASCII/UTF-8 y UTF-16LE: **production_server_literal_occurrences = 0**. Evidencia: `e46_apk_revision.json`. La firma sintética es de revisión y no permite actualizar instalaciones firmadas con la clave de producción. Ese APK local no fue publicado ni reemplazó v1.0.2; se conserva únicamente como evidencia histórica local.

### APK v1.0.3 — release oficial actual

- versionName: `1.0.3`
- versionCode: `4`
- tag oficial: `v1.0.3`
- commit: `09999410434829d60b4246a520b20a1cf089b3af`
- SHA-256: `52ad00c1a0f31fdf3b7d9ad595ae80544bfb1fa3ae729c8184ada10bf301a227`
- SHA256SUMS validado
- firma APK válida
- certificado coincide
- referencias de producción = `0`

El release histórico v1.0.2 permanece publicado como referencia documental, pero el 2026-09-23 se retiraron `app-release.apk`, `app-release.aab`, `CERTIFICATE_SHA256.txt` y `SHA256SUMS.txt`. La verificación posterior en GitHub mostró que solo permanece `TA-PFC-E4_BCEL.pdf`. El release oficial limpio para la aplicación móvil continúa siendo v1.0.3.

## CI

Workflow con Gitleaks 8.18.0, checkout completo y cuatro modos del helper. El histórico usa `--all --full-history -m` y rechaza clones shallow. El scanner puede devolver `scannerExit = 2` al encontrar hallazgos. La compuerta de regresión devuelve `gateExit = 2` solamente ante `new`, `replaced` o `extra`; si únicamente hay `known`, devuelve `gateExit = 0` y el proceso puede terminar correctamente. El baseline clasifica deuda conocida, pero no acredita saneamiento histórico. Errores de ejecución devuelven 1. E46 sigue pendiente mientras exista deuda histórica, aunque los hallazgos `known` no bloqueen CI.

La consola solo muestra resultados sanitizados; el reporte temporal redactado se elimina. La metadata incluye ruta, regla, commit, ubicación e identidad derivada únicamente de metadata, nunca valores ni hashes de secretos. Tree cubre tracked y la lista explícita de nuevos archivos E46 bajo revisión; no incluye untracked ajenos.

El autotest de árbol inserta una credencial sintética en una copia temporal. El histórico prueba además el detector real en un repositorio desechable y mutaciones de metadata. No modifica refs del proyecto. La evidencia publicada de CI para JWT_SECRET se conserva en su sección; las verificaciones locales siguientes no se presentan como nuevas ejecuciones de CI.

## Verificaciones locales

| Verificación | Resultado |
|---|---|
| tree | 0 hallazgos; código 0 |
| history, después de sincronizar `Juliana-Emanuel` con `main` y antes de la reescritura real | known = 7; new = 0; replaced = 4; extra = 8; scannerExit = 2; gateExit = 2 |
| self-test | PASS; mutación rechazada con 2, autotest 0 |
| self-test-history | PASS; detector histórico rechaza con 2, autotest 0 |
| Frontend Principal | npm ci --ignore-scripts y npm run build: 0 |
| Frontend Secretaría | npm ci --ignore-scripts y npm run build: 0 |
| Android | assembleRelease testDebugUnitTest jacocoTestReport: BUILD SUCCESSFUL; 94 pruebas, 0 fallos/errores/omitidas |
| Python | Sintaxis válida; cuatro pruebas sin red: OK |
| git diff --check | 0, sin errores |

Android requiere JAVA_HOME, ANDROID_HOME y GRADLE_USER_HOME locales y keystore de revisión. Desde su directorio: `./gradlew.bat assembleRelease testDebugUnitTest jacocoTestReport --console=plain`. Los intentos iniciales fallaron por permisos y SDK sin configurar; con rutas correctas pasaron. Secretaría requirió ejecutar el build fuera del sandbox por acceso denegado de esbuild. Los frontends no declaran script de tests; se verificaron builds. Estas pruebas locales no incluyeron conexiones de producción; la verificación operativa posterior de JWT_SECRET se documenta por separado en su sección.

**Falta para cerrar:**

- aprobar y ejecutar la reescritura REAL del historial remoto hasta obtener history=0;
- revisar copias, cachés, forks y refs antiguas después de la reescritura;

La simulación aislada de reescritura sí fue ejecutada exitosamente, pero no equivale a la limpieza ni publicación del historial remoto real.
