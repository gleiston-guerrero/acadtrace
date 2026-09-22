# Procedimiento Operativo Estándar: rotación de la contraseña del rol `sga_app`

**Punto de entrega:** 45 — Restricción de privilegios de modificación sobre la bitácora
**Responsable:** Pedro Castro (LEO23as)
**Fecha de emisión:** 2026-09-22
**Rama de referencia:** `Leonardo-Castro`
**Clasificación:** documento operativo sin credenciales. Ningún valor secreto se versiona en este archivo ni en el script asociado.

---

## 1. Necesidad técnica

La migración institucional `V18__rol_aplicacion_y_revocacion_auditoria.sql` aprovisiona el rol de aplicación con una cláusula condicional idempotente:

```sql
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'sga_app') THEN
        CREATE ROLE sga_app WITH LOGIN PASSWORD '${sga_app_password}' NOSUPERUSER NOCREATEDB NOCREATEROLE;
    END IF;
END $$;
```

Este diseño es deliberado y correcto para su propósito: garantiza que la cadena de migraciones sea determinista tanto sobre una base nueva como sobre una base existente, sin fallar con «rol ya existe». La consecuencia operativa es que, **si el rol `sga_app` ya existía en el motor, `V18` no actualiza su contraseña**: el `CREATE ROLE` no se ejecuta y la credencial vigente permanece intacta.

Por tanto, la rotación periódica de credenciales —y cualquier remediación de una contraseña comprometida, fuga o vencimiento de política— requiere una **operación administrativa explícita, versionada y verificable** fuera del pipeline de migraciones. Ese rol lo cubre este procedimiento y su script asociado `scripts/rotar_password_sga_app.sh`.

> Rotar la contraseña **no** modifica ninguna migración ni su suma de comprobación: `ALTER ROLE` es una operación sobre el catálogo de roles, ajena al historial de Flyway. La cadena `V8..V26` permanece congelada conforme al Punto 5.

## 2. Separación de roles vigente

| Rol | Origen | Uso | Credencial |
|---|---|---|---|
| `sga_app` | Migración `V18` (`NOSUPERUSER NOCREATEDB NOCREATEROLE`) | Conexión de aplicación de los microservicios | `SGA_APP_PASSWORD` / `DB_PASSWORD` (variables de entorno) |
| `postgres` | Rol propietario del motor | Migraciones Flyway y administración | `FLYWAY_USER` / `FLYWAY_PASSWORD` (variables de entorno) |

La rotación tratada aquí afecta **exclusivamente** a `sga_app`. El rol administrador utilizado por Flyway no se modifica, de modo que el arranque del servicio y la validación del historial de migraciones no se ven alterados.

En `application.properties`, `spring.flyway.user` y `spring.flyway.password` toman como valor por defecto `spring.datasource.username` y `spring.datasource.password`. La configuración de despliegue **debe mantener `FLYWAY_USER=postgres`** de forma explícita: si se omitiera, Flyway intentaría migrar con el rol de aplicación, que carece de privilegios de creación de esquemas y objetos.

## 3. Prerrequisitos

- Cliente `psql` disponible en el PATH de la estación u host que ejecuta la operación.
- Conectividad de red hacia el motor (`DB_HOST`, `DB_PORT`) y credenciales de un rol con privilegio de administración de roles (`postgres` por defecto).
- Nueva contraseña generada conforme a la política institucional: mínimo 12 caracteres; no reutilizar la anterior; no proceder de listas públicas ni de valores presentes en el repositorio.
- Ventana de mantenimiento comunicada (la verificación final exige reinicio de servicios).
- Registro previo de la rotación conforme a la plantilla de la sección 8.

## 4. Paso A — Rotación en el motor PostgreSQL

Ejecutar desde la raíz del repositorio:

```bash
NUEVA_PASSWORD='<valor-generado-externamente>' \
DB_HOST='<host-del-motor>' \
DB_PORT=5433 \
DB_NAME=sga \
DB_ADMIN_USER=postgres \
bash scripts/rotar_password_sga_app.sh
```

También puede suministrarse la contraseña mediante la bandera `--nueva-password` o `NUEVA_PASSWORD` exportada en la sesión. El script:

1. Valida parámetros (host, puerto numérico, base, longitud mínima de la contraseña) y la presencia de `psql`.
2. Comprueba la conexión administrativa y la existencia del rol `sga_app`.
3. Ejecuta como administrador:

   ```sql
   ALTER ROLE sga_app WITH PASSWORD '<nueva-valor>';
   ```

4. Realiza de inmediato una **verificación de conformidad autenticando con la nueva contraseña como `sga_app`**, compuesta por:

   | Verificación | Condición exigida |
   |---|---|
   | `SELECT current_user` | `sga_app` (autenticación correcta) |
   | `SELECT rolsuper FROM pg_roles WHERE rolname = current_user` | `f` (sin superusuario) |
   | `has_table_privilege(..., 'sga_principal.auditoria', 'INSERT')` | `t` (append-only) |
   | `has_table_privilege(..., 'sga_principal.auditoria', 'UPDATE')` | `f` |
   | `has_table_privilege(..., 'sga_principal.auditoria', 'DELETE')` | `f` |
   | `has_table_privilege(..., 'sga_principal.auditoria', 'TRUNCATE')` | `f` |

   La verificación se apoya únicamente en consultas de catálogo: **no se envía ninguna sentencia de escritura contra `sga_principal.auditoria`**.
5. Limpia las variables sensibles de la sesión (`trap` de salida) y retorna el código de estado.

Códigos de salida: `0` rotación y verificación correctas; `1` fallo de conexión o de verificación; `2` parámetros inválidos. Ante cualquier código distinto de `0`, **no** continuar con los pasos B y C y escalar al responsable del servicio.

> Nota sobre comillas simples: el script aplica el escapado SQL estándar (`'` → `''`) antes de construir el literal, por lo que la contraseña puede contener comillas simples.

## 5. Paso B — Actualización de variables de entorno

La contraseña debe actualizarse en **todos** los puntos de suministro antes de reiniciar. En ningún caso se registra el valor en el repositorio.

| Punto de suministro | Acción |
|---|---|
| `.env` de la instancia (host de despliegue) | Sustituir `SGA_APP_PASSWORD` y `DB_PASSWORD` en el archivo externo (no versionado, excluido por `.gitignore`). |
| Docker Compose (raíz y `sga-principal/docker-compose.yml`) | El Compose transmite `SGA_APP_PASSWORD` al contenedor de Principal y rechaza su ausencia o valor vacío; regenerar el secreto de runtime correspondiente. |
| AWS Secrets Manager / Systems Manager Parameter Store | Actualizar la entrada del secreto del rol de aplicación (donde esté configurado el suministro) y propagarla a los consumidores. |
| GitHub Actions — Secrets del repositorio | Actualizar el secreto `SGA_APP_PASSWORD`. Desde el saneamiento del Punto 45, el job `test-backend` de `.github/workflows/ci-cd.yml` consume **exclusivamente** `secrets.SGA_APP_PASSWORD` para `DB_PASSWORD` y `SGA_APP_PASSWORD`, sin respaldo sobre `secrets.DB_PASSWORD` (rol administrador). |

`FLYWAY_USER` y `FLYWAY_PASSWORD` **no** se modifican en esta operación.

Tras actualizar los suministros, reiniciar las instancias de la aplicación para que el pool de conexiones adquiera la nueva credencial.

## 6. Paso C — Validación del arranque

En el arranque del servicio, `RestriccionBitacoraValidator`
(`sga-principal/src/main/java/ec/edu/uteq/sga/infrastructure/config/RestriccionBitacoraValidator.java`)
ejecuta su comprobación en la fase de inicialización del contexto de Spring (`@PostConstruct` del componente), dentro de la secuencia de arranque previa a `ApplicationReadyEvent`. El componente falla deliberadamente el arranque si:

- el usuario efectivo resulta superusuario, o
- dispone de cualquiera de los privilegios `UPDATE`, `DELETE` o `TRUNCATE` sobre `sga_principal.auditoria`.

Verificar en los logs de Spring Boot del arranque:

```
RestriccionBitacoraValidator : Verificado: usuario sga_app no puede modificar la bitacora
Successfully validated N migrations
Started SgaPrincipalApplication
```

Comprobaciones complementarias:

1. Cero ocurrencias de `checksum mismatch` en el log (la rotación no altera migraciones).
2. `Schema "sga_principal" is up to date. No migration necessary.`
3. Salud del servicio en Actuator: `UP`.
4. Prueba unitaria del validador: `RestriccionBitacoraValidatorTest` — 3 pruebas en verde.

Si el validador no emite la línea de verificación o el servicio no inicia, reversar la operación restaurando la credencial anterior en los mismos puntos de suministro y escalar.

## 7. Verificación de la restricción sobre la bitácora

La comprobación de la sección 4 acredita la restricción de privilegios en el mismo momento de la rotación. De forma periódica, puede repetirse con la misma lógica (conexión como `sga_app` y consultas de `has_table_privilege` sobre `sga_principal.auditoria`), sin emitir escrituras contra la bitácora.

## 8. Registro de rotación

Registrar cada rotación **sin incluir valores secretos**, conforme a lo exigido por `docs/seguridad/gestion_rotacion_secretos.md`:

| Campo | Contenido |
|---|---|
| Fecha y hora (UTC-5) | |
| Sistema / cuenta | PostgreSQL — rol `sga_app` |
| Base y endpoint | `sga` @ `DB_HOST:DB_PORT` (identificador no sensible) |
| Responsable | |
| Método | `scripts/rotar_password_sga_app.sh` |
| Resultado de verificación | código de salida, autenticación, `rolsuper`, privilegios |
| Suministros actualizados | `.env`, Compose, Secrets Manager/Parameter Store, GitHub Actions |
| Reinicio y log de arranque | presencia de la línea de `RestriccionBitacoraValidator` |

## 9. Restricciones vigentes durante el procedimiento

- No modificar, editar ni renombrar ningún archivo de `sga-principal/src/main/resources/db/migration/` (cadena `V8..V26` congelada por el Punto 5; cualquier edición provoca `checksum mismatch`).
- No ejecutar `flyway repair` ni desactivar la validación (`validate-on-migrate=false`).
- No registrar credenciales en el repositorio, en commits, en este documento ni en los registros de rotación.
- No usar el rol `sga_app` para operaciones administrativas ni de migración.
- No emitir sentencias de escritura (`UPDATE`, `DELETE`, `TRUNCATE`) contra `sga_principal.auditoria` en ningún contexto operativo.

## 10. Referencias

- Migración de aprovisionamiento: `sga-principal/src/main/resources/db/migration/V18__rol_aplicacion_y_revocacion_auditoria.sql`
- Reafirmación de la restricción: `V19__ampliar_permisos_sga_app.sql`, `V23__grants_sga_secretaria_y_auditoria_diferidos.sql`, `V24__acotar_revocacion_bitacora.sql`
- Validador de arranque: `sga-principal/src/main/java/ec/edu/uteq/sga/infrastructure/config/RestriccionBitacoraValidator.java`
- Script de rotación: `scripts/rotar_password_sga_app.sh`
- Reconstrucción y arranque de producción: `docs/db/RECONSTRUCCION_PRODUCCION.md`
- Gestión de secretos: `docs/seguridad/gestion_rotacion_secretos.md`
- Plantilla de variables: `.env.example`
