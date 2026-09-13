# Arquitectura Unificada de Bitácora Criptográfica y Auditoría Distribuida — AcadTrace (E3)

**Responsable:** Ernesto Luna (Microservicio Secretaría y Seguridad)  
**Coordinación:** Equipo de Arquitectura AcadTrace  
**Fecha:** 11 de Septiembre de 2026  
**Criterio:** E3 — Consolidación y Alineación Arquitectónica de Bitácora  

---

## 1. Diagnóstico del Estado Previo: Discrepancias Identificadas

En las fases tempranas del desarrollo de AcadTrace, cada subsistema implementó mecanismos de auditoría con divergencias conceptuales y técnicas:

| Subsistema | Mecanismo Previo | Estructura de Integridad | Limitación o Discrepancia |
| :--- | :--- | :--- | :--- |
| **SGA Principal** | Inserción en `sga_principal.auditoria` | Firma HMAC-SHA256 fila a fila con clave simétrica | Protegía contra manipulación de filas individuales, pero sin encadenamiento estricto entre eventos independientes. |
| **Microservicio Docente** | `docentes.models.AuditoriaAcademica` | Cadena SHA-256 (`hash_anterior` -> `hash_actual`) | Mantenía una cadena estilo blockchain local en su propio esquema `sga_docente`, aislada de la historia global de la institución. |
| **Microservicio Secretaría** | Inserción directa en `sga_principal.auditoria` | Firma HMAC-SHA256 de campos concatenados | En la documentación previa se afirmaba la existencia de una "cadena criptográfica de secretaría", cuando en realidad **únicamente se firmaban filas con HMAC**, generando confusión de diseño. |

---

## 2. Decisión Arquitectónica Adoptada (Opción Recomendada)

Para garantizar consistencia global bajo **ISO/IEC 25010** (Inmutabilidad, Integridad y No Repudio), se adopta la **Opción Recomendada**:

> **Arquitectura Centralizada con Formato Canónico Común:**  
> Todos los servicios del ecosistema (**SGA Principal, Docente, Secretaría y Soporte**) emiten sus eventos de auditoría bajo un **modelo canónico institucional común**. La bitácora central reside en `sga_principal.auditoria`, donde se mantiene la secuencia encadenada global y se garantiza el principio append-only mediante triggers y restricción de privilegios de motor (E6).

### Aclaración explícita sobre el Microservicio Secretaría:
**Secretaría NO mantiene una cadena blockchain ni SHA-256 independiente.**  
Secretaría genera eventos con el esquema de campos institucional común, firma cada registro con HMAC-SHA256 para integridad de transporte/persistencia, adjunta la marca del reloj lógico de **Lamport**, y delega la bitácora histórica unificada a `sga_principal.auditoria`.

---

## 3. Especificación del Formato Canónico Común de Eventos

Todos los eventos emitidos o registrados en la bitácora deben contener los siguientes campos normalizados:

| Campo Canónico | Tipo de Dato | Descripción y Propósito | Ejemplo |
| :--- | :--- | :--- | :--- |
| `id_auditoria` | `BIGINT / SERIAL` | Identificador único autoincremental del registro en la bitácora central. | `4501` |
| `schema_origen` | `VARCHAR(30)` | Microservicio o subsistema origen del evento (`PRINCIPAL`, `DOCENTE`, `SECRETARIA`, `SOPORTE`). | `'SECRETARIA'` |
| `accion` | `VARCHAR(50) / ENUM` | Operación ejecutada (`CREAR`, `MODIFICAR`, `ELIMINAR`, `LOGIN_EXITOSO`, `LLAMADA_GRPC`, etc.). | `'CREAR'` |
| `tabla_afectada` | `VARCHAR(100)` | Entidad o agregado de dominio modificado. | `'matricula'` |
| `registro_id` | `BIGINT` | ID primario del registro afectado en su esquema correspondiente. | `105` |
| `descripcion` | `TEXT` | Detalle legible de la operación de negocio. | `'Matricula ordinaria grado 5to'` |
| `payload_canonico` | `TEXT / JSON` | Serialización determinística de los atributos modificados para verificación criptográfica. | `'{"folio":"F-2026","estudiante":12}'` |
| `hash_anterior` | `VARCHAR(64)` | Digest SHA-256 del evento inmediatamente precedente en la bitácora (encadenamiento blockchain-style). En génesis: `'0000000000000000000000000000000000000000000000000000000000000000'`. | `'a3f8c1...e45b'` |
| `hash_actual` | `VARCHAR(64)` | Digest SHA-256 del bloque actual: `SHA-256(hash_anterior + payload_canonico + trace_id + lamport)`. | `'b7e2d9...10ca'` |
| `hmac` | `VARCHAR(64)` | Firma HMAC-SHA256 computada con el secreto institucional `JWT_SECRET` sobre la tupla canónica. | `'f41e0a...98db'` |
| `reloj_lamport` | `BIGINT` | Marca de tiempo lógica escalar para ordenamiento causal sin dependencia de relojes físicos de pared. | `42` |
| `vector_reloj` | `TEXT` | Vector clock para concurrencia multi-nodo en modo distribuido avanzado (ej. `[12, 18, 5]`). | `"[1, 0, 0]"` |
| `trace_id` | `UUID` | Identificador de correlación distribuida de extremo a extremo propagado vía HTTP (`X-Trace-Id`) y gRPC metadata (`trace_id`). | `'c9b2f1e4-8a12-4212-9a7f-89a90252c8e5'` |
| `fecha` | `TIMESTAMPTZ` | Marca de tiempo UTC generada por el motor de base de datos (`DEFAULT NOW()`). | `2026-09-11 21:15:00.000Z` |

---

## 4. Algoritmo de Verificación de Integridad

El verificador de auditoría ejecuta una doble comprobación:

1. **Integridad de Contenido (HMAC):**
   ```
   HMAC-SHA256(schema_origen | trace_id | username | accion | tabla | registro_id | descripcion | resultado | timestamp) == fila.hmac
   ```
2. **Integridad de Secuencia Histórica (Cadena SHA-256):**
   Para cada fila $i > 0$:
   ```
   fila[i].hash_anterior == fila[i-1].hash_actual
   ```
   Cualquier inserción intermedia, borrado o reordenamiento romperá el eslabón de la cadena de hashes y será detectado en tiempo $O(N)$.
