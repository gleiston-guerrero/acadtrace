# ADR-004: Autenticación Stateless JWT y Criptografía de Sesión

## Estado
Aceptado

## Contexto
En un entorno de microservicios distribuidos, el estado de sesión centralizado introduce cuellos de botella y puntos únicos de fallo. Se requiere un esquema de autenticación sin estado seguro y verificable por cualquier nodo perimetral.

## Decisión
Se adopta JSON Web Tokens (JWT) firmados con algoritmo HMAC-SHA256 (RFC 7519) con expiración corta y cifrado simétrico AES-256 para almacenamiento seguro de tokens en el cliente móvil y cookies HttpOnly en la web. La propagación de identidad se complementa con identificadores de correlación `X-Trace-Id`.

## Consecuencias
- Autenticación distribuida sin estado desacoplada de la base de datos para verificación de permisos.
- Control de acceso basado en roles (RBAC: Administrador, Docente, Secretaría, Representante).
