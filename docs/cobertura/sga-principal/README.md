# Cobertura de sga-principal

El reporte HTML de este módulo se elimina del árbol para no publicar sesiones
mezcladas ni paquetes huérfanos.

La compuerta activa está definida en `sga-principal/pom.xml`:

- Elemento: `BUNDLE` (módulo entero, no una lista de clases).
- Umbral: `INSTRUCTION >= 0.30` (30 %).
- Exclusiones: código generado por protoc/gRPC, DTOs, entidades JPA, configuración
  Spring, excepciones, clase de arranque, constantes, enums y mappers. Cada
  exclusión está documentada por bloque en el propio `pom.xml`.

## Regenerar el reporte

Desde la raíz del repositorio, con JDK 17:

```bash
cd sga-principal
./mvnw clean test -q
```

El reporte se genera en `sga-principal/target/site/jacoco/`. Se puede copiar
aquí para publicarlo, siempre desde una única ejecución.

## Cifra publicada

Última medición oficial: **32,79 % de instrucciones cubiertas
(4668 de 14237)** a nivel de módulo completo (`BUNDLE`). La cifra procede
de `docs/cobertura/sga-principal/jacoco.xml`, descargado del CI #876,
run `35693153935`, sobre el commit `6c1f67ab28d569643b4c7ec4f740d7221bd60b0f`.

La compuerta configurada en `pom.xml` exige un mínimo de **30 % de
instrucciones**. El CI ejecuta la suite mediante `./mvnw clean test -q`,
por lo que la evidencia documentada utiliza el mismo alcance que el flujo
automático y no excluye manualmente `ContainerTest` ni `ConcurrencyE3Test`.