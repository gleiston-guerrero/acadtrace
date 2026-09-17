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
