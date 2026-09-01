# ADR-001: Arquitectura en 4 Capas y Principios SOLID en el SGA

## Estado
Aceptado

## Contexto
El Sistema de Gestión Académica (SGA) de la Escuela de Educación Básica "Provincias Unidas" requiere un alto nivel de cohesión y bajo acoplamiento para soportar concurrencia masiva en calificaciones, asistencias y autenticación distribuida. Para evitar clases monolíticas y facilitar la mantenibilidad, se requirió una separación física y lógica del backend.

## Decisión
Se refactorizó el microservicio `sga-principal` adoptando una **Arquitectura en 4 Capas Limpias (Clean Architecture / Onion Architecture)** con la siguiente distribución de responsabilidades:

1. **Capa de Presentación (`presentation.controller`):**
   - Controladores REST (`@RestController`) que exponen la API pública y privada.
   - Validación sintáctica de peticiones mediante Bean Validation (`@Valid`).
   - Transformación de solicitudes HTTP hacia casos de uso.

2. **Capa de Aplicación (`application.service`):**
   - Implementación de casos de uso y lógica de negocio institucional.
   - Orquestación de transacciones (`@Transactional`).
   - Coordinación de autorización docente (`TeacherAuthorizationService`).

3. **Capa de Dominio (`domain.entity`, `domain.dto`):**
   - Entidades centrales del negocio (`Usuario`, `Matricula`, `Asignacion`, `Grado`).
   - Data Transfer Objects (DTOs) que desacoplan el modelo interno de la API externa.

4. **Capa de Infraestructura (`infrastructure.repository`, `infrastructure.grpc`, `infrastructure.security`, `infrastructure.config`):**
   - Adaptadores de persistencia JPA y repositorios PostgreSQL.
   - Servidor gRPC binario (`PrincipalGrpcService`) en el puerto 9092.
   - Filtros de seguridad JWT (`JwtAuthenticationFilter`) y configuración perimetral.

## Consecuencias

### Positivas
- **Mantenibilidad:** El código cumple los 5 principios SOLID, especialmente Responsabilidad Única (SRP) e Inversión de Dependencias (DIP).
- **Testabilidad:** Cada capa se puede probar unitariamente de forma aislada utilizando dobles de prueba (Mocks).
- **Desacoplamiento:** Cambios en la base de datos o en el protocolo de transporte (REST / gRPC) no impactan las reglas de negocio en la capa de aplicación.

### Negativas / Mitigaciones
- Mayor número de paquetes y archivos DTO, mitigado mediante convenciones claras de nombrado y mapeos directos.
