# Demo real: aplicación móvil de representante

Fecha de evidencia: 4 de septiembre de 2026.

## Estado validado en teléfono físico

- Dispositivo ADB: TECNO CL7 (`119774047C100335`).
- Paquete instalado: `ec.edu.uteq.sga.representante`, versión 1.0.0 (code 1).
- Sesión de representante vigente y Home accesible.
- `BiometricPrompt` real mostrado; Logcat confirmó autenticación satisfactoria y retorno a Home.
- Principal respondió HTTP 200 a `GET /api/representante/me/estudiantes` y devolvió `idEstudiante=681`.
- Android llamó con JWT Bearer a `GET /api/docente/representante/me/estudiantes/681/calificaciones/` y AWS devolvió HTTP 403: `{"detail":"JWT_SECRET no configurado"}`.
- No se modificó backend y la app conserva el error visible. La causa demostrada es la configuración de AWS.

El APK instalado es anterior a los últimos cambios locales. No se atribuye a ese APK la corrección de permisos, Comunicados ni las nuevas pruebas.

## Cambios locales pendientes de instalar

- Compatibilidad de Fragment fijada en 1.6.2 para evitar el `requestCode` fuera de 16 bits al solicitar `POST_NOTIFICATIONS`.
- Comunicados sólo informa la ausencia de endpoint para representante; no consume ni expone escrituras docentes.
- Room conserva únicamente caché de lectura para representados, calificaciones y asistencia y marca las respuestas recuperadas como offline.
- WorkManager refresca esas consultas GET; `SyncManager` rechaza expresamente escrituras académicas.
- Se añadieron seis pruebas JVM de `RepresentanteViewModel` y una prueba instrumentada Compose del Home exclusivo de representante.

## Bloqueo reproducible del host

Estas órdenes se ejecutaron y fallaron antes de iniciar las tareas Gradle con `java.io.IOException: Unable to establish loopback connection`:

```text
.\gradlew.bat testDebugUnitTest jacocoTestReport
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

En consecuencia, no existen resultados JaCoCo actuales ni un APK nuevo válido para copiar a `release/apk/app-debug.apk`; tampoco se informa tamaño o SHA-256 inventados.

## Interacción humana requerida después de resolver Gradle

1. Instalar el APK nuevo en el TECNO CL7.
2. Abrir Seguridad, pulsar la opción de notificaciones y aceptar `POST_NOTIFICATIONS`; confirmar que la app permanece abierta.
3. Bloquear y desbloquear con huella cuando aparezca `BiometricPrompt`.
4. Entrar en el representado y abrir Calificaciones y Asistencia para verificar ambas respuestas AWS con el mismo `idEstudiante`.
5. Activar modo avión y repetir las tres consultas para confirmar visualmente la lectura desde Room; después desactivarlo para observar el refresh GET de WorkManager.

No deben ejecutarse acciones docentes ni escrituras académicas durante la demo.
