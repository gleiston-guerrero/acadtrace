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

## API

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
