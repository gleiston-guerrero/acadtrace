# Diagramas de Arquitectura C4 - SGA Distribuido

Este directorio contiene los diagramas arquitectónicos del **Sistema de Gestión Académica (SGA Escuela Provincias Unidas)** estructurados bajo el modelo **C4 (Contexto, Contenedores, Componentes y Código)**.

---

## 1. Nivel 1: Diagrama de Contexto del Sistema

Describe los actores que interactúan con el SGA y los sistemas externos que forman parte de la solución.

```mermaid
C4Context
    title Diagrama C4 - Nivel 1: Contexto del Sistema SGA Distribuido

    Person(docente, "Docente", "Registra asistencia, calificaciones y planificaciones vía Web y App Móvil.")
    Person(secretaria, "Personal Secretaría", "Gestiona matrículas, actas y expedientes.")
    Person(soporte, "Personal Soporte", "Atiende incidencias y asigna tickets técnicos.")
    Person(estudiante, "Estudiante / Tutor", "Consulta notas y reportes académicos.")

    System(sga, "Sistema de Gestión Académica (SGA)", "Sistema distribuido de gestión académica, matrículas y soporte.")

    System_Ext(gemini, "Google Gemini 1.5 Flash", "Inferencia de IA para resúmenes de actas e informes.")
    System_Ext(grafana_cloud, "Grafana Cloud", "Plataforma de métricas remotas y observabilidad.")
    System_Ext(smtp, "Servidor SMTP", "Notificaciones de correos y tickets.")

    Rel(docente, sga, "Gestiona notas y asistencia", "HTTPS / REST / Móvil")
    Rel(secretaria, sga, "Administra expedientes y matrículas", "HTTPS / REST")
    Rel(soporte, sga, "Gestiona tickets y asignaciones", "HTTPS / REST")
    Rel(estudiante, sga, "Consulta calificaciones", "HTTPS / REST")

    Rel(sga, gemini, "Solicita inferencia y resúmenes", "HTTPS / JSON API")
    Rel(sga, grafana_cloud, "Envía métricas de observabilidad", "HTTPS / remote_write")
    Rel(sga, smtp, "Envía notificaciones", "SMTP / TLS")
```

---

## 2. Nivel 2: Diagrama de Contenedores

Describe las aplicaciones, microservicios, bases de datos y herramientas de monitoreo desplegadas en contenedores Docker.

```mermaid
C4Container
    title Diagrama C4 - Nivel 2: Contenedores del SGA Distribuido

    Person(docente, "Docente", "Usuario docente")
    Person(admin, "Personal Secretaría / Soporte", "Usuarios administrativos")

    Container(app_movil, "App Móvil Docente", "Android Kotlin", "Offline-first con SQLite Room")
    Container(web_spa, "Portales Web", "Vue.js / HTML5", "Interfaces SPA de gestión")

    Container(haproxy, "HAProxy API Gateway", "HAProxy 2.9", "Balanceo Round-Robin, CORS y Métricas en :8404")

    Container(sga_principal, "sga-principal (2 Réplicas)", "Spring Boot", "Catálogo, Auth JWT y gRPC :9092")
    Container(ms_secretaria, "microservicio-secretaria", "Spring Boot", "Matrículas y Event Sourcing")
    Container(ms_docente, "microservicio-docente", "Django REST", "Asistencia y Notas")
    Container(ms_soporte, "microservicio-soporte", "Spring Boot", "Tickets, IDOR fix, Líder etcd, gRPC :9094")
    Container(ms_ia, "microservicio-ia", "FastAPI", "Google Gemini 1.5 Flash")

    ContainerDb(db, "PostgreSQL 16", "PostgreSQL AWS", "Esquemas: public, secretaria, docente, soporte")
    ContainerDb(etcd, "etcd (Raft)", "etcd v3.5", "Consenso Raft para elección de líder")

    Container(prometheus, "Prometheus", "Prometheus v2.51", "Scraping cada 15s")
    Container(cadvisor, "cAdvisor", "cAdvisor v0.49", "Métricas CPU/RAM")
    Container(grafana, "Grafana", "Grafana v10.4", "Dashboards de 6 vistas")

    Rel(docente, app_movil, "Usa app", "")
    Rel(docente, web_spa, "Usa portal", "")
    Rel(admin, web_spa, "Usa portal", "")

    Rel(app_movil, haproxy, "Sync / REST", "HTTPS :8081")
    Rel(web_spa, haproxy, "Peticiones REST", "HTTPS :8080-:8084")

    Rel(haproxy, sga_principal, "Proxy REST/gRPC", "HTTP :8080, TCP :9092")
    Rel(haproxy, ms_secretaria, "Proxy", "HTTP :5176")
    Rel(haproxy, ms_docente, "Proxy", "HTTP :8000")
    Rel(haproxy, ms_soporte, "Proxy", "HTTP :5178")
    Rel(haproxy, ms_ia, "Proxy", "HTTP :8084")

    Rel(ms_soporte, etcd, "Líder Raft", "gRPC :2379")
    Rel(ms_soporte, db, "Persistencia", "JDBC :5432")
    Rel(sga_principal, db, "Persistencia", "JDBC :5432")
    Rel(ms_secretaria, db, "Persistencia", "JDBC :5432")
    Rel(ms_docente, db, "Persistencia", "SQL :5432")

    Rel(prometheus, ms_soporte, "Métricas", "/actuator/prometheus")
    Rel(prometheus, haproxy, "Métricas", "/metrics :8404")
    Rel(prometheus, etcd, "Métricas", "/metrics :2379")
    Rel(prometheus, cadvisor, "Métricas", ":8080")
    Rel(grafana, prometheus, "Consultas PromQL", ":9090")
```

---

## 3. Nivel 3: Diagrama de Componentes de `microservicio-soporte`

Detalla la arquitectura hexagonal/por capas del microservicio de soporte técnico asignado.

```mermaid
C4Component
    title Diagrama C4 - Nivel 3: Componentes de microservicio-soporte

    Container_Boundary(ms_soporte, "microservicio-soporte") {
        Component(ctrl_ticket, "TicketController", "REST Controller", "Endpoints de tickets con validación RBAC anti-IDOR.")
        Component(ctrl_tecnico, "TecnicoController", "REST Controller", "Gestión de técnicos.")
        Component(ctrl_election, "LeaderElectionController", "REST Controller", "Estado de líder etcd.")

        Component(srv_ticket, "TicketService", "Service Layer", "Lógica de negocio, estados y permisos.")
        Component(srv_election, "LeaderElectionService", "Service Layer", "Elección de líder en etcd con lease de 5s.")
        Component(task_cron, "TicketScheduledTasks", "Scheduler", "Cierre automático solo en instancia líder.")

        Component(grpc_incidencias, "IncidenciaGrpcServer", "gRPC Server", "Escucha en :9094 para registrar incidencias inter-servicio.")
        Component(grpc_principal, "PrincipalGrpcClient", "gRPC Client", "Consulta usuarios en sga-principal :9092.")

        Component(sec_jwt, "JwtAuthenticationFilter", "Security Filter", "Valida tokens JWT compartidos.")
        Component(dao_jdbc, "DataSourceConfig & JdbcTemplate", "JDBC Repo", "Consultas directas a schema soporte.")
        Component(actuator, "Actuator / Micrometer", "Metrics", "Histogramas de latencia P50/P95/P99.")
    }

    Rel(ctrl_ticket, srv_ticket, "Llama")
    Rel(ctrl_tecnico, srv_ticket, "Llama")
    Rel(grpc_incidencias, srv_ticket, "Crea ticket por falla")
    Rel(task_cron, srv_election, "Verifica si es líder")
    Rel(task_cron, srv_ticket, "Ejecuta cierre")
    Rel(srv_ticket, grpc_principal, "Valida usuario")
    Rel(srv_ticket, dao_jdbc, "Operaciones SQL")
```
