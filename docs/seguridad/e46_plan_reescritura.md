# E46 — Plan de reescritura para revisión; NO EJECUTADO

Fecha: 2026-09-22. Este documento no autoriza ejecutar sus comandos. No se ejecutó ninguna reescritura, commit, publicación ni push. La rotación externa debe preceder a la limpieza publicada.

## Herramienta y alcance

Usar `git-filter-repo` con soporte de `--sensitive-data-removal`, en un clon espejo desechable y aislado. Reemplazar contenido con `--replace-text` y mensajes con `--replace-message`; eliminar rutas de secretos/dumps con `--invert-paths`. La sintaxis procede de la [documentación oficial](https://github.com/newren/git-filter-repo/blob/main/Documentation/git-filter-repo.txt). No ejecutar sobre el directorio de trabajo de Juliana.

El inventario completo de refs, SHAs, 102 hallazgos, cuatro mensajes y alcanzabilidad está en `e46_historial_metadata.json`. Solo acredita refs locales: antes de aprobar la ejecución hay que contrastar el inventario remoto, incluido el tag v1.0.2 que no está disponible localmente. No se promete un número exacto de commits remotos a partir de este clon.

- Ramas locales: `Juliana-Emanuel`, `main`, `respaldo-Juliana-Emanuel-antes-limpieza`, `respaldo-Juliana-antes-sync`.
- Ramas remotas visibles: `Ernesto-Luna`, `Juliana-Emanuel`, `Leonardo-Castro`, `devBedon`, `feat/cluster-db-fragmentacion`, `fix/e45-cierre-total`, `main`; `origin/HEAD` es simbólica.
- Tags locales: `pre-e4`, `v1.0.0`, `v1.0.1`; también se debe inspeccionar `v1.0.2` antes de ejecutar en el remoto.
- Referencias auxiliares: `refs/stash` y referencias locales de captura de Codex. No deben trasladarse al servidor ni dejarse como raíces públicas de la historia antigua.
- Recuento local: **1.169 commits alcanzables; al menos 1.143 afectados**, según los campos `commits_total` y `commits_affected_minimum` de la metadata. El total definitivo lo dará el `commit-map` de una simulación aislada, que también requiere autorización. Este límite impide presentar una estimación como cifra exacta final.

## Transformaciones propuestas

Eliminar de todas las revisiones las rutas siguientes (incluidas sus antiguas ubicaciones):

```text
.env
microservicio-soporte/.env
sga-principal/sql/supabase_dump_completo.sql
sql/supabase_dump_completo.sql
scripts/backups/backup_pre_seed_20260806_135036.sql
sga-principal/src/main/resources/db/init/baseline_completo.sql
```

Conservar `scripts/populate_all_docentes_full.py`, reemplazando únicamente credenciales y endpoints históricos. Conservar las propiedades y los tests, sustituyendo los literales detectados. Los archivos afectados por hallazgos son los enumerados por ocurrencia en `e46_inventario_historico.md`; no basta con limpiar las tres rutas citadas inicialmente.

Preparar fuera del repositorio, con acceso restringido, dos mapas exactos: `reemplazos-contenido.txt` y `reemplazos-mensajes.txt`. Extraer valores solo en memoria desde los blobs identificados por el scanner y las dos referencias de producción, nunca de stdout ni del informe Markdown. El primer mapa debe cubrir las 102 ocurrencias (deduplicando valores), ambas IP, sus codificaciones presentes y cualquier secreto real adicional confirmado. El segundo incluye ambas IP para todos los mensajes. No generar expresiones vacías ni sustituciones globales de palabras comunes. Revisar colisiones: hashes/fixtures y textos cortos pueden necesitar un callback acotado por blob/ruta. **Los mapas aún no se han generado ni aprobado**; no ejecutar el comando siguiente sin ese control.

Los APK son ZIP comprimidos: `--replace-text` no sanea sus DEX comprimidos. Revisar todos los APK/AAB históricos con el auditor; si son objetos Git contaminados, eliminarlos por ruta/blob. El APK externo de v1.0.2 exige una retirada o sustitución de asset separada y autorizada; modificar tags no cambia los assets de una release.

## Respaldo y coordinación

Juliana y todos los titulares de clones de las ramas anteriores deben guardar trabajo local y, tras publicar, reclonar. Los nombres de ramas no acreditan una lista completa de colaboradores: confirmar también a Emanuel, propietario del repositorio, integraciones y forks con acceso al proyecto. Nadie debe fusionar o empujar la historia antigua de nuevo. Preferir reclonado a reset; rescatar únicamente cambios revisados sin secretos.

Antes de la simulación: congelar pushes, registrar SHAs remotos, preservar cambios tracked y untracked en copia local protegida fuera del repo y crear un bundle completo. El bundle contiene los secretos históricos: no subirlo a GitHub, no ponerlo en la carpeta pública del proyecto y acordar caducidad. Copiar el árbol de trabajo de forma explícita, incluidos untracked de E46 y trabajo ajeno; `git bundle` no los incluye.

## Comandos previstos (PowerShell; NO ejecutados)

Primero acordar una ruta externa protegida para `$e46Vault` y resolver todos los puntos anteriores. Los siguientes comandos son la secuencia propuesta; no incluyen autorización de publicación.

```powershell
$e46Repo = 'C:\Users\Juliana\Downloads\Proyectos\acadtrace'
$e46Vault = 'D:\RespaldoPrivadoE46'
git -C $e46Repo bundle create "$e46Vault\antes-e46.bundle" --all
git -C $e46Repo bundle verify "$e46Vault\antes-e46.bundle"
git -C $e46Repo for-each-ref --format='%(refname) %(objectname)' > "$e46Vault\refs-antes.txt"
git ls-remote --heads --tags https://github.com/gleiston-guerrero/acadtrace.git > "$e46Vault\refs-remotas-antes.txt"
git clone --mirror --no-local $e46Repo "$e46Vault\ensayo.git"
```

Revisar refs remotas ausentes y traerlas al espejo de ensayo solo tras acordar el alcance exacto. Con los mapas revisados, en el espejo:

```powershell
git -C "$e46Vault\ensayo.git" filter-repo --sensitive-data-removal --no-fetch --invert-paths --path .env --path microservicio-soporte/.env --path sga-principal/sql/supabase_dump_completo.sql --path sql/supabase_dump_completo.sql --path scripts/backups/backup_pre_seed_20260806_135036.sql --path sga-principal/src/main/resources/db/init/baseline_completo.sql --replace-text "$e46Vault\reemplazos-contenido.txt" --replace-message "$e46Vault\reemplazos-mensajes.txt"
git clone --no-local "$e46Vault\ensayo.git" "$e46Vault\verificacion"
git -C "$e46Vault\verificacion" fetch "$e46Vault\ensayo.git" '+refs/*:refs/audit/*'
```

La opción `--no-fetch` mantiene el ensayo acotado al espejo inventariado; no se debe interpretar como cobertura de refs remotas faltantes. No usar `--force` para saltarse la protección de clon fresco. Si el inventario encuentra APK históricos u otras rutas/secretos, revisar este comando antes de ejecutarlo.

El fetch local incluye también refs auxiliares y remotas del espejo bajo `refs/audit/`; un clon normal solo traería ramas/tags y podría omitir deuda. En el clon de verificación, aplicar las correcciones E46 revisadas antes de probar y usar la configuración ampliada del detector:

```powershell
node scripts/scan-secrets.cjs tree
node scripts/scan-secrets.cjs history
node scripts/scan-secrets.cjs self-test
node scripts/scan-secrets.cjs self-test-history
git diff --check
```

Exigir tree=0 e history=0, código 0 en ambos escaneos; los autotests deben detectar las mutaciones y retornar 0. La identidad del baseline anterior quedará obsoleta tras cambiar SHAs: no ampliar ni recrear baseline para aceptar deuda. Mantener la prueba de regresión sintética separada de la evidencia histórica archivada.

Verificar ambas IP y cada secreto confirmado **en todos los commits alcanzables**, no solo en tips: `git rev-list --all` y, para cada SHA, `git grep -q -F -f <patrones-externos> <SHA>`. Capturar únicamente el código: 1 significa sin coincidencias, 0 incumple; otro código es error, no éxito. Usar patrones literales no vacíos sin imprimirlos. Repetir en mensajes completos y en todas las entradas descomprimidas de APK/AAB. Los hashes protegidos/codificaciones deben incluirse; una búsqueda textual no certifica imágenes. El auditor actual toma las IP de commits históricos: después de reescribir debe recibirlas desde la política externa protegida, pues los SHAs originales ya no existirán.

Comparar `commit-map`, `ref-map` y árbol final: explicar toda eliminación, contar SHAs modificados, comprobar tags y firmas, ejecutar builds/tests, verificar que no queden refs antiguas y solicitar revisión del ensayo. Presentar entonces comandos de publicación por refs explícitas y SHAs de lease capturados. **No se propone `push --mirror` automático**, porque puede publicar refs auxiliares o borrar ramas. El push, las protecciones de rama, la sustitución del APK y la limpieza de cachés/PR/forks requieren aprobación separada.

## Riesgos

Cambian SHAs de commits y descendientes, firmas y tags; enlaces de evidencia académica, PR, cachés de CI y referencias de releases pueden quedar obsoletos. Los merges desde clones antiguos pueden reintroducir secretos. GitHub puede conservar vistas cacheadas o refs de PR y terceros pueden mantener forks/copias; coordinar su retirada con responsables/soporte. Reescribir no revoca una sola credencial y no borra assets publicados. Las cuentas y claves de cifrado requieren su propio procedimiento de rotación y recuperación.

**Punto de detención:** aprobar primero alcance remoto, mapas protegidos y ensayo; este documento no es un script listo para publicar. No se ejecutó la fase 8.
