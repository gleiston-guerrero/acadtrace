# AcadTrace

> **Nota histórica:** Este proyecto se denominó anteriormente *SGA — Escuela Provincias Unidas*. A partir de la Entrega 4 adopta oficialmente la denominación **AcadTrace** (*Capa de auditoría verificable para expedientes académicos en sistemas escolares distribuidos*).

Sistema distribuido desacoplado bajo arquitectura de Microservicios con capa de auditoría verificable y criptográfica (SHA-256 / Relojes de Lamport y Vectoriales) para la gestión académica, control docente, asistencias, administración de matrícula y soporte técnico. La arquitectura se comunica mediante Protocolos Híbridos (REST API y gRPC de alto rendimiento) con persistencia de datos distribuida en PostgreSQL sobre AWS EC2.

---


## Arquitectura General y Mapeo de Puertos

El sistema esta compuesto por un modulo principal y tres microservicios autonomos:

| Servicio | Tecnologia Backend | Puerto REST | Puerto gRPC | Puerto Frontend | Responsabilidad Principal |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **SGA Principal** | Java 21 (Spring Boot) | 8080 | 9092 | 5173 | Core Academico, Usuarios, Autenticacion y Modulos |
| **Microservicio Docente** | Python 3.12 (Django REST) | 8081 | 9091 | 5174 | Gestion de Asistencia, Evaluaciones y Calificaciones |
| **Microservicio Secretaria** | Java 21 (Spring Boot) | 8082 | 9093 | 5176 | Control de Tramites, Certificados, Matricula y Auditoria HMAC |
| **Microservicio Soporte** | Java 17 (Spring Boot) | 8083 | 9094 | 8083 | Tickets de Incidencias, Eleccion de Lider etcd y Actuator |

---
## 🔐 Seguridad y Gestión de Variables de Entorno

En cumplimiento con los estándares de seguridad y la norma **ISO/IEC 25010:2023**:
* Las credenciales de acceso a bases de datos y llaves criptográficas JWT/AES se gestionan exclusivamente mediante **variables de entorno** (`.env`) y secretos de GitHub Actions (`secrets.EC2_SSH_KEY`).
* Se provee la plantilla formal [`.env.example`](.env.example) con la estructura requerida para el despliegue del clúster distribuido en AWS o en local.
* Por higiene de seguridad en repositorios públicos, las contraseñas no se almacenan en texto plano.

---
## 🗺️ Mapa de Estructura del Repositorio y Trazabilidad (Criterio E16)

En cumplimiento con el Listado 3 de la guía de consolidación, a continuación se detalla el mapeo entre la estructura prescrita y los módulos del proyecto:

| Estructura Prescrita | Carpeta en AcadTrace | Contenido y Responsabilidad | Comando de Reproducción |
| :--- | :--- | :--- | :--- |
| `src/core` | `sga-principal/` | Núcleo académico en Spring Boot 3 / Java 21, autenticación JWT, entidades JPA y gRPC server (:9092) | `cd sga-principal && ./mvnw test` |
| `src/docente` | `microservicio-docente/` | Gestión de evaluaciones, asistencia y auditoría criptográfica SHA-256 en Django 5 / Python 3.12 | `cd microservicio-docente && pytest` |
| `src/secretaria` | `microservicio-secretaria/` | Trámites, emisión de certificados y bitácora con HMAC en Spring Boot | `cd microservicio-secretaria && ./mvnw test` |
| `src/soporte` | `microservicio-soporte/` | Sistema de tickets, elección de líder etcd y trazabilidad Zipkin | `cd microservicio-soporte && ./mvnw test` |
| `apps/mobile` | `app-movil-docente/` | Cliente nativo Android (Kotlin/Jetpack Compose) con persistencia offline Room | `cd app-movil-docente && ./gradlew test` |
| `apps/web` | `sga-principal/sga-frontend/` | Portal web reactivo en React + TypeScript y Vite | `npm run build` |
| `infra/gateway` | `infra/haproxy/` | Balanceador perimetral HAProxy 2.9 (HTTP y gRPC) | `docker compose up haproxy -d` |
| `infra/observability`| `infra/prometheus/`, `infra/grafana/` | Métricas Prometheus (:9090) y tableros Grafana (:3001) | `docker compose up prometheus grafana -d`|
| `docs/experiments` | `experimentos/`, `docs/experimentos/` | Scripts de verificación de bitácora y datasets de reproducibilidad | `python experimentos/verificador_cadena.py` |

---

## Guia de Ejecucion

Existen dos alternativas para poner en marcha el sistema:

---

### Opcion A: Ejecucion Automatizada con Un Solo Comando (Piso P3 - Recomendado)

Pone en marcha todos los contenedores de backend, gateway y microservicios con un solo comando:

# 1. Clonar el repositorio
git clone https://github.com/LEO23as/acadtrace.git
cd acadtrace

# 2. Levantar el stack completo (valida entorno, .env y levanta los servicios)
./scripts/start.sh
```

Alternativamente con Docker Compose directo:
```bash
docker compose up --build -d
```

* Acceso Frontend Principal: http://localhost:5173
* Acceso API Gateway HAProxy: http://localhost:8080 (Dashboard: http://localhost:8404)
* Acceso Monitoreo Prometheus: http://localhost:9090
* Acceso Tableros Grafana: http://localhost:3001 (credenciales: admin / admin)

---

### Opcion B: Ejecucion Manual Local Paso a Paso

Si se requiere ejecutar los componentes de manera individual en consolas independientes:

#### 1. SGA Principal (Spring Boot)
```bash
cd sga-principal
mvn spring-boot:run
```
* Servidor activo en: http://localhost:8080

#### 2. Microservicio Docente (Django REST & gRPC)
Abrir dos consolas en la carpeta `microservicio-docente`:

* **Consola 1 (Servidor REST):**
  ```bash
  cd microservicio-docente
  python manage.py runserver 0.0.0.0:8081
  ```
* **Consola 2 (Servidor gRPC):**
  ```bash
  cd microservicio-docente
  python manage.py rungrpcserver
  ```

#### 3. Microservicio Secretaria (Spring Boot)
```bash
cd microservicio-secretaria/backend
mvn spring-boot:run
```
* Servidor activo en: http://localhost:8082 (o 5176)

#### 4. Microservicio Soporte (Spring Boot)
```bash
cd microservicio-soporte/backend
mvn spring-boot:run
```
* Servidor activo en: http://localhost:8083

#### 5. Frontend Unificado React
```bash
cd sga-principal/sga-frontend
npm install
npm run dev
```
* Aplicacion web lista en: http://localhost:5173

---

## Ejecucion de Suites de Pruebas Automatizadas

```bash
# 1. Pruebas de Contratos (gRPC y OpenAPI), Integracion y E2E:
python -m pytest tests/contract tests/integration tests/e2e -v

# 2. Banco Experimental Cuantitativo (Modulo G) y Metricas ISO 25010:
python -m pip install -r experimentos/requirements.txt
python experimentos/run_experimentos.py --mode local
python experimentos/verificar_reproducibilidad.py
```

E7: [alcance, dependencias y verificación](experimentos/REPRODUCIBILIDAD.md).
La generación local usa datos sintéticos y mediciones temporales variables;
SHA-256 verifica integridad de una ejecución concreta, no hashes idénticos entre
ejecuciones. El modo HTTP requiere selección explícita. La regeneración y las
pruebas del proceso corregido siguen pendientes de un entorno con Python.

---

## Verificacion de Comunicacion gRPC y Tolerancia a Fallos

Para realizar la demostracion practica del protocolo gRPC en tiempo real:

1. Iniciar el SGA Principal (8080) y el Servidor gRPC de Docente (9091).
2. Apagar el servidor REST del docente (detener el proceso en puerto 8081).
3. Navegar en el Frontend a http://localhost:5173/grados y seleccionar un curso/materia.
4. El SGA Principal continuara solicitando y recibiendo la informacion de la base de datos y notificando las transacciones directamente mediante el canal gRPC (9092 - 9091) sin depender de la API REST de docente.

---

## Consulta SQL de Verificacion de Datos

Para verificar los registros insertados en el esquema docente desde cualquier cliente PostgreSQL (pgAdmin / DBeaver):

```sql
SELECT 
    a.id_asistencia, 
    a.fecha, 
    a.estado, 
    a.id_asignacion, 
    CONCAT(e.apellidos, ' ', e.nombres) AS estudiante,
    m.id_matricula
FROM sga_docente.asistencias a
JOIN sga_principal.matriculas m ON m.id_matricula = a.id_matricula
JOIN sga_principal.estudiantes e ON e.id_estudiante = m.id_estudiante
ORDER BY a.fecha DESC, a.id_asistencia DESC
LIMIT 20;
```

---

## Requisitos del Sistema

* **Java JDK:** 17 o superior
* **Python:** 3.10 o superior (con django, djangorestframework, grpcio, grpcio-tools, psycopg2-binary)
* **Node.js:** v18.0.0 o superior (npm v9+)
* **Docker & Docker Compose:** (Opcional para despliegue en contenedores)

---

Juliana-Emanuel
## Declaracion de Uso de Inteligencia Artificial

En cumplimiento de la transparencia academica exigida por la catedra, se declara el uso de asistentes de IA (Claude, Antigravity/Gemini) durante el desarrollo de la Entrega 4 del PFC, con el siguiente alcance:

| Integrante | Herramienta | Proposito del uso | Revision realizada |
|---|---|---|---|
| Emanuel Pino Juliana (microservicio-soporte, Observabilidad) | Claude, Antigravity | Generacion de locustfile.py (prueba de carga), configuracion de Prometheus/remote_write a Grafana Cloud, paneles adicionales del dashboard (P50/P99/errores 4xx-5xx), correccion de vulnerabilidad de secreto JWT hardcodeado, redaccion asistida de la Seccion 5.4, Reflexion Etica, Anexos y Conclusion Individual del informe LaTeX | Se ejecutaron localmente todas las pruebas de carga y se verificaron sus resultados reales (CSV/dashboard) antes de documentarlos; se corrigieron manualmente discrepancias entre corridas (ver Anexo A del informe); se verifico que ninguna clave o credencial real quedara expuesta en los archivos subidos al repositorio |

*(Los demas integrantes deben completar su fila correspondiente segun el uso que hayan dado a estas u otras herramientas de IA en sus propios modulos.)*

Ningun contenido generado por IA fue incorporado sin revision humana previa; los hallazgos tecnicos documentados (cuello de botella, tasas de error, latencias) provienen de ejecuciones reales de las herramientas (Locust, Prometheus, Grafana) sobre el sistema, no de datos simulados o inventados por el modelo de IA.

---

## Compilacion del Informe LaTeX

Los informes individuales de cada integrante (carpeta `Informe-E4_BCEL/`) se compilan con `pdflatex` (TeX Live 2023 o superior). Desde la carpeta `Informe-E4_BCEL/`:

```bash
# 1ra pasada: genera el .aux con las referencias de citas pendientes
pdflatex -interaction=nonstopmode TA_PFC_E4_Soporte.tex

# Resuelve las citas bibliograficas contra referencias.bib
bibtex TA_PFC_E4_Soporte

# 2da y 3ra pasada: incorpora la bibliografia resuelta y fija la numeracion
# de figuras/secciones cruzadas (se corre dos veces por convencion de LaTeX)
pdflatex -interaction=nonstopmode TA_PFC_E4_Soporte.tex
pdflatex -interaction=nonstopmode TA_PFC_E4_Soporte.tex
```

El PDF resultante es `TA_PFC_E4_Soporte.pdf`, en la misma carpeta. El mismo procedimiento aplica para el resto de informes individuales del equipo (reemplazando el nombre del archivo `.tex`).

## 🤖 Declaración de Uso de Inteligencia Artificial Generativa

En cumplimiento con los lineamientos académicos e institucionales, se declara el uso ético y transparente de herramientas de Asistencia de Inteligencia Artificial (Google Antigravity / Gemini 2.5 Pro) durante el desarrollo de la Entrega 4 del proyecto **AcadTrace**:

* **Propósito del uso:** Generación de estructuras base para pruebas unitarias (`Mockito`), depuración de configuraciones de pipelines CI/CD en YAML y asistencia en sintaxis LaTeX.
* **Supervisión y Verificación Humana:** Todo el código generado, configuraciones de infraestructura en AWS EC2, reglas de negocio en Java/Python y redacción del informe técnico fueron rigurosamente revisados, ejecutados, medidos y validados por los 4 integrantes del equipo **BCEL** (Leonardo Castro, Keyla Bedon, Gregory Luna y Romina Emanuel).
* **Autoría:** La lógica académica transaccional, el modelo de datos distribuido y los resultados experimentales son de autoría exclusiva del equipo de trabajo.

---

## 📄 Instrucciones de Compilación del Documento LaTeX Acumulativo

El informe técnico final acumulativo de la Entrega 4 se encuentra en la carpeta `Informe-E4_BCEL/` y se compila de manera reproducible siguiendo estos pasos:

### Prerrequisitos:
Tener instalado una distribución completa de TeX Live (`pdflatex`, `bibtex`):
```bash
sudo apt-get install texlive-latex-base texlive-latex-extra texlive-fonts-recommended texlive-lang-spanish
```

### Compilación limpia del informe maestro:
```bash
cd Informe-E4_BCEL
pdflatex -interaction=nonstopmode TA-PFC-E4_BCEL.tex
bibtex TA-PFC-E4_BCEL
pdflatex -interaction=nonstopmode TA-PFC-E4_BCEL.tex
pdflatex -interaction=nonstopmode TA-PFC-E4_BCEL.tex
```
*(El PDF final resultante se generará en `Informe-E4_BCEL/TA-PFC-E4_BCEL.pdf`).*

## Resultados oficiales de carga — E5

La única corrida **OFICIAL NOMINAL de Soporte** es [locust_esc1_stats.csv](microservicio-soporte/locust_esc1_stats.csv), junto con [su historial](microservicio-soporte/locust_esc1_stats_history.csv), [fallos](microservicio-soporte/locust_esc1_failures.csv) y [excepciones](microservicio-soporte/locust_esc1_exceptions.csv). La [tabla E5](experimentos/resultados/corridas-e5.md) clasifica las demás corridas y conserva las referencias históricas.

Es una **prueba de carga reproducible ejecutada en entorno local/contenedorizado**. Perfil configurado: 50 usuarios virtuales, spawn rate 5 usuarios/s, 5 minutos y `http://localhost:8083`. No representa tráfico real de producción.

| Métrica de Aggregated | Valor oficial |
|---|---|
| Peticiones / fallos | **12.994 / 0**; sin HTTP 401 ni HTTP 500 registrados |
| RPS | 43,537580 req/s |
| Promedio | 109,113664 ms |
| P50 / P95 / P99 | 6 / 440 / 850 ms |
| Máximo | 2.037,104700 ms |

Inicio registrado: **2026-09-11 03:59:05 UTC** (2026-09-10 22:59:05 UTC−05:00); 299 segundos entre muestras. Commit de conservación: `956cafcb` (normalización posterior `c5c0e6f5`). Commit del código ejecutado: **No disponible en la evidencia conservada**.

**Corrida oficial de estrés: NO DISPONIBLE — las evidencias conservadas no satisfacen el criterio.** El conjunto D es FALLIDA/HISTÓRICA: 106.735 peticiones, 26 fallos (15 HTTP 500 y 11 HTTP 503).

La [captura de Juliana](evidencias/Juliana_Emanuel/backend/pruebas-carga/image.png) es histórica/complementaria: muestra 13.031 peticiones en terminal, mientras el CSV oficial contiene 12.994. La causa no está demostrada; prevalece el CSV. Está pendiente una captura manual de su ruta y fila `Aggregated` con todas las métricas. **E5: PARCIAL** por esa evidencia visual y la ausencia de estrés válido.
