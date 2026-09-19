# Interfaz Web — Microservicio Docente (AcadTrace)

Portal web docente para gestión de cursos, evaluaciones, calificaciones, actividades y registro de asistencia académica dentro del ecosistema distribuido AcadTrace.

## Tecnologías principales

- **Framework UI:** React 19 (`react`, `react-dom`)
- **Enrutamiento:** React Router DOM 7 (`react-router-dom`)
- **Estilos:** Tailwind CSS 4 (`@tailwindcss/vite`)
- **Empaquetador y servidor de desarrollo:** Vite 8
- **Pruebas de navegador E2E:** Playwright (`@playwright/test`)

## Requisitos previos

- **Node.js:** `^20.19.0 || >=22.12.0` (alineado con la política de soporte de Vite 8 y el flujo de CI)
- **npm:** `10+`

## Instalación reproducible desde un clon limpio

Para garantizar instalaciones reproducibles y deterministas, use siempre `npm ci` en lugar de `npm install`:

```bash
cd microservicio-docente/frontend
npm ci --no-audit
```

## Ejecución en desarrollo (independiente fuera de Compose)

Para iniciar el servidor local de desarrollo con Hot Module Replacement (HMR):

```bash
npm run dev
```

El servidor Vite quedará disponible en:

```text
http://localhost:3000
```

## Compilación para producción

Para construir el bundle optimizado para despliegue:

```bash
npm run build
```

Los artefactos listos para producción se generan en el directorio `dist/`.

## Previsualización de la compilación

Para previsualizar localmente los archivos compilados en `dist/`:

```bash
npm run preview
```

## Análisis estático (Linting)

Para ejecutar el linter rápido con Oxlint:

```bash
npm run lint
```

## Pruebas de extremo a extremo (E2E)

Para ejecutar la suite de pruebas de navegador con Playwright contra el sistema en ejecución:

```bash
npm run test:e2e
```

## Integración arquitectónica

- **Backend Docente:** consume las rutas API REST del microservicio docente (`/api/docente/*`) en el puerto 8000 (o 8081 en Compose).
- **SGA Principal:** valida la sesión y autenticación del docente mediante el token JWT institucional en el puerto 8080.
- **Despliegue integrado:** el `docker-compose.yml` raíz levanta esta interfaz en el contenedor `docente-frontend` exponiendo el puerto 3000.
