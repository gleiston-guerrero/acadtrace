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
