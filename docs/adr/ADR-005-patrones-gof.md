# ADR-005: Aplicación de Patrones de Diseño GoF (Equipo BCEL)

## Estado
Aceptado

## Contexto
La Guía de Entrega E4 exige la justificación y aplicación de al menos 5 patrones de diseño de la banda de los cuatro (GoF) en el código del sistema distribuido del equipo `BCEL` (SGA Escuela).

## Decisión
Se implementaron y documentaron los siguientes 5 patrones GoF en los microservicios:

### 1. Patrón Repository (Estructural / Creacional)
- **Ubicación:** `ec.edu.uteq.sga.infrastructure.repository.*`
- **Propósito:** Mediar entre la capa de dominio/aplicación y la capa de acceso a datos en PostgreSQL, encapsulando las consultas JPA y liberando a los servicios de detalles SQL.

### 2. Patrón Strategy (Comportamiento)
- **Ubicación:** `microservicio-ia.app.main` y `ConfiguracionCalificacionService`
- **Propósito:** Encapsular algoritmos de promedios ponderados según la normativa ministerial (Formativas 70% + Sumativas 30%) y el motor de inferencia pedagógica, permitiendo alternar estrategias de evaluación en tiempo de ejecución.

### 3. Patrón Facade (Estructural)
- **Ubicación:** Balanceador HAProxy (`infra/haproxy/haproxy.cfg`) y controladores fachada
- **Propósito:** Proveer una interfaz perimetral unificada hacia el subsistema de microservicios políglotas (`sga-principal:8080`, `microservicio-docente:8081`, `microservicio-ia:8084`, `sga-principal-grpc:9092`), ocultando la topología interna.

### 4. Patrón Observer (Comportamiento)
- **Ubicación:** `ec.edu.uteq.sga.application.service.AuditoriaService`
- **Propósito:** Desacoplar el registro de eventos de auditoría y notificaciones ante eventos del sistema (login, modificación de calificaciones, matriculaciones).

### 5. Patrón Template Method (Comportamiento)
- **Ubicación:** Servicios de exportación y generación de reportes (`MatriculaService.generarPdfMatricula`, reportes de sábanas)
- **Propósito:** Definir el esqueleto de construcción de documentos oficiales (encabezado ministerial, matriz de calificaciones, cálculo de promedios y pie de firmas), delegando la renderización específica a cada formato.

## Consecuencias
- Cumplimiento estricto de la rúbrica E4 (Dimensión D1, criterio 1.2).
- Alta modularidad y facilidad de extensión para futuras integraciones.
