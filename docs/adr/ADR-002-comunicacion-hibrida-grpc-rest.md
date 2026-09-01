# ADR-002: Protocolos Híbridos de Comunicación (REST API y gRPC)

## Estado
Aceptado

## Contexto
El sistema distribuido AcadTrace opera con múltiples microservicios políglotas (Java Spring Boot, Python Django, Node.js Express). Se requiere comunicación de baja latencia y alta concurrencia para la sincronización entre microservicios, al tiempo que se debe ofrecer una interfaz estándar HTTP/JSON para las aplicaciones cliente (Web SPA y Móvil).

## Decisión
Se adopta una arquitectura de comunicación híbrida:
1. **Perímetro Externo (Clientes Web / Móvil -> Backend):** REST sobre HTTP/JSON gestionado por el API Gateway HAProxy (`:80`, `:443`).
2. **Comunicación Inter-Servicio (Backend -> Backend):** Protocol Buffers v3 sobre HTTP/2 utilizando gRPC (`:9091`, `:9092`, `:9093`, `:9094`) para llamadas sincrónicas de alto rendimiento y tolerancia a fallos.

## Consecuencias
- Latencia reducida en transferencias internas gracias a la serialización binaria de Protobuf.
- Compatibilidad abierta y estándar para los navegadores y clientes móviles vía REST/JSON.
