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
| **JaCoCo Plugin** | `0.8.11` | Compuerta de calidad de cobertura de código ($\ge 70\%$) |
| **Locust** | `2.46.4` | Generador de carga distribuida y estrés |
| **SciPy** | `1.18.1` | Pruebas estadísticas (Mann-Whitney U, Bootstrap) |
| **Pandas** | `3.0.5` | Procesamiento y persistencia de CSVs de telemetría |
| **NumPy** | `2.5.1` | Manejo vectorial y generadores pseudoaleatorios |
| **Matplotlib** | `3.11.1` | Generación de diagramas boxplot en 300 DPI |

---

## 2. Parámetros y Semillas Fijas del Banco Experimental

Para eliminar el sesgo estocástico y permitir la replicación exacta de los experimentos factoriales:

- **Semilla Pseudoaleatoria Global:** `SEED = 20260831` (fijada en `random.seed(SEED)` y `np.random.seed(SEED)`).
- **Población Estudiantil:** $N = 344$ estudiantes de educación básica distribuidos uniformemente en 10 grados y paralelos.
- **Población Docente:** $M = 14$ docentes titulares asignados a las materias curriculares.
- **Ponderación de Calificaciones (LOEI):** Evaluación Formativa = 70\%, Evaluación Sumativa = 30\%.
- **Diseño Factorial:** 30 repeticiones $\times$ 4 mecanismos de auditoría $\times$ 5 tipos de manipulación = **120 corridas factoriales independientes**.
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
| **$T_1$** | **Inserción / Modificación directa en BD** | Alteración arbitraria de la nota final en la tabla relacional sin pasar por el servicio. | `HASH_MISMATCH_SHA256`: El hash recalculado del registro no coincide con el hash almacenado. | **100.0\%** | 0.0\% |
| **$T_2$** | **Borrado de evento transaccional** | Eliminación de una fila intermedia en la cadena histórica de auditoría. | `BROKEN_HASH_CHAIN`: El $H_{\text{prev}}$ del evento subsecuente $e_{i+1}$ no apunta a un nodo existente válido. | **100.0\%** | 0.0\% |
| **$T_3$** | **Permutación de orden causal (Swap)** | Intercambio de posición entre dos eventos consecutivos en la secuencia. | `LAMPORT_INVARIANT_VIOLATION`: Se detecta que $L(e_i) \ge L(e_{i+1})$, rompiendo el invariante de causalidad. | **100.0\%** | 0.0\% |
| **$T_4$** | **Inyección de evento retroactivo** | Inserción de una calificación con fecha pasada dentro de la cadena ya cerrada. | `RETROACTIVE_HASH_INVALID`: Invalida recursivamente todos los hashes encadenados posteriores. | **100.0\%** | 0.0\% |
| **$T_5$** | **Falsificación de timestamp / reloj** | Modificación del timestamp físico o manipulación del contador de Lamport. | `MONOTONIC_TIMESTAMP_VIOLATION` / `HASH_MISMATCH_SHA256`: Discrepancia en la firma del bloque. | **100.0\%** | 0.0\% |

---

## 5. Escenarios Formales de Pruebas de Carga con Locust

Las pruebas de carga fueron instrumentadas en el directorio `tests/load/` para someter el sistema completo (a través del API Gateway HAProxy en puerto 80/8080/5176) a tres perfiles operativos:

### Escenario 1: Carga Nominal Sostenida
- **Usuarios concurrentes ($U$):** 50 usuarios virtuales.
- **Tasa de aparición (Spawn rate):** 5 usuarios/segundo.
- **Duración total:** 5 minutos (300 segundos).
- **Endpoints evaluados:** `/health`, `/actuator/health`, `/api/soporte/tickets`, `/api/secretario/estudiantes`, `/api/v1/auth/login`.
- **Métricas obtenidas:** Throughput medio de **57.4 RPS**, latencia mediana $MD = 68.5$\,ms, latencia $P_{95} = 285.0$\,ms, tasa de fallos HTTP 5xx = **0.0\%**.

### Escenario 2: Carga Crítica de Calificaciones
- **Usuarios concurrentes ($U$):** 14 docentes titulares simultáneos.
- **Tasa de aparición (Spawn rate):** 14 usuarios/segundo (ingreso instantáneo).
- **Duración total:** 3 minutos (180 segundos).
- **Endpoints evaluados:** Transacciones de registro de notas formativas (70\%) y sumativas (30\%) con encadenamiento SHA-256.
- **Métricas obtenidas:** Throughput de **24.8 RPS**, latencia mediana $MD = 42.0$\,ms, latencia $P_{95} = 165.0$\,ms, 0 fallos transaccionales.

### Escenario 3: Cierre de Período Académico (Rampa de Estrés)
- **Usuarios concurrentes ($U$):** Rampa escalonada de 0 a 200 usuarios concurrentes.
- **Tasa de aparición (Spawn rate):** 1 usuario/segundo durante 200 segundos + 400 segundos de sostenimiento (10 minutos totales = 600\,s).
- **Endpoints evaluados:** Consulta masiva de actas de secretaría, descarga de libretas PDF, consulta de asistencias y auditoría.
- **Métricas obtenidas:** Throughput pico de **142.6 RPS**, latencia $P_{95} \le 412.0$\,ms ($< 500$\,ms SLA), 0.0\% errores 5xx.

---

## 6. Métodos Estadísticos Inferenciales No Paramétricos

Dado que las distribuciones de latencia en sistemas distribuidos no siguen una distribución normal (gaussiana) debido a colas largas y fluctuaciones de red, se emplearon métodos estadísticos no paramétricos rigurosos:

1. **Estimador de Tendencia Central:** Mediana ($MD$) y Percentil 95 ($P_{95}$).
2. **Intervalos de Confianza (IC 95\%):** Calculados mediante remuestreo **Bootstrap no paramétrico** con $B = 10{,}000$ réplicas sintéticas.
3. **Prueba de Hipótesis de Mann-Whitney $U$:** Prueba bilateral para determinar si las distribuciones de latencia entre mecanismos difieren significativamente ($p < 0.05$).
4. **Magnitud del Efecto de Vargha-Delaney ($\hat{A}_{12}$):**
   $$\hat{A}_{12} = \frac{R_1 - \frac{n_1(n_1 + 1)}{2}}{n_1 n_2}$$
   - $|\hat{A}_{12} - 0.5| < 0.06$: Despreciable.
   - $0.06 \le |\hat{A}_{12} - 0.5| < 0.14$: Pequeño.
   - $0.14 \le |\hat{A}_{12} - 0.5| < 0.21$: Mediano.
   - $|\hat{A}_{12} - 0.5| \ge 0.21$: **Grande**.

### Resultados del Análisis:
- **$M_0$ vs $M_2$:** $U = 0.0$, $p = 1.08 \times 10^{-50}$ $\to$ $\hat{A}_{12} = 1.000$ (Efecto Grande).
- **$M_1$ vs $M_2$:** $U = 311.0$, $p = 5.01 \times 10^{-48}$ $\to$ $\hat{A}_{12} = 0.986$ (Efecto Grande).
- **$M_2$ vs $M_3$:** $U = 917.5$, $p = 4.88 \times 10^{-43}$ $\to$ $\hat{A}_{12} = 0.959$ (Efecto Grande).

---

## 7. Instrucciones de Reproducción

Para ejecutar y validar todo el banco experimental en una nueva máquina:

```powershell
# 1. Clonar el repositorio y situarse en la raíz
cd C:\Users\DEYNER\acadtrace

# 2. Instalar dependencias estadísticas y Locust
pip install pandas scipy locust matplotlib numpy

# 3. Ejecutar el banco experimental completo (Módulo G)
python experimentos/run_experimentos.py

# 4. Verificar los artefactos generados
ls experimentos/resultados/
# deteccion.csv, manipulaciones.csv, iso25010.csv, boxplot_latencia.png

# 5. Ejecutar pruebas de carga Locust (modo headless)
locust -f tests/load/locustfile.py --headless -u 50 -r 5 -t 5m --csv=experimentos/resultados/locust_esc1
```
