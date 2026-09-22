# Punto 45 — Restricción de privilegios de modificación sobre la bitácora

**Responsable:** Pedro Castro (LEO23as)
**Rama:** `Leonardo-Castro`
**Fecha del informe:** 2026-09-22
**Alcance:** rol de aplicación `sga_app`, bitácora institucional `sga_principal.auditoria`, validador de arranque, saneamiento de integración continua y procedimiento de rotación de credenciales.

---

## 1. Resumen de cumplimiento

La restricción de privilegios de modificación sobre la bitácora opera en cuatro capas verificables:

1. **Restricción a nivel de motor relacional:** el rol `sga_app` carece de `UPDATE`, `DELETE` y `TRUNCATE` sobre `sga_principal.auditoria`; el motor los rechaza con SQLState `42501` (`permission denied`).
2. **Restricción a nivel de propiedad y disparadores:** los disparadores `tg_auditoria_append_only` (V13, `BEFORE UPDATE OR DELETE`) y `tg_auditoria_no_truncate` (V21, `BEFORE TRUNCATE`) rechazan la modificación con `P0001` (`Operacion rechazada`) desde cualquier sesión que logre privilegios, y el rol de aplicación no puede desactivarlos porque no es propietario de la tabla.
3. **Sin superusuario:** el rol se aprovisiona con `NOSUPERUSER NOCREATEDB NOCREATEROLE` (V18), de modo que ninguna prerrogativa de superusuario puede eludir las capas anteriores.
4. **Comprobación obligatoria en el arranque:** `RestriccionBitacoraValidator` detiene el inicio del servicio si la restricción no se cumple.

---

## 2. Evidencia de conformidad en el motor relacional

### 2.1 Comprobación de permisos sobre `sga_principal.auditoria`

Ejecutada como el rol de aplicación contra un PostgreSQL 16.15 efímero descartable (2026-09-22), reproduciendo las concesiones de `V18__rol_aplicacion_y_revocacion_auditoria.sql` sobre una tabla cuyo propietario es el rol administrador:

| Operación solicitada a `sga_app` | Resultado | SQLState | Mensaje del motor |
|---|---|---|---|
| `INSERT` | **Permitido** (append-only) | — | `INSERT 0 1` |
| `UPDATE` | Rechazado | `42501` | `permission denied for table auditoria` |
| `DELETE` | Rechazado | `42501` | `permission denied for table auditoria` |
| `TRUNCATE` | Rechazado | `42501` | `permission denied for table auditoria` |
| `SELECT has_table_privilege(..., 'INSERT')` | `t` | — | — |
| `SELECT has_table_privilege(..., 'UPDATE' \| 'DELETE' \| 'TRUNCATE')` | `f` | — | — |

### 2.2 Superusuario deshabilitado

```sql
SELECT rolname, rolsuper FROM pg_roles WHERE rolname = 'sga_app';
 rolname | rolsuper
---------+----------
 sga_app | f
```

`rolsuper = false` (V18: `NOSUPERUSER`). Consecuencia verificada: el rol no puede eludir ni los disparadores ni las revocaciones mediante prerrogativas de superusuario.

### 2.3 Intento de desactivar disparadores

| Operación solicitada a `sga_app` | Resultado | SQLState | Mensaje del motor |
|---|---|---|---|
| `ALTER TABLE sga_principal.auditoria DISABLE TRIGGER tg_auditoria_append_only` | Rechazado | `42501` | `must be owner of table auditoria` |

Justificación: en PostgreSQL, el cambio de estado de un disparador exige ser propietario de la tabla (o superusuario). La tabla pertenece al rol administrador que ejecuta el pipeline de migraciones y `sga_app` es `NOSUPERUSER`, por lo que la desactivación queda vedada. Las pruebas de integración del repositorio desactivan y reactivan el disparador **únicamente desde la sesión propietaria**, lo que confirma que la vía administrativa es la única hábil y que, aun con el disparador desactivado, la capa de privilegios vuelve a rechazar la modificación con `42501`.

### 2.4 Fuente versionada de estas comprobaciones

- `sga-principal/src/test/java/ec/edu/uteq/sga/integration/AuditoriaFlywayMigrationContainerTest.java`
  - `pipelineRealDeFlywayCreaTriggerDeInmutabilidad`: ejecuta el pipeline real V8→V26 sobre PostgreSQL efímero, inserta un registro testigo (id positivo), y exige que `UPDATE`, `DELETE` y `TRUNCATE` fallen con `P0001` / `Operacion rechazada`.
  - `criterioE6_rolCreadoPorMigracionRechazaModificacionInclusoSinTrigger`: desactiva el disparador como propietario y exige que `sga_app` reciba `42501` / `permission denied` en `UPDATE` y `DELETE`.
- Informe Surefire auténtico versionado: `docs/evidencia/pruebas/sga-principal/TEST-ec.edu.uteq.sga.integration.AuditoriaFlywayMigrationContainerTest.xml` — `tests="2" errors="0" skipped="0" failures="0"`, tiempo `9.673`, ejecución `2026-09-22` (Windows 11, JDK 21.0.11), SHA-256 `2015DB00A3BCA5876D6F950F1A193B846920BF5F2C2E1BA9F56265519EB9FA4F`.
- Prueba complementaria de la capa de permisos con disparador desactivado: `sga-principal/src/test/java/ec/edu/uteq/sga/infrastructure/repository/AuditoriaInmutabilidadTest.java`.

---

## 3. Validador dinámico de arranque

**Clase:** `sga-principal/src/main/java/ec/edu/uteq/sga/infrastructure/config/RestriccionBitacoraValidator.java`

El componente se registra en el contexto de Spring y ejecuta su comprobación en su fase de inicialización (`@PostConstruct`), dentro de la secuencia de arranque del contexto que precede a la publicación de `ApplicationReadyEvent`. Si la comprobación falla, el contexto no termina de inicializarse: la aplicación **no** alcanza `ApplicationReadyEvent` ni declara `Started SgaPrincipalApplication`, de modo que la restricción es condición de disponibilidad del servicio y no una advertencia registrable.

Secuencia de la comprobación, sobre la conexión del propio datasource:

1. `SELECT current_user` — identifica el usuario efectivo.
2. `SELECT rolsuper FROM pg_roles WHERE rolname = current_user` — si es superusuario, lanza `IllegalStateException` y aborta el arranque.
3. `SELECT has_table_privilege(current_user, 'sga_principal.auditoria', 'UPDATE' | 'DELETE' | 'TRUNCATE')` — cualquier `true` lanza `IllegalStateException` y aborta el arranque.
4. En caso contrario registra la línea de acreditación observada en los entornos verificados:

```
RestriccionBitacoraValidator : Verificado: usuario sga_app no puede modificar la bitacora
```

**Pruebas unitarias (ejecución local verificada, 2026-09-22, JDK 17):**

```bash
cd sga-principal
./mvnw test "-Dtest=RestriccionBitacoraValidatorTest" "-Djacoco.skip=true"
```

Resultado Surefire: `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0` — **3 de 3 en verde**, `BUILD SUCCESS`.

Cobertura de las tres pruebas (`RestriccionBitacoraValidatorTest`):

| Prueba | Comportamiento exigido |
|---|---|
| `verificar_ConSuperusuario_LanzaExcepcion` | Aborta si el usuario efectivo es superusuario |
| `verificar_SinPrivilegios_NoLanzaExcepcion` | Arranca si no hay privilegios de modificación (verifica las 3 consultas) |
| `verificar_ConPrivilegioUpdate_LanzaExcepcion` | Aborta ante cualquier privilegio de modificación |

---

## 4. Resolución de las observaciones de la evaluación

### 4.1 Observación 1 — Saneamiento del respaldo al superusuario en CI

**Antes:** en el job `test-backend` de `.github/workflows/ci-cd.yml`:

```yaml
DB_PASSWORD: ${{ secrets.SGA_APP_PASSWORD || secrets.DB_PASSWORD }}
SGA_APP_PASSWORD: ${{ secrets.SGA_APP_PASSWORD || secrets.DB_PASSWORD }}
```

La expresión `||` permitía que, ante la ausencia del secreto de la aplicación, el job siguiera adelante usando `secrets.DB_PASSWORD`, es decir la contraseña del rol administrador, para el rol de aplicación.

**Después (cambio aplicado):**

```yaml
DB_PASSWORD: ${{ secrets.SGA_APP_PASSWORD }}
SGA_APP_PASSWORD: ${{ secrets.SGA_APP_PASSWORD }}
```

El rol `sga_app` en CI utiliza exclusivamente su propio secreto. Si `SGA_APP_PASSWORD` no estuviera configurado, el valor resultaría vacío y las pruebas fallarán de forma visible: el comportamiento es **fail-closed**, sin degradación silenciosa hacia credenciales de administración.

Se conservan deliberadamente `FLYWAY_USER: postgres` y `FLYWAY_PASSWORD: ${{ secrets.DB_PASSWORD }}` en ese mismo bloque: corresponden al rol administrador que ejecuta las migraciones, que es una responsabilidad distinta y no puede delegarse en el rol restringido (las migraciones crean esquemas y objetos). La separación es la que ya opera en producción y en el flujo efímero E10, donde `SGA_APP_PASSWORD` se genera con `openssl rand -hex 32` y `DB_PASSWORD` del contenedor toma ese mismo valor efímero, jamás la contraseña del superusuario.

### 4.2 Observación 2 — Procedimiento versionado de rotación de contraseñas

La observación es correcta en su diagnóstico: `V18` envía el aprovisionamiento en

```sql
IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'sga_app') THEN
    CREATE ROLE sga_app WITH LOGIN PASSWORD '${sga_app_password}' ...;
END IF;
```

Si el rol ya existía, el `CREATE ROLE` no se ejecuta y la contraseña vigente permanece sin cambios. Esta cláusula es necesaria para la idempotencia de la cadena de migraciones (una edición de `V18` quedaría fuera de la política de estabilidad descrita en la observación 4.1 de este informe) y, por tanto, la rotación queda formalizada como operación administrativa:

- **Script versionado:** `scripts/rotar_password_sga_app.sh`
  - `set -euo pipefail`, control de parámetros (host, puerto numérico, base, rol administrador, longitud mínima de 12 caracteres, banderas desconocidas rechazadas con código 2).
  - Ejecuta como administrador `ALTER ROLE sga_app WITH PASSWORD '<nueva>'`, con escapado SQL de comillas simples.
  - Verifica de inmediato autenticando como `sga_app` con la credencial nueva: `current_user`, `rolsuper = false`, `INSERT` habilitado y `UPDATE`/`DELETE`/`TRUNCATE` denegados sobre `sga_principal.auditoria`, sin emitir sentencias de escritura contra la bitácora.
  - Limpia las variables sensibles de la sesión mediante `trap` al finalizar.
  - Códigos de salida: `0` correcto, `1` fallo de conexión o verificación, `2` parámetros inválidos.
- **Procedimiento operativo estándar (SOP):** `docs/db/ROTACION_SGA_APP.md`
  - Justificación técnica del `IF NOT EXISTS`, prerrequisitos, pasos A (rotación), B (actualización de `.env`, Compose, AWS Secrets Manager / Parameter Store y GitHub Actions Secrets) y C (validación del arranque con `RestriccionBitacoraValidator` en los logs de Spring Boot), plantilla de registro de rotación sin valores secretos y restricciones vigentes.

La rotación **no** modifica ninguna migración ni su suma de comprobación: `ALTER ROLE` es una operación sobre el catálogo de roles, ajena al historial de Flyway.

### 4.3 Observación 3 — `GRANT ALL PRIVILEGES` sobre `sga_secretaria` en `V23`

Contenido actual de la migración:

```sql
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA sga_secretaria TO sga_app;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA sga_secretaria TO sga_app;
GRANT INSERT ON sga_principal.auditoria TO sga_app;
```

**Por qué no se edita retroactivamente `V23`.** La estabilidad de la cadena de migraciones fue aprobada y congelada en el Punto 5 (PR #191): `flyway_schema_history` almacena la suma MD5 de cada migración aplicada y el arranque ejecuta `validate` de forma estricta por omisión, sin `repair()`, sin `FlywayMigrationStrategy` y sin `validate-on-migrate=false`. Modificar el texto de `V23` provocaría `checksum mismatch` en producción y en cualquier base ya migrada, impidiendo el arranque; la prohibición de editar `V8..V26` está registrada en `docs/db/RECONSTRUCCION_PRODUCCION.md`. Por política de estabilidad de base de datos, las migraciones históricas aplicadas no se alteran retroactivamente: las correcciones de alcance se emiten, cuando proceden, como nuevas versiones superiores.

**Por qué la defensa sobre la bitácora permanece garantizada.** El esquema `sga_secretaria` es estrictamente operativo: alberga las tablas del microservicio Secretaría y **no** contiene la bitácora institucional, que reside exclusivamente en `sga_principal.auditoria`. Un `GRANT ALL PRIVILEGES` limitado al esquema `sga_secretaria` no modifica en ningún punto los ACL de `sga_principal.auditoria`. Sobre esa tabla, la posición vigente tras la cadena completa es:

| Origen | Efecto sobre `sga_principal.auditoria` |
|---|---|
| `V18` | `GRANT SELECT, INSERT`; `REVOKE UPDATE, DELETE, TRUNCATE` de `sga_app` y de `PUBLIC` |
| `V19` | Reafirma la revocación después de las concesiones generales de los cuatro esquemas |
| `V23` | Única concesión puntual: `GRANT INSERT` (append-only) |
| `V24` | `REVOKE UPDATE, DELETE, TRUNCATE ... FROM sga_app` como última palabra de la cadena |
| `V13` / `V21` | Disparadores de fila y de sentencia que rechazan con `P0001` |
| Arranque | `RestriccionBitacoraValidator` aborta el servicio ante cualquier privilegio de modificación |

Es decir, aun el único `GRANT` de `V23` sobre la bitácora es de `INSERT`, coherente con el modelo *append-only*, y la revocación definitiva la emite `V24`, que sí es posterior en la cadena. La defensa en profundidad —disparadores, propiedad de la tabla, revocación de ACL, ausencia de superusuario y compuerta de arranque— permanece sobre `sga_principal.auditoria` y es verificada en cada inicio del servicio y en cada ejecución de las pruebas de integración.

### 4.4 Observación 4 — Trazabilidad de E10 frente a producción

| Entorno | Mecanismo verificado | Evidencia |
|---|---|---|
| **Pruebas de integración en contenedor (E13/E4/E6)** | Carga del baseline oficial `V8__baseline_completo.sql` en PostgreSQL efímero y pipeline real de Flyway V9→V26; rol `sga_app` creado por `V18`; rechazos `42501` y `P0001` exigidos por aserción | `AuditoriaFlywayMigrationContainerTest.java`; informe Surefire versionado en `docs/evidencia/pruebas/sga-principal/` (`tests="2"`, `failures="0"`) |
| **Integración continua (E10)** | Contenedores efímeros con `SGA_APP_PASSWORD` generado por `openssl rand -hex 32`, transmitido como `DB_PASSWORD` del contenedor de la aplicación, y rol de migraciones separado (`FLYWAY_USER=postgres` con credencial distinta); desde este punto, el job `test-backend` consume únicamente `secrets.SGA_APP_PASSWORD` | `.github/workflows/ci-cd.yml` (job `e2e-docente` y job `test-backend`) |
| **Despliegue real en AWS** | Servicios operando con el rol restringido `sga_app`, Flyway y el validador de bitácora activos | Ejecución de GitHub Actions **`35678671785`** en verde (`BUILD SUCCESS`, servicios Principal y Secretaría sanos); logs del arranque: `Successfully validated 19 migrations`, `Schema "sga_principal" is up to date. No migration necessary.`, `RestriccionBitacoraValidator : Verificado: usuario sga_app no puede modificar la bitacora`, `Started SgaPrincipalApplication`, salud Actuator `UP` en las instancias `sga-principal-1` y `sga-principal-2` |

Conclusión de trazabilidad: la restricción del rol y la cadena de Flyway están formalmente acreditadas tanto en el arranque de producción de AWS (ejecución `35678671785`, registrada en `docs/db/RECONSTRUCCION_PRODUCCION.md`) como en las pruebas de integración en contenedor que reproducen exactamente la misma línea base y el mismo pipeline de migraciones.

---

## 5. Archivos afectados por este punto

| Archivo | Naturaleza |
|---|---|
| `.github/workflows/ci-cd.yml` | Modificado: eliminación del fallback `|| secrets.DB_PASSWORD` en `DB_PASSWORD` y `SGA_APP_PASSWORD` del job `test-backend` |
| `scripts/rotar_password_sga_app.sh` | Nuevo: script de rotación con verificación inmediata |
| `docs/db/ROTACION_SGA_APP.md` | Nuevo: procedimiento operativo estándar de rotación |
| `evidencias/Pedro_Castro/Punto_45_Restriccion_Privilegios/README.md` | Nuevo: este informe |

**Sin migraciones modificadas:** no se alteró, editó ni renombró ningún archivo de `sga-principal/src/main/resources/db/migration/`. La cadena `V8..V26` permanece congelada conforme al Punto 5.

## 6. Reproducción de las verificaciones

```bash
# Pruebas unitarias del validador de arranque (3 pruebas)
cd sga-principal
./mvnw test "-Dtest=RestriccionBitacoraValidatorTest" "-Djacoco.skip=true"

# Pruebas de integración del pipeline real de migraciones (requiere Docker)
./mvnw test "-Dtest=AuditoriaFlywayMigrationContainerTest" "-Dapi.version=1.44" "-Djacoco.skip=true"

# Rotación de la credencial del rol de aplicación (entorno con acceso al motor)
NUEVA_PASSWORD='<valor-generado-externamente>' DB_HOST='<host>' \
  bash scripts/rotar_password_sga_app.sh
```
