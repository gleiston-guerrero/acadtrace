# Gestión de secretos fuera del árbol versionado — E46

Revisión: 2026-09-22. Rama `Juliana-Emanuel`; commit de referencia `9c49a4b7`.
**Estado: PARCIAL. E46 no está cerrado. Rotaciones verificadas: Ninguna acreditada.**
Los cambios están en el árbol de trabajo, sin commit. No se modificó historia ni se publicó ningún artefacto.

## HEAD ACTUAL / árbol de trabajo

El HEAD comprometido conserva las 15 apariciones evaluadas en seis archivos. El contenido actual de esos archivos está corregido localmente:

- Android obtiene `SGA_GATEWAY_URL` y `SGA_DOCENTE_URL` del entorno mediante BuildConfig, con valores locales para emulador. Constants y SettingsScreen comparten esa configuración. Las preferencias persistidas por instalaciones anteriores no se borran automáticamente; puede ser necesario restablecer endpoints en Ajustes.
- Los dos Calificaciones.jsx importan `IA_API_BASE_URL` desde el módulo de configuración existente. Usan `VITE_IA_API_URL`, con fallback al hostname actual y puerto del servicio IA.
- El script de docentes usa `SGA_API_BASE_URL`, `SGA_ADMIN_USERNAME` y `SGA_ADMIN_PASSWORD`, exige credenciales no vacías y rechaza URLs con credenciales embebidas. Llama a la API HTTP, no a PostgreSQL: añadir DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD no tendría efecto; cambiarlo a acceso directo alteraría su funcionamiento.
- El HTML móvil se regeneró mediante `:app:jacocoTestReport`, sin editarlo manualmente. Tras clasificar las 1.094 diferencias, se restauraron 1.093 salidas: 1.087 cambios de formato regional (idioma, porcentajes, millares y orden de filas), cuatro páginas con cambios de cinco instrucciones de SettingsScreen, el XML y la página de sesiones. Se conserva únicamente `ec.edu.uteq.sga.representante.ui.screens.settings/SettingsScreen.kt.html`, tal como lo produjo JaCoCo, porque era la única página que conservaba la dirección pública. Las métricas e índices canónicos siguen siendo la evidencia de CI #876 documentada en `docs/cobertura/movil/README.md`; esta sustitución mínima de fuente no representa una nueva medición integral de cobertura.

La búsqueda completa con `scripts/auditar_referencias_e46.py` examina los archivos de `git ls-files`, incluidos binarios como bytes: **0 apariciones de ambas direcciones de producción; 0 IPv4 públicas sin clasificar**. Los 40 candidatos numéricos en cinco XML son versiones de JDK/bibliotecas, no endpoints; se conservan. El detalle regenerable por ruta y URLs HTTP se conserva fuera del árbol versionable, en la carpeta temporal `acadtrace-e46-tools`. Puede regenerarse con `python scripts/auditar_referencias_e46.py --output <ruta-externa.json>`. Las referencias HTTP documentales no son secretos por su protocolo. No se certifica ausencia visual en imágenes ni configuración desplegada.

Se conservan las plantillas, exclusiones de archivos de entorno reales y configuración externa de Firebase/firma. Retirar un archivo de Git no acredita custodia externa ni revocación de su contenido.

## HISTORIAL

**COMPUERTA ACTIVA:**
- known = 11
- new = 0
- replaced = 0
- extra = 0
- scannerExit = 2
- gateExit = 0
- processExit = 0

**AUDITORÍA AMPLIADA:**
- baseline occurrences = 11
- additional occurrences = 91
- total observed occurrences = 102
- rules e46-* active in CI = false

El [inventario](e46_inventario_historico.md) y [metadata por ocurrencia](e46_historial_metadata.json) documentan commits y refs alcanzables. Confirman los tres archivos señalados y otras ubicaciones antiguas de dumps/configuraciones. El detector no certifica haber encontrado todos los secretos ni su vigencia.

Se identificaron cuatro mensajes históricos con direcciones. Las refs locales incluyen main, ramas remotas, tags y stash; no se afirma sincronización completa con el servidor. La limpieza histórica es necesaria para E46 y sigue pendiente conforme al [plan de reescritura](e46_plan_reescritura.md).

## ROTACIÓN

**Rotaciones verificadas: Ninguna acreditada.**

El [registro](registro_rotacion_e46.md) separa PostgreSQL, JWT, gRPC, SMTP, cuenta administrativa, hashes de usuarios, APIs, Firebase y cifrado. Todos están PENDIENTE; algunas categorías requieren confirmar exposición. Juliana debe aportar evidencia externa verificable para cada caso aplicable. No se intentó autenticar con valores históricos ni cambiar servicios de producción.

## APK

La copia local del APK v1.0.2 coincide con el digest del asset consultado mediante la API de GitHub. Tiene **1 aparición en classes.dex**. Evidencia: `e46_apk_anterior.json`.

SHA-256 del APK anterior: `cfd6b03995b292cba2a9926fc9adde3c6e9ebc075e2e034fee68b117d476e27c`.

APK de revisión: `app-movil-docente/app/build/outputs/apk/release/app-release.apk`.

SHA-256: `ff5bda44157d60407d085ef4d9b1ae67a6aaca856ce651b528c5dd29d4945f7a`.

Se generó con wrapper oficial, endpoints locales y keystore sintético temporal. Inspección de todas las entradas ZIP, incluidos DEX/recursos, como ASCII/UTF-8 y UTF-16LE: **production_server_literal_occurrences = 0**. Evidencia: `e46_apk_revision.json`. La firma sintética es de revisión y no permite actualizar instalaciones firmadas con la clave de producción. No se publicó ni reemplazó v1.0.2. Distribuirlo requiere firma/configuración autorizadas, nueva verificación y retirada o sustitución del asset antiguo.

## CI

Workflow con Gitleaks 8.18.0, checkout completo y cuatro modos del helper. El histórico usa `--all --full-history -m`, rechaza clones shallow y **falla con 2 ante cualquier hallazgo, incluso conocido**. El baseline clasifica deuda; no la autoriza ni vuelve verde CI. Errores devuelven 1. El workflow seguirá bloqueado mientras exista deuda.

La consola solo muestra resultados sanitizados; el reporte temporal redactado se elimina. La metadata incluye ruta, regla, commit, ubicación e identidad derivada únicamente de metadata, nunca valores ni hashes de secretos. Tree cubre tracked y la lista explícita de nuevos archivos E46 bajo revisión; no incluye untracked ajenos.

El autotest de árbol inserta una credencial sintética en una copia temporal. El histórico prueba además el detector real en un repositorio desechable y mutaciones de metadata. No modifica refs del proyecto. No se afirma ejecución publicada de CI.

## Verificaciones locales

| Verificación | Resultado |
|---|---|
| tree | 0 hallazgos; código 0 |
| history | 102; 11 conocidos, 91 adicionales; scanner/compuerta 2 |
| self-test | PASS; mutación rechazada con 2, autotest 0 |
| self-test-history | PASS; detector histórico rechaza con 2, autotest 0 |
| Frontend Principal | npm ci --ignore-scripts y npm run build: 0 |
| Frontend Secretaría | npm ci --ignore-scripts y npm run build: 0 |
| Android | assembleRelease testDebugUnitTest jacocoTestReport: BUILD SUCCESSFUL; 94 pruebas, 0 fallos/errores/omitidas |
| Python | Sintaxis válida; cuatro pruebas sin red: OK |
| git diff --check | 0, sin errores |

Android requiere JAVA_HOME, ANDROID_HOME y GRADLE_USER_HOME locales y keystore de revisión. Desde su directorio: `./gradlew.bat assembleRelease testDebugUnitTest jacocoTestReport --console=plain`. Los intentos iniciales fallaron por permisos y SDK sin configurar; con rutas correctas pasaron. Secretaría requirió ejecutar el build fuera del sandbox por acceso denegado de esbuild. Los frontends no declaran script de tests; se verificaron builds. No se probaron conexiones de producción.

**Falta para cerrar:** acreditar rotaciones/revocaciones aplicables, aprobar y ejecutar limpieza de todas las refs con history=0, verificar copias/cachés/forks y retirar o sustituir el APK publicado. El plan conserva pendientes explícitos sobre refs remotas, mapas protegidos y número definitivo de commits; una simulación no ejecutada no es evidencia.
