# Protos gRPC (pendiente)

Carpeta destino para el `.proto` que definirá sga-principal para exponer
lectura de usuarios / contexto por gRPC hacia soporte técnico.

Una vez llegue el archivo:
1. Colocarlo aquí (`backend/src/main/proto/*.proto`).
2. `./mvnw generate-sources` genera los stubs Java en `target/generated-sources`.
3. Implementar el cliente gRPC (canal + stub) para consultar al principal.

El `protobuf-maven-plugin` y las dependencias de `grpc-java` ya están
configuradas en `pom.xml`.
