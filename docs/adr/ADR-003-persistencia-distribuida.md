# ADR-003: Persistencia Distribuida y Fragmentación de Datos

## Estado
Aceptado

## Contexto
El sistema académico debe soportar alta concurrencia durante períodos de matriculación y cierre de notas, garantizando consistencia fuerte (ACID) y tolerancia a fallos ante caídas de nodos sin pérdida transaccional.

## Decisión
Se implementa una base de datos distribuida relacional (PostgreSQL / CockroachDB con protocolo de consenso Raft) organizada con fragmentación lógica por **período lectivo** y por **grado/paralelo**, con pools de conexiones gestionados por HikariCP y migraciones versionadas mediante Flyway.

## Consecuencias
- Garantía de consistencia transaccional y disponibilidad continua.
- Aislamiento de cargas de trabajo por año lectivo y grado académico.
