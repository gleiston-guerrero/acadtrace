# Paquete Binario Móvil: AcadTrace Representantes

- **Artefacto:** `app-representante-debug.apk`
- **Destinatario Oficial:** Rol `REPRESENTANTE` (Estudiantes / Padres de Familia de Escuela Provincias Unidas)
- **Plataforma:** Android Nativo (Kotlin 1.9.23 + Jetpack Compose)
- **Versión:** 1.0.0 (`versionCode: 1`)
- **Package ID:** `ec.edu.uteq.sga.representante`
- **Compilación:** `assembleDebug` (Gradle 8.4, AGP 8.3.2)
- **SDK:** `minSdk 26` (Android 8.0 Oreo), `targetSdk 34` (Android 14)
- **Tamaño:** 19,992,081 bytes (~19.06 MB)
- **SHA-256:** `E97D8BAD09ECF7AA2C590D7480F54735A17946859BB52C917DE54EDF165A595B`

## Capacidades de Hardware del Dispositivo
1. **Autenticación Biométrica (`USE_BIOMETRIC`):** Desbloqueo seguro de sesión local mediante `BiometricPrompt` (huella dactilar / biometría facial).
2. **Notificaciones Locales (`POST_NOTIFICATIONS`):** Alertas locales de seguridad sobre sesiones activas y comunicados escolares institucionales.

## Persistencia y Sincronización
- **Offline-First:** Motor de base de datos local SQLite con **Room 2.6.1** para consulta en ausencia de conectividad.
- **Sincronización:** Tareas programadas con **WorkManager 2.9** para refresco de caché al recuperar red (`NetworkType.CONNECTED`).
- **Consumo:** API REST de `sga-principal` (puerto 8080) con autenticación JWT redactada y validación de parentesco representante-estudiante.
