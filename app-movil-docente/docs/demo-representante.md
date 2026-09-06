# Demo real: aplicación móvil de representante

Fecha de evidencia: 4 de septiembre de 2026.

## Estado validado en teléfono físico

- Dispositivo ADB: TECNO CL7 (`119774047C100335`).
- Paquete instalado: `ec.edu.uteq.sga.representante`, versión 1.0.0 (code 1).
- Sesión de representante vigente y Home accesible.
- `BiometricPrompt` real mostrado; Logcat confirmó autenticación satisfactoria y retorno a Home.
- Principal respondió HTTP 200 a `GET /api/representante/me/estudiantes` y devolvió `idEstudiante=681`.
- La llamada directa histórica a Docente produjo HTTP 403 por `JWT_SECRET no configurado`; la fachada nueva elimina ese flujo inseguro.

El APK instalado es anterior a los últimos cambios locales. No se atribuye a ese APK la corrección de permisos, Comunicados ni las nuevas pruebas.

## Cambios locales pendientes de instalar

- Compatibilidad de Fragment fijada en 1.6.2 para evitar el `requestCode` fuera de 16 bits al solicitar `POST_NOTIFICATIONS`.
- Android consume calificaciones, asistencia y comunicados únicamente desde SGA Principal; el flujo activo ya no crea un cliente `RepresentanteApi` hacia Docente/8081.
- Comunicados consulta `Anuncio`, propiedad real de Docente, mediante Principal REST → gRPC Docente.
- Room conserva únicamente caché de lectura para representados, calificaciones, asistencia y comunicados y marca las respuestas recuperadas como offline.
- WorkManager refresca esas cuatro consultas GET; `SyncManager` rechaza expresamente escrituras académicas.
- Las pruebas de `RepresentanteViewModel` cubren también loading, success, empty, error y caché offline de Comunicados; se conserva la prueba instrumentada Compose.

## Estado de Compilación y Suite de Pruebas

Se configuró el entorno local y se ejecutaron satisfactoriamente las tareas de Gradle:

```text
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

- **Suite JVM Android:** 36 pruebas ejecutadas y aprobadas (0 fallos, 100% de tasa de éxito).
- **Artefacto Binario Generado:** `app-debug.apk` generado en `app/build/outputs/apk/debug/app-debug.apk`, con un tamaño de 19,992,081 bytes (~19.06 MB) y SHA-256 `E97D8BAD09ECF7AA2C590D7480F54735A17946859BB52C917DE54EDF165A595B`.
- **Ubicación en Release (Listado 3):** `release/apk/app-representante-debug.apk`.
- Las pruebas específicas de la fachada en SGA Principal aprobaron 11/11. La suite de Docente aprobó 83/83 con cobertura total de 79.28%.

## Interacción humana requerida después de resolver Gradle

1. Instalar el APK nuevo en el TECNO CL7.
2. Abrir Seguridad, pulsar la opción de notificaciones y aceptar `POST_NOTIFICATIONS`; confirmar que la app permanece abierta.
3. Bloquear y desbloquear con huella cuando aparezca `BiometricPrompt`.
4. Entrar en el representado y abrir Calificaciones y Asistencia; ambas solicitudes Android deben dirigirse a Principal en el puerto 8080.
5. Abrir Comunicados y verificar que únicamente aparecen anuncios de asignaciones asociadas a matrículas activas de los representados.
6. Activar modo avión y repetir las cuatro consultas para confirmar visualmente la lectura desde Room; después desactivarlo para observar el refresh GET de WorkManager.

No deben ejecutarse acciones docentes ni escrituras académicas durante la demo.
