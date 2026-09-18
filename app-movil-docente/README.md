# SGA Representante — Android

Aplicación móvil de consulta para representantes de AcadTrace, construida con Kotlin, Jetpack Compose, MVVM, Retrofit/OkHttp, Room, WorkManager y almacenamiento cifrado de sesión.

## Flujo activo

`Login → HomeRepresentante → MisRepresentados → ResumenRepresentado → Calificaciones | Asistencia`

- El login exige el valor backend exacto `REPRESENTANTE`.
- Los estudiantes se obtienen exclusivamente desde `GET /api/representante/me/estudiantes`.
- Calificaciones y asistencia pasan por la autorización usuario–representante–estudiante del backend.
- Room conserva los últimos representados, calificaciones y asistencias consultados.
- El flujo activo no crea actividades, no registra notas o asistencia y no programa colas de escritura.

## Servicios

- Principal: `POST /api/auth/login` y `GET /api/representante/me/estudiantes`.
- Microservicio docente: consultas bajo `/api/docente/representante/me/estudiantes/{id}/`.

Las URLs se conservan configurables mediante el almacenamiento seguro existente.

## Paquete de release

El APK y el Android App Bundle de producción se generan únicamente con una firma de release configurada. El keystore y sus contraseñas deben permanecer fuera de Git.

Configure la firma mediante estas variables de entorno:

- `SGA_RELEASE_STORE_FILE`: ruta al keystore.
- `SGA_RELEASE_STORE_PASSWORD`: contraseña del keystore.
- `SGA_RELEASE_KEY_ALIAS`: alias de la clave.
- `SGA_RELEASE_KEY_PASSWORD`: contraseña de la clave.

Como alternativa local, cree `keystore.properties` en la raíz de `app-movil-docente` con las propiedades `storeFile`, `storePassword`, `keyAlias` y `keyPassword`. Ese archivo, al igual que `*.jks` y `*.keystore`, está ignorado y no debe versionarse.

Desde `app-movil-docente`, use exclusivamente el wrapper versionado:

```powershell
.\gradlew.bat clean assembleRelease bundleRelease
```

Salidas esperadas:

- APK: `app/build/outputs/apk/release/app-release.apk`
- AAB: `app/build/outputs/bundle/release/app-release.aab`

El job `build-mobile-apk` comprueba la configuración de firma, genera el APK y el AAB de Release, verifica sus firmas y calcula el SHA-256 de ambos artefactos. La creación del tag de versión es una acción explícita; al recibir un tag con formato `vN.N.N`, CI comprueba que coincida con `versionName` y publica automáticamente en GitHub Release el `app-release.apk`, `app-release.aab` y `SHA256SUMS.txt` generados y verificados en esa misma ejecución.
