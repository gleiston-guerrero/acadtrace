# Evidencias — Keyla Bedón

## Proyecto AcadTrace — Entrega 4

**Responsable:** Keyla Bedón  
**Componentes:** Aplicación Móvil para Representantes y Microservicio Docente  
**Proyecto:** AcadTrace  
**Entrega:** Entrega 4  
**Repositorio:** `acadtrace`  
**Rama de desarrollo utilizada:** `devBedon`  
**Integración al proyecto:** mediante Pull Request hacia `main`

---

# 1. Descripción general

Este directorio contiene las evidencias correspondientes al trabajo desarrollado en la **Aplicación Móvil para Representantes** y en el **Microservicio Docente** del proyecto AcadTrace.

El propósito de esta carpeta es permitir la revisión técnica y funcional del trabajo realizado, incluyendo:

- interfaz de usuario;
- navegación;
- autenticación;
- consulta de representados;
- calificaciones;
- asistencia;
- notificaciones Firebase Cloud Messaging;
- seguridad de sesión;
- compilación Android;
- pruebas automatizadas;
- generación del APK;
- integración continua;
- funcionalidades del Microservicio Docente;
- pruebas del backend;
- auditoría y trazabilidad;
- verificadores experimentales;
- pruebas E2E;
- evidencias de CI/CD.

Las evidencias corresponden a ejecuciones, pruebas y funcionalidades reales realizadas sobre el proyecto.

---

# 2. Estructura de evidencias

La carpeta se encuentra organizada de la siguiente manera:

```text
evidencias/
└── Bedon/
    ├── README.md
    │
    ├── app-movil/
    │   ├── 00-interfaz-y-flujos/
    │   ├── 01-compilacion-pruebas/
    │   ├── 02-calificaciones/
    │   ├── 03-asistencia/
    │   ├── 04-notificaciones-fcm/
    │   ├── 05-seguridad-sesion/
    │   ├── 06-ci-cd/
    │   ├── 07-E12-reproducibilidad/
    │   └── 08-E14-artefacto-apk/
    │
    └── microservicio-docente/
        ├── 01-pruebas-backend/
        ├── 02-E2-verificador/
        ├── 03-E3-auditoria/
        ├── 04-E10-e2e-navegador/
        └── 05-ci-cd/
```

Cada carpeta contiene capturas, resultados de pruebas, reportes o artefactos relacionados con una funcionalidad concreta.

---

# 3. Aplicación móvil para Representantes

## 3.1 Descripción

La aplicación móvil fue desarrollada en Android para usuarios con rol:

```text
REPRESENTANTE
```

Su objetivo es permitir que el representante consulte desde su dispositivo móvil la información académica correspondiente a sus estudiantes representados.

Entre las funcionalidades principales se encuentran:

- inicio de sesión;
- validación del rol REPRESENTANTE;
- consulta de estudiantes representados;
- resumen del estudiante;
- consulta de calificaciones;
- filtrado de calificaciones por trimestre;
- consulta de actividades;
- consulta de asistencia;
- filtrado de asistencia por trimestre;
- filtros de asistencia por estado;
- consulta de comunicados;
- recepción de notificaciones FCM;
- navegación desde las notificaciones;
- almacenamiento seguro de sesión;
- recuperación segura ante preferencias cifradas incompatibles;
- generación e instalación del APK.

---

# 4. Arquitectura de comunicación de la aplicación móvil

La aplicación móvil consume los servicios del ecosistema AcadTrace.

El flujo utilizado es:

```text
Aplicación móvil REPRESENTANTE
            ↓
       REST + JWT
            ↓
       SGA Principal
            ↓
Comunicación interna / gRPC
            ↓
   Microservicio Docente
```

Para las notificaciones se utiliza:

```text
Evento académico
      ↓
Backend AcadTrace
      ↓
Firebase Cloud Messaging
      ↓
Dispositivo Android
      ↓
Aplicación Representante
```

La aplicación utiliza autenticación mediante JWT y mantiene la información de sesión protegida localmente.

---

# 5. Evidencias de interfaz y navegación

Las capturas correspondientes a la interfaz principal se almacenan en:

```text
app-movil/00-interfaz-y-flujos/
```

Esta carpeta permite comprobar visualmente el recorrido de la aplicación.

Las principales pantallas documentadas son:

```text
01-login.png
02-home-representante.png
03-mis-representados.png
04-resumen-estudiante.png
05-calificaciones.png
06-detalle-asignatura.png
07-asistencia.png
08-comunicados.png
09-configuracion.png
```

El recorrido funcional principal es:

```text
Login
  ↓
Home Representante
  ↓
Mis representados
  ↓
Seleccionar estudiante
  ↓
Resumen del estudiante
  ├── Calificaciones
  ├── Asistencia
  └── Comunicados
```

---

# 6. Inicio de sesión

La aplicación permite iniciar sesión utilizando las credenciales del usuario.

El proceso general es:

```text
Ingreso de credenciales
        ↓
Petición al backend
        ↓
Validación de usuario
        ↓
Recepción de JWT
        ↓
Validación del rol
        ↓
Acceso a la aplicación
```

La aplicación fue diseñada específicamente para usuarios con rol:

```text
REPRESENTANTE
```

Después de autenticarse correctamente, el usuario puede acceder a la información de sus estudiantes representados.

---

# 7. Gestión de sesión

La aplicación mantiene la sesión utilizando almacenamiento cifrado mediante:

```text
EncryptedSharedPreferences
```

Dentro de la sesión se almacenan datos necesarios para el funcionamiento de la aplicación, entre ellos:

- JWT;
- identificador de usuario;
- username;
- correo;
- roles;
- identificador de persona;
- configuración biométrica;
- configuración de notificaciones.

La información sensible de sesión no se almacena utilizando preferencias de texto plano.

---

# 8. Consulta de estudiantes representados

Después de iniciar sesión, el usuario puede consultar los estudiantes asociados a su cuenta de representante.

La aplicación permite seleccionar uno de los estudiantes y acceder a su información académica.

El flujo utilizado es:

```text
Representante autenticado
        ↓
Mis representados
        ↓
Seleccionar estudiante
        ↓
Resumen académico
```

Desde el resumen se puede acceder a:

- calificaciones;
- asistencia;
- comunicados.

---

# 9. Calificaciones

Las evidencias de este módulo se encuentran en:

```text
app-movil/02-calificaciones/
```

La aplicación permite consultar las calificaciones del estudiante organizadas por trimestre.

Se encuentran disponibles:

```text
T1 — Primer Trimestre
T2 — Segundo Trimestre
T3 — Tercer Trimestre
```

---

# 10. Calificaciones — Primer Trimestre

Durante la prueba del Primer Trimestre se verificó que la aplicación muestra únicamente la información correspondiente al período seleccionado.

Ejemplo observado para la asignatura:

```text
Acompañamiento Integral
```

Valores observados:

```text
Formativo: 7.12
Sumativo: 7.04
Promedio: 7.08
```

También se muestran las actividades correspondientes al mismo trimestre.

Evidencia:

```text
app-movil/02-calificaciones/01-calificaciones-T1.png
```

---

# 11. Calificaciones — Segundo Trimestre

Al seleccionar el Segundo Trimestre, los datos mostrados cambian.

Ejemplo observado:

```text
Formativo: 7.21
Sumativo: 7.25
Promedio: 7.23
```

Las actividades mostradas corresponden al período seleccionado.

Evidencia:

```text
app-movil/02-calificaciones/02-calificaciones-T2.png
```

---

# 12. Calificaciones — Tercer Trimestre

Al seleccionar el Tercer Trimestre también se muestran valores diferentes.

Ejemplo observado:

```text
Formativo: 7.4
Sumativo: 7.99
Promedio: 7.67
```

Evidencia:

```text
app-movil/02-calificaciones/03-calificaciones-T3.png
```

---

# 13. Verificación del filtrado de calificaciones

Las pruebas realizadas permiten comprobar que el selector de trimestre no cambia únicamente el nombre visual del período.

Al cambiar entre:

```text
T1
T2
T3
```

cambian:

- actividades;
- promedio formativo;
- promedio sumativo;
- promedio trimestral;
- información correspondiente al período.

Durante las pruebas no se observaron mezclas de información entre los diferentes trimestres.

También se realizaron cambios repetidos entre T1, T2 y T3 sin producir cierre inesperado de la aplicación.

---

# 14. Manejo de datos nulos en Calificaciones

Durante el desarrollo se detectaron respuestas donde determinados datos relacionados con asignaturas podían llegar como `null`.

Para evitar errores durante la conversión de los datos recibidos, se adaptaron los DTO y los mapeos correspondientes.

El comportamiento implementado permite que:

```text
Dato válido
    ↓
Se muestra normalmente
```

y:

```text
Dato nulo o inexistente
    ↓
Se procesa de forma controlada
    ↓
La aplicación continúa funcionando
```

Esto evita errores como:

```text
NullPointerException
```

durante la visualización de las calificaciones.

---

# 15. Asistencia

Las evidencias se encuentran en:

```text
app-movil/03-asistencia/
```

La pantalla de Asistencia permite visualizar:

- porcentaje general de asistencia;
- total de registros;
- presentes;
- ausentes;
- atrasos;
- justificados.

También permite filtrar por trimestre:

```text
T1
T2
T3
```

y por estado:

```text
Todos
Ausentes
Atrasos
Justificados
```

---

# 16. Asistencia — Primer Trimestre

Durante la prueba de T1 se observaron:

```text
Total de registros: 876

Presentes: 774
Ausentes: 102
Atrasos: 0
Justificados: 0
```

La suma de registros coincide con el total:

```text
774 + 102 = 876
```

Los registros visibles corresponden al:

```text
Primer Trimestre 2026-2027
```

Evidencia:

```text
app-movil/03-asistencia/01-asistencia-T1.png
```

---

# 17. Asistencia — Segundo Trimestre

Durante la prueba de T2 se observaron:

```text
Total de registros: 580

Presentes: 509
Ausentes: 66
Atrasos: 5
Justificados: 0
```

La suma coincide con el total:

```text
509 + 66 + 5 = 580
```

Los registros visibles corresponden al:

```text
Segundo Trimestre 2026-2027
```

Evidencia:

```text
app-movil/03-asistencia/02-asistencia-T2.png
```

---

# 18. Asistencia — Tercer Trimestre

Durante la prueba de T3 se observaron:

```text
Total de registros: 714

Presentes: 633
Ausentes: 76
Atrasos: 5
Justificados: 0
```

La suma coincide con el total:

```text
633 + 76 + 5 = 714
```

Los registros pertenecen al:

```text
Tercer Trimestre 2026-2027
```

Evidencia:

```text
app-movil/03-asistencia/03-asistencia-T3.png
```

---

# 19. Filtros de Asistencia

Además del filtrado por trimestre, se verificaron los siguientes filtros:

```text
Todos
Ausentes
Atrasos
Justificados
```

Al seleccionar:

```text
Atrasos
```

la aplicación muestra únicamente registros cuyo estado corresponde a atraso.

Al seleccionar:

```text
Ausentes
```

se muestran las ausencias correspondientes.

Esto permite comprobar que el filtrado se realiza sobre los registros reales y no únicamente sobre la interfaz.

Las evidencias adicionales pueden almacenarse como:

```text
04-filtro-atrasos.png
05-filtro-ausentes.png
06-filtro-justificados.png
```

---

# 20. Firebase Cloud Messaging

Las evidencias de las notificaciones se almacenan en:

```text
app-movil/04-notificaciones-fcm/
```

La aplicación utiliza Firebase Cloud Messaging para recibir eventos académicos.

Se realizaron pruebas reales sobre dispositivo físico.

Los eventos comprobados fueron:

- ATRASO;
- AUSENTE;
- COMUNICADO.

---

# 21. FCM — ATRASO

Se realizó el registro de un atraso desde el sistema académico.

El proceso comprobado fue:

```text
Registro de ATRASO
        ↓
Persistencia académica
        ↓
Generación del evento
        ↓
Firebase Cloud Messaging
        ↓
Notificación Android
        ↓
Usuario toca la notificación
        ↓
Asistencia del estudiante
```

Se verificó:

- recepción real de la notificación;
- estudiante correcto;
- información contextual;
- fecha;
- navegación hacia Asistencia;
- historial completo visible.

Las evidencias pueden almacenarse como:

```text
01-fcm-atraso-notificacion.png
02-fcm-atraso-navegacion.png
```

---

# 22. Corrección de navegación de FCM

Durante una prueba anterior se detectó que al tocar una notificación de asistencia la aplicación podía abrir una ruta incorrecta.

El resultado observado era:

```text
Estudiante no encontrado
```

La causa estaba relacionada con la utilización de una ruta dinámica como destino inicial de navegación.

La navegación fue modificada para:

```text
Crear primero el NavHost
        ↓
Utilizar un destino inicial estable
        ↓
Procesar la notificación
        ↓
Navegar posteriormente a la ruta correcta
```

Después de la corrección se realizó nuevamente la prueba física.

El resultado fue:

```text
Notificación ATRASO
        ↓
Tap
        ↓
Asistencia
        ↓
Estudiante correcto
        ↓
Historial completo
```

---

# 23. FCM — AUSENTE

También se probó un evento real de ausencia.

Se comprobó:

- persistencia del registro;
- recepción del push;
- nombre del estudiante;
- fecha;
- navegación;
- apertura de Asistencia;
- visualización del registro correspondiente.

Las evidencias pueden almacenarse como:

```text
03-fcm-ausencia-notificacion.png
04-fcm-ausencia-navegacion.png
```

---

# 24. FCM — COMUNICADO

Se realizó una prueba mediante un comunicado real.

El flujo comprobado fue:

```text
Comunicado
    ↓
Backend
    ↓
FCM
    ↓
Notificación Android
    ↓
Tap
    ↓
Pantalla Comunicados
```

La notificación fue recibida correctamente y la navegación abrió la sección correspondiente.

Evidencias:

```text
05-fcm-comunicado-notificacion.png
06-fcm-comunicado-navegacion.png
```

---

# 25. Información estructurada de las notificaciones

Para evitar depender del texto mostrado al usuario, las notificaciones utilizan datos estructurados.

Entre los campos utilizados se encuentran:

```text
type
studentId
periodId
studentName
attendanceId
date
```

El identificador del estudiante se utiliza para dirigir al usuario hacia el representado correspondiente.

El texto visible de la notificación no se utiliza como identificador lógico.

---

# 26. Seguridad de sesión

Las evidencias de seguridad se almacenan en:

```text
app-movil/05-seguridad-sesion/
```

La sesión local está protegida mediante:

```text
EncryptedSharedPreferences
```

y una clave administrada mediante Android Keystore.

---

# 27. Problema detectado durante reinstalación

Durante pruebas de reinstalación se detectó anteriormente una excepción:

```text
javax.crypto.AEADBadTagException
```

El problema se producía cuando Android restauraba preferencias cifradas pero la clave criptográfica utilizada originalmente ya no se encontraba disponible en el Keystore.

El comportamiento anterior podía provocar:

```text
Inicio de la aplicación
        ↓
EncryptedSharedPreferences
        ↓
No puede descifrar datos restaurados
        ↓
AEADBadTagException
        ↓
Cierre de la aplicación
```

---

# 28. Corrección de restauración de sesión cifrada

Se implementaron dos mecanismos de protección.

## 28.1 Reglas de backup

Se añadieron:

```text
app/src/main/res/xml/backup_rules.xml
app/src/main/res/xml/data_extraction_rules.xml
```

Estos archivos excluyen:

```text
sga_representante_secure_prefs.xml
```

de los mecanismos de backup y restauración.

El `AndroidManifest.xml` utiliza:

```xml
android:allowBackup="true"
android:dataExtractionRules="@xml/data_extraction_rules"
android:fullBackupContent="@xml/backup_rules"
```

---

## 28.2 Recuperación automática

`SessionManager` también posee un mecanismo de recuperación.

El comportamiento es:

```text
Crear EncryptedSharedPreferences
          ↓
Forzar lectura
          ↓
Datos válidos
          ↓
Continuar normalmente
```

Si los datos no pueden ser descifrados:

```text
Error de lectura
      ↓
Eliminar únicamente las preferencias locales inválidas
      ↓
Crear nuevas preferencias cifradas
      ↓
Aplicación continúa
      ↓
Usuario puede iniciar sesión nuevamente
```

Este proceso no elimina:

- estudiantes;
- asistencia;
- calificaciones;
- comunicados;
- información académica del backend.

---

# 29. Prueba de reinstalación

La corrección fue probada sobre dispositivo físico.

Se realizó:

```text
Instalación de APK
        ↓
Aplicación abre correctamente
        ↓
Desinstalación
        ↓
Reinstalación
        ↓
SIN ejecutar pm clear
        ↓
Aplicación abre correctamente
```

Después de la reinstalación no se volvió a presentar:

```text
AEADBadTagException
```

---

# 30. Compilación y pruebas Android

Las evidencias se almacenan en:

```text
app-movil/01-compilacion-pruebas/
```

Se ejecutaron pruebas mediante:

```powershell
.\gradlew.bat testDebugUnitTest
```

Resultado:

```text
BUILD SUCCESSFUL
```

También se realizó:

```powershell
.\gradlew.bat assembleDebug
```

Resultado:

```text
BUILD SUCCESSFUL
```

Esto permitió generar correctamente el APK debug.

---

# 31. Instalación mediante ADB

El APK generado se instaló sobre un dispositivo Android físico.

ADB detectó correctamente el dispositivo:

```text
List of devices attached
XXXXXXXXXXXX    device
```

Posteriormente se utilizó:

```powershell
adb -d install -r app-debug.apk
```

Resultado:

```text
Performing Streamed Install
Success
```

También se realizaron pruebas de desinstalación y reinstalación.

---

# 32. Evidencias de compilación

Se recomienda almacenar:

```text
app-movil/01-compilacion-pruebas/
```

con los siguientes archivos:

```text
01-testDebugUnitTest-success.png
02-assembleDebug-success.png
03-adb-device.png
04-instalacion-apk-success.png
05-reinstalacion-success.png
```

---

# 33. Integración continua de la aplicación móvil

Las evidencias del pipeline se almacenan en:

```text
app-movil/06-ci-cd/
```

La aplicación se encuentra integrada al workflow del repositorio.

Durante las ejecuciones observadas se ejecutaron satisfactoriamente jobs relacionados con:

```text
CI Aplicación Móvil Representante
Pruebas Unitarias App Móvil
Compilación de Paquete APK Android
```

También se verificó una ejecución general del pipeline con estado:

```text
Success
```

---

# 34. Integración mediante Pull Request

Los cambios se desarrollaron en la rama:

```text
devBedon
```

y posteriormente fueron enviados mediante Pull Request hacia:

```text
main
```

Antes de integrar se ejecutaron los checks automáticos definidos en GitHub Actions.

Se verificó una ejecución con los checks correspondientes en estado exitoso.

Las capturas del Pull Request y del pipeline se almacenan en:

```text
app-movil/06-ci-cd/
```

---

# 35. Validación después de despliegue

Después de la integración se verificó que la infraestructura continuara respondiendo.

Los servicios principales utilizados por la aplicación son:

```text
SGA Principal
Puerto 8080
```

y:

```text
Microservicio Docente
Puerto 8081
```

Después del despliegue se comprobó nuevamente la aplicación móvil y las notificaciones.

---

# 36. Resumen funcional de la aplicación móvil

| Funcionalidad | Resultado |
|---|---|
| Inicio de sesión | ✅ Verificado |
| Rol REPRESENTANTE | ✅ Verificado |
| Consulta de representados | ✅ Verificado |
| Resumen del estudiante | ✅ Verificado |
| Calificaciones T1 | ✅ Verificado |
| Calificaciones T2 | ✅ Verificado |
| Calificaciones T3 | ✅ Verificado |
| Filtrado real por trimestre | ✅ Verificado |
| Detalle de actividades | ✅ Verificado |
| Manejo de datos nulos | ✅ Verificado |
| Asistencia T1 | ✅ Verificado |
| Asistencia T2 | ✅ Verificado |
| Asistencia T3 | ✅ Verificado |
| Totales por trimestre | ✅ Verificado |
| Filtro Ausentes | ✅ Verificado |
| Filtro Atrasos | ✅ Verificado |
| Filtro Justificados | ✅ Verificado |
| FCM ATRASO | ✅ Verificado |
| FCM AUSENTE | ✅ Verificado |
| FCM COMUNICADO | ✅ Verificado |
| Navegación desde FCM | ✅ Verificado |
| Sesión cifrada | ✅ Verificado |
| Reinstalación sin crash | ✅ Verificado |
| testDebugUnitTest | ✅ Exitoso |
| assembleDebug | ✅ Exitoso |
| Instalación APK mediante ADB | ✅ Exitosa |
| CI aplicación móvil | ✅ Exitoso |

---

# 37. Evidencias de reproducibilidad Android

La documentación y los artefactos relacionados con instalación y compilación reproducible se organizan en:

```text
app-movil/07-E12-reproducibilidad/
```

Esta carpeta concentra evidencia relacionada con:

- configuración Gradle;
- wrapper Gradle;
- versiones de dependencias;
- bloqueo de dependencias;
- ejecución de pruebas;
- generación de cobertura;
- generación del APK;
- compilación desde entorno limpio;
- instrucciones de construcción.

Esto permite separar las evidencias funcionales de las evidencias relacionadas con reproducibilidad.

---

# 38. Evidencias del artefacto Android

Los archivos correspondientes al APK y sus verificaciones se almacenan en:

```text
app-movil/08-E14-artefacto-apk/
```

Esta carpeta se utiliza para incluir:

```text
app-debug.apk
app-debug.sha256.txt
reporte-pruebas/
reporte-cobertura/
captura-ci-apk.png
```

De esta manera el APK puede relacionarse con:

- una compilación concreta;
- un commit concreto;
- un checksum SHA-256;
- los resultados de pruebas asociados.

---

# 39. Microservicio Docente

Las evidencias correspondientes al backend Docente se almacenan en:

```text
microservicio-docente/
```

El Microservicio Docente participa en procesos relacionados con:

- períodos académicos;
- actividades;
- calificaciones;
- asistencia;
- integración con SGA Principal;
- comunicación interna;
- eventos académicos;
- generación de notificaciones;
- auditoría;
- trazabilidad;
- experimentos;
- métricas;
- pruebas automatizadas.

---

# 40. Pruebas del Microservicio Docente

Las evidencias generales de pruebas se almacenan en:

```text
microservicio-docente/01-pruebas-backend/
```

Esta carpeta puede contener:

```text
01-tests-docente.png
02-tests-notificaciones.png
03-tests-api.png
04-metricas.png
05-integracion.png
```

Se busca demostrar que las funcionalidades principales del backend poseen validación automatizada y que los cambios realizados pueden ser comprobados mediante pruebas.

---

# 41. Integración de Docente con notificaciones

El Microservicio Docente genera eventos académicos relacionados con asistencia.

Entre los eventos utilizados por la aplicación móvil se encuentran:

```text
ATRASO
AUSENTE
```

Los eventos contienen información académica obtenida desde el sistema, como:

```text
studentId
studentName
attendanceId
date
```

Esta información posteriormente es utilizada para construir el payload enviado al dispositivo Android.

---

# 42. Evidencias del verificador

Las evidencias relacionadas con la validación criptográfica y los experimentos se organizan en:

```text
microservicio-docente/02-E2-verificador/
```

Esta carpeta permite documentar pruebas relacionadas con:

- generación de eventos;
- normalización;
- serialización canónica;
- hashes;
- cadena de eventos;
- `hash_anterior`;
- reloj de Lamport;
- detección de manipulación;
- validación de cadenas reales;
- rechazo de cadenas alteradas.

Ejemplos de evidencia:

```text
01-cadena-real-valida.png
02-payload-manipulado-rechazado.png
03-hash-anterior-invalido.png
04-lamport-no-monotonico.png
05-estado-academico-manipulado.png
06-prueba-integracion.png
```

---

# 43. Evidencias de auditoría

Las evidencias relacionadas con trazabilidad y auditoría se organizan en:

```text
microservicio-docente/03-E3-auditoria/
```

El objetivo de esta sección es permitir observar el recorrido de una operación académica hasta su mecanismo de auditoría.

La evidencia puede demostrar el flujo:

```text
Operación en Docente
        ↓
Evento de auditoría
        ↓
Mecanismo central del proyecto
        ↓
Bitácora
        ↓
Verificación
```

También pueden almacenarse evidencias de:

- documentación;
- eventos generados;
- registros en bitácora;
- pruebas de verificación;
- trazabilidad entre servicios.

---

# 44. Evidencias E2E del frontend Docente

Las pruebas de navegador se organizan en:

```text
microservicio-docente/04-E10-e2e-navegador/
```

Los recorridos documentados pueden incluir:

```text
Inicio de sesión
Dashboard Docente
Consulta de cursos
Registro de calificaciones
Consulta de asistencia
Cierre de sesión
Acceso sin autenticación
```

Las evidencias pueden incluir:

- capturas automáticas;
- reportes HTML;
- vídeos;
- traces;
- resultados de CI.

Ejemplos:

```text
01-login-docente.png
02-dashboard-docente.png
03-consulta-cursos.png
04-registro-calificacion.png
05-consulta-asistencia.png
06-logout.png
07-acceso-no-autorizado.png
08-playwright-report.png
09-ci-e2e.png
```

---

# 45. CI/CD del Microservicio Docente

Las evidencias relacionadas con integración continua se almacenan en:

```text
microservicio-docente/05-ci-cd/
```

Pueden incluir:

- ejecución de tests;
- construcción;
- resultados de CI;
- integración;
- despliegue;
- logs relevantes;
- checks del Pull Request.

Esto permite relacionar los cambios del Microservicio Docente con una ejecución automatizada del repositorio.

---

# 46. Criterio utilizado para seleccionar evidencias

Las evidencias incluidas en este directorio deben demostrar comportamiento real del sistema.

Se priorizan:

1. capturas de ejecución real;
2. pruebas sobre dispositivo físico;
3. resultados de pruebas automatizadas;
4. resultados de GitHub Actions;
5. reportes generados por herramientas;
6. artefactos producidos por el pipeline;
7. checksums;
8. pruebas de integración.

Una captura de código por sí sola no reemplaza una prueba funcional.

Cuando se incluye código como evidencia, se utiliza como complemento para explicar el comportamiento demostrado mediante las pruebas.

---

# 47. Convención de nombres

Para mantener las evidencias ordenadas se utiliza la siguiente convención:

```text
NN-descripcion-evidencia.extension
```

Ejemplos:

```text
01-login.png
02-home-representante.png
01-calificaciones-T1.png
02-calificaciones-T2.png
03-calificaciones-T3.png
01-asistencia-T1.png
02-asistencia-T2.png
03-asistencia-T3.png
01-testDebugUnitTest-success.png
02-assembleDebug-success.png
```

Esto permite revisar las pruebas siguiendo el orden natural del flujo.

---

# 48. Relación entre capturas y comportamiento

Las capturas deben mostrar claramente el resultado que se desea demostrar.

Por ejemplo:

## Calificaciones

No basta con mostrar tres botones.

Las capturas de T1, T2 y T3 muestran valores diferentes y actividades correspondientes a cada período.

## Asistencia

Las capturas muestran diferentes:

- totales;
- presentes;
- ausentes;
- atrasos;
- registros.

## FCM

Se busca tener:

```text
notificación
+
pantalla abierta después de tocarla
```

## Compilación

La captura debe mostrar:

```text
BUILD SUCCESSFUL
```

## APK

La captura debe mostrar:

```text
Success
```

después de la instalación mediante ADB.

---

# 49. Resultado general de la aplicación móvil

Las pruebas realizadas permiten comprobar el siguiente recorrido:

```text
Instalar APK
     ↓
Abrir aplicación
     ↓
Iniciar sesión
     ↓
Consultar representados
     ↓
Seleccionar estudiante
     ↓
Consultar calificaciones T1/T2/T3
     ↓
Consultar asistencia T1/T2/T3
     ↓
Filtrar estados de asistencia
     ↓
Consultar comunicados
     ↓
Recibir FCM
     ↓
Navegar desde FCM
```

También se comprobó:

```text
Desinstalar
     ↓
Reinstalar
     ↓
Abrir nuevamente
     ↓
Sin crash criptográfico
```

---

# 50. Conclusión

Las evidencias contenidas en este directorio documentan el trabajo desarrollado sobre la **Aplicación Móvil para Representantes** y el **Microservicio Docente**.

En la aplicación móvil se encuentran documentados:

- interfaz;
- autenticación;
- navegación;
- representados;
- calificaciones por trimestre;
- asistencia por trimestre;
- filtros de asistencia;
- Firebase Cloud Messaging;
- navegación desde notificaciones;
- seguridad de sesión;
- reinstalación;
- pruebas unitarias;
- compilación;
- instalación del APK;
- integración continua.

Las evidencias del Microservicio Docente se organizan de forma independiente para facilitar la revisión de:

- pruebas backend;
- eventos académicos;
- verificación;
- auditoría;
- trazabilidad;
- pruebas de navegador;
- CI/CD.

La estructura utilizada busca que cada funcionalidad pueda relacionarse directamente con una evidencia concreta y que la revisión pueda realizarse sin depender únicamente de una explicación verbal.

---

**Responsable:** Keyla Bedón  
**Proyecto:** AcadTrace  
**Componentes:** Aplicación Móvil Representante y Microservicio Docente  
**Entrega:** Entrega 4