# Migraciones historicas (pre-Flyway)

Estos scripts (V2 a V8) se aplicaron **a mano** contra la base compartida
antes de integrar Flyway al proyecto. Se quedan aqui como referencia
historica, pero **no se ejecutan automaticamente** y no hace falta tocarlos.

Desde Flyway (baseline en V8), las migraciones nuevas van en
`src/main/resources/db/migration/` con el formato `V9__descripcion.sql`,
`V10__descripcion.sql`, etc., y se aplican solas al arrancar la aplicacion.

Nota: hay dos archivos `V5__` (`V5__esquema_sga_secretaria.sql` y
`V5__notificaciones.sql`) porque en su momento se numeraron a mano sin
verificar duplicados. No afecta a Flyway porque esta carpeta esta fuera de
su alcance (`spring.flyway.locations` apunta solo a `db/migration`).
