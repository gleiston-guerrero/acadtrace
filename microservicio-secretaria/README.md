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

## Instalación Reproducible y Primer Uso (Criterios E12 & E14)

### Requisitos Previos
- **Java:** JDK 21 (Eclipse Temurin 21 recomendado).
- **Maven:** 3.9+ (o utilizar el wrapper incluido `./mvnw` / `mvnw.cmd`).
- **Node.js:** 20.x LTS y npm con soporte para lockfile v3 (`package-lock.json`).

### Pasos de Construcción Limpia desde Cero

```bash
# 1. Configurar variables de entorno requeridas
cp .env.example .env
# → Definir contraseñas y llaves obligatorias (ver sección Variables de Entorno)

# 2. Instalación determinística y compilación del frontend (E12 con npm ci)
cd client
npm ci --no-audit
npm run build
cd ..

# 3. Compilación, verificación de pruebas y empaquetado del backend
cd backend
./mvnw clean package -DskipTests   # Linux / macOS
# o en Windows:
.\mvnw.cmd clean package -DskipTests
cd ..

# 4. Ejecutar el microservicio Secretaría
java -jar backend/target/sga-secretaria-backend.jar
```

Abre **http://localhost:5176** — verás la interfaz web integrada del panel de secretaría.

---

## Modos de Desarrollo

### Opción A — Solo backend (usando build previo del frontend)
```bash
cd backend
./mvnw spring-boot:run
```
Con `spring-boot-devtools` en el classpath, el backend se reinicia automáticamente al recompilar.

### Opción B — Desarrollo activo del frontend (Vite Hot Reload)
```bash
# Terminal 1: Backend Spring Boot
cd backend
./mvnw spring-boot:run

# Terminal 2: Vite dev con proxy hacia :5176
cd client
npm ci
npm run dev
# Abre http://localhost:5174
```

---

## Variables de Entorno Obligatorias (Fail-Fast E1)

| Variable | Descripción | Valor Ejemplo / Placeholder |
|---|---|---|
| `PORT` | Puerto del servidor HTTP | `5176` |
| `DB_HOST` | Host PostgreSQL | `192.0.2.1` |
| `DB_PORT` | Puerto PostgreSQL | `5433` |
| `DB_NAME` | Nombre base de datos | `sga` |
| `DB_USER` | Usuario base de datos | `postgres` |
| `DB_PASSWORD` | Contraseña obligatoria (fail-fast) | `change-me` |
| `JWT_SECRET` | Clave secreta obligatoria HMAC-SHA256 (mín. 256 bits) | `change-me` |
| `AES_SECRET_KEY` | Clave AES-256 (32 bytes Base64) para campos sensibles | `change-me` |
| `GRPC_INTERNAL_TOKEN` | Token interno de autenticación inter-servicios | `change-me` |
| `GRPC_PRINCIPAL_HOST` | Host del servidor gRPC Principal | `sga-principal` (o `localhost`) |
| `GRPC_PRINCIPAL_PORT` | Puerto del servidor gRPC Principal | `9092` |
| `INST_NOMBRE` / `INST_CIUDAD` | Encabezados institucionales de reportes PDF | `Escuela de Educación Básica Provincias Unidas` |
| `CORS_ORIGIN` *(opcional)* | Origen CORS permitido (default `*`) | `*` |
| `FRONTEND_DIST_PATH` *(opcional)* | Ruta de artefactos estáticos | `client/dist` |

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
