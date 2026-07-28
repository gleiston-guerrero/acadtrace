# Secretaría — Análisis STRIDE y SLA

Microservicio: `sga-secretaria-backend` (Spring Boot, Java 21). Expone
`/api/secretario/*` para 5 módulos: **estudiantes, matrículas, usuarios,
historial, reportes**. Autenticación vía JWT emitido por sga-principal
(mismo `JWT_SECRET`), autorización por rol (`SECRETARIA` / `DIRECTOR`,
los nombres reales de `sga_principal.roles`) validada en `JwtAuthFilter`.
Dentro del módulo, la gestión de usuarios (crear, resetear password,
asignar roles, cambiar estado) queda reservada a `DIRECTOR`.

> Corrección post-análisis: la primera versión de este documento y del
> código asumía roles `ROLE_SECRETARIO`/`ROLE_ADMIN` (convención típica de
> Spring Security), pero **esos nombres nunca existieron en los tokens
> reales** que emite sga-principal — los roles reales son `DIRECTOR`,
> `SECRETARIA`, `DOCENTE`, `SOPORTE_TECNICO`. Esto bloqueaba el acceso de
> *todo el mundo* al módulo (403 con credenciales correctas). Corregido al
> probar el login end-to-end contra sga-principal real.

## 1. STRIDE

### Spoofing (suplantación de identidad)

| Amenaza | Mitigación actual | Riesgo residual |
|---|---|---|
| Token JWT robado o falsificado | Firma HMAC verificada (`JwtService`, mismo secret que sga-principal) | El secret vive en `.env` compartido entre dos servicios: si se filtra uno, se compromete el otro. Rotarlo obliga a coordinar deploy simultáneo. |
| Reutilización de un token expirado o revocado | `jjwt` valida expiración de la firma | No hay lista de revocación: un token robado sigue siendo válido hasta que expira, aunque se desactive el usuario (`estado=false` en `usuarios`). |

### Tampering (manipulación de datos)

| Amenaza | Mitigación actual | Riesgo residual |
|---|---|---|
| Modificar `direccion`/`telefono`/`tipo_discapacidad` de un estudiante en tránsito o en la base | Cifrado AES-256-GCM (`CryptoService`): el tag de autenticación de GCM hace que cualquier alteración del ciphertext invalide el descifrado | Solo cubre esos 3 campos. `cedula`, `nombres`, `correo`, notas de `historial_promocion` siguen en texto plano — un acceso directo a Supabase (fuera de la app) puede alterarlos sin detección. |
| Editar filas de `sga_principal.*` sin pasar por la API (SQL directo a Supabase) | Ninguna hoy | Éste es justamente el problema que motiva la migración a gRPC: mientras Secretaría y sga-principal escriban SQL directo a las mismas tablas, ninguno puede garantizar que los datos que lee fueron validados por su propia lógica de negocio. |

### Repudiation (repudio / negar una acción)

| Amenaza | Mitigación actual | Riesgo residual |
|---|---|---|
| Un secretario niega haber creado/editado un estudiante o registrado una promoción | `creado_por` / `registrado_por` (FK a `usuarios`) en `estudiantes` y `historial_promocion` | No hay tabla de auditoría genérica: updates y cambios de estado (`cambiarEstado`, `resetearPassword`, `asignarRoles`) no quedan registrados con quién los hizo ni cuándo, solo el estado final. |
| Orden de eventos en el historial académico cuestionable entre servicios | `lamport_ts` (reloj de Lamport) en `historial_promocion` da orden causal independiente del reloj de pared de cada proceso | El reloj es en memoria y por instancia: si se corren réplicas del servicio sin coordinación, cada una lleva su propio contador (mitigado parcialmente porque `seedLamportClock()` arranca desde el máximo persistido, pero no hay sincronización entre instancias concurrentes). |

### Information Disclosure (divulgación de información)

| Amenaza | Mitigación actual | Riesgo residual |
|---|---|---|
| Filtración de datos de menores (estudiantes) si Supabase es comprometido o hay un dump de la base | AES-256-GCM en `direccion`/`telefono`/`tipo_discapacidad` | `cedula`, `nombres`, `apellidos`, correo, datos médicos de `fichas_estudiante` (alergias, enfermedad catastrófica) **no están cifrados**. Es el conjunto de datos más sensible del sistema (menores de edad) y hoy viaja en claro en la base compartida. |
| `AES_SECRET_KEY` comprometida | Vive en `.env` (gitignored), no en el repo | Una sola clave para todos los estudiantes: si se filtra, se puede descifrar todo el histórico. No hay rotación de clave ni versión de clave (key ID) en el ciphertext. |
| Respuestas de error con detalle interno (stack traces, SQL) | `GlobalExceptionHandler` centraliza el formato de error | No confirmado si oculta el mensaje real de `PSQLException` al cliente; revisar antes de exponer el servicio fuera de la red interna. |

### Denial of Service (denegación de servicio)

| Amenaza | Mitigación actual | Riesgo residual |
|---|---|---|
| Agotar el pool de conexiones de Supabase (límite compartido entre microservicios) | Cambio de `DB_PORT` a 6543 (pooler transaction-mode de Supabase, más conexiones lógicas que el puerto directo 5432) | Sin límite de tamaño de pool configurado explícitamente en `DataSourceConfig`/HikariCP ni rate-limiting por IP/usuario en los endpoints. |
| Reportes pesados (`nomina-matriculas`, PDFs) usados para saturar CPU/memoria | Ninguna | Un usuario autorizado podría pedir reportes repetidamente sin límite de frecuencia. |

### Elevation of Privilege (escalación de privilegios)

| Amenaza | Mitigación actual | Riesgo residual |
|---|---|---|
| Usuario sin rol autorizado accede a `/api/secretario/*` | `JwtAuthFilter` exige `SECRETARIA` o `DIRECTOR` (bug histórico de `requireRole` como no-op ya corregido; bug posterior de nombres de rol inexistentes — `ROLE_SECRETARIO`/`ROLE_ADMIN` — también corregido, ver nota arriba) | — |
| Un `SECRETARIA` realiza acciones que deberían ser solo de `DIRECTOR` | `UsuarioController.requireDirector()` exige `DIRECTOR` explícito en `POST /usuarios`, `PATCH /usuarios/{id}/reset-password`, `PATCH /usuarios/{id}/estado` y `PATCH /usuarios/{id}/roles` | **Corregido durante este análisis.** Antes cualquier `SECRETARIA` (una vez arreglado el bug de nombres de rol) podía crear usuarios, resetear contraseñas, activar/desactivar cuentas y reasignar roles arbitrariamente — el filtro global solo exige `SECRETARIA\|DIRECTOR` a nivel de ruta, no por endpoint. Política acordada: dentro de `usuarios`, `SECRETARIA` queda limitada a listar/ver; la gestión (crear, resetear, cambiar estado, asignar roles) es exclusiva de `DIRECTOR`. |

## 2. SLA propuesto (módulo Secretaría)

| Métrica | Objetivo | Cómo se mide |
|---|---|---|
| Disponibilidad mensual | 99.0% (≈ 7h 18m de downtime/mes tolerado — proyecto académico, no 99.9%) | Uptime del endpoint `GET /api/secretario/*/health` o equivalente |
| Latencia p95, endpoints CRUD (estudiantes, matrículas, usuarios) | < 400 ms | Tiempo de respuesta medido en el servidor, excluyendo generación de PDF |
| Latencia p95, generación de reportes PDF | < 3 s | `ReportesController` (certificado, nómina, ficha) |
| Tasa de error 5xx | < 1% de las requests | Logs / contador de `GlobalExceptionHandler` |
| Integridad del cifrado | 0% de fallos de descifrado en escrituras propias del servicio | Contador de warnings de `descifrarFila` (fallback a texto plano) — un valor > 0 en filas escritas *después* de la migración indica un bug, no dato legado |
| Tiempo de recuperación tras caída (RTO) | < 5 min | Reinicio del contenedor Docker + reconexión a Supabase |

## 3. Notas de diseño relacionadas

- **AES-256-GCM**: implementado en `CryptoService`
  (`backend/src/main/java/.../security/CryptoService.java`), aplicado a
  `direccion`, `telefono`, `tipo_discapacidad` de `estudiantes`. No se cifran
  `cedula` ni `correo` porque se usan en búsquedas (`ILIKE`, `WHERE =`) y
  unicidad — cifrar con GCM (no determinístico) rompería esas queries.
- **Relojes de Lamport**: implementado en `LamportClock`, usado por
  `HistorialService` para poner `lamport_ts` en cada registro de
  `historial_promocion`. Ver `db/migrations/002_lamport_clock.sql`.
- Ambos puntos anteriores son parches del lado de Secretaría mientras
  Secretaría siga escribiendo SQL directo a `sga_principal.*`. La solución de
  fondo para varias de las amenazas de Tampering/Information Disclosure
  listadas arriba es la migración a gRPC (punto 3 del correo de indicaciones):
  centralizar la escritura en un solo servicio permite aplicar cifrado y
  auditoría en un único lugar en vez de duplicarlo.
