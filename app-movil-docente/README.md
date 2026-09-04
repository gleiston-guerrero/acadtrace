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
