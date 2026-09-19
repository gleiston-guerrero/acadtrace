# ADR-003: Persistencia PostgreSQL con separación lógica multiesquema

## Estado
Aceptado. Descripción corregida el 16 de septiembre de 2026 para reflejar la configuración versionada; no cambia la infraestructura.

## Contexto
La aplicación necesita separar los datos de sus dominios y microservicios. El despliegue versionado no contiene un clúster PostgreSQL/CockroachDB de tres nodos. `docker-compose.yml` conecta los servicios al mismo endpoint mediante variables de entorno `DB_HOST`, `DB_PORT` y `DB_NAME` (puerto predeterminado `5433` y base `sga`), sin declarar un servidor PostgreSQL. `sga-principal/docker-compose.yml` declara una única instancia `postgres:17`, base `sga`, volumen `pgdata` y puerto `5433:5432`. Son configuraciones alternativas, no nodos de un clúster. No permiten certificar la topología interna ni la versión del servidor externo.

## Decisión
Utilizar PostgreSQL con separación lógica multiesquema dentro de la instancia/base configurada. Los esquemas evidenciados son:

| Esquema | Evidencia versionada | Uso demostrado |
|---|---|---|
| `sga_principal` | `sga-principal/src/main/resources/application.properties`: `spring.flyway.schemas` y `hibernate.default_schema`; entidades de Principal | Catálogo, usuarios y auditoría central. |
| `sga_docente` | `microservicio-docente/micro_docente/settings.py`: `search_path=sga_docente,sga_principal,public`; `docentes/models.py`: `db_table`; `sga-principal/src/main/resources/db/migration/V8__baseline_completo.sql` (18 tablas del esquema `sga_docente`) | Calificaciones, actividades y asistencias. El search path incluye otros esquemas; no implica exclusividad de acceso. |
| `sga_secretaria` | `sga-principal/sql/V5__esquema_sga_secretaria.sql`; migraciones y consultas de Secretaría | Estudiantes, representantes y matrículas. |
| `sga_soporte` | `microservicio-soporte/backend/src/main/resources/db/migrations/001_init_soporte.sql`; `JdbcTicketRepository.java` | Tickets y comentarios. |

`public` también aparece en el dump y en el search path de Docente. `docs/db/schema.sql` es un SQL de referencia con nombres distintos: crea `sga_principal`, `secretaria`, `docente` y `soporte`, y define tablas en `sga_principal` y `soporte`. No prueba que esos nombres sin prefijo sean los usados por los servicios ni que dicho SQL se ejecute automáticamente al levantar los Compose. La existencia de SQL/migraciones versionadas tampoco certifica su aplicación al servidor externo.

Esta separación es lógica, no particionamiento físico ni distribución de filas por período/grado. No se encontró DDL `PARTITION BY`/`PARTITION OF` en los SQL y migraciones revisados. Los esquemas comparten recursos del servidor; no garantizan eliminación de bloqueos, independencia física de bases ni aislamiento de cargas.

### Coordinación distribuida, separada de la persistencia
Soporte utiliza `etcd` mediante `LeaderElectionService` y la API Election de jetcd, con leases y elección sobre `/sga/leader`; `TicketScheduledTasks` condiciona tareas al liderazgo. Raft es el protocolo interno de etcd. El Compose principal declara un servicio etcd, no evidencia un despliegue etcd de tres miembros.

**Raft de etcd no replica PostgreSQL y no constituye un clúster de persistencia PostgreSQL.** No se implementa CockroachDB ni consenso Raft de base relacional en la configuración versionada.

## Consecuencias y limitaciones
- Se mantiene PostgreSQL y la organización lógica por dominios, sin cambios de infraestructura.
- Las transacciones locales de PostgreSQL no constituyen un protocolo distribuido 2PC entre servicios.
- La única instancia PostgreSQL declarada y el único endpoint del Compose principal constituyen un punto único de fallo de persistencia en la topología demostrada. No hay configuración versionada que demuestre réplicas, standby, failover automático o alta disponibilidad de PostgreSQL.
- La elección de líder de Soporte coordina tareas; no protege los datos frente a la caída de PostgreSQL ni demuestra disponibilidad del servidor externo.
- Replicación, recuperación y particionamiento físico requerirían una decisión e implementación futura verificable; no se presentan como capacidades actuales.
