# ADR-005: Aplicación de Patrones de Diseño GoF (Equipo BCEL)

## Estado
Aceptado

## Contexto
La Guía de Entrega E4 y la rúbrica de evaluación (Dimensión D1, criterio 1.2) exigen la justificación, diseño y aplicación de al menos 5 patrones de diseño de la banda de los cuatro (GoF) en el código del sistema distribuido del equipo `BCEL` (AcadTrace / SGA Escuela). Los patrones asignados en la Tabla 1 de la guía corresponden al camino crítico del dominio académico y deben estar respaldados por clases, interfaces y jerarquías reales en el código.

## Decisión
Se implementaron y formalizaron en el código fuente los siguientes 5 patrones de diseño GoF:

### 1. Patrón Strategy (Comportamiento)
- **Propósito:** Encapsular algoritmos intercambiables de cálculo de promedios académicos (ponderación ministerial formativa 70% + sumativa 30% vs. aritmético simple) y alternar las estrategias de auditoría criptográfica en tiempo de ejecución según la variable `AUDIT` (`m0`, `m1`, `m2`, `m3`).
- **Participantes en Código:**
  - *Interfaz Estrategia (Java):* `ec.edu.uteq.sga.domain.strategy.CalculoPromedioStrategy`
  - *Estrategias Concretas (Java):* `ec.edu.uteq.sga.domain.strategy.PromedioPonderado7030Strategy` y `ec.edu.uteq.sga.domain.strategy.PromedioAritmeticoSimpleStrategy`
  - *Estrategias de Auditoría (Python):* `docentes.auditoria.strategies.NoAuditStrategy` (m0), `FlatAuditStrategy` (m1), `HashChainAuditStrategy` (m2) y `VectorClockAuditStrategy` (m3).

### 2. Patrón Template Method (Comportamiento)
- **Propósito:** Definir el esqueleto invariable del algoritmo de emisión de reportes y actas académicas por período (validación de matrícula, encabezado ministerial oficial, matriz tabular de notas, cómputo ponderado y bloque de firmas de auditoría), delegando los pasos de renderizado específico a las subclases.
- **Participantes en Código:**
  - *Clase Abstracta Base (Java):* `ec.edu.uteq.sga.application.report.GeneradorReporteAcademicoTemplate`
  - *Subclase Concreta (Java):* `ec.edu.uteq.sga.application.report.ReporteNotasPeriodoPDF`

### 3. Patrón Observer (Comportamiento)
- **Propósito:** Desacoplar la publicación de eventos de dominio (registro/modificación de calificaciones y confirmación de matrículas) de las acciones subsecuentes de notificación a representantes y registro en la bitácora inmutable.
- **Participantes en Código:**
  - *Evento de Dominio:* `ec.edu.uteq.sga.application.event.NotaPublicadaEvent`
  - *Escucha / Observer:* `ec.edu.uteq.sga.application.event.NotificacionRepresentanteListener`
  - *Publicador:* `org.springframework.context.ApplicationEventPublisher` inyectado en `CalificacionService`.

### 4. Patrón Facade (Estructural)
- **Propósito:** Proveer una interfaz unificada de alto nivel para el portal web y clientes externos, coordinando llamadas atómicas entre múltiples servicios del dominio (estudiantes, asignaciones, notas y autorizaciones) sin exponer el acoplamiento interno.
- **Participantes en Código:**
  - *Clase Fachada (Java):* `ec.edu.uteq.sga.application.facade.PortalAcademicoFacade`

### 5. Patrón Repository (Estructural / Creacional)
- **Propósito:** Mediar entre la capa de dominio/aplicación y la capa de infraestructura de persistencia sobre el clúster de datos, aislando las entidades de las consultas SQL/JPA y permitiendo la sustitución mediante dobles de prueba.
- **Participantes en Código:**
  - *Interfaces de Repositorio:* `ec.edu.uteq.sga.infrastructure.repository.EstudianteRepository`, `MatriculaRepository`, `CalificacionRepository`, `AuditoriaRepository`.

## Consecuencias
- Cumplimiento estricto del criterio 1.2 (Nivel 4) con clases e interfaces reales verificables en el repositorio.
- Alto desacoplamiento y extensibilidad del núcleo académico ante nuevas políticas ministeriales o esquemas de persistencia.
