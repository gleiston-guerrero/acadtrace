# SGA Secretaría — Backend Java + Frontend React

Un proyecto que contiene el **backend Spring Boot** y el **frontend React** del panel de secretaría.

```
sga-secretaria/
├── .env                        ← variables de entorno (no se commitea)
├── backend/                    ← Spring Boot: API + sirve el frontend
│   ├── pom.xml
│   └── src/main/java/ec/uteq/sga/secretaria/
│       ├── config/             ← DataSource, CORS, resource handler del SPA
│       ├── security/           ← filtro JWT (mismo secret que sga-principal)
│       ├── common/             ← manejo global de errores, paginación, RowMapper JDBC
│       ├── controller/         ← todos los @RestController
│       ├── service/            ← toda la lógica de negocio (SQL vía NamedParameterJdbcTemplate)
│       ├── dto/                ← todos los DTOs de request
│       └── pdf/                ← generador de reportes PDF (Apache PDFBox)
└── client/
    ├── package.json            ← dependencias del frontend
    ├── vite.config.js          ← build + proxy dev
    └── src/
        ├── pages/               ← Login, Dashboard, Estudiantes, Matrículas, etc.
        ├── components/          ← Layout con sidebar
        └── utils/               ← api.js con axios
```

---

## Instalación y primer uso

```bash
# 1. Configurar variables de entorno
cp .env.example .env
# → Editar .env: DB_PASSWORD y demás valores

# 2. Compilar el frontend (una sola vez, o cada vez que cambies el React)
cd client && npm install && npm run build && cd ..

# 3. Compilar y ejecutar el backend (desde la raíz del repo)
mvn -f backend/pom.xml spring-boot:run
```

Abre **http://localhost:5176** — verás el login del panel de secretaría.

> El backend usa el Maven Wrapper si no tenés Maven instalado: `./backend/mvnw -f backend/pom.xml spring-boot:run` (Linux/Mac) o `backend\mvnw.cmd -f backend\pom.xml spring-boot:run` (Windows).

---

## Modos de desarrollo

### Opción A — Solo backend (si ya tenés el build del frontend)
```bash
mvn -f backend/pom.xml spring-boot:run
```
Con `spring-boot-devtools` en el classpath, el backend se reinicia solo al recompilar (`mvn -f backend/pom.xml compile`).

### Opción B — Desarrollo activo del frontend (hot reload)
```bash
# Terminal 1: backend Spring Boot
mvn -f backend/pom.xml spring-boot:run

# Terminal 2: Vite dev con proxy automático → :5176
cd client && npm run dev
# Abre http://localhost:5174
```

El `vite.config.js` tiene configurado el proxy: todas las peticiones `/api/*` desde el puerto de Vite se redirigen al backend.

---

## Variables de entorno

| Variable | Descripción | Ejemplo |
|---|---|---|
| `PORT` | Puerto del servidor | `5176` |
| `DB_HOST` | Host Supabase | `aws-1-us-east-1.pooler.supabase.com` |
| `DB_PORT` | Puerto PostgreSQL | `5432` |
| `DB_NAME` | Nombre base de datos | `postgres` |
| `DB_USER` | Usuario Supabase | `postgres.xxxxx` |
| `DB_PASSWORD` | Contraseña | `tu_password` |
| `DB_SSL` | SSL activado | `true` |
| `JWT_SECRET` | **Misma secret que sga-principal** | `sga-provincias-unidas-...` |
| `VITE_API_PRINCIPAL` | URL del Spring Boot de login (solo dev, la usa el frontend) | `http://localhost:8080/api` |
| `INST_NOMBRE` / `INST_CIUDAD` | Encabezados institucionales de los PDFs | — |
| `CORS_ORIGIN` *(opcional)* | Origen permitido, default `*` | — |
| `FRONTEND_DIST_PATH` *(opcional)* | Carpeta del build del frontend, default `client/dist` | — |

---

## API disponible

Todas las rutas requieren `Authorization: Bearer <token>` y el rol `ROLE_SECRETARIO` o `ROLE_ADMIN` en el JWT (verificado de verdad, no solo la presencia del token).

| Módulo | Base URL |
|---|---|
| Estudiantes | `GET/POST/PUT/PATCH /api/secretario/estudiantes` |
| Matrículas | `GET/POST/PATCH /api/secretario/matriculas` |
| Usuarios | `GET/POST/PUT/PATCH /api/secretario/usuarios` |
| Historial | `GET/POST /api/secretario/historial` |
| Reportes PDF | `GET /api/secretario/reportes/...` |

---

## Cómo funciona la unificación

En producción:
1. `npm run build` (dentro de `client/`) compila React → `client/dist/`
2. Spring Boot sirve `client/dist/` como archivos estáticos en `/`, leído directamente del disco (no se copia al `.jar`, así no hace falta recompilar el backend cuando cambia el frontend)
3. Las rutas `/api/*` van al backend
4. Cualquier otra ruta devuelve `index.html` (SPA routing de React Router)

Todo desde **un solo proceso, un solo puerto**.
