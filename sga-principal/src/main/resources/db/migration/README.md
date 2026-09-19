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

1. `createdb sga_principal`
2. `cd sga-principal && ./mvnw spring-boot:run`
3. Verificar que Flyway aplica la línea base `V8__baseline_completo.sql` y
   luego V9 hasta V23 automáticamente. La aplicación debe arrancar sin
   errores escuchando en su puerto.

Flyway lee las migraciones desde `spring.flyway.locations=classpath:db/migration`.
La versión V8 carga el esquema inicial completo, y las siguientes versiones aplican
las modificaciones progresivas.

## Arranque sobre base ya migrada

Para una base que ya tenía aplicadas las migraciones hasta V22, el proceso de
arranque es idéntico:

1. `cd sga-principal && ./mvnw spring-boot:run`

La validación por omisión de Flyway (`spring.flyway.validate-on-migrate=true`)
se ejecutará correctamente sin necesidad de mecanismos de bypass ni `repair()`.
Flyway validará las migraciones existentes y aplicará automáticamente las nuevas
(como V23).

### Corrección de V19 y creación de V23

Anteriormente se editó la migración V19 retroactivamente, lo que alteraba su
suma de comprobación e impedía la validación en bases ya migradas. Para solucionar
esto sin recurrir a `repair()`, se restauró V19 a su estado original (commit `186593cb`)
y las adiciones se colocaron en una nueva migración `V23__grants_sga_secretaria_y_auditoria_diferidos.sql`.

Constancia de arranque validado en base nueva y base migrada:
`evidencias/Pedro_Castro/Punto_05_Esquema_y_Migraciones/arranque_base_nueva.log`
`evidencias/Pedro_Castro/Punto_05_Esquema_y_Migraciones/arranque_base_migrada.log`
