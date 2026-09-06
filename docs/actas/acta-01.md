# Acta de Reunión N.° 01 — Planificación y Arquitectura de la Entrega 4

- **Proyecto:** AcadTrace — Sistema de Gestión Académica y Trazabilidad Distribuida (SGA)
- **Fecha:** 18 de agosto de 2026
- **Hora:** 19:00 - 20:30 (UTC-5)
- **Modalidad:** Virtual (Google Meet)
- **Participantes:**
  - Pedro Leonardo Castro López (Líder de Proyecto / Módulo A: Principal)
  - Keyla Betzabe Bedon Viteri (Módulo C: Docente / App Móvil)
  - Ernesto Gregory Luna Mora (Módulo B: Secretaría / Gateway)
  - Juliana Romina Emanuel Pino (Módulo D: Soporte / Observabilidad)

---

## 1. Orden del Día
1. Revisión de los requerimientos y rúbrica oficial de la Entrega 4 (E4).
2. Definición de la arquitectura de observabilidad integral (Prometheus, Grafana, cAdvisor).
3. Planificación de pruebas de rendimiento y carga con Locust (escenarios nominal y estrés).
4. Estandarización de compuertas de calidad CI/CD (JaCoCo >= 70%) y contratos gRPC.

---

## 2. Puntos Tratados y Discusión
- **Observabilidad:** Juliana expuso el diseño del tablero central de Grafana (`pfc-dashboard.json`) estructurado en 6 vistas operativas (RPS, Latencias P50/P95/P99, Errores 4xx/5xx, Consenso Raft/HikariCP, CPU/RAM de contenedores y Latencia Móvil E2E).
- **Pruebas de Carga:** Se acordó instrumentar dos perfiles de prueba en Locust: Escenario 1 de carga nominal (50 usuarios por 5 minutos) y Escenario 2 de estrés con rampa progresiva (0 a 200 usuarios por 10 minutos).
- **Integración de Microservicios:** Pedro y Ernesto coordinaron la exposición de puertos gRPC internos (`:9091` a `:9094`) y la terminación perimetral en HAProxy (`:80`).
- **Control de Calidad:** Se estableció como política estricta que ningún Pull Request será fusionado sin superar las pruebas unitarias automatizadas con cobertura >= 70% en JaCoCo.

---

## 3. Acuerdos y Compromisos

| ID | Compromiso / Tarea | Responsable | Fecha Límite |
|---|---|---|---|
| AC-01.1 | Implementar suite de observabilidad Prometheus/Grafana y servicio `postgres-exporter`. | Juliana Emanuel | 25/08/2026 |
| AC-01.2 | Configurar pipeline CI/CD en GitHub Actions con los 7 jobs encadenados y compuertas JaCoCo. | Pedro Castro | 26/08/2026 |
| AC-01.3 | Finalizar módulo de auditoría con Relojes Vectoriales y persistencia offline en Room SQLite. | Keyla Bedon | 27/08/2026 |
| AC-01.4 | Integrar HAProxy como API Gateway perimetral y asegurar propagación de `X-Trace-Id`. | Ernesto Luna | 28/08/2026 |

---
*Firmado por todos los integrantes del equipo BCEL.*
