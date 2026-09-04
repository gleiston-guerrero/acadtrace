# ADR-006: Elección de arquitectura y tecnologías para la aplicación móvil de representantes

## Estado

Aceptado

## Contexto

La aplicación móvil de AcadTrace está destinada exclusivamente al rol `REPRESENTANTE`. Sus casos de uso principales son de consulta: autenticarse, listar sus representados, consultar calificaciones y asistencia, recibir avisos locales de seguridad y proteger una sesión vigente mediante biometría. La autorización de cada estudiante permanece en el backend; la aplicación no permite elegir identificadores arbitrarios ni realizar escrituras académicas.

El proyecto Android existente ya estaba construido con Kotlin y componentes Jetpack. La decisión debía conservar esa inversión, mantener el caché offline y añadir capacidades del dispositivo sin crear una segunda aplicación ni introducir infraestructura externa ficticia.

## Decisión

Se mantiene una aplicación Android nativa con:

- Kotlin 1.9.23 y Android Gradle Plugin 8.3.2.
- Jetpack Compose para la interfaz y Navigation Compose para el flujo.
- MVVM, con UI separada de ViewModels, repositories, Retrofit y Room.
- Retrofit 2.11 y OkHttp 4.12 para los cuatro contratos REST consumidos por el flujo activo.
- Room 2.6.1 para caché de representados, calificaciones y asistencia.
- WorkManager 2.9 para refrescar la caché de lectura al recuperar conectividad y para avisos locales de expiración de sesión.
- `EncryptedSharedPreferences` y Android Keystore para JWT, roles y preferencias sensibles.
- AndroidX Biometric 1.1 para desbloquear únicamente una sesión local que siga vigente.
- `NotificationChannel` y `POST_NOTIFICATIONS` para notificaciones locales de seguridad.

No se integra FCM porque el repositorio no contiene `google-services.json`, plugin Google Services, dependencia Firebase Messaging, proyecto Firebase ni endpoint backend para registrar tokens. Esos datos deben provenir de una configuración externa real.

## Justificación cuantitativa

Estado comprobado el 4 de septiembre de 2026. Los porcentajes no se publican hasta que JaCoCo logre completar una ejecución real:

| Métrica | Valor comprobado |
| --- | ---: |
| `minSdk` | 26 |
| `targetSdk` | 34 |
| `compileSdk` | 34 |
| Rutas/pantallas finales del representante | 9 |
| ViewModels activos en el flujo final | 2 |
| Clases ViewModel presentes, incluyendo implementación histórica inaccesible | 13 |
| Repositories activos en el flujo final | 2 |
| Implementaciones repository presentes, incluyendo código histórico | 12 |
| Entidades Room activas para caché de representante | 3 |
| Entidades Room totales conservadas por compatibilidad de esquema | 17 |
| Endpoints REST consumidos por el flujo activo | 4 |
| Casos JVM Android descubiertos en fuente | 26 |
| Resultado JVM actual | No ejecutado: Gradle falla antes del worker con `Unable to establish loopback connection` |
| Cobertura JaCoCo (instructions/lines/branches/classes) | Pendiente de ejecución real; no disponible en este host |
| Tamaño APK debug actual | No disponible: no se generó artefacto en esta ejecución |
| Tamaño APK release | Pendiente de keystore y firma interactiva |
| Capacidades del dispositivo implementadas | 2 |
| Roles móviles permitidos | 1 (`REPRESENTANTE`) |
| Escrituras académicas permitidas desde la app | 0 |

Las nueve rutas incluyen Login, desbloqueo biométrico, fallback biométrico, Home, Seguridad, Mis representados, Resumen, Calificaciones y Asistencia. Las implementaciones históricas de docente no aparecen en el grafo activo.

## Alternativas consideradas

### 1. Android nativo con Kotlin

Reutiliza el proyecto existente, sus 74 archivos Kotlin iniciales, Compose, Room, WorkManager, seguridad y pruebas. Biometría, permisos y canales de notificación se integran directamente con APIs Android. El costo de migración de plataforma es cero; solo se transforma el dominio y se incorporan capacidades.

### 2. Flutter

Habría requerido crear y mantener un proyecto Dart, reescribir las nueve pantallas, los cuatro contratos, la persistencia Room equivalente, navegación, sesión segura y las pruebas. Aunque dispone de plugins biométricos y de notificaciones, no reutiliza directamente la implementación Kotlin existente.

### 3. React Native

Habría requerido una capa JavaScript/TypeScript y puentes o bibliotecas para biometría, almacenamiento seguro, tareas periódicas y persistencia. También obligaría a reescribir las nueve pantallas y sustituir la arquitectura Compose/Room ya validada.

Se elige Android nativo porque es la única alternativa que conserva directamente el 100% de la plataforma tecnológica existente y evita una reescritura completa del flujo activo.

## Consecuencias positivas

- Integración directa con biometría, permisos, notificaciones y ciclo de vida Android.
- Reutilización del flujo funcional y de la identidad visual existente.
- Caché offline de las tres consultas relevantes.
- Menor superficie de autorización: un rol permitido y cero escrituras académicas.
- La ausencia de Firebase no bloquea las notificaciones locales de seguridad.

## Consecuencias negativas

- La aplicación es específica de Android.
- FCM necesita configuración externa antes de ofrecer push remoto.
- Se conservan entidades y clases históricas para mantener compatibilidad del esquema; permanecen fuera de navegación y del inicializador activo.
- La firma release depende de un keystore privado que no puede almacenarse en el repositorio.

## Seguridad

- El backend emite el JWT y valida la relación usuario–representante–estudiante para cada consulta.
- Android acepta exactamente el rol externo `REPRESENTANTE` y rechaza `DOCENTE`.
- El JWT debe estar vigente antes de permitir Home o desbloqueo biométrico.
- La biometría no crea, renueva ni sustituye el JWT; solo desbloquea localmente una sesión válida.
- No se almacenan contraseña, huellas ni plantillas biométricas.
- JWT, roles y preferencias se guardan mediante almacenamiento cifrado respaldado por Android Keystore.
- El flujo activo Room usa únicamente `representados_cache`, `calificaciones_representado_cache` y `asistencia_hijo_cache`; las entidades docentes restantes son históricas por compatibilidad del esquema.
- Los encabezados `Authorization` se redactan en logs.
- El keystore de firma y sus contraseñas se suministran mediante variables de entorno y están excluidos de Git.

## Evidencia

- Proyecto: `C:\acadtrace\app-movil-docente`.
- Package físico y lógico: `app/src/main/java/ec/edu/uteq/sga/representante`.
- Salida esperada del APK debug: `app/build/outputs/apk/debug/app-debug.apk`.
- Contratos: `/api/auth/login`, `/api/representante/me/estudiantes`, `/api/docente/representante/me/estudiantes/{id}/calificaciones/` y `/api/docente/representante/me/estudiantes/{id}/asistencia/`.
- JaCoCo está configurado para publicar HTML en `docs/cobertura/movil/index.html` y XML en `docs/cobertura/movil/jacoco.xml`.
- En Windows, `testDebugUnitTest`, JaCoCo, lint y assemble se bloquean antes de ejecutar tareas por `java.io.IOException: Unable to establish loopback connection`; el job Ubuntu es la fuente prevista de evidencia real.
- APK release: **PENDIENTE ÚNICAMENTE FIRMA INTERACTIVA**; no existe keystore versionado.
