# Notificaciones FCM para representantes

## Arquitectura

FCM se usa solamente como transporte push. JWT, roles, PostgreSQL, reglas académicas, Room, WorkManager y gRPC conservan sus responsabilidades.

El teléfono obtiene el token FCM y lo registra con JWT en `POST /api/representante/me/dispositivos`. Principal toma el usuario del JWT; el cuerpo no acepta `idRepresentante`. La tabla `sga_principal.dispositivos_representante` admite varios dispositivos por usuario y hace globalmente único cada token. Un token renovado se reasocia al usuario autenticado; logout puede llamar `DELETE` al mismo recurso antes de borrar la sesión (la implementación actual conserva el token activo para no perder avisos al cerrar la UI).

Docente emite, después del commit, eventos de `AUSENTE`, `ATRASO` y `COMUNICADO` al endpoint interno de Principal, protegido por `X-Internal-Token` y `GRPC_INTERNAL_TOKEN`. Principal deriva el representante desde matrícula/asignación reales. Un fallo HTTP o FCM se registra sin payload, credenciales ni token completo y nunca revierte la operación académica.

El cierre no se infiere de `fecha_fin`. No había una operación inequívoca, por lo que se añadió `POST /api/configuracion/calificacion/periodos/{id}/cerrar`: únicamente la transición explícita `activo=true` a `false` representa el cierre. Repetirla no vuelve a notificar.

`sga_principal.eventos_notificacion_push` guarda una clave lógica única por usuario. Las claves son `ASISTENCIA:{id}:{estado}`, `COMUNICADO:{id}` y `CIERRE_CALIFICACIONES:{periodId}:{studentId}`. Esto evita duplicados por ediciones o reintentos. Tokens rechazados como inválidos se desactivan.

## Configuración Firebase

1. Cree un proyecto en Firebase Console.
2. Agregue una aplicación Android con package exacto `ec.edu.uteq.sga.representante`.
3. Descargue el archivo real `google-services.json` y colóquelo en `app-movil-docente/app/google-services.json`. Está ignorado y no debe versionarse. El plugin Google Services se aplica sólo cuando el archivo existe, por lo que el árbol puede compilar antes de recibirlo.
4. En Google Cloud/Firebase cree o seleccione una cuenta de servicio con permiso para Firebase Cloud Messaging. Descargue su JSON fuera del repositorio.
5. En el entorno de Principal defina `GOOGLE_APPLICATION_CREDENTIALS` con la ruta absoluta al JSON y `FCM_ENABLED=true`.
6. En Principal y Docente defina el mismo `GRPC_INTERNAL_TOKEN`, largo y aleatorio. En Docente defina además `SGA_PRINCIPAL_URL`, por ejemplo `http://localhost:8080`.
7. No copie claves a `application.properties`, Kotlin, Dockerfiles ni Git.

## Prueba en dispositivo físico

1. Inicie PostgreSQL, Docente y Principal con las variables anteriores.
2. Compile Android desde `app-movil-docente` con `gradlew.bat assembleDebug` e instale el APK.
3. En Android 13+ conceda el permiso de notificaciones cuando la aplicación lo solicite desde la configuración de seguridad existente; no se solicita repetidamente.
4. Inicie sesión con un usuario que tenga rol `REPRESENTANTE`. Confirme en Principal, sin imprimir el token, que existe una fila activa en `dispositivos_representante` para ese usuario.
5. Publique un anuncio real para una asignación del representado. Debe seguir apareciendo en Comunicados y producir el push.
6. Registre una asistencia real `AUSENTE` y luego otra `ATRASO`; `PRESENTE` y `JUSTIFICADO` no generan esos avisos.
7. Cierre explícitamente un período activo mediante el endpoint de cierre. Antes de esa llamada no debe existir push; repetirla no debe duplicarlo.
8. Repita cada caso con la app abierta, en segundo plano y cerrada. En foreground `FirebaseMessagingService` crea la notificación; en background/cerrada FCM entrega el payload y el servicio prepara el destino.
9. Al tocar, la app valida JWT y rol. Si corresponde aplica `BiometricAccessPolicy`; después abre Comunicados, Asistencia o Calificaciones. Una sesión vencida abre Login. `periodId` queda transportado para el futuro selector trimestral, sin añadir esa UI ahora.

## Canales y alcance

Existen canales separados para comunicados, asistencia y calificaciones, con contenido privado en pantalla bloqueada. El handler no ejecuta trabajos largos; Room y WorkManager siguen sincronizando datos.

Quedan fuera Firebase Auth, Firestore, Realtime Database, Functions, Hosting, selector trimestral, promedio anual y asistencia por trimestre. Una prueba física extremo a extremo requiere el `google-services.json`, credenciales Admin válidas, red y un teléfono real; no puede certificarse sólo con tests locales.
