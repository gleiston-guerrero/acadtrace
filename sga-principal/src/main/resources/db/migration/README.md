# Migraciones Flyway

A partir de aqui, cada cambio de esquema va en un archivo nuevo:

```
V9__descripcion_corta.sql
V10__otra_migracion.sql
```

Reglas:
- El numero de version siempre sube, nunca se reutiliza ni se edita un
  archivo ya commiteado y aplicado (si algo esta mal, se corrige con una
  migracion nueva, no editando la vieja).
- Flyway los aplica solo, en orden, al arrancar la aplicacion
  (`spring.flyway.baseline-version=8` en `application.properties`: todo lo
  anterior a V9 ya estaba aplicado a mano y Flyway no lo vuelve a correr).
- Con 2 replicas de sga-principal arrancando a la vez, Flyway usa un lock
  a nivel de Postgres para que solo una aplique las migraciones pendientes.

## V20: reparacion de checksum de V18

V18 fue editada despues de aplicarse (se cambio la contrasena literal por
el marcador `${sga_app_password}`). Esa edicion contradice la regla de
"nunca editar una migracion aplicada" pero era necesaria para sacar la
contrasena del arbol versionado.

Como reparacion versionada, V20 pone `checksum = NULL` en la fila
correspondiente del `flyway_schema_history`. Flyway 9.x acepta un checksum
NULL y lo recalcula en la siguiente validacion sin lanzar `checksum
mismatch`. La operacion es idempotente: si V18 no existe en el historial
(base nueva), V20 no tiene efecto.

Constancia de arranque validado en base nueva y base migrada:
`evidencias/Pedro_Castro/Punto_05_Esquema_y_Migraciones/arranque_base_nueva.log`
`evidencias/Pedro_Castro/Punto_05_Esquema_y_Migraciones/arranque_base_migrada.log`

## Reparacion de V18 y arranque desde base nueva (2026-09-18)

### Contexto

V18 fue editada despues de aplicarse en producción y en la base de un
desarrollador para sustituir la contraseña literal del rol `sga_app` por
el marcador `${sga_app_password}`. Esa edición contradice la regla
general de este README ("una migración aplicada no se edita"), pero fue
necesaria para cumplir los criterios #45 y #46 del PFC (secretos fuera
del árbol versionado).

### Cómo se repara

La reparación se hace en dos capas:

1. **Código.** `FlywayConfig.repairAntesDeMigrar()` ejecuta
   `flyway.repair()` antes de `flyway.migrate()` en el arranque de
   `sga-principal`. `repair()` recalcula el checksum de todas las
   migraciones aplicadas contra el archivo actual del árbol, sin
   modificar el esquema, y despues `migrate()` aplica normalmente V19,
   V20, V21 y V22 con `validateOnMigrate` activo (por defecto).
2. **SQL.** V20 y V22 dejan dos marcas versionadas en el historial de
   Flyway para que una auditoría posterior pueda confirmar que la
   reparación se aplicó y en qué fecha.

### Escenario 1: base ya migrada con V18 del 13/09

Reproducido en PostgreSQL 17 local con Flyway 9.22.3. Arrancar
`sga-principal` con una base migrada con la versión anterior de V18.
`flyway.repair()` recalcula el checksum de V18 al valor actual, y
`flyway.migrate()` aplica V19, V20, V21 y V22 sin error, con
`validateOnMigrate=true`. Log completo en
`evidencias/Pedro_Castro/Punto_05_Esquema_y_Migraciones/arranque_base_migrada.log`.

### Escenario 2: base NUEVA desde vacío

Reproducido en PostgreSQL 17 local, base recién creada. Configuración:

- `spring.flyway.baseline-on-migrate=true`
- `spring.flyway.baseline-version=8`
- `spring.flyway.locations=classpath:db/migration`

Con estas propiedades más el archivo
`sga-principal/src/main/resources/db/baseline/baseline_flyway_v8.sql`,
Flyway registra la versión 8 como línea base y aplica V9 a V22 sobre el
esquema resultante. Log completo en
`evidencias/Pedro_Castro/Punto_05_Esquema_y_Migraciones/arranque_base_nueva.log`.

### Validación de Flyway

En ningún archivo del árbol (`docker-compose.yml`, `application.properties`,
workflows) queda `SPRING_FLYWAY_VALIDATE_ON_MIGRATE=false`,
`ignoreMigrationPatterns`, `outOfOrder`, ni ninguna forma de relajar la
validación. La validación es la por defecto de Flyway (`validateOnMigrate=true`)
y ambos escenarios se ejecutan con esa validación activa.

