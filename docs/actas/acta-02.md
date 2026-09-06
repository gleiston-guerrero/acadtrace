# Acta de Reunión N.° 02 — Integración de Microservicios, API Gateway y Carga

- **Proyecto:** AcadTrace — Sistema de Gestión Académica y Trazabilidad Distribuida (SGA)
- **Fecha:** 28 de agosto de 2026
- **Hora:** 20:00 - 21:45 (UTC-5)
- **Modalidad:** Virtual (Google Meet)
- **Participantes:**
  - Pedro Leonardo Castro López (Líder de Proyecto / Módulo A: Principal)
  - Keyla Betzabe Bedon Viteri (Módulo C: Docente / App Móvil)
  - Ernesto Gregory Luna Mora (Módulo B: Secretaría / Gateway)
  - Juliana Romina Emanuel Pino (Módulo D: Soporte / Observabilidad)

---

## 1. Orden del Día
1. Evaluación del estado de integración entre microservicios bajo HAProxy y Docker Compose.
2. Ejecución y análisis de las primeras corridas de carga con Locust.
3. Diagnóstico de cuellos de botella en conexiones HikariCP y tiempos de respuesta de base de datos.
4. Verificación de métricas de negocio obligatorias en Prometheus.

---

## 2. Puntos Tratados y Discusión
- **Resultados de Carga:** Juliana presentó los resultados de las pruebas con Locust. En carga nominal (50 usuarios), el sistema mantuvo 0% errores con latencia mediana de 6 ms. En la prueba de estrés (rampa a 200 usuarios), se identificó saturación del pool de conexiones en endpoints transaccionales dependientes de PostgreSQL.
- **Seguridad de Secretos:** Se detectó y corrigió una clave JWT quemada en el generador de carga, migrándola a variables de entorno para cumplimiento de políticas de seguridad.
- **Métricas de Negocio:** Se confirmaron los 4 contadores Prometheus exigidos por la rúbrica (`sga_notas_registradas_total`, `sga_notas_modificadas_total`, `sga_matriculas_confirmadas_total`, `sga_notificaciones_representantes_total`).
- **Observabilidad en Windows/WSL2:** Se documentó la limitación metodológica de cAdvisor en virtualización anidada WSL2 para reporte agregado de recursos.

---

## 3. Acuerdos y Compromisos

| ID | Compromiso / Tarea | Responsable | Fecha Límite |
|---|---|---|---|
| AC-02.1 | Versionar resultados CSV de Locust en `docs/locust/` con su documento de entorno y hashes SHA-256. | Juliana Emanuel | 01/09/2026 |
| AC-02.2 | Optimizar configuración del pool HikariCP y validar degradación elegante bajo saturación. | Ernesto Luna / Pedro Castro | 01/09/2026 |
| AC-02.3 | Verificar sincronización offline y autenticación biométrica en la app móvil Kotlin. | Keyla Bedon | 02/09/2026 |
| AC-02.4 | Consolidar capturas de pantalla de Grafana y diagramas C4 en la documentación. | Juliana Emanuel / Pedro Castro | 02/09/2026 |

---
*Firmado por todos los integrantes del equipo BCEL.*
