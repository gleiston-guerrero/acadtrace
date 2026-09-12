# Matriz de Trazabilidad: Temas de la Asignatura vs. Evidencias en el Repositorio (Criterio C5)

**Proyecto:** AcadTrace — Gestión Académica con Auditoría Criptográfica  
**Asignatura:** Aplicaciones Distribuidas (ISR-701) — UTEQ  
**Equipo:** BCEL  

Esta matriz formal vincula los **14 temas obligatorios del curso** directamente con sus archivos fuente, pruebas y decisiones de arquitectura en el repositorio:

| # | Tema de la Asignatura | Implementación en AcadTrace | Archivos de Evidencia en el Repositorio | Justificación y Validación Técnica |
|---|---|---|---|---|
| **1** | **Microservicios y Descomposición** | 4 microservicios desacoplados (Principal, Docente, Secretaría, Soporte). | `docker-compose.yml`<br>`sga-principal/`<br>`microservicio-docente/`<br>`microservicio-secretaria/`<br>`microservicio-soporte/` | Aislamiento de dominios, despliegue independiente y variables de entorno por servicio. |
| **2** | **Principios SOLID y Capas** | Arquitectura Hexagonal / Clean Architecture en 4 capas desacopladas. | `sga-principal/src/main/java/ec/edu/uteq/sga/`<br>`docs/adr/ADR-001-arquitectura-capas.md` | Cumplimiento de SRP, OCP y DIP con interfaces en el dominio puro. |
| **3** | **Comunicación gRPC / Protobuf** | Canal síncrono binario HTTP/2 con esquemas versionados Protobuf v3. | `sga-principal/src/main/proto/principal.proto`<br>`microservicio-docente/grpc_protos/`<br>`sga-principal/.../grpc/` | Comunicación inter-servicios de alta velocidad y tipado estricto. |
| **4** | **Relojes de Lamport** | Reloj lógico escalar monótono ($L = \max(L_{\text{loc}}, L_{\text{rx}}) + 1$). | `sga-principal/.../service/LamportClock.java`<br>`sga-principal/.../LamportClockTest.java` | Ordenamiento causal determinista sin sincronización de reloj físico. |
| **5** | **Relojes Vectoriales** | Vectores lógicos $\vec{V}$ para detectar causalidad y concurrencia ($A \parallel B$). | `microservicio-docente/.../clocks.py`<br>`experimentos/run_experimentos.py` (Exp 3) | Reconciliación determinista automática de notas ante ediciones offline. |
| **6** | **Particionamiento de Datos** | Aislamiento multi-esquema en PostgreSQL y particionamiento por rangos. | `docs/db/schema.sql`<br>`docs/adr/ADR-003-persistencia-distribuida.md` | Eliminación de contención de bloqueos y separación por dominio. |
| **7** | **Balanceo de Carga (HAProxy)** | API Gateway y balanceador con Round Robin (REST) y Leastconn (gRPC). | `infra/haproxy/haproxy.cfg`<br>`docker-compose.yml` (servicio `haproxy`) | Alta disponibilidad, terminación segura y monitoreo con Health Checks. |
| **8** | **Sincronización Offline Móvil** | Arquitectura Offline-First con Room SQLite y sincronización WorkManager. | `app-movil-docente/.../sync/SyncWorker.kt`<br>`app-movil-docente/.../AppDatabase.kt` | Disponibilidad de notas desconectada y sincronización en background. |
| **9** | **Auditoría Criptográfica** | Firma HMAC-SHA256 y encadenamiento criptográfico sucesivo de hashes. | `sga-principal/.../AuditoriaService.java`<br>`microservicio-secretaria/.../HmacService.java` | Detección infalsificable ante alteraciones directas en base de datos. |
| **10** | **Inmutabilidad Append-Only** | Triggers PL/pgSQL que abortan cualquier UPDATE o DELETE en bitácora. | `sga-principal/sql/V9__trigger_auditoria_append_only.sql`<br>`docs/db/schema.sql` | Garantía de no-repudio a nivel de motor de persistencia. |
| **11** | **Observabilidad Distribuida** | Micrometer, Prometheus (scrape 15s) y Grafana con 6 vistas. | `infra/prometheus/prometheus.yml`<br>`ops/grafana/pfc-dashboard.json` | Telemetría unificada de RPS, percentiles (P50/P95/P99) y códigos de error. |
| **12** | **PostgreSQL Exporter** | Exportador dedicado para telemetría interna del motor PostgreSQL. | `docker-compose.yml` (servicio `postgres-exporter`)<br>`infra/prometheus/prometheus.yml` | Monitoreo de disponibilidad, transacciones y conexiones a la base. |
| **13** | **Pruebas de Carga con Locust** | Escenario nominal (50 usuarios) y de estrés (hasta 200 usuarios). | `docs/locust/escenario1_nominal_stats.csv`<br>`docs/locust/escenario2_estres_stats.csv`<br>`tests/load/locustfile.py` | Evaluación empírica de throughput, latencias percentiles y estabilidad. |
| **14** | **Tolerancia a Fallos y Consenso** | Coordinación distribuida con `etcd`/Raft para elección de líder. | `docker-compose.yml` (servicio `etcd`)<br>`microservicio-soporte/.../LeaderElectionService.java` | Prevención de tareas programadas redundantes entre réplicas. |