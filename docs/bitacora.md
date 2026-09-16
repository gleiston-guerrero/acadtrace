# Bitacora de Registro y Estado de Partida - AcadTrace (Entrega 4)

- Fecha de Registro: 26 de Agosto de 2026
- Etiqueta Git de Congelacion: pre-e4
- Commit SHA Base: 14f6e654550cadbff0744681984796a85db8ac08
- Repositorio: https://github.com/LEO23as/sga-sistema-distribuido
- Denominacion del Sistema: AcadTrace (Capa de auditoria verificable para expedientes academicos en sistemas escolares distribuidos)
- Caso de Estudio Institucional: Escuela de Educacion Basica "Provincias Unidas" (344 estudiantes, 14 docentes)

## Integrantes y Roles Asignados:
- Pedro Leonardo Castro Lopez - Arquitecto y Lider de Proyecto
- Keyla Betzabe Bedon Viteri - Desarrolladora
- Ernesto Gregory Luna Mora - Responsable de Calidad
- Juliana Romina Emanuel Pino - Documentalista

---

Nota de correccion documental (16 de septiembre de 2026, punto 6): se rectifican las afirmaciones de persistencia contra la configuracion versionada actual; no se certifica el despliegue historico pre-e4. Vease ADR-003.

## 1. Auditoria Tecnica del Estado de Partida (Mapeo por Unidades Curriculares)

### UNIDAD 1: Fundamentos de Sistemas Distribuidos y Comunicacion

| Tema | Estado en pre-e4 | Mecanismo Tecnico Implementado y Evidencia en Repositorio | Pendiente / Por Mejorar en E4 |
| :--- | :---: | :--- | :--- |
| Transparencias ANSA | Parcial | Acceso (REST/JSON uniforme), Ubicacion (nombres logicos en Docker), Concurrencia (HikariCP y aislamiento transaccional), Fallos (HAProxy health check). | Documentar las transparencias evidenciadas sin atribuir replicacion a PostgreSQL. |
| Sockets TCP | Implementado | Servidor TCP en puerto :9095 con delimitacion de trama por longitud (cabecera de 4 bytes) para recepcion de tramas de asistencia desde dispositivos de aula. | Rehacer medicion de latencia comparativa (100 envios) vs gRPC. |
| gRPC y RPC | Implementado | Contratos .proto versionados en sga-principal/src/main/proto/. Servidor en :9092 y cliente en microservicio-docente para distribucion de catalogo institucional. | Documentar regeneracion limpia con protoc y medicion latency_grpc.csv. |
| Relojes de Lamport y Vectoriales | Pendiente | Solo marcas de tiempo fisicas (timestamp ISO 8601). | Implementar mecanismos M2 (Lamport) y M3 (Relojes Vectoriales) en el motor de auditoria de calificaciones. |
| Tolerancia a Fallos | Parcial | Heartbeats configurados en HAProxy hacia backends. Recuperacion automatica de contenedores con restart: always. | Registrar fallo de omision en sincronizacion movil y documentar latidos entre replicas. |
| Eleccion de Lider | Pendiente | No implementado de forma distribuida. | Algoritmo de eleccion para el nodo que consolida actas y ejecuta el cierre de periodo lectivo. |
| Teorema CAP | Implementado | Persistencia relacional PostgreSQL; la configuracion de una instancia no demuestra clasificacion CP/AP de un cluster de datos. | Revalidar la posicion con los datos de concurrencia del Paso 8. |
| Coordinacion y Consistencia | Parcial | Transacciones locales ACID en sga-principal. Aislamiento Read Committed en Postgres. | Documentar transacciones locales; no se demuestra 2PC entre servicios ni consenso de persistencia. |
| Seguridad | Implementado | Autenticacion JWT (HS256) con expiracion de 24h. Control de acceso basado en 4 roles (Director, Docente, Secretaria, Soporte). Controles reforzados para proteccion de datos de menores. | Validar que el 100% de endpoints protegidos devuelvan 401 Unauthorized sin token. |
| Acuerdo de Nivel de Servicio (SLA) | Parcial | Objetivo operativo declarado: Disponibilidad >= 99.5% y latencia P95 < 500ms. | Medir formalmente el SLA bajo carga real con Locust. |

---

### UNIDAD 2: Datos Distribuidos, Fragmentacion y Consistencia

| Tema | Estado en pre-e4 | Mecanismo Tecnico Implementado y Evidencia en Repositorio | Pendiente / Por Mejorar en E4 |
| :--- | :---: | :--- | :--- |
| Fragmentacion de Datos | Parcial | Separacion logica por esquemas sga_principal, sga_docente, sga_secretaria y sga_soporte segun configuracion, modelos y SQL; no se demuestra particionado fisico por rango/lista. | Documentar la diferencia con los nombres de docs/db/schema.sql en ADR-003. |
| Replicacion y Consistencia | No demostrada | Compose principal: endpoint PostgreSQL externo unico (predeterminado 192.0.2.1:5433, base sga). Compose de Principal: una instancia postgres:17. No hay configuracion versionada de replicacion ni standby. | Describir la instancia configurada; no certificar la topologia interna del servidor externo. |
| Tolerancia a Fallos en Datos | No demostrada | Persistencia con punto unico de fallo en la topologia versionada; no se demuestra failover de PostgreSQL. | Registrar la limitacion; la eleccion de lider etcd de Soporte no replica ni recupera PostgreSQL. |

---

### UNIDAD 3: Arquitectura de Microservicios y Contenedores

| Tema | Estado en pre-e4 | Mecanismo Tecnico Implementado y Evidencia en Repositorio | Pendiente / Por Mejorar en E4 |
| :--- | :---: | :--- | :--- |
| Microservicios con BD Propia | Parcial | Los servicios comparten por defecto endpoint y base PostgreSQL, con separacion logica multiesquema y accesos adicionales segun configuracion/consultas. No son bases fisicamente independientes. | Alinear C4 y LaTeX con la instancia compartida descrita en ADR-003. |
| API REST y OpenAPI 3.0 | Parcial | Endpoints documentados con Swagger/SpringDoc en sga-principal. | Exportar y validar las especificaciones OpenAPI 3.0 de todos los microservicios en docs/api/*.yaml. |
| API Gateway | Implementado | HAProxy configurado como reverse proxy, enrutador por prefijo de ruta (/api/v1/...), verificador de salud y limitador de tasa (rate limiting). | Registrar marca de tiempo, origen y codigo HTTP de cada peticion en el Gateway. |
| Mensajeria Asincrona | Parcial | Eventos internos de auditoria de calificaciones y confirmacion de matricula. | Justificar tecnicamente la eleccion del canal de eventos. |
| Contenedores y Orquestacion | Implementado | docker-compose.yml multi-servicio con redes aisladas (sga-net) y limites de memoria definidos. | Verificar compilacion multi-etapa en todos los Dockerfile. |
| Patrones de Despliegue | Implementado | Despliegue continuo en AWS EC2 mediante GitHub Actions sin detencion total de servicios. | Documentar estrategia Rolling Update para el periodo de matricula. |

---

### UNIDAD 4: Procesamiento Distribuido y Analitica

| Tema | Estado en pre-e4 | Mecanismo Tecnico Implementado y Evidencia en Repositorio | Pendiente / Por Mejorar en E4 |
| :--- | :---: | :--- | :--- |
| Procesamiento Distribuido | Implementado | Pipeline de consolidacion de rendimiento academico e indicadores institucionales sobre conjunto de datos de calificaciones. | Re-ejecutar pipeline sobre el conjunto sintetico oficial de 344 estudiantes y 14 docentes. |
| Ley de Amdahl | Implementado | Medicion de tiempos de ejecucion secuencial (Pandas) vs distribuido (PySpark) con 1, 2 y 4 ejecutores. | Despejar numericamente la fraccion no escalable observada (p) y generar curvas teoricas a 300 DPI. |

---

### UNIDAD 5: Calidad, CI/CD, Observabilidad y Patrones de Diseno

| Tema | Estado en pre-e4 | Mecanismo Tecnico Implementado y Evidencia en Repositorio | Pendiente / Por Mejorar en E4 |
| :--- | :---: | :--- | :--- |
| Arquitectura en Capas y SOLID | Implementado | Clean Architecture en 4 capas (Presentation, Application, Domain, Infrastructure) en sga-principal, microservicio-secretaria y microservicio-soporte. | Documentar la reduccion de acoplamiento en el informe LaTeX. |
| Patrones de Diseno GoF | Implementado | Repository (acceso JPA/Mocks), Facade gRPC (PrincipalGrpcService), Strategy (ponderacion 70/30), Observer (auditoria), Proxy/Decorator (JwtFilter). | Incluir fragmentos de codigo en lstlisting dentro del LaTeX. |
| Inyeccion de Dependencias | Implementado | Inversion de Control (IoC) gestionada por Spring Boot (@Service, @Repository) y sustitucion limpia con @Mock en pruebas. | Ejemplo de sustitucion documentado en el informe. |
| Integracion Continua (CI/CD) | Implementado | Workflow .github/workflows/ci-cd.yml con ejecucion automatizada de pruebas unitarias y empaquetado Maven. | Evidenciar corrida con fallo intencional (rojo) y pase corregido a verde. |
| Observabilidad Distribuida | Parcial | Endpoints /actuator/prometheus y Prometheus recolectando metricas cada 15s. | Configurar logging estructurado JSON con traceId, panel Grafana Cloud y exportar traza distribuida. |
| Pruebas Automatizadas | Implementado | Evidencia histórica: 5 pruebas unitarias con JUnit 5 y Mockito en EstudianteServiceTest.java y 88% en servicios medido con JaCoCo; no es cobertura global actual verificada contra HEAD. | Implementar pruebas unitarias en Python (pytest) para Docente y pruebas de integracion por Gateway. |
| Evaluacion ISO/IEC 25010 | Pendiente | No evaluado formalmente con metricas de produccion. | Construir tabla booktabs con mediciones reales de Disponibilidad, Rendimiento, Fiabilidad, Mantenibilidad y Seguridad. |

---

## 2. El Experimento Central de AcadTrace (Objetivo Clave de la Entrega 4)

El diferencial cientifico de esta entrega consiste en implementar y medir el costo computacional de la auditoria criptografica:

1. Mecanismo M0: Linea base sin auditoria (escritura directa).
2. Mecanismo M1: Bitacora convencional en tabla relacional sin encadenamiento.
3. Mecanismo M2: Bitacora encadenada por resumen criptografico SHA-256 del evento anterior y marca logica de Lamport.
4. Mecanismo M3: Mecanismo M2 mas Relojes Vectoriales para resolucion automatica de conflictos en modo desconectado.
5. Banco Experimental:
   - Generador de datos sinteticos con semilla fija (344 estudiantes, 14 docentes).
   - Inyector de manipulaciones (T1: BD directa, T2: borrado de evento, T3: swap de orden, T4: evento retroactivo, T5: alteracion de timestamp).
   - Verificador de integridad de cadena e invariantes causales.
   - Ejecucion de 120 corridas factoriales con analisis estadistico no parametrico (Mann-Whitney y Vargha-Delaney).

---

## 3. Verificacion de Criterios de Piso
- Visibilidad del repositorio: Publico y accesible de forma anonima.
- Integridad de archivos base: Existen .gitignore y LICENSE.