# Evidencias de aplicación en release

## Objetivo

Esta carpeta documenta la trazabilidad de las capturas funcionales y de observabilidad asociadas a AcadTrace. El objetivo es distinguir evidencia visual existente, evidencia histórica y capturas que todavía deben obtenerse desde las aplicaciones reales.

Las capturas no sustituyen la validación funcional ni constituyen por sí solas evidencia de un release final.

## Ubicación canónica

La ubicación canónica de las capturas existentes es:

```text
release/screenshots/
```

El archivo [manifest.yml](manifest.yml) relaciona cada captura con su componente, propósito, SHA-256, commit de incorporación, fecha, estado y uso en el informe cuando esos datos pueden comprobarse.

Estados usados:

- `current`: corresponde al estado actual demostrado y asociado a una versión identificable.
- `historical`: evidencia conservada de una ejecución o estado anterior.
- `needs_replacement`: el archivo existe, pero su contenido no sirve para demostrar la interfaz indicada.
- `unverified`: existe evidencia visual, pero no puede vincularse de forma concluyente al release final actual.

## Release versionado

El tag encontrado actualmente es `pre-e4`. No existe un tag final confirmado para las capturas o el APK. Por tanto, `pre-e4` no debe interpretarse como el release final de AcadTrace.

Las capturas móviles y el APK fueron incorporados en el commit `03216202` el 2026-09-04. La documentación de la demo móvil indica que el APK instalado era anterior a cambios locales posteriores, por lo que esas capturas permanecen como `unverified`.

Las capturas de Grafana y Locust se conservan como evidencia histórica. El informe también las describe como históricas o complementarias.

## Verificar SHA-256

Desde la raíz del repositorio, en PowerShell:

```powershell
Get-FileHash release/screenshots/sga_principal.png -Algorithm SHA256
Get-FileHash release/screenshots/secretaria_portal.png -Algorithm SHA256
Get-FileHash release/screenshots/soporte_tickets.png -Algorithm SHA256
```

El resultado debe compararse con el campo `sha256` correspondiente de [manifest.yml](manifest.yml). La incorporación histórica de cada archivo puede consultarse con:

```powershell
git log --all --diff-filter=A --format="%H|%ad|%s" --date=short -- release/screenshots/<archivo>.png
```

## Capturas actualizadas

### SGA Principal

- archivo: `release/screenshots/sga_principal.png`
- estado: `sga_principal.png` es una captura actual que corresponde al dashboard real de SGA Principal, obtenida en entorno local de demostración. Su trazabilidad está registrada en [manifest.yml](manifest.yml).

### Secretaría

- archivo: `release/screenshots/secretaria_portal.png`
- estado: captura actual del dashboard de Secretaría. Su trazabilidad está registrada en [manifest.yml](manifest.yml).

### Soporte

- archivo: `release/screenshots/soporte_tickets.png`
- estado: captura actual del Módulo de Tickets/Kanban. Su trazabilidad está registrada en [manifest.yml](manifest.yml).

### IA

- archivo: `release/screenshots/microservicio_ia.png`
- estado: captura actual de Swagger/OpenAPI del microservicio IA. Su trazabilidad está registrada en [manifest.yml](manifest.yml).

## Capturas pendientes de actualización

No quedan pendientes de actualización entre las capturas de Secretaría, Soporte e IA.

Docente y móvil también requieren confirmación contra el release final antes de considerarse evidencia definitiva. La evidencia E10 de Docente se conserva en evidencias/Bedon/microservicio-docente/04-E10-e2e-navegador/ y contiene seis capturas generadas automáticamente por Playwright junto con el resultado JUnit de una ejecución satisfactoria.
