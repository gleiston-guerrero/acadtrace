# Evidencias — Criterio E14: Publicación de Imágenes Docker

**Responsable:** Juliana  
**Criterio:** E14 – Publicación de imágenes Docker  
**Rama:** `Juliana-Emanuel`  
**Repositorio:** `acadtrace`  

---

## 1. Objetivo de la Evidencia

Demostrar la configuración del flujo de integración continua en GitHub Actions (`.github/workflows/ci-cd.yml`) para la construcción y publicación de las cinco imágenes Docker de los microservicios del sistema AcadTrace en GitHub Container Registry (GHCR), utilizando una estrategia de matrix y control estricto de publicación y etiquetado por rama.

---

## 2. Lista de Capturas

1. `e14_matrix_imagenes.png`
2. `e14_publicacion_ghcr.png`

---

## 3. Descripción de las Capturas y Explicación Técnica

### `e14_matrix_imagenes.png`
Muestra la sección del job `build-images` en `.github/workflows/ci-cd.yml` donde se define la matrix de compilación con las cinco imágenes propias del proyecto:
1. `sga-principal` (context: `sga-principal`, dockerfile: `sga-principal/Dockerfile`)
2. `microservicio-docente` (context: `microservicio-docente`, dockerfile: `microservicio-docente/Dockerfile`)
3. `microservicio-secretaria` (context: `microservicio-secretaria`, dockerfile: `microservicio-secretaria/backend/Dockerfile`)
4. `microservicio-soporte` (context: `microservicio-soporte`, dockerfile: `microservicio-soporte/backend/Dockerfile`)
5. `microservicio-ia` (context: `microservicio-ia`, dockerfile: `microservicio-ia/Dockerfile`)

Se evidencia la configuración de `fail-fast: false` para asegurar la compilación independiente de cada componente.

### `e14_publicacion_ghcr.png`
Muestra la configuración de los pasos de autenticación y publicación en GHCR:
- **Paso `Autenticación en GitHub Container Registry (GHCR)`**: Utiliza `docker/login-action@v3` autenticándose mediante `secrets.GITHUB_TOKEN` únicamente cuando el evento no es un Pull Request y la rama corresponde a `refs/heads/main`.
- **Paso `Preparar tags de imagen`**: Genera dinámicamente el tag inmutable `${IMAGE}:${GITHUB_SHA}` para todas las ejecuciones, y restringe la inclusión del tag `${IMAGE}:latest` exclusivamente cuando la publicación está habilitada en `main`.
- **Paso `Construir imagen y publicar solo en main`**: Utiliza `docker/build-push-action@v6` con `push: true` en `main` y `push: false` en ramas feature / PR, impidiendo la publicación no autorizada fuera de la rama principal.