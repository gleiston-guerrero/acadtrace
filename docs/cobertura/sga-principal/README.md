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
./mvnw clean test jacoco:report "-Dtest=!*ContainerTest,!*ConcurrencyE3Test"
```

El reporte se genera en `sga-principal/target/site/jacoco/`. Se puede copiar
aquí para publicarlo, siempre desde una única ejecución.

## Cifra publicada

Última medición: 30,9 % de instrucciones cubiertas (4394 / 14202) sobre el
BUNDLE completo con las exclusiones anteriores. La cifra se toma del
`jacoco.xml` generado en una sola ejecución, no del acumulado de sesiones.
