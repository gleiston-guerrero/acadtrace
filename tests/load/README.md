# Suite de Pruebas de Carga y Estrés (Locust) — AcadTrace Entrega 4

Esta suite contiene los escenarios formales de prueba de carga exigidos en la **Guía de Evaluación E4** para validar el comportamiento del API Gateway y los microservicios distribuidos bajo concurrencia real.

---

## 📂 Estructura de Archivos

```text
tests/load/
├── locustfile.py                  # Definición de tareas, endpoints y generación de JWT
├── escenario1_carga_nominal.py    # Escenario 1: 50 usuarios concurrentes durante 5 minutos
├── escenario3_cierre_periodo.py   # Escenario 3: Rampa escalonada de 0 a 200 usuarios durante 10 minutos
└── README.md                      # Esta guía de uso y ejecución
```

---

## 🚀 Escenarios Formales Evaluados

### 1. Escenario 1: Carga Nominal Sostenida
- **Usuarios virtuales:** 50
- **Tasa de aparición:** 5 usuarios/segundo
- **Duración:** 5 minutos (300 segundos)
- **Objetivo:** Medir rendimiento nominal y estabilidad bajo flujo constante.

**Ejecución directa (Headless CLI):**
```bash
python tests/load/escenario1_carga_nominal.py http://localhost:8080
```
*O mediante comando Locust estándar:*
```bash
locust -f tests/load/locustfile.py --headless --host http://localhost:8080 -u 50 -r 5 -t 5m --csv=experimentos/resultados/locust_esc1 --html=experimentos/resultados/reporte_escenario1.html
```

---

### 2. Escenario 3: Cierre de Período Académico (Rampa de Estrés)
- **Perfil de carga:** Rampa de 0 a 200 usuarios concurrentes en 5 etapas progresivas (50, 100, 150, 200 usuarios).
- **Duración total:** 10 minutos (600 segundos).
- **Objetivo:** Evaluar el comportamiento del sistema en momentos de máxima demanda académica y certificar que la latencia $P_{95}$ se mantenga $< 500$\,ms con 0\% errores 5xx.

**Ejecución directa (Headless CLI):**
```bash
python tests/load/escenario3_cierre_periodo.py http://localhost:8080
```
*O mediante comando Locust estándar:*
```bash
locust -f tests/load/locustfile.py -f tests/load/escenario3_cierre_periodo.py --headless --host http://localhost:8080 -t 10m --csv=experimentos/resultados/locust_esc3 --html=experimentos/resultados/reporte_escenario3.html
```

---

## 🌐 Ejecución con Interfaz Gráfica Web

Si deseas monitorear las gráficas interactivas en tiempo real a través del navegador:

```bash
locust -f tests/load/locustfile.py --host http://localhost:8080
```
Luego abre en tu navegador: **[http://localhost:8089](http://localhost:8089)**
