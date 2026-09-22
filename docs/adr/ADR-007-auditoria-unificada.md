# ADR-007: Arquitectura de Auditoría Criptográfica y Contrato Canónico de Auditoría

## Estado

Aceptado

## Contexto

AcadTrace opera con una arquitectura distribuida y políglota que incluye servicios Java Spring Boot y Python Django. Las iteraciones del sistema incorporaron mecanismos de auditoría implementados de forma independiente en los distintos servicios, por lo que no existe una biblioteca común de serialización canónica compartida entre Java y Python.

La interoperabilidad de la cadena criptográfica se sostiene sobre un contrato canónico v1, un vector patrón común y el almacenamiento del contenido canónico utilizado para calcular cada eslabón. Esta decisión no implica que las implementaciones Java y Python produzcan una representación idéntica para cualquier valor posible.

## Decisión

Se adopta un contrato institucional para los eventos versionados de auditoría y para el encadenamiento criptográfico. Las implementaciones productivas permanecen separadas por lenguaje: Java dispone de `AuditHashService`, mientras que Docente implementa la lógica correspondiente en `docentes/auditoria/hashing.py`.

No se ha extraído la serialización canónica a una biblioteca común entre Java y Python. Por ello, la sincronización del contrato se sostiene sobre el vector patrón compartido y sus pruebas de conformidad. Esta protección cubre los valores representados por dicho vector, pero no demuestra equivalencia carácter por carácter para todo el dominio de valores.

### 1. Mecanismos de auditoría

El sistema utiliza modos de auditoría controlados por la variable de entorno `AUDIT`:

- **M0 (Baseline):** ejecución utilizada como control sin el mecanismo criptográfico versionado.
- **M1 (Convencional):** registro de auditoría sin la cadena criptográfica v1.
- **M2 (Cadena SHA-256 + Reloj de Lamport):** encadenamiento criptográfico de eventos mediante el hash anterior, el contenido canónico v1 y el avance monotónico del reloj lógico de Lamport.
- **M3 (M2 + Relojes Vectoriales):** extiende M2 con información vectorial para los escenarios de concurrencia y reconciliación contemplados por el sistema.

Para los eslabones versionados, la fórmula institucional implementada es:

`H_k = SHA-256(H_{k-1} + contenido_canonico_v1)`

El reloj de Lamport forma parte del contenido canónico v1; no se concatena como un tercer elemento independiente fuera de dicho contenido.

### 2. Contrato canónico v1

El contenido canónico v1 se construye con once campos:

- `actor_id`
- `entidad`
- `entidad_id`
- `estado_reconciliacion`
- `modo`
- `operacion`
- `payload`
- `reloj_lamport`
- `reloj_vectorial`
- `timestamp`
- `tipo_evento`

Este contrato no debe confundirse con las columnas físicas de las tablas de auditoría ni con metadatos adicionales de correlación o persistencia.

Las implementaciones Java de Principal y Secretaría mantienen su propia implementación de `AuditHashService`, mientras que Docente mantiene la implementación Python en `docentes/auditoria/hashing.py`. El repositorio no contiene una biblioteca canónica única importada por los tres servicios.

### 3. Alcance del vector patrón

El vector patrón constituye la referencia compartida utilizada por las pruebas de conformidad de las implementaciones. Su finalidad es detectar cambios incompatibles en el contrato representado por ese conjunto de datos.

El vector patrón no demuestra equivalencia general de las serializaciones Java y Python. El arnés ejecutable `experimentos/arnes_12_vectores.py` mide el alcance real sobre doce vectores: Java Principal y Java Secretaría coinciden en 12/12 (100.0%), y Python coincide con Java en 8/12 (66.7%). Las cuatro divergencias reales fuera del dominio de cobertura son: flotantes de rango extremo (`1.0E-7` frente a `1e-07`, `1.0E21` frente a `1e+21`), orden de claves con caracteres suplementarios (UTF-16 frente a puntos de código) y el valor especial `NaN` (cadena `"NaN"` en Java frente a literal `NaN` en Python).

Adicionalmente, fuera del vector patrón, los tipos nativos de marca de tiempo producen representaciones distintas en producción: Java `Instant.toString()` genera la representación ISO-8601 con sufijo `Z` (por ejemplo `2026-09-21T20:00:00Z`), mientras que Python `datetime.isoformat()` con timezone UTC genera el offset explícito `+00:00` (por ejemplo `2026-09-21T20:00:00+00:00`). El vector patrón v1 protege la marca de tiempo preformateada como cadena, pero ante tipos nativos se manifiesta esta diferencia; por ello la arquitectura persiste y verifica el `contenido_canonico` textual emitido por cada servicio, en lugar de depender de una representación idéntica entre lenguajes.

Por ello, la garantía documentada es deliberadamente limitada: las pruebas protegen la compatibilidad del vector patrón y permiten detectar regresiones sobre ese contrato, pero no justifican afirmar una representación idéntica carácter por carácter para cualquier entrada.

### Divergencias reales fuera del vector patrón

Medidas por `experimentos/arnes_12_vectores.py` sobre doce vectores
(Java Principal == Java Secretaría: 12/12; Java == Python: 8/12), más
una divergencia adicional observada en producción cuando la marca de
tiempo se serializa como objeto nativo en lugar de cadena preformateada:

| Tipo de valor | Representación Java | Representación Python |
|---|---|---|
| Flotante de rango extremo pequeño | `1.0E-7` | `1e-07` |
| Flotante de rango extremo grande | `1.0E21` | `1e+21` |
| Orden de claves suplementarias | U+1F600 antes de U+FF01 (UTF-16) | orden por punto de código |
| `NaN` | cadena `"NaN"` | literal JSON no estándar `NaN` |
| Marca de tiempo como tipo nativo en producción (fuera de los doce vectores) | `Instant.toString()` con sufijo UTC: `2026-09-21T20:00:00Z` | `datetime.isoformat()` con offset: `2026-09-21T20:00:00+00:00` |

La última fila no forma parte de los doce vectores del arnés: corresponde
a la divergencia de producción entre `Instant.toString()` (sufijo `Z`) y
`datetime.isoformat()` con timezone UTC (offset `+00:00`). El vector
patrón v1 protege las marcas de tiempo preformateadas como cadena, pero
ante tipos nativos se manifiesta esta diferencia, lo que refuerza la
decisión arquitectónica de persistir y verificar el `contenido_canonico`
textual emitido por cada servicio.

### Evidencia reproducible

`experimentos/arnes_12_vectores.py` compila e invoca las tres
implementaciones reales (`AuditHashService` de sga-principal y de
secretaría vía `experimentos/java_harness/CanonicoRunner.java`, y
`docentes/auditoria/hashing.py`) sobre
`experimentos/vectores_canonicos_v1.json`, calcula los porcentajes y
falla con código 1 si Java Principal deja de coincidir con Java
Secretaría o si el conteo Java == Python cambia respecto a 8/12.
Procedimiento en `experimentos/README_arnes.md`; reproducible desde un
clon limpio.

### 4. Persistencia e interoperabilidad de la cadena

Los productores que participan en la cadena institucional versionada calculan el hash correspondiente al evento antes de persistir el eslabón. Para los eventos institucionales versionados se almacenan, entre otros datos necesarios para la verificación, `hash_anterior`, `hash_actual`, `reloj_lamport`, `contenido_canonico` y `version_canonica`.

La interoperabilidad de la cadena se apoya en el contenido canónico almacenado. Un verificador puede comprobar un eslabón utilizando el `hash_anterior` y el `contenido_canonico` persistido sin tener que reconstruir en otro lenguaje el objeto original que produjo ese texto.

Esto permite verificar cadenas compuestas por eventos producidos por implementaciones distintas sin afirmar que Java y Python serialicen de manera idéntica todos los tipos de entrada.

### 5. Rol de HMAC y de la cadena SHA-256

HMAC-SHA256 existe como mecanismo complementario en componentes Java que lo utilizan. Sin embargo, HMAC no forma parte de la fórmula institucional de encadenamiento:

`H_k = SHA-256(H_{k-1} + contenido_canonico_v1)`

Tampoco forma parte de las comprobaciones realizadas por el verificador de cadena de Docente. Por tanto, no se atribuye a dicho verificador una validación de firmas HMAC.

La cadena SHA-256 permite detectar inconsistencias que alteren la continuidad entre los eslabones sometidos a verificación.

### 6. Relojes lógicos

El reloj de Lamport proporciona el orden lógico utilizado por los eventos encadenados y forma parte del contrato canónico v1 mediante `reloj_lamport`.

El contrato también incluye `reloj_vectorial` para los escenarios que requieren información causal o reconciliación. Su presencia en el contrato no implica que todos los componentes del sistema implementen exactamente la misma estrategia local de persistencia.

### 7. Concurrencia y autoridad de la cabeza

Los escritores de la cadena institucional deben coordinar la actualización de la cabeza para evitar que dos operaciones concurrentes utilicen simultáneamente el mismo estado anterior.

La autoridad de la cabeza y del reloj lógico de los eventos institucionales versionados se mantiene en la persistencia correspondiente, utilizando mecanismos transaccionales de bloqueo donde están implementados.

Esta decisión describe el contrato de la cadena y no supone que todas las bitácoras históricas o auxiliares del sistema hayan sido eliminadas.

### 8. Verificación de la cadena

El verificador productivo de Docente comprueba la continuidad de `hash_anterior`, recalcula el hash esperado a partir de hash_anterior y contenido_canonico y valida la monotonicidad del reloj de Lamport. Para la cadena institucional versionada, el verificador opera sobre los eslabones v1 almacenados en `sga_principal.auditoria`.

Cuando detecta una inconsistencia, devuelve el primer eslabón afectado y el tipo de inconsistencia correspondiente.

El verificador de cadena no contrasta firmas HMAC. HMAC y el encadenamiento SHA-256 son mecanismos distintos y no deben documentarse como una única comprobación.

## Limitaciones conocidas

La arquitectura actual conserva implementaciones canónicas independientes entre Java y Python y no dispone de una biblioteca común compartida por los tres servicios productores.

Las pruebas del vector patrón protegen el contrato que representan, pero no cubren todas las diferencias de serialización posibles entre lenguajes. Por ello, cualquier ampliación del dominio canónico debe incorporar nuevos vectores de conformidad antes de considerarse interoperable.

También pueden coexistir proyecciones o bitácoras locales utilizadas por componentes específicos. La existencia de estas persistencias auxiliares no debe describirse como una unificación física total de todas las bitácoras del sistema.

## Consecuencias

- Se documenta explícitamente que la forma canónica no procede de una biblioteca común entre Java y Python.
- El contrato canónico v1 queda identificado mediante sus once campos reales.
- El vector patrón se presenta como una protección de compatibilidad acotada y no como prueba de identidad universal entre serializadores.
- La fórmula documentada de la cadena coincide con el mecanismo `SHA-256(hash_anterior + contenido_canonico_v1)`.
- HMAC queda separado conceptualmente de la verificación de la cadena SHA-256 y no se atribuye al verificador una comprobación que no realiza.
- Se reconoce la coexistencia de implementaciones y persistencias auxiliares en lugar de afirmar una unificación total que el repositorio no demuestra.