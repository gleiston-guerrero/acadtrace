# Reconstrucción de la base de producción con migraciones oficiales (punto 5)

Fecha del procedimiento: 2026-09-21. Responsable de ejecución: por designar.
Rama de referencia: `Leonardo-Castro` (commit con V8/V19 en checksums de `92f2ec91` + V25/V26).

## Por qué este procedimiento

La base de producción (`192.0.2.1:5433/sga`) tiene aplicadas versiones de V8
y V19 distintas de las oficiales (`-292896054` / `-1469650242` en vez de
`-107706312` / `-554118186` de `92f2ec91`). Todo arranque con el código
entregado falla con `checksum mismatch` antes de migrar (E10, despliegue).
La guía del punto 5 prohíbe `repair()`, `FlywayMigrationStrategy` y
`validate-on-migrate=false`. Por tanto el historial existente NO se parcha:
la base se reconstruye desde cero con las migraciones oficiales y todo el
proceso queda registrado públicamente aquí.

## Requisitos previos

- Acceso SSH al host de producción y credenciales de rol `postgres`
  (administración) y `sga_app` (aplicación). No se versionan en este repo.
- Respaldo lógico completo de la base actual (solo como resguardo; los datos
  académicos vigentes se recargan desde las fuentes oficiales del equipo,
  no desde el dump divergente).
- Código en el commit de referencia, con V8..V26 y validación activa.

## Pasos (orden estricto, sin atajos)

1. Respaldar la base actual:
   `pg_dump -h 192.0.2.1 -p 5433 -U postgres sga > respaldo_previo_rebuild_YYYYMMDD.sql`
   y conservar su SHA-256 en el acta de la sección Registro.
2. Crear la base nueva vacía (mismo nombre tras renombrar la anterior, o
   nombre temporal para conmutar al final):
   `createdb -h 192.0.2.1 -p 5433 -U postgres sga_rebuild`
3. Arrancar `sga-principal` del commit de referencia contra `sga_rebuild`
   con la configuración productiva (sin `validate-on-migrate=false`,
   sin `repair`, sin `FlywayMigrationStrategy`).
4. Verificar en el log, en este orden:
   - `Migrating schema "sga_principal" to version "8 - baseline completo"`
     hasta `"26 - objetos faltantes codigo"`,
   - `Successfully applied 18 migrations`,
   - `RestriccionBitacoraValidator : Verificado: usuario sga_app no puede
     modificar la bitacora`,
   - `Started SgaPrincipalApplication`,
   - cero ocurrencias de `checksum mismatch` y de `repair`.
5. Segundo arranque contra la misma base: debe registrar
   `Successfully validated 18 migrations` y `is up to date. No migration
   necessary.`
6. Recargar los datos académicos vigentes desde las fuentes oficiales y
   reejecutar las comprobaciones del punto 5 (tablas JPA completas, vistas
   actualizables, `lamport_ts` sin `bad SQL grammar` en Secretaría).
7. Conmutar el tráfico a la base reconstruida y reejecutar el pipeline del
   commit entregado hasta verlo en verde (incluido E10).
8. Publicar en esta carpeta el acta con los hashes y los dos logs
   (`arranque_rebuild_nueva.log`, `arranque_rebuild_validada.log`).

## Registro público de ejecución (rellenar al ejecutar)

- Fecha/hora (UTC-5):
- Ejecutado por:
- Commit desplegado:
- SHA-256 del respaldo previo:
- SHA-256 de `arranque_rebuild_nueva.log`:
- SHA-256 de `arranque_rebuild_validada.log`:
- Resultado del pipeline del commit entregado (enlace a la ejecución):

## Prohibiciones vigentes durante todo el procedimiento

- No ejecutar `flyway repair` ni `migrate` con validación desactivada.
- No editar V8..V26 para "hacerlos calzar" con el historial anterior.
- No borrar `.github/workflows/diagnostico-flyway.yml`; usarlo solo como
  consulta de solo lectura del historial.
