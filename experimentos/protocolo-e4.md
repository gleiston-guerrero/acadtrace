# Protocolo Experimental de Pruebas de Carga y Cripto-Auditoría (Entrega 4)

**Proyecto:** AcadTrace — Sistema de Gestión Académica Distribuido  
**Cátedra:** Aplicaciones Distribuidas (PFC Entrega 4)  
**Módulos Evaluados:** `microservicio-secretaria`, `infra/haproxy/`, `sga-principal`, `microservicio-soporte`, `microservicio-docente`  
**Responsable de Calidad y Gateway:** Ernesto Gregory Luna Mora (`elunam4@uteq.edu.ec`)  
**Fecha de Ejecución:** 31 de Agosto de 2026 / 01 de Septiembre de 2026  
**Estándar de Calidad:** ISO/IEC 25010:2023  

---

## 1. Especificaciones del Entorno de Ejecución

Para garantizar la reproducibilidad científica estricta de las mediciones, se documentan las características del hardware, sistema operativo, motor de contenedores y versiones de software del entorno de pruebas:

### 1.1 Hardware del Host de Pruebas (Entorno Local de Medición)
| Componente | Especificación Técnica |
| :--- | :--- |
| **Procesador (CPU)** | AMD Ryzen 5 7520U with Radeon Graphics |
| **Arquitectura** | x86_64 (64-bit), 4 núcleos físicos, 8 procesadores lógicos |
| **Frecuencia de Reloj** | 2.80 GHz (Base) / hasta 4.30 GHz (Max Boost) |
| **Memoria RAM** | 16.0 GB LPDDR5 (15.24 GB visible) |
| **Almacenamiento** | SSD NVMe PCIe M.2 512 GB |
| **Sistema Operativo** | Microsoft Windows 11 Pro 64-bit (Compilación 10.0.26200) |

### 1.2 Entorno de Servidor de Producción / Staging (AWS EC2)
| Componente | Especificación Técnica |
| :--- | :--- |
| **Instancia Cloud** | AWS EC2 `t3.medium` (Región `us-east-1`) |
| **vCPUs y Memoria** | 2 vCPUs Intel Xeon Platinum / 4.0 GB RAM |
| **Sistema Operativo** | Ubuntu 22.04 LTS (Kernel Linux 5.15 x86_64) |
| **IP Pública / Host** | `192.0.2.1` |

### 1.3 Versiones de Software, Motores y Librerías
| Software / Herramienta | Versión Exacta | Propósito en el Sistema |
| :--- | :--- | :--- |
| **Docker Engine** | `29.5.3` (build d1c06ef) | Contenedorización de microservicios |
| **Docker Compose** | `v2.27.0+` | Orquestación local y en AWS EC2 |
| **PostgreSQL** | `16.2-alpine` | Base de datos relacional transaccional (puerto 5433) |
| **HAProxy** | `2.9.5-alpine` | API Gateway perimetral y balanceador de carga |
| **Java JDK** | `OpenJDK 21.0.11 LTS` (Eclipse Temurin) | Runtime para Secretaría, Principal y Soporte |
| **Spring Boot** | `3.2.5` | Framework backend en microservicios Java |
| **Python** | `3.14.6` (Local) / `3.12.3` (Docente) | Ejecución de Locust, análisis estadístico y Django |
| **JaCoCo Plugin** | `0.8.11` | Umbral mínimo de 70 % de cobertura de líneas (LINE) para módulos Java configurados; no es un resultado medido |
| **Locust** | `2.46.4` | Generador de carga distribuida y estrés |
| **Biblioteca estándar de Python** | Incluida con Python | Lectura CSV, mediana, percentil y Bootstrap reproducible de la mediana |
| **Pandas** | `3.0.5` | Procesamiento y persistencia de CSVs de telemetría |
| **NumPy** | `2.5.1` | Manejo vectorial y generadores pseudoaleatorios |
| **Matplotlib** | `3.11.1` | Generación de diagramas boxplot en 300 DPI |

---

## 2. Parámetros y Semillas Fijas del Banco Experimental

El umbral mínimo de cobertura es **70 % de líneas (LINE), medido por JaCoCo, para los módulos Java donde esté configurado**. Esta regla de calidad no debe interpretarse como una cifra obtenida en esta ejecución experimental.

Para eliminar el sesgo estocástico y permitir la replicación exacta de los experimentos factoriales:

- **Semilla Pseudoaleatoria Global:** `SEED = 20260831` (fijada en `random.seed(SEED)` y `np.random.seed(SEED)`).
- **Población Estudiantil:** $N = 344$ estudiantes de educación básica distribuidos uniformemente en 10 grados y paralelos.
- **Población Docente:** $M = 14$ docentes titulares asignados a las materias curriculares.
- **Ponderación de Calificaciones (LOEI):** Evaluación Formativa = 70\%, Evaluación Sumativa = 30\%.
- **Diseño Factorial:** 30 repeticiones $\times$ 4 mecanismos de auditoría $\times$ 5 tipos de manipulación = **600 corridas factoriales independientes**.
- **Muestras Totales de Manipulación:** 600 eventos transaccionales evaluados individualmente.

---

## 3. Definición de Mecanismos de Auditoría Evaluados

| Identificador | Nombre del Mecanismo | Descripción Técnica y Primitivas Empleadas |
| :---: | :--- | :--- |
| **$M_0$** | **Línea Base (Sin auditoría)** | Escritura relacional directa sin registro de bitácora ni cálculo criptográfico. |
| **$M_1$** | **Bitácora SQL Plana** | Inserción en tabla `auditoria` convencional (timestamp local, sin encadenamiento de hash). |
| **$M_2$** | **Criptográfico Encadenado** | Bitácora encadenada mediante resumen **SHA-256** del bloque previo ($H_{i} = \text{SHA256}(H_{i-1} \parallel D_i \parallel L_i)$) y reloj lógico de **Lamport**. |
| **$M_3$** | **Distribuido con Vector Clocks** | Mecanismo $M_2$ complementado con **Relojes Vectoriales** ($V \in \mathbb{N}^{14}$) para orden causal estricto en escenarios de concurrencia y modo desconectado. |

---

## 4. Matriz de Inyección de Manipulaciones ($T_1$ a $T_5$)

| Código | Tipo de Ataque / Manipulación | Vector de Inyección | Regla de Detección Violada | Tasa en $M_2$/$M_3$ | Tasa en $M_0$/$M_1$ |
| :---: | :--- | :--- | :--- | :---: | :---: |
| **$T_1$** | **Alteración de calificación fuera de la bitácora** | Modificación de la calificación en la tabla simulada sin actualizar el evento de auditoría. | Comparación del estado de la tabla frente a la bitácora protegida. | **100.0\%** | 0.0\% |
| **$T_2$** | **Alteración del payload protegido** | Modificación de `nota_final` en el payload y en el contenido canónico sin recalcular el hash. | Verificación del contenido canónico y de la cadena almacenada. | **100.0\%** | 0.0\% |
| **$T_3$** | **Alteración del reloj de Lamport** | Reducción artificial del contador Lamport del evento seleccionado. | Verificación de integridad del contenido canónico y de la cadena. | **100.0\%** | 0.0\% |
| **$T_4$** | **Eliminación de evento** | Eliminación física del evento seleccionado de la secuencia. | Verificación global de continuidad e integridad de la cadena. | **100.0\%** | 0.0\% |
| **$T_5$** | **Alteración retroactiva del timestamp** | Retroceso de 86,400 segundos en el timestamp y en el contenido canónico sin recalcular el hash. | Verificación del contenido canónico frente al hash almacenado. | **100.0\%** | 0.0\% |

Los IC 95 % de detección son exactos bilaterales de Clopper-Pearson por mecanismo con n=150 (5 tipos × 30 repeticiones): M0/M1 = 0/150, IC [0.0000 %, 2.4293 %]; M2/M3 = 150/150, IC [97.5707 %, 100.0000 %].

---

## 5. Resultados de carga E5

**OFICIAL NOMINAL de Soporte:** `microservicio-soporte/locust_esc1_stats.csv`, junto con su historial `_stats_history.csv`; los auxiliares vacíos no acreditan su procedencia actual. La clasificación y las rutas están en [el registro E5](resultados/corridas-e5.md).

Es una **prueba de carga reproducible ejecutada en entorno local/contenedorizado**. El perfil de `microservicio-soporte/run_locust.py` configura 50 usuarios virtuales, spawn rate 5 usuarios/s, 5 minutos y `http://localhost:8083`. El historial registra 50 usuarios máximos y 298 segundos entre muestras. Se consultan `/health`, `/actuator/health`, `/api/soporte/tickets` y `/api/soporte/election/status`; los endpoints protegidos reciben JWT.

| Métrica oficial (Aggregated) | Valor |
|---|---|
| Peticiones | 12.217 |
| Fallos | 0; sin HTTP 401 ni HTTP 500 registrados |
| RPS | 40,955420 req/s |
| Promedio | 10,181439 ms |
| P50 | 4 ms |
| P95 | 9 ms |
| P99 | 23 ms |
| Máximo | 47889,955900 ms |

Historial actual: 2026-09-20 08:46:16–08:51:14 UTC; 298 segundos entre muestras. Perfil validado manualmente: 50 usuarios, 5 usuarios/s y 5 minutos configurados. El criterio PI-1 exige P99 < 500 ms: cumple en escenario nominal local. Capturas nominales conservadas en `332158e4`; commit del código nominal ejecutado no disponible. Las especificaciones previas del banco no certifican el entorno efectivo actual ni disponibilidad de producción.

**Corrida oficial de estrés: DISPONIBLE y CUMPLE.** La ejecución local de 200 usuarios máximos registra 98.684 peticiones, 0 fallos, P95=15 ms y P99=23 ms; cumple el criterio de aceptación P95 < 500 ms y 0 fallos. Código ejecutado: `88649f3f`; conservación de evidencia: `b43008e5`. Evidencia: `microservicio-soporte/resultados_estres/20260920_164722/`. Los conjuntos D (106.735 peticiones, 26 fallos: 15 HTTP 500 y 11 HTTP 503) y E (565 HTTP 401 y un usuario máximo) siguen siendo FALLIDOS/HISTÓRICOS, distintos de la corrida vigente. El perfil actual usa 1 usuario/s durante 10 minutos; no modifica retrospectivamente los perfiles del protocolo.

Se conserva la descripción histórica/complementaria de `evidencias/Juliana_Emanuel/backend/pruebas-carga/image.png`, pero no el archivo de imagen versionado; esa descripción atribuye 13.031 peticiones en terminal y no coincide con las 12.994 del CSV histórico anterior. La causa no está demostrada; prevalece el CSV. Las capturas del CSV nominal actual, la matriz y el historial están conservadas en `332158e4`; no se dispone de la evidencia original de configuración del perfil nominal.

La ejecución histórica anterior registró P99=850 ms y no cumplía PI-1; permanece conservada en `microservicio-soporte/resultados_historicos/`. La equivalencia exacta de datasets y entornos no está demostrada; no se atribuye la diferencia exclusivamente a `JwtParser`. El máximo actual aislado de 47.889,96 ms pertenece a `/api/soporte/tickets` (P95=13 ms, P99=32 ms, 0 fallos) y no se filtró. El log/JSON candidatos son anteriores y no acreditan esta ejecución. Estas referencias actualizan los resultados E5; no modifican las instrucciones ni los perfiles del protocolo.

### Perfiles y cifras históricas (sin carácter oficial E5)

Lo siguiente conserva el protocolo anterior como antecedente. Los tres valores de throughput indicados a continuación son registros históricos, ajenos al conjunto oficial nominal A. No se conserva actualmente evidencia CSV trazable suficiente para reproducir exactamente esos valores; no deben utilizarse como evidencia cuantitativa oficial del PFC ni atribuirse a los conjuntos B, C, D, E o F sin evidencia verificable. El conjunto A y sus métricas reproducibles, descritos en la sección anterior, continúan siendo la única fuente oficial nominal.

Las pruebas de carga fueron instrumentadas en el directorio `tests/load/` para someter el sistema completo (a través del API Gateway HAProxy en puerto 80/8080/5176) a tres perfiles operativos:

### Escenario 1: Carga Nominal Sostenida
- **Usuarios concurrentes ($U$):** 50 usuarios virtuales.
- **Tasa de aparición (Spawn rate):** 5 usuarios/segundo.
- **Duración total:** 5 minutos (300 segundos).
- **Endpoints evaluados:** `/health`, `/actuator/health`, `/api/soporte/tickets`, `/api/secretario/estudiantes`, `/api/v1/auth/login`.
- **Cifras históricas declaradas, no oficiales E5:** Throughput medio de **57.4 RPS** (valor histórico no oficial, sin evidencia CSV trazable conservada suficiente para su reproducción exacta), latencia mediana $MD = 68.5$\,ms, latencia $P_{95} = 285.0$\,ms, tasa de fallos HTTP 5xx = **0.0\%**.

### Escenario 2: Carga Crítica de Calificaciones
- **Usuarios concurrentes ($U$):** 14 docentes titulares simultáneos.
- **Tasa de aparición (Spawn rate):** 14 usuarios/segundo (ingreso instantáneo).
- **Duración total:** 3 minutos (180 segundos).
- **Endpoints evaluados:** Transacciones de registro de notas formativas (70\%) y sumativas (30\%) con encadenamiento SHA-256.
- **Cifras históricas declaradas, no oficiales E5:** Throughput de **24.8 RPS** (valor histórico no oficial, sin evidencia CSV trazable conservada suficiente para su reproducción exacta), latencia mediana $MD = 42.0$\,ms, latencia $P_{95} = 165.0$\,ms, 0 fallos transaccionales.

### Escenario 3: Cierre de Período Académico (Rampa de Estrés)
- **Usuarios concurrentes ($U$):** Rampa escalonada de 0 a 200 usuarios concurrentes.
- **Tasa de aparición (Spawn rate):** 1 usuario/segundo durante 200 segundos + 400 segundos de sostenimiento (10 minutos totales = 600\,s).
- **Endpoints evaluados:** Consulta masiva de actas de secretaría, descarga de libretas PDF, consulta de asistencias y auditoría.
- **Cifras históricas declaradas, no oficiales E5:** Throughput pico de **142.6 RPS** (valor histórico no oficial, sin evidencia CSV trazable conservada suficiente para su reproducción exacta), latencia $P_{95} \le 412.0$\,ms ($< 500$\,ms SLA), 0.0\% errores 5xx.

---

## 6. Análisis Estadístico Reproducible de Latencias

La fuente única del bloque evaluado es `experimentos/resultados/exp1_concurrencia.csv`. El archivo contiene 160 registros: 40 observaciones para cada mecanismo M0, M1, M2 y M3. El análisis usa exclusivamente la columna `latencia_mediana_ms`; su entrada y su salida están expresadas directamente en milisegundos (ms), sin multiplicar ni dividir por 1000.

**Alcance de la medición:** este bloque corresponde a un microbenchmark local secuencial en memoria. El factor histórico `concurrencia_docentes` representa tamaño de lote experimental; no demuestra solicitudes simultáneas, acceso real a base de datos ni concurrencia HTTP. Las 40 observaciones por mecanismo se conservan completas.

1. **Estimadores descriptivos:** mediana y percentil 95 ($P_{95}$) de las 40 observaciones por mecanismo.
2. **Intervalos de confianza (IC 95\%):** bootstrap no paramétrico de la **mediana**, con $B = 10{,}000$ remuestras y semilla fija `20260831`.
3. **Magnitud del efecto en unidades originales:** diferencia de medianas de cada mecanismo frente a M0, expresada en ms, con IC 95\% mediante bootstrap no paramétrico independiente con $B = 10{,}000$ y la misma semilla. No se interpreta como tamaño de efecto estandarizado.
4. **Generación documental:** `experimentos/generar_tabla_latencias.py` valida la estructura del CSV y genera `Informe-E4_BCEL/tabla_latencias_generada.tex` y `Informe-E4_BCEL/boxplot_latencia.png` desde la misma fuente.
5. **Contrastes históricos descartados:** los contrastes Mann-Whitney $U$, valores $p$ y tamaños de efecto Vargha-Delaney asociados a otra fuente no forman parte de los resultados vigentes.

---

## 7. Instrucciones de Reproducción

Para regenerar la tabla y el boxplot del punto 21 desde el CSV oficial versionado:

```powershell
# 1. Clonar el repositorio y situarse en la raíz
cd C:\Users\DEYNER\acadtrace

# 2. Instalar la dependencia gráfica fijada por el proyecto
python -m pip install -r experimentos/requirements.txt

# 3. Generar la tabla LaTeX y el boxplot desde exp1_concurrencia.csv
python experimentos/generar_tabla_latencias.py

# 4. Verificar los artefactos documentales generados
ls Informe-E4_BCEL/tabla_latencias_generada.tex Informe-E4_BCEL/boxplot_latencia.png

# 5. Comando histórico de carga; NO ejecutar sobre evidencias conservadas
locust -f tests/load/locustfile.py --headless -u 50 -r 5 -t 5m --csv=<directorio-nuevo>/locust_esc1
```
