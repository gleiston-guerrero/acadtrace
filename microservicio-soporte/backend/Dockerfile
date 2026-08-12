# Stage 1: Build backend (Spring Boot)
FROM maven:3.9-eclipse-temurin-21 AS backend-builder
WORKDIR /build
COPY backend/pom.xml .
COPY backend/src ./src
RUN mvn clean package -DskipTests

# Stage 2: Build frontend (React + Vite)
FROM node:20-alpine AS frontend-builder
WORKDIR /web
COPY package.json package-lock.json ./
RUN npm ci
COPY vite.config.js index.html tailwind.config.js postcss.config.js ./
COPY public ./public
COPY src ./src
RUN npm run build

# Stage 3: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-builder /build/target/sga-soporte-backend.jar app.jar
COPY --from=frontend-builder /web/dist ./dist
EXPOSE 5178
ENTRYPOINT ["java", "-jar", "app.jar"]
