# Demo offline reproducible — Representante

## Preparación

1. Levantar `sga-principal` y `microservicio-docente` con sus bases y configurar en la app las URL accesibles desde el emulador/dispositivo.
2. Ejecutar `gradlew.bat assembleDebug`, instalar `app/build/outputs/apk/debug/app-debug.apk` e iniciar la app.
3. Iniciar sesión con un usuario cuyo rol devuelto sea exactamente `REPRESENTANTE`.

## Consulta y caché

1. Abrir **Mis representados**, seleccionar un hijo y entrar en **Resumen**.
2. Abrir **Calificaciones** y **Asistencia** con conectividad; comprobar los datos recibidos.
3. En App Inspection > Database Inspector verificar las tablas `representados_cache`, `calificaciones_representado_cache` y `asistencia_hijo_cache`.
4. Activar modo avión y volver a ambas pantallas. Los mismos datos deben aparecer desde Room.
5. Solicitar refresh sin red. La UI conserva el caché y `SyncWorker` queda condicionado a red/reintento; no se crea ninguna escritura académica.
6. Recuperar conectividad y ejecutar el trabajo desde WorkManager Inspector (o esperar su ejecución). Verificar que consulta nuevamente y actualiza `lastUpdated` en Room.

## Seguridad y notificación

1. En **Seguridad**, habilitar biometría con una sesión vigente; cerrar y abrir la app.
2. Mostrar el `BiometricPrompt` real. Cancelar o provocar error no debe abrir Home; autenticar correctamente sí debe abrirlo.
3. Expirar la sesión de prueba y ejecutar `SessionExpiryNotificationWorker` desde WorkManager Inspector para mostrar la notificación local del canal **Seguridad de la sesión**.

No se crean ni editan notas, asistencias, actividades o materiales durante esta demo. La app no implementa FCM: el push institucional requiere infraestructura Firebase y un endpoint backend real de registro de token.
