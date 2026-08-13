# SGA · Microservicio de Soporte Técnico

Módulo de **soporte técnico** (tickets / incidencias) del Sistema de Gestión Académica
de la Escuela "Provincias Unidas". Parte de una arquitectura distribuida: cada
microservicio es dueño de su propio esquema y comparte información por gRPC.

## Estructura

```
sga-soporte/
├── backend/            # Spring Boot (Java 21) — API del módulo de soporte
│   ├── src/main/java/ec/uteq/sga/soporte/
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── db/migrations/001_init_soporte.sql
│   ├── Dockerfile
│   └── pom.xml
├── src/                # Frontend React + Vite (portal de soporte)
├── .env.example
└── README.md
```

## Arquitectura (importante)

- **Login único:** no tiene login propio. El usuario inicia sesión en el **SGA Principal**
  y este redirige aquí con el token en el fragmento del URL (SSO handoff, ver `src/main.jsx`).
- **JWT compartido:** el backend valida el **mismo `JWT_SECRET`** que el principal.
  Solo entran los roles `SOPORTE_TECNICO` y `DIRECTOR`.
- **Esquema propio:** toda la data vive en `sga_soporte` (tablas `tickets`,
  `comentarios_ticket`). Ningún otro servicio lee esta base directamente.
- **gRPC:** listo en `pom.xml`; el `.proto` del principal se coloca en
  `backend/src/main/proto/` cuando se necesite consultar datos del principal.

## Puertos

| Componente | Puerto |
|-----------|--------|
| Backend (Spring Boot) | 5178 |
| Frontend (Vite dev)   | 6000 |

## Puesta en marcha (desarrollo)

1. Copiar `.env.example` a `.env` en la raíz y completar credenciales de Supabase
   y el `JWT_SECRET` (el mismo del principal).
2. Aplicar la migración en la base: `backend/src/main/resources/db/migrations/001_init_soporte.sql`.
3. Backend:
   ```
   cd backend
   ./mvnw spring-boot:run
   ```
4. Frontend:
   ```
   npm install
   npm run dev
   ```

## Eleccion de lider (etcd)

Cuando corren varias replicas de este backend (`deploy.replicas > 1` en
`docker-compose.yml`), las tareas programadas (`@Scheduled`) no deben
ejecutarse en todas a la vez -- por ejemplo, cerrar tickets inactivos o
mandar recordatorios dos veces duplicaria el trabajo. `LeaderElectionService`
resuelve esto usando la **Election API nativa de etcd** (etcd ya corre Raft
internamente; esta clase es solo cliente de esa API, no reimplementa Raft).

**Como funciona:**
1. Al arrancar, cada replica pide un lease corto en etcd (`etcd.lease.ttl-seconds`,
   default 5s) y "compite" por el nombre de eleccion `/sga/leader` proponiendo
   su propio id (`app.instance.id`, en Docker es el id del contenedor).
2. Solo una replica gana en un momento dado. Las demas quedan en cola.
3. Mientras el proceso siga vivo, el lease se renueva solo. Si el lider se
   cae (crash, `docker stop`, se corta la red), el lease expira solo y la
   siguiente replica en cola gana la eleccion -- sin intervencion manual.
4. `LeaderElectionService.isLeader()` es lo que consultan los `@Scheduled`
   de `TicketScheduledTasks` antes de hacer cualquier trabajo real.

**Configuracion** (`application.properties` / variables de entorno):

| Propiedad | Variable de entorno | Default |
|---|---|---|
| `etcd.endpoints` | `ETCD_ENDPOINTS` | `http://localhost:2379` |
| `etcd.election.name` | — | `/sga/leader` |
| `etcd.lease.ttl-seconds` | `ETCD_LEASE_TTL` | `5` |
| `app.instance.id` | `HOSTNAME` (Docker lo fija solo) | uuid aleatorio |

**Verificar en la demo:**

```bash
# Ver quien es el lider actual directamente en etcd
docker exec etcd etcdctl get --prefix /sga/leader

# Ver el estado que ve cada replica (requiere Bearer token valido)
curl -H "Authorization: Bearer <token>" http://localhost:8083/api/soporte/election/status

# Probar el failover: apaga el contenedor lider y mide cuanto tarda el otro
docker stop microservicio-soporte   # (o el nombre de la replica lider)
docker exec etcd etcdctl get --prefix /sga/leader   # deberia mostrar al otro id en <5s
```

**Nota sobre `deploy.replicas`:** este cambio deja la infraestructura de
eleccion lista para cuando el servicio corra con varias replicas, pero el
`docker-compose.yml` actual todavia levanta una sola instancia de
`microservicio-soporte` -- agregar `deploy.replicas: 2` (y el load balancer
correspondiente) es coordinacion con la Persona 1, no de este cambio.



Base: `/api/soporte` (protegida por JWT, roles SOPORTE_TECNICO / DIRECTOR).

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET    | `/tickets`                    | Listar (filtros: `estado`, `prioridad`, paginación) |
| GET    | `/tickets/{id}`               | Detalle de un ticket |
| POST   | `/tickets`                    | Crear ticket |
| PATCH  | `/tickets/{id}/estado`        | Cambiar estado (ABIERTO/EN_PROCESO/RESUELTO/CERRADO) |
| PATCH  | `/tickets/{id}/asignar`       | Asignar a un técnico |
| GET    | `/tickets/{id}/comentarios`   | Listar comentarios |
| POST   | `/tickets/{id}/comentarios`   | Agregar comentario |
| GET    | `/health`                     | Estado del servicio |