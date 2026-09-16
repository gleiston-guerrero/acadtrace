# Diagramas de arquitectura C4 de AcadTrace

Los archivos `.puml` de este directorio son la fuente canónica y regenerable de los tres niveles C4:

- `c4_nivel1_contexto.puml`: contexto de AcadTrace.
- `c4_nivel2_contenedores.puml`: contenedores y despliegue lógico.
- `c4_nivel3_componentes.puml`: componentes de `microservicio-soporte`.

Los SVG publicados en este directorio y las copias PNG/SVG de `Informe-E4_BCEL` son artefactos generados. No se mantienen diagramas Mermaid independientes.

## Herramienta y versiones

- PlantUML `1.2024.6` (`plantuml-1.2024.6.jar`).
- C4-PlantUML `v2.12.0`, incluido por URL versionada en cada fuente `.puml`.
- Java 8 o superior.

## Regeneración

Desde la raíz del repositorio, en PowerShell:

```powershell
$jar = Join-Path $env:TEMP 'plantuml-1.2024.6.jar'
if (-not (Test-Path $jar)) {
    Invoke-WebRequest -Uri 'https://github.com/plantuml/plantuml/releases/download/v1.2024.6/plantuml-1.2024.6.jar' -OutFile $jar
}

Push-Location docs/diagrams
java -jar $jar -tsvg c4_nivel1_contexto.puml c4_nivel2_contenedores.puml c4_nivel3_componentes.puml
java -jar $jar -tpng c4_nivel1_contexto.puml c4_nivel2_contenedores.puml c4_nivel3_componentes.puml
Pop-Location

Copy-Item docs/diagrams/c4_nivel1_contexto.svg Informe-E4_BCEL/c4_nivel1_contexto.svg -Force
Copy-Item docs/diagrams/c4_nivel2_contenedores.svg Informe-E4_BCEL/c4_nivel2_contenedores.svg -Force
Copy-Item docs/diagrams/c4_nivel3_componentes.svg Informe-E4_BCEL/c4_nivel3_componentes.svg -Force
Copy-Item docs/diagrams/C4_Nivel1_Contexto.png Informe-E4_BCEL/c4_nivel1_contexto.png -Force
Copy-Item docs/diagrams/C4_Nivel2_Contenedores.png Informe-E4_BCEL/c4_nivel2_contenedores.png -Force
Copy-Item docs/diagrams/C4_Nivel3_Componentes_Soporte.png Informe-E4_BCEL/c4_nivel3_componentes.png -Force
```

Entradas: los tres archivos `c4_nivel*.puml` de este directorio. PlantUML deriva el nombre de salida del identificador `@startuml`; en Windows las mayúsculas de ese nombre no constituyen un archivo distinto.

Destinos SVG: `docs/diagrams/c4_nivel*.svg` y `Informe-E4_BCEL/c4_nivel*.svg`.

Destinos PNG: `Informe-E4_BCEL/c4_nivel*.png`, utilizados por `TA-PFC-E4_BCEL.tex`.

Describe los servicios y su endpoint PostgreSQL compartido. El Compose principal usa un servidor externo; el Compose de Principal declara una única instancia local postgres:17 como alternativa. La separación por esquemas es lógica y la persistencia tiene un punto único de fallo en la topología versionada. Los nombres de esquemas se basan en configuración/modelos/SQL; docs/db/schema.sql usa nombres de referencia distintos (véase ADR-003). etcd/Raft coordina Soporte y no replica PostgreSQL.
