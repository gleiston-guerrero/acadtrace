# syntax=docker/dockerfile:1
#
# Build de contexto = raiz del repo (docker build -f backend/Dockerfile .),
# porque el jar necesita empaquetar tambien client/dist (frontend estatico
# que sirve WebConfig). Ver docker-compose.yml en la raiz.

# ---- Frontend (Vite) ----
FROM node:20-alpine AS frontend-build
WORKDIR /src/client
COPY client/package*.json ./
RUN npm ci
COPY client/ ./
RUN npm run build

# ---- Backend (Maven) ----
FROM maven:3.9-eclipse-temurin-21 AS backend-build
WORKDIR /src/backend
COPY backend/pom.xml ./
RUN mvn -B -q dependency:go-offline || true
COPY backend/src ./src
RUN mvn -B -q package -DskipTests

# ---- Runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-build /src/backend/target/sga-secretaria-backend.jar app.jar
COPY --from=frontend-build /src/client/dist client/dist

# PORT/DB_*/JWT_SECRET/AES_SECRET_KEY se pasan como variables de entorno
# (ver .env.example) o via docker-compose "environment:".
EXPOSE 5176
ENTRYPOINT ["java", "-jar", "app.jar"]
