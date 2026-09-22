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
- Flyway los aplica solo, en orden, al arrancar la aplicacion.
- Con 2 replicas de sga-principal arrancando a la vez, Flyway usa un lock
  a nivel de Postgres para que solo una aplique las migraciones pendientes.

## Arranque desde base nueva

Para levantar la base desde un clon limpio y llegar al esquema completo:

1. `createdb sga`
2. `cd sga-principal && ./mvnw spring-boot:run`
3. Verificar que Flyway aplica la línea base `V8__baseline_completo.sql` y
   luego V9 hasta V26 automáticamente. La aplicación debe arrancar sin
   errores escuchando en su puerto.

Flyway lee las migraciones desde `spring.flyway.locations=classpath:db/migration`.
La versión V8 carga el esquema inicial completo, y las siguientes versiones aplican
las modificaciones progresivas.

## Arranque sobre base ya migrada

Para una base migrada hasta V24 (o cualquier estado intermedio con checksums
correctos), el proceso de arranque es idéntico:

1. `cd sga-principal && ./mvnw spring-boot:run`

La validación por omisión de Flyway (`spring.flyway.validate-on-migrate=true`)
se ejecutará correctamente sin necesidad de mecanismos de bypass ni `repair()`.
Flyway validará las migraciones existentes y aplicará automáticamente solo las
nuevas (V25 y V26). Bases que contengan checksums divergentes o estados no
oficiales (como los generados temporalmente por `7898744a` o anteriores al
13/09/2026) se normalizan mediante el procedimiento oficial documentado en
`docs/db/RECONSTRUCCION_PRODUCCION.md`, inicializando con la línea base oficial
V8 y aplicando ordenadamente V9 hasta V26 sin anular la validación ni alterar
historiales pasados.

### Corrección de V19 y creación de V23

Anteriormente se editó la migración V19 retroactivamente, lo que alteraba su
suma de comprobación e impedía la validación en bases ya migradas. Para solucionar
esto sin recurrir a `repair()`, se restauró V19 a su estado original (commit `186593cb`)
y las adiciones se colocaron en una nueva migración `V23__grants_sga_secretaria_y_auditoria_diferidos.sql`.

Constancia de arranque validado en base nueva y base migrada:
`evidencias/Pedro_Castro/Punto_05_Esquema_y_Migraciones/arranque_base_nueva.log`
`evidencias/Pedro_Castro/Punto_05_Esquema_y_Migraciones/arranque_base_migrada.log`

## V20 y V22 — marca histórica de reparación de V18

V20 y V22 mencionan un `FlywayConfig.repairAntesDeMigrar()` que ejecutaba
`flyway.repair()` antes de `flyway.migrate()`. Ese componente ya no existe en
`src/main` desde el commit `13db4bbe`. La ausencia es intencional: la guía del
punto 5 exige validación activa sin bypass ni `repair()`; no restaurar ese
archivo. V20/V22 se conservan sin editar como marcas versionadas de auditoría.

## V25 y V26 — cierre del punto 5

V8 y V19 volvieron a editarse después de aplicadas (V8 con 165 líneas nuevas,
V19 con un comentario), lo que rompía el checksum en bases ya migradas. Se
restauraron ambas al estado oficial de `92f2ec91` y los cambios se trasladaron
hacia adelante:

- `V25__delta_v8_tablas_vistas.sql`: vistas `sga_principal.estudiantes`,
  `matriculas` y `fichas_estudiante` sobre `sga_secretaria`, tablas
  `tipos_aporte`, `periodos_horario`, `notificaciones`, `malla_curricular`,
  `esquema_calificacion`, `periodos_evaluacion`, `escala_calificaciones`,
  `dispositivos_representante`, `eventos_notificacion_push`,
  `estado_cadena_auditoria` y columnas sobre tablas existentes.
- `V26__objetos_faltantes_codigo.sql`: `sga_principal.historial_promocion`
  (con `lamport_ts`), `eventos_academicos`, vistas `sga_secretaria.grados` y
  `paralelos`, `sga_soporte.historial_ticket` y la función
  `sga_principal.fn_horario_no_choque` con su trigger.

Constancias de arranque regeneradas: Los archivos versionados en
`evidencias/Pedro_Castro/Punto_05_Esquema_y_Migraciones/` (`arranque_base_nueva.log`
y `arranque_base_migrada.log`) provienen de ejecuciones reales de `sga-principal`
sobre PostgreSQL 16 con validación activa (`validate-on-migrate=true`) y sin `repair()`.
Acreditan que la base nueva aplica limpiamente las migraciones hasta V26 (y valida
19 en el segundo arranque), y que la base migrada desde V24 valida las existentes
sin mismatch y aplica únicamente V25 y V26.

## Nota sobre la NOTA de V19

La revocación por omisión amplia sobre `sga_principal` introducida en V19
quedó acotada en `V24__acotar_revocacion_bitacora.sql`, que restablece los
privilegios por omisión y deja el REVOKE únicamente sobre `auditoria`. Esta
relación queda documentada aquí y NO dentro de `V19.sql`, para preservar su
checksum original de `92f2ec91`.
