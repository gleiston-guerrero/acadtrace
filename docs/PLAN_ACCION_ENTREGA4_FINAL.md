# 📋 PLAN DE ACCIÓN INTEGRAL — CORRECCIONES PFC ENTREGA 4 (BCEL)
**Fecha límite:** Viernes 4 de septiembre de 2026, 23:55  
**Objetivo:** 10/10 en Evaluación Final y Criterios de Piso

---

## 🟢 BLOQUE 1: MICROSERVICIO DOCENTE (Django + Python)
**Ubicación:** `microservicio-docente/`

### Tareas obligatorias a realizar:
1. **Ejecutar y generar el reporte de cobertura real:**
   - Correr `pytest --cov=docentes --cov-report=html:docs/cobertura/docente/`
   - Asegurar que el reporte HTML quede generado y versionado en `docs/cobertura/docente/index.html`.
2. **Fijar el umbral de cobertura en configuración:**
   - Abrir `.coveragerc` o `pytest.ini` y configurar `fail_under = 70` de forma explícita.
3. **Verificar endpoints y pruebas unitarias de calificaciones:**
   - Asegurar que existan pruebas automatizadas para: registro de calificaciones, consulta por paralelo y cálculo de promedios.
4. **Validar middleware de observabilidad estructurada:**
   - Verificar que el middleware emita logs JSON con: `trace_id`, `method`, `path`, `status_code`, `latency_ms` y `timestamp`.
   - Verificar que el endpoint `/metrics` responda métricas Prometheus en formato estándar.
5. **Documentar el layout modular:**
   - Asegurar que los puertos de entrada REST y clientes de salida hacia `sga-principal` estén desacoplados.

---

## 🟣 BLOQUE 2: MICROSERVICIO SECRETARÍA (Spring Boot + Java)
**Ubicación:** `microservicio-secretaria/`

### Tareas obligatorias a realizar:
1. **Escribir pruebas unitarias de capa de aplicación:**
   - Crear pruebas JUnit 5 + Mockito para `MatriculaService`, `EstudianteService` y `ReporteService`.
2. **Generar y versionar el reporte JaCoCo real:**
   - Ejecutar `mvn clean test jacoco:report`.
   - Copiar el reporte resultante de `target/site/jacoco/` hacia `docs/cobertura/secretaria/`.
3. **Sincronizar porcentaje de cobertura con la documentación:**
   - Anotar el porcentaje real exacto que arrojó JaCoCo para reportar la cifra verídica en la tabla ISO 25010.
4. **Verificar propagación de trazas gRPC:**
   - Comprobar que `TraceContext` y `TraceIdFilter` inyecten el `trace_id` en las llamadas gRPC hacia `sga-principal`.
5. **Probar empaquetado y arranque limpio:**
   - Ejecutar `mvn package -DskipTests=false` y verificar que el JAR construya con todas las pruebas pasando en verde.

---

## 🔵 BLOQUE 3: SGA PRINCIPAL & BACKEND CENTRAL (Spring Boot + Java)
**Ubicación:** `sga-principal/`

### Tareas obligatorias a realizar:
1. **Implementar la conmutación real de la variable `AUDIT` (Piso P5):**
   - En `AuditoriaService.java`, leer la variable `@Value("${AUDIT:m2}")`.
   - Programar la lógica exacta para los 4 modos:
     * `m0`: Escritura de nota directa sin bitácora.
     * `m1`: Inserción en tabla relacional `sga_principal.auditoria` sin hashes.
     * `m2`: Inserción con hash SHA-256 del registro previo (`hash_anterior`) y reloj lógico de Lamport (`LamportClock`).
     * `m3`: `m2` + vector de versiones para detección de ediciones concurrentes.
2. **Aumentar la cobertura JaCoCo real del núcleo:**
   - Escribir pruebas unitarias para `CalificacionServiceTest`, `AuditoriaServiceTest`, `EstudianteServiceTest` y `LamportClockTest`.
3. **Generar reporte JaCoCo actualizado:**
   - Ejecutar `mvn clean test jacoco:report` y actualizar `docs/cobertura/sga-principal/index.html`.
4. **Higiene estricta de código y seguridad:**
   - Verificar que no exista ningún `System.out.println` imprimiendo contraseñas o hashes en `SgaPrincipalApplication.java`.
   - Verificar que `application.properties` no contenga contraseñas en texto plano.
5. **Verificar endpoints consumidos por el Frontend:**
   - Asegurar que `/api/auth/login`, `/api/calificaciones`, `/api/estudiantes` y `/api/auditoria` respondan con código 200 y cabeceras JWT correctas.

---

## 🔬 BLOQUE 4: BANCO EXPERIMENTAL Y EVALUACIÓN CUANTITATIVA
**Ubicación:** `experimentos/`

### Tareas obligatorias a realizar:
1. **Reemplazar la simulación por medición real sobre la API:**
   - Modificar `run_experimentos.py` para que use `requests` y envíe peticiones HTTP reales a `http://localhost:8080/api/calificaciones` (o AWS).
   - Medir el tiempo de respuesta con `time.perf_counter()` para $M_0, M_1, M_2, M_3$ con varianza real del sistema.
2. **Implementar detección real de manipulación T1 (Cambio directo en BD):**
   - En `verificador_cadena.py`, programar la función que compara el estado de la tabla de calificaciones con el historial de la bitácora encadenada para detectar inconsistencias.
3. **Implementar el Experimento 1 (24 condiciones factoriales):**
   - Ejecutar combinaciones de usuarios concurrentes (1, 5, 10, 14) $\times$ mecanismos ($M_0, M_1, M_2, M_3$) con 10 repeticiones.
4. **Implementar el Experimento 3 (Reconciliación Offline M2 vs M3):**
   - Simular 2 ediciones offline simultáneas; demostrar que $M_2$ detecta desorden causal pero no reconcilia, mientras que $M_3$ reconcilia deterministamente con relojes vectoriales.
5. **Generar los archivos de resultados reales:**
   - Guardar los CSVs reales en:
     * `experimentos/resultados/deteccion.csv`
     * `experimentos/resultados/manipulaciones.csv`
     * `experimentos/resultados/iso25010.csv`
     * `experimentos/resultados/boxplot_latencia.png`

---

## 📱 BLOQUE 5: APLICACIÓN MÓVIL ANDROID (Kotlin + Jetpack Compose)
**Ubicación:** `app-movil-docente/` (Refactorizada a Representante)

### Tareas obligatorias a realizar:
1. **Generar reporte de cobertura real de la app móvil:**
   - Ejecutar `./gradlew testDebugUnitTest jacocoTestReport` (o `koverHtmlReport`).
   - Versionar el reporte HTML en `docs/cobertura/movil/index.html`.
2. **Verificar las 2 capacidades nativas del dispositivo:**
   - Comprobar la autenticación biométrica (`BiometricUnlockGate`) al iniciar sesión.
   - Comprobar el servicio de notificaciones institucionales.
3. **Validar la sincronización offline:**
   - Verificar la persistencia local en Room Database y el encolado de transacciones con `WorkManager` (`SyncWorker`).
4. **Verificar compilación del APK en CI/CD:**
   - Comprobar que `./gradlew assembleDebug` genere el archivo `app-debug.apk` sin errores de memoria.
5. **Preparar la demo para la defensa oral:**
   - Tener listo el emulador/dispositivo para demostrar: inicio con biometría, consulta de notas en modo avión y sincronización al reconectar.

---

## 📄 BLOQUE 6: DOCUMENTO MAESTRO LATEX, BIBLIOGRAFÍA Y METADATOS
**Ubicación:** `Informe-E4_BCEL/` y raíz del repositorio

### Tareas obligatorias a realizar:
1. **Limpieza y validación de Bibliografía (`referencias.bib`):**
   - Reemplazar la cita `alharbi2025microservices` por una referencia real de IEEE/ACM con DOI resoluble.
   - Eliminar los 8 DOIs no citados que dan error 404.
   - Asegurar que toda referencia en el `.bib` esté citada en el texto y su DOI abra en `https://doi.org/...`.
2. **Redactar la Declaración de Inteligencia Artificial (Piso P8):**
   - Agregar la subsección formal en el documento `.tex` y en el `README.md` declarando el uso asistido de IA con supervisión humana.
3. **Redactar la Tabla de Revisión Cruzada e Issues (Criterio C8):**
   - Crear la tabla con los issues recibidos de otros equipos, observaciones y commits de solución.
4. **Incluir la Tabla de Trazabilidad de Temas (Criterio C5):**
   - Agregar la tabla que cruza los temas de la asignatura con las rutas de código del repositorio.
5. **Sincerar las métricas de la Tabla ISO 25010:**
   - Ajustar las cifras de latencia y cobertura en el PDF para que coincidan con los CSVs reales y reportes JaCoCo.
6. **Eliminar los 25 desbordes de caja (`Overfull \hbox`):**
   - Ajustar tablas con `tabularx` y guiones de separación en nombres de clases largas.
7. **Crear el archivo `CITATION.cff`:**
   - Añadir `CITATION.cff` en la raíz del repositorio con los metadatos oficiales del proyecto.
