# syntax=docker/dockerfile:1
# =============================================================================
# Dockerfile Multi-Stage - Microservicio Secretaría (Backend Spring Boot + SPA React)
# Criterio E14: No-root, healthcheck nativo, sin secretos, build reproducible
# Contexto de construcción: ./microservicio-secretaria
# =============================================================================

# ---- 1. Etapa de Construcción Frontend (Vite) ----
FROM node:20-alpine AS frontend-build
WORKDIR /src/client

# Instalación determinística con package-lock.json (E12)
COPY client/package*.json ./
RUN npm ci

# Compilación de la interfaz React
COPY client/ ./
RUN npm run build

# ---- 2. Etapa de Construcción Backend (Maven + Java 21) ----
FROM maven:3.9-eclipse-temurin-21 AS backend-build
WORKDIR /src/backend

COPY backend/pom.xml ./
RUN mvn -B -q dependency:go-offline || true

COPY backend/src ./src
RUN mvn -B -q package -DskipTests

# ---- 3. Etapa de Runtime Final Ligera y Segura ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Crear usuario sin privilegios para ejecución segura (no-root)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copiar artefactos empaquetados desde las etapas previas
COPY --from=backend-build /src/backend/target/sga-secretaria-backend.jar app.jar
COPY --from=frontend-build /src/client/dist client/dist

# Asegurar propiedad de archivos para el usuario no privilegiado
RUN chown -R appuser:appgroup /app

# Exponer el puerto del microservicio
ENV PORT=5176
EXPOSE 5176

# Healthcheck nativo consultando Actuator Health
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:5176/actuator/health || exit 1

# Cambiar al usuario no-root
USER appuser

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
