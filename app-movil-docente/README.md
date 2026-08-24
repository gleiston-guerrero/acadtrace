# SGA Docente - Aplicación Móvil Nativa (Android Studio)

Aplicación móvil nativa en **Kotlin** para docentes del Sistema de Gestión Académica (SGA) Distribuido. Desarrollada con **Jetpack Compose**, **Material 3**, **Room Database** (persistencia offline), **Retrofit** y **WorkManager** (sincronización en segundo plano).

---

## 📱 Características Principales

1. **Autenticación Segura**:
   - Inicio de sesión mediante `/api/auth/login` con validación estricta de rol `DOCENTE`.
   - Almacenamiento seguro del token JWT mediante `EncryptedSharedPreferences`.
   - Control de sesión y cierre seguro.

2. **Gestión de Asignaciones y Estudiantes**:
   - Visualización restringida únicamente a los cursos, materias, grados, paralelos y estudiantes asignados al docente autenticado.
   - Consulta de horarios semanales organizados de lunes a viernes.

3. **Actividades Académicas y Calificaciones**:
   - CRUD completo de actividades (tareas, lecciones, talleres, exámenes).
   - Control de ponderaciones según la normativa académica (máximo 70% formativa y 30% sumativa).
   - Registro de calificaciones numéricas y conversión automática a escala cualitativa oficial (`A+`, `A-`, `B+`, `B-`, `C+`, `C-`, `D`).
   - Consulta y cálculo de promedios formativos, sumativos, trimestrales y anuales.

4. **Control de Asistencia**:
   - Toma de lista diaria por fecha con un toque para estados: `PRESENTE`, `AUSENTE`, `JUSTIFICADO` y `ATRASO`.
   - Acción rápida "Marcar Todos Presentes".
   - Resumen estadístico acumulado de asistencias e inasistencias por paralelo.

5. **Aula Virtual por Semanas**:
   - Visualización de la agenda académica organizada en semanas (Lunes-Viernes) y trimestres.
   - Resumen de actividades pendientes y comunicados del curso.

6. **Seguimiento y Bitácora Estudiantil**:
   - Registro de observaciones conductuales, académicas, DECE, médicas y familiares con soporte para bandera de seguimiento prioritario.

7. **Comunicados y Materiales de Estudio**:
   - Publicación de anuncios con opción de fijado.
   - Compartición de enlaces, documentos y guías de clase.

8. **Motor Offline-First y Sincronización Automática**:
   - Creación, modificación y registro de actividades, notas, asistencias, comunicados y seguimientos sin conexión a internet.
   - Almacenamiento en Room Database y encolado en `pending_sync`.
   - Sincronización automática en segundo plano con `WorkManager` y `SyncManager` al restablecer la conexión.
   - Reconciliación y actualización de IDs remotos evitando duplicados.

---

## 🚀 Cómo Abrir y Ejecutar el Proyecto en Android Studio

1. **Abrir en Android Studio**:
   - Selecciona **File > Open...**
   - Elige la carpeta: `C:\Users\eduin\Music\Keyla\sga-sistema-distribuido\app-movil-docente`
   - Espera a que Gradle sincronice las dependencias (`Sync Project with Gradle Files`).

2. **Requisitos de Compilación**:
   - **Android Studio**: Hedgehog (2023.1.1) o superior (Iguana, Jellyfish, Koala, Ladybug).
   - **JDK**: Java 17.
   - **Min SDK**: Android 8.0 (API 26).
   - **Target / Compile SDK**: Android 14 (API 34).

3. **Configuración de Conexión al Backend**:
   - En el **Emulador Android**, la app se conecta por defecto a `http://10.0.2.2:8080/api/` (SGA Principal) y `http://10.0.2.2:8081/api/` (Microservicio Docente).
   - Para **Dispositivos Físicos** o servidores personalizados, puedes ingresar a la pantalla de **Configuración de Servidor** (icono ⚙️ en el login o en el panel) y cambiar las IPs a la dirección de tu red local (ej. `http://192.168.1.100:8080/api/`).

---

## 🏛️ Estructura del Código

```
app/src/main/java/ec/edu/uteq/sga/docente/
├── MainActivity.kt                  # Punto de entrada Compose
├── SgaDocenteApp.kt                 # Inyección de dependencias & Application
├── core/
│   ├── AuthInterceptor.kt          # Inyector Bearer Token
│   ├── Constants.kt                # URLs y llaves
│   ├── NetworkConnectivityObserver.kt # Detección de conectividad en tiempo real
│   ├── Resource.kt                 # Estados Success, Error, Loading
│   └── SessionManager.kt           # Token seguro y datos de sesión
├── data/
│   ├── local/                      # Room DB, Entities y DAOs
│   ├── remote/                     # Retrofit, DTOs y DocenteApi
│   ├── repository/                 # Implementaciones de repositorios
│   └── sync/                       # SyncManager & SyncWorker
├── domain/
│   ├── model/                      # Modelos limpios de dominio
│   ├── repository/                 # Interfaces de repositorios
│   └── rules/                      # Reglas oficiales 70/30 y notas cualitativas
└── ui/
    ├── components/                 # Componentes visuales reutilizables
    ├── navigation/                 # NavGraph & Rutas Compose
    ├── screens/                    # Pantallas y ViewModels
    └── theme/                      # Paleta de colores y estilos Material 3
```
