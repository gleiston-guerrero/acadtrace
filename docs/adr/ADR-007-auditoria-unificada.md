# ADR-007: Arquitectura de Auditoria Criptografica Unificada y Conmutador de Mecanismos

## Estado
Aceptado

## Contexto
El sistema AcadTrace opera bajo una arquitectura de microservicios distribuidos y poliglotas (Java Spring Boot, Python Django REST Framework, Node.js Express). Durante iteraciones previas existian discrepancias en la implementacion de la bitacora entre servicios (uso asimetrico de HMAC-SHA256, cadenas SHA-256 independientes y relojes logicos sin unificacion causal formal). Para garantizar el cumplimiento estricto del estandar ISO/IEC 25010 (Integridad, No-repudio y Trazabilidad), se requiere una decision arquitectonica vinculante y definitiva que unifique el esquema de auditoria en todos los nodos del cluster.

## Decision

Se establece una **Arquitectura de Auditoria Unificada** obligatoria para todos los microservicios de AcadTrace (`sga-principal`, `microservicio-docente`, `microservicio-secretaria`, `microservicio-soporte` y clientes).

### 1. Mecanismo Oficial
El sistema opera mediante un conmutador determinista de cuatro mecanismos controlado por la variable de entorno `AUDIT`:
- **M0 (Baseline):** Persistencia transaccional sin registro de bitacora (control negativo de rendimiento).
- **M1 (Convencional):** Registro relacional estandar sin sellos criptograficos ni orden logico causal.
- **M2 (Oficial de Produccion — HMAC-SHA256 + Relojes de Lamport):** Hash SHA-256 encadenado ($H_k = \text{SHA-256}(H_{k-1} \parallel \text{payload} \parallel L_k)$), firma de autenticidad local HMAC-SHA256 y avance monotono del reloj logico de Lamport.
- **M3 (Concurrencia Offline — M2 + Relojes Vectoriales):** Incorporacion de un vector de versiones para reconciliacion determinista ante ediciones concurrentes desconectadas.

### 2. Formato Canonico del Evento
Todo evento de auditoria generado en el cluster se serializa bajo la siguiente estructura JSON canonica estandarizada:
- `trace_id`: UUID global de correlacion (propagado en headers HTTP y metadata gRPC).
- `schema_origen`: Identificador del microservicio emisor (`PRINCIPAL`, `DOCENTE`, `SECRETARIA`, `SOPORTE`).
- `actor`: Nombre de usuario autenticado o servicio emisor.
- `accion`: Operacion ejecutada (`CREAR`, `MODIFICAR`, `ELIMINAR`, `LLAMADA_GRPC`).
- `tabla_afectada`: Entidad de dominio afectada.
- `registro_id`: Identificador numerico del registro intervenido.
- `descripcion`: Resumen legible de la transaccion con traza de relojes.
- `lamport_clock`: Marca de tiempo logica escalar creciente.
- `vector_clock`: Vector de versiones logico para eventos concurrentes.
- `hash_anterior`: Hash SHA-256 del eslabon previo (o 64 ceros para genesis).
- `hash_actual`: Hash SHA-256 computado sobre el estado actual.
- `hmac`: Firma de integridad calculada con clave secreta compartida.
- `timestamp`: Marca temporal UTC en precision de milisegundos.

### 3. Autoridad que Genera el Hash y Mantiene la Cabeza
- **Generador del Hash:** Cada microservicio productor calcula el hash SHA-256 de su transaccion en su capa de aplicacion/infraestructura antes de persistir.
- **Autoridad de la Cabeza de Cadena:** La persistencia relacional en PostgreSQL (`sga_principal.auditoria` y `sga_docente.eventos_auditoria`) actua como la autoridad centralizada de ordenacion fisica. El puntero al ultimo hash emitido se actualiza atomicamente dentro de la transaccion.

### 4. Tratamiento de Eventos entre Microservicios
La correlacion distribuida se propaga mediante:
- **gRPC Metadata:** Los clientes gRPC inyectan en las cabeceras binarias HTTP/2 las claves `trace_id`, `actor_username` y `internal_token`.
- **Intercepcion y MDC:** El interceptor `InternalAuthInterceptor` (Java) y el middleware de Django capturan estos encabezados, los inyectan en el Mapped Diagnostic Context (MDC de SLF4J / Python Logger) y los asocian al evento de auditoria generado en el servicio receptor.

### 5. Rol de HMAC vs Cadena Criptografica
- **HMAC-SHA256:** Se define y conserva formalmente como una **firma de autenticidad e integridad local complementaria**. Certifica que el registro fue emitido legitimamente por un nodo que posee la clave secreta compartida (`JWT_SECRET`), impidiendo falsificaciones externas.
- **Cadena SHA-256:** Garantiza la **secuencia e inmutabilidad temporal** de la historia completa.

### 6. Relojes Logicos (Lamport y Vectoriales)
- **Lamport Clock:** Define el orden monotono total en llamadas secuenciales e inter-servicio ($L = \max(L_{\text{loc}}, L_{\text{rx}}) + 1$).
- **Relojes Vectoriales:** Gestionados por la app movil y el microservicio docente para detectar bifurcaciones ($A \parallel B$) originadas por sincronizaciones fuera de linea (*offline-first* via Room SQLite y WorkManager).

### 7. Manejo de Concurrencia
Para prevenir condiciones de carrera al calcular el siguiente eslabon:
1. En transacciones concurrentes sobre la misma entidad se aplica bloqueo pesimista a nivel de fila (`SELECT ... FOR UPDATE`).
2. En operaciones masivas (asistencia, calificaciones) se calculan los eslabones secuencialmente en lote dentro del mismo bloque transaccional.

### 8. Comportamiento y Deteccion ante Manipulacion
El sistema cuenta con una doble barrera de defensa:
1. **Barrera Fisica Preventiva (Base de Datos):** El disparador PL/pgSQL `tg_auditoria_append_only` rechaza a nivel de motor cualquier sentencia `UPDATE` o `DELETE` sobre la tabla de auditoria, lanzando una excepcion `RAISE EXCEPTION (SQLState P0001)`.
2. **Deteccion Correctiva (Verificador Criptografico):** El modulo `verificador_cadena.py` recorre la bitacora recalculando hashes y contrastando firmas HMAC. Ante una alteracion directa ($T_1$) o eliminacion ($T_2$), el verificador reporta el primer eslabon roto y aborta la validacion de la cadena.

## Consecuencias
- Unificacion total de criterios entre los 4 microservicios y la aplicacion movil.
- Imposibilidad de repudiar o alterar transacciones sin deteccion inmediata.
- Cumplimiento estricto del criterio de consolidacion E3 de la asignatura.
