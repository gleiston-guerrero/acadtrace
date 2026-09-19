# AcadTrace

> **Nota histórica:** Este proyecto se denominó anteriormente *SGA — Escuela Provincias Unidas*. A partir de la Entrega 4 adopta oficialmente la denominación **AcadTrace** (*Capa de auditoría verificable para expedientes académicos en sistemas escolares distribuidos*).

Sistema distribuido desacoplado bajo arquitectura de Microservicios con capa de auditoría verificable y criptográfica (SHA-256 / Relojes de Lamport y Vectoriales) para la gestión académica, control docente, asistencias, administración de matrícula y soporte técnico. La arquitectura se comunica mediante Protocolos Híbridos (REST API y gRPC de alto rendimiento) con persistencia relacional en un endpoint PostgreSQL compartido con separación lógica multiesquema (véase [ADR-003](docs/adr/ADR-003-persistencia-distribuida.md)).

---

Los tres niveles C4 canónicos y sus instrucciones de regeneración están documentados en [docs/diagrams/README.md](docs/diagrams/README.md).


## Arquitectura General y Mapeo de Puertos

El sistema está compuesto por un módulo principal y cuatro microservicios autónomos:

| Servicio | Tecnología Backend | Puerto REST | Puerto gRPC | Puerto Frontend | Responsabilidad Principal |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **SGA Principal** | Java 21 (Spring Boot) | 8080 | 9092 | 5173 | Core Académico, Usuarios, Autenticación y Módulos |
| **Microservicio Docente** | Python 3.12 (Django REST) | 8081 | 9091 | 5174 | Gestión de Asistencia, Evaluaciones y Calificaciones |
| **Microservicio Secretaría** | Java 21 (Spring Boot) | 8082 | 9093 | 5176 | Control de Trámites, Certificados, Matrícula y Auditoría HMAC |
| **Microservicio Soporte** | Java 17 (Spring Boot) | 8083 | 9094 | 8083 | Tickets de Incidencias, Elección de Líder etcd y Actuator |
| **Microservicio IA** | Python (FastAPI) | 8084 | — | — | Diagnóstico académico y asistencia mediante inteligencia artificial |

> **Nota sobre puertos:** Los puertos REST corresponden a los servicios backend. Los puertos de frontend pueden representar el acceso publicado por Docker Compose o el servidor de desarrollo Vite, según el modo de ejecución de cada componente.


---
## 🔐 Seguridad y Gestión de Variables de Entorno

En cumplimiento con los estándares de seguridad y la norma **ISO/IEC 25010:2023**:
* Las credenciales de acceso a bases de datos y llaves criptográficas JWT/AES se gestionan exclusivamente mediante **variables de entorno** (`.env`) y secretos de GitHub Actions (`secrets.EC2_SSH_KEY`).
* Se provee la plantilla formal [`.env.example`](.env.example) con la estructura requerida para el despliegue del clúster distribuido en AWS o en local.
* Por higiene de seguridad en repositorios públicos, las contraseñas no se almacenan en texto plano.

---
## 🗺️ Mapa de Estructura del Repositorio y Trazabilidad (Criterio E16)

Según la estructura de consolidación documentada en el proyecto, el siguiente mapa relaciona componentes lógicos con su ubicación física actual. Los elementos de la estructura prescrita son referencias lógicas, no carpetas que deban existir con esos nombres. Las filas complementarias describen componentes y recursos reales; no atribuyen nuevos nombres a la prescripción. La fuente normativa original del denominado "Listado 3 de la guía de consolidación" no está disponible para verificación independiente.

| Estructura Prescrita | Carpeta en AcadTrace | Contenido y Responsabilidad | Comando de Reproducción |
| :--- | :--- | :--- | :--- |
| `src/core` | [`sga-principal/`](sga-principal/) | Núcleo académico en Spring Boot 3 / Java 21, autenticación JWT, entidades JPA y gRPC server (:9092) | `cd sga-principal && ./mvnw test` |
| `src/docente` | [`microservicio-docente/`](microservicio-docente/) | Gestión de evaluaciones, asistencia y auditoría criptográfica SHA-256 en Django 5 / Python 3.12 | `cd microservicio-docente && python -m venv .venv && .venv\\Scripts\\activate && pip install -r requirements.txt && GRPC_INTERNAL_TOKEN=test-internal-token pytest` |
| `src/secretaria` | [`microservicio-secretaria/`](microservicio-secretaria/) | Trámites, emisión de certificados y bitácora con HMAC en Spring Boot | `cd microservicio-secretaria/backend && ./mvnw test` |
| `src/soporte` | [`microservicio-soporte/`](microservicio-soporte/) | Sistema de tickets, elección de líder etcd y trazabilidad Zipkin | `cd microservicio-soporte/backend && ./mvnw test` |
| Inteligencia artificial (complementario) | [`microservicio-ia/`](microservicio-ia/) | Microservicio FastAPI para diagnóstico académico y asistencia mediante inteligencia artificial | No aplica al mapa |
| `apps/mobile` | [`app-movil-docente/`](app-movil-docente/) | Ruta física actual de la aplicación móvil para representantes; cliente Android (Kotlin/Jetpack Compose) con persistencia offline Room | `cd app-movil-docente && ./gradlew test` |
| `apps/web` — Principal | [`sga-principal/sga-frontend/`](sga-principal/sga-frontend/) | Portal web de Principal en React y Vite | `cd sga-principal/sga-frontend && npm ci --no-audit && npm run build` |
| `apps/web` — Secretaría | [`microservicio-secretaria/client/`](microservicio-secretaria/client/) | Interfaz web de Secretaría | No aplica al mapa |
| `apps/web` — Docente | [`microservicio-docente/frontend/`](microservicio-docente/frontend/) | Interfaz web de Docente | No aplica al mapa |
| `apps/web` — Soporte | [`microservicio-soporte/src/`](microservicio-soporte/src/) | Código fuente de la interfaz web de Soporte | No aplica al mapa |
| Infraestructura (complementario) | [`infra/`](infra/), [`docker-compose.yml`](docker-compose.yml) | Configuración de infraestructura del sistema, gateway y despliegue; el Compose raíz integra los servicios y componentes operativos | No aplica al mapa |
| `infra/gateway` | [`infra/haproxy/`](infra/haproxy/) | Balanceador perimetral HAProxy 2.9 (HTTP y gRPC) | `docker compose up haproxy -d` |
| `infra/observability` | [`infra/prometheus/`](infra/prometheus/), [`infra/grafana/`](infra/grafana/), [`docker-compose.yml`](docker-compose.yml) | Observabilidad con Prometheus (:9090), Grafana (:3001) y Dozzle (:8888); el Compose raíz contiene su configuración de despliegue | `docker compose up prometheus grafana dozzle -d` |
| Operación (complementario) | [`ops/`](ops/) | Recursos y configuración operativa de Prometheus y Grafana, en una ubicación distinta de la infraestructura agrupada en `infra/` | No aplica al mapa |
| `docs/experiments` | [`experimentos/`](experimentos/), [`docs/experimentos/`](docs/experimentos/) | Scripts de verificación de bitácora y datasets de reproducibilidad | `DB_HOST=<host> DB_PORT=<port> DB_USER=<user> DB_PASSWORD=<pass> DB_NAME=sga python experimentos/verificador_cadena.py` |
| Documentación técnica (complementario) | [`docs/`](docs/), [`docs/api/openapi.yaml`](docs/api/openapi.yaml), [`docs/api/README.md`](docs/api/README.md) | Arquitectura, seguridad, documentación API y contrato OpenAPI versionado | No aplica al mapa |
| Utilidades (complementario) | [`scripts/`](scripts/), [`scripts/verificar_openapi.py`](scripts/verificar_openapi.py), [`docs/api/validate_openapi.py`](docs/api/validate_openapi.py) | Utilidades del proyecto; verificación del contrato OpenAPI runtime y validación estática del contrato versionado | No aplica al mapa |
| Entregables (complementario) | [`release/`](release/) | Artefactos y capturas asociados al Release v1.0.1 y su manifiesto oficial | No aplica al mapa |
| Trazabilidad del release (complementario) | [`docs/evidencias/release/`](docs/evidencias/release/) | Manifiesto y documentación de las evidencias del release | No aplica al mapa |
| Informe académico (complementario) | [`Informe-E4_BCEL/`](Informe-E4_BCEL/), [`TA-PFC-E4_BCEL.tex`](Informe-E4_BCEL/TA-PFC-E4_BCEL.tex) | Informe académico acumulativo del proyecto y sus recursos | No aplica al mapa |
| Evidencias (complementario) | [`evidencias/`](evidencias/) | Evidencias organizadas del proyecto y por integrantes | No aplica al mapa |
| Pruebas generales (complementario) | [`tests/`](tests/) | Pruebas generales del proyecto | No aplica al mapa |

> Nota E16: el comando de `microservicio-docente` requiere la variable
> `GRPC_INTERNAL_TOKEN`. El valor `test-internal-token` es el mismo que fija
> `.github/workflows/ci-cd.yml` para las corridas de pytest. En Windows
> PowerShell usar `$env:GRPC_INTERNAL_TOKEN="test-internal-token"; pytest`.

---

## Cobertura documentada (E8)

La tabla central de cobertura, con la separación entre el último reporte local conservado de Soporte y las cifras históricas no regeneradas de otros módulos, está en [`docs/cobertura/README.md`](docs/cobertura/README.md). No se presenta ninguna cifra histórica como cobertura actual.

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

#### 5. Frontend de Principal (React)
```bash
cd sga-principal/sga-frontend
npm ci --no-audit
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

## Publicación de imágenes Docker (E14)

El job `build-images` de `.github/workflows/ci-cd.yml` publica imágenes propias
en `ghcr.io/<repository_owner-en-minúsculas>/<imagen>`, mediante `GITHUB_TOKEN`.
Conserva las dependencias `test-backend`, `test-soporte-backend` y `test-web`.

| Imagen | Contexto de construcción | Dockerfile desde la raíz |
|---|---|---|
| `sga-principal` | `sga-principal` | `sga-principal/Dockerfile` |
| `microservicio-docente` | `microservicio-docente` | `microservicio-docente/Dockerfile` |
| `microservicio-secretaria` | `microservicio-secretaria` | `microservicio-secretaria/backend/Dockerfile` |
| `microservicio-soporte` | `microservicio-soporte` | `microservicio-soporte/backend/Dockerfile` |
| `microservicio-ia` | `microservicio-ia` | `microservicio-ia/Dockerfile` |

Solo publica en `main`, tras un evento `push` o `workflow_dispatch` y cuando
sus dependencias terminan correctamente. Cada imagen recibe el SHA completo
del commit como tag y `latest`; este último se aplica exclusivamente a `main`.
Las ramas feature y los pull requests no publican ni sobrescriben `latest`.

Las imágenes externas usadas por el Compose raíz (HAProxy, Spark, Zipkin, etcd,
Node para frontends de desarrollo, Prometheus, Grafana, cAdvisor y
postgres-exporter) no se republican. PostgreSQL en el Compose de Principal
también es externo. Las imágenes base de los Dockerfiles son dependencias,
no módulos propios a publicar.

Esta configuración publica paquetes; no cambia el despliegue existente, que
continúa construyendo mediante Compose. La construcción y publicación efectiva
de las cinco imágenes deberá confirmarse en GitHub Actions/GHCR después de
integrar y enviar los cambios. La modificación local no acredita publicación.

## Requisitos del Sistema

* **Java JDK:** JDK 17 o 21 (JDK 25 NO es compatible con Lombok 1.18.36 ni con JaCoCo 0.8.11)
* **Python:** 3.10 o superior (con django, djangorestframework, grpcio, grpcio-tools, psycopg2-binary)
* **Node.js:** ^20.19.0 || >= 22.12.0 (recomendado LTS v22.x, npm v10+)
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
python ../scripts/recalcular_metricas_carga.py --generate-latex --check-latex
python ../generar_matriz.py --write-csv --write-latex
pdflatex -interaction=nonstopmode TA-PFC-E4_BCEL.tex
bibtex TA-PFC-E4_BCEL
pdflatex -interaction=nonstopmode TA-PFC-E4_BCEL.tex
pdflatex -interaction=nonstopmode TA-PFC-E4_BCEL.tex
```
`generar_matriz.py` usa solo la biblioteca estándar de Python (CI usa Python 3.13) y genera tanto `docs/experimentos/resultados/matriz_iso25010.csv` como `Informe-E4_BCEL/matriz_iso25010_generada.tex`. El manuscrito incorpora el segundo mediante `\input{matriz_iso25010_generada.tex}`.

Fuentes y límites de la evaluación #22:

- **Nominal local:** `microservicio-soporte/locust_esc1_stats.csv` (fila `Aggregated`) y `locust_esc1_stats_history.csv` (máximo `User Count`). P99 es una medición; el umbral estricto de 500 ms procede del criterio documental PI-1.
- **Histórica / perfil no validado:** ventanas de `experimentos/resultados/iso25010.csv` derivadas de `experimentos/resultados/locust_esc3_stats_history.csv`. No constituyen estrés oficial actual ni disponibilidad temporal de producción.
- **Sintética:** observaciones M2 de `experimentos/resultados/falsos_positivos.csv`; no miden seguridad en producción.
- **Cobertura:** contadores globales `LINE` de `docs/cobertura/secretaria/jacoco.xml` y `docs/cobertura/soporte/jacoco.xml`; porcentaje calculado como `covered / (covered + missed) * 100`, con las exclusiones de esos reportes. Se compara sin redondear con el criterio de la matriz de 70% LINE. Reporte ausente o inválido impide generar; no se sustituye por porcentajes de respaldo.
- **Principal:** `No verificable con la evidencia versionada`. Su compuerta particular de 30% INSTRUCTION no es la métrica LINE de la matriz. Las compuertas de Secretaría y Soporte son 70% LINE, según sus respectivos `pom.xml`.

Los reportes versionados no prueban una nueva ejecución sobre el HEAD actual. La integridad de un hash tampoco demuestra vigencia de una medición.

Desde la raíz, regenerar ambos artefactos y comprobar que coinciden con las versiones almacenadas (la comprobación falla si hay cambios pendientes en ellos):

```bash
python generar_matriz.py --write-csv --write-latex
git diff --exit-code -- docs/experimentos/resultados/matriz_iso25010.csv Informe-E4_BCEL/matriz_iso25010_generada.tex
```

El job de matriz ISO en CI ejecuta esta misma comprobación para ambos archivos. Para consultar sin escribir, usar `python generar_matriz.py --preview --format json` (también admite `csv` y `latex`). La generación reproduce los artefactos desde las fuentes conservadas; no ejecuta carga ni regenera las mediciones históricas.

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

La captura histórica/complementaria de Juliana no está disponible en el árbol actual. La descripción conservada le atribuye 13.031 peticiones en terminal, mientras el CSV oficial contiene 12.994. La causa no está demostrada; prevalece el CSV. Está pendiente una captura manual de su ruta y fila `Aggregated` con todas las métricas. **E5: PARCIAL** por esa evidencia visual y la ausencia de estrés válido.


### Reproducción de cifras de carga (#48)

```bash
python scripts/recalcular_metricas_carga.py --json
python scripts/recalcular_metricas_carga.py --check-latex
python scripts/recalcular_metricas_carga.py --emit-latex-block
```

`--json` informa métricas derivadas de A, auxiliares, endpoints e históricos B y E; no comprueba documentos. `--emit-latex-block` imprime las macros de A sin escribir. `--check-latex` compara las macros almacenadas y las afirmaciones oficiales seleccionadas del manuscrito (incluido el abstract), además de las secciones oficiales de `README.md`, `docs/locust/README.md`, `docs/locust/entorno_medicion.md`, ambos `protocolo-e4.md` y la tabla de métricas de A en `corridas-e5.md`. Termina con error ante discrepancias o métricas requeridas ausentes. No es un parser general: no valida todo el manuscrito, imágenes, fechas ni resultados históricos. Las cifras se derivan de A, admitiendo el redondeo publicado; cero fallos no demuestra disponibilidad de producción.

Antes de compilar, `--generate-latex --check-latex` regenera `Informe-E4_BCEL/cifras_carga_generadas.tex` desde A y comprueba las publicaciones oficiales. Para comprobar sin regenerar, usar solo `--check-latex`; no confundir regeneración con validación del archivo previamente almacenado. Los pasos de #22 se mantienen independientes.
