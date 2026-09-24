# E46 — Evidencia sanitizada de rotación de cuentas

Fecha: 2026-09-23.

Este documento registra únicamente cantidades y estados no sensibles. No contiene contraseñas, hashes bcrypt, tokens ni secretos.

## Cuenta usada históricamente por el script operativo

- La cuenta histórica continúa existiendo en producción.
- Rol actual comprobado: `ROLE_REPRESENTANTE`.
- Estado: activa.
- La credencial histórica fue invalidada mediante una credencial aleatoria no almacenada.
- Estado posterior: `primer_ingreso=true`, `intentos_fallidos=0`, sin bloqueo.
- No se modificaron el rol ni el estado de la cuenta.

## Hashes históricos

- Hashes bcrypt históricos únicos identificados: 21.
- Cuentas actuales revisadas: 36.
- Cuentas con hash histórico vigente: 19.
- Cuentas activas afectadas: 19.
- Cuentas afectadas con correo: 19.
- Cuentas afectadas con acceso previo: 0.
- Distribución: 3 `ROLE_REPRESENTANTE` y 16 `SIN_ROL`.

## Rotación

Las 19 coincidencias fueron sustituidas por hashes nuevos derivados de credenciales aleatorias independientes. Las credenciales planas no fueron mostradas ni almacenadas.

Resultado:

```text
CUENTAS_A_ROTAR=19
CUENTAS_ROTADAS=19
HASHES_HISTORICOS_AUN_VIGENTES=0
ROTACION_HASHES_E46_OK
```

Las cuentas quedaron con `primer_ingreso=true`, `intentos_fallidos=0` y sin bloqueo.

Esta evidencia acredita la invalidación de las credenciales vigentes. El historial Git todavía requiere saneamiento.
