# Guía de Pruebas de Carga con Locust · Microservicio Soporte

Este documento detalla la configuración y ejecución de la prueba de carga del **Microservicio de Soporte Técnico** utilizando **Locust**.

---

## 1. Configuración de la Prueba

- **Archivo de prueba:** `locustfile.py`
- **Usuarios concurrentes:** 50
- **Tasa de rampa (spawn rate):** 10 usuarios / segundo (alcanza los 50 en 5 segundos)
- **Duración de la prueba:** 60 segundos
- **Endpoints evaluados (reales y existentes en el microservicio):**
  1. `GET /health` (Público, peso 3): Mide la capacidad de respuesta base y concurrencia directa del servidor HTTP/Tomcat.
  2. `GET /api/soporte/tickets` (Protegido por JWT, peso 2): Mide el rendimiento real consultando la base de datos PostgreSQL.
  3. `GET /api/soporte/election/status` (Protegido por JWT, peso 1): Evalúa el endpoint de coordinación distribuida y estado de líder con `etcd`.
  4. `GET /actuator/health` (Público, peso 1): Endpoint estándar de salud de Spring Boot Actuator.

---

## 2. Prerrequisitos

1. **Instalar Locust en Python:**
   ```powershell
   pip install locust
   ```

2. **Asegurar que el microservicio esté en ejecución:**
   - **Opción A (Docker Compose):**
     ```powershell
     # Desde la raíz del repositorio:
     docker compose up -d etcd microservicio-soporte
     ```
     Host: `http://localhost:8083`

   - **Opción B (Local con Spring Boot):**
     ```powershell
     # Desde microservicio-soporte/backend:
     .\mvnw.cmd spring-boot:run
     ```
     Host: `http://localhost:5178`

---

## 3. Comandos de Ejecución

### Modo Headless (Recomendado para generar evidencias del informe)

Ejecuta la prueba directamente desde la terminal `microservicio-soporte/`:

#### Si el microservicio está en Docker (puerto 8083):
```powershell
locust -f locustfile.py --headless -u 50 -r 10 --run-time 60s --host http://localhost:8083 --html reporte_soporte.html --csv resultados_soporte
```

#### Si el microservicio está en Local (puerto 5178):
```powershell
locust -f locustfile.py --headless -u 50 -r 10 --run-time 60s --host http://localhost:5178 --html reporte_soporte.html --csv resultados_soporte
```

### Modo Interfaz Web (Interactiva)

```powershell
locust -f locustfile.py --host http://localhost:8083
```
1. Abrir en el navegador: `http://localhost:8089`
2. Configurar:
   - **Number of users:** 50
   - **Ramp up:** 10
   - **Host:** `http://localhost:8083`
   - **Run time:** 60s
3. Clic en **Start swarming**.

---

## 4. Cómo comprobar que la prueba se está ejecutando

1. **En la terminal de Locust:**
   - La tabla se actualizará cada segundo mostrando el contador de peticiones (`# reqs`), peticiones por segundo (`RPS`), tiempos de respuesta (`Avg`, `Min`, `Max`) y fallos (`# fails`).
2. **En los logs del microservicio:**
   - Si usas Docker:
     ```powershell
     docker logs -f microservicio-soporte
     ```
   - Si usas Spring Boot local: Verás el flujo constante de peticiones HTTP en la consola.
3. **Verificación de éxito:**
   - El porcentaje de fallos debe permanecer en `0%`.
   - A los 60 segundos la prueba se detendrá automáticamente y mostrará el resumen estadístico final.

---

## 5. Métricas y evidencias a guardar para el informe

Al finalizar la prueba se generan automáticamente en la carpeta `microservicio-soporte/`:

1. **`reporte_soporte.html`**:
   - Reporte interactivo con gráficos de RPS en el tiempo, distribución de tiempos de respuesta y usuarios activos.
2. **`resultados_soporte_stats.csv`**:
   - Resumen numérico con:
     - **Peticiones totales (Requests):** Total procesado en 60s.
     - **Throughput (RPS):** Peticiones promedio por segundo.
     - **Tiempos de respuesta (ms):** Promedio, Mínimo, Máximo, Mediana (p50), p90, p95 y p99.
     - **Tasa de fallos (Failures):** Porcentaje de error.
3. **`resultados_soporte_stats_history.csv`**:
   - Historial segundo a segundo de la evolución de la carga.
