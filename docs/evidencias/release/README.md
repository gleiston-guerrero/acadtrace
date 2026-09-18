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
- `no_verificado`: existe evidencia visual, pero no puede vincularse de forma concluyente al release final actual.

## Release versionado

El tag confirmado del release final es `v1.0.1`, publicado en la punta de main. Las capturas y el APK versionado corresponden a ese release.

Las capturas móviles y el APK fueron incorporados en el commit `03216202` el 2026-09-04. La documentación de la demo móvil indica que el APK instalado era anterior a cambios locales posteriores, por lo que esas capturas ahora se marcan como `verified` tras su reemplazo real.

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
- estado: captura real y actual del dashboard de Secretaría. Su trazabilidad está registrada en [manifest.yml](manifest.yml) y está integrada en el manuscrito; la copia utilizada por el informe está en `Informe-E4_BCEL/secretaria_portal.png`.

### Soporte

- archivo: `release/screenshots/soporte_tickets.png`
- estado: captura real y actual del portal de Soporte (Módulo de Tickets/tablero Kanban). Su trazabilidad está registrada en [manifest.yml](manifest.yml) y está integrada en el manuscrito; la copia utilizada por el informe está en `Informe-E4_BCEL/soporte_tickets.png`.

### IA

- archivo: `release/screenshots/microservicio_ia.png`
- estado: captura actual de Swagger/OpenAPI del microservicio IA. Su trazabilidad está registrada en [manifest.yml](manifest.yml).

## Capturas pendientes de actualización

No quedan pendientes de actualización entre las capturas de Secretaría, Soporte e IA.

La evidencia E10 de Docente se conserva en evidencias/Bedon/microservicio-docente/04-E10-e2e-navegador/ y contiene seis capturas generadas automáticamente por Playwright junto con el resultado JUnit de una ejecución satisfactoria.
