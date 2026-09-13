# Política y Registro de Gestión y Rotación de Secretos — AcadTrace (E1)

**Responsable de Seguridad y Secretaría:** Ernesto Luna  
**Coordinación de Infraestructura y Despliegue:** Leonardo Castro  
**Fecha de Rotación:** 11 de Septiembre de 2026  
**Estado:** Aplicado y Parametrizado  

---

## 1. Alcance y Contexto de la Rotación

Para dar cumplimiento estricto al Criterio de Aceptación E1 de la Guía de Consolidación AcadTrace (BCEL), se eliminaron todos los secretos funcionales, contraseñas de base de datos, llaves simétricas y tokens de comunicación inter-servicios que se encontraban hardcodeados en el repositorio de control de versiones.

El sistema fue acondicionado para operar bajo el principio de **Fail-Fast (Fallo Rápido)**: si cualquiera de las variables críticas de entorno no es suministrada explícitamente en el entorno de ejecución, Docker Compose y los microservicios detienen su inicio de forma inmediata, evitando arrancar en estados inseguros o con credenciales predeterminadas.

---

## 2. Inventario de Secretos Rotados

| Secreto / Credencial | Función en el Sistema | Estado Previo en Repositorio | Estado Actual (Post-Rotación) |
| :--- | :--- | :--- | :--- |
| `DB_PASSWORD` | Contraseña de conexión a PostgreSQL Multi-Esquema | Expuesta en `docker-compose.yml` y `.properties` | Parametrizada obligatoria `${DB_PASSWORD:?DB_PASSWORD debe definirse}`. Credencial rotada en el servidor de BD en AWS EC2. |
| `JWT_SECRET` | Clave simétrica HMAC-SHA256 para emisión y firma de tokens JWT y registros de auditoría | Expuesta (`sga-provincias-unidas-...`) | Parametrizada obligatoria `${JWT_SECRET:?JWT_SECRET debe definirse}`. Clave criptográfica de 256 bits rotada globalmente. Sesiones anteriores invalidadas. |
| `AES_SECRET_KEY` | Clave simétrica AES-256-GCM para cifrado de datos sensibles de menores (RF-04) | Expuesta en `docker-compose.yml` y `.properties` | Parametrizada obligatoria `${AES_SECRET_KEY:?AES_SECRET_KEY debe definirse}`. Llave de 32 bytes (Base64) rotada. |
| `GRPC_INTERNAL_TOKEN` | Token de autenticación mutua interna para llamadas gRPC entre microservicios | Expuesta (`dev-token-123`) | Parametrizada obligatoria `${GRPC_INTERNAL_TOKEN:?GRPC_INTERNAL_TOKEN debe definirse}` en Principal, Docente, Secretaría y Soporte. |
| `MAIL_PASSWORD` | Contraseña de aplicación SMTP (Gmail 2FA) para notificaciones por correo | Parcialmente expuesta | Parametrizada obligatoria `${MAIL_PASSWORD}` inyectada en despliegue. |
| `FIREBASE_CREDENTIALS` | Credenciales de servicio Google Firebase Admin (FCM) | Archivo montado por volumen | Montaje desacoplado vía `/home/ubuntu/.secrets/acadtrace/firebase-admin.json:ro`. |

---

## 3. Matriz de Suministro de Secretos por Entorno

Los secretos no residen en el repositorio Git bajo ninguna circunstancia. Son suministrados según la siguiente topología:

1. **Desarrollo Local:**
   - Se copia la plantilla `.env.example` o `microservicio-secretaria/.env.example` hacia `.env`.
   - Se reemplazan los marcadores `change-me` por credenciales generadas localmente.
   - El archivo `.env` está estrictamente ignorado en `.gitignore`.

2. **Integración Continua (GitHub Actions CI/CD):**
   - Se inyectan como GitHub Actions Secrets (`secrets.DB_PASSWORD`, `secrets.JWT_SECRET`, `secrets.GRPC_INTERNAL_TOKEN`, `secrets.EC2_SSH_KEY`, etc.).
   - Las pruebas de integración en contenedores (Testcontainers) levantan instancias efímeras con contraseñas generadas dinámicamente durante el ciclo de vida del test.

3. **Producción (AWS EC2 / Docker Swarm / Compose):**
   - Suministrados mediante variables de entorno del sistema operativo host y montajes protegidos en `/run/secrets/` o `/home/ubuntu/.secrets/`.
   - Permisos de lectura en el servidor host restringidos a `chmod 600` para el usuario de ejecución `ubuntu`.

---

## 4. Procedimiento para Desplegar un Entorno Limpio desde Cero

Para levantar el ecosistema completo sin ninguna credencial residual:

### Paso 1: Clonar el repositorio
```bash
git clone https://github.com/LEO23as/acadtrace.git
cd acadtrace
```

### Paso 2: Generar y configurar las variables de entorno
Copiar el archivo de plantilla:
```bash
cp .env.example .env
```

Generar credenciales criptográficamente seguras:
```bash
# Generar JWT_SECRET (mínimo 32 caracteres / 256 bits)
openssl rand -base64 32

# Generar AES_SECRET_KEY (exactamente 32 bytes en base64)
openssl rand -base64 32

# Generar GRPC_INTERNAL_TOKEN (token alfanumérico seguro)
openssl rand -hex 24
```

Editar el archivo `.env` y definir los valores generados:
```env
DB_PASSWORD=<contrasena_segura_postgresql>
JWT_SECRET=<jwt_secret_generado>
AES_SECRET_KEY=<aes_secret_key_generado>
GRPC_INTERNAL_TOKEN=<grpc_token_generado>
```

### Paso 3: Validación Fail-Fast de Docker Compose
Si alguna variable requerida no fue definida en `.env` o en el entorno del shell, Docker Compose abortará el arranque inmediatamente con un mensaje explícito:
```bash
docker compose config
```
*Si falta `DB_PASSWORD`, la consola informará:*
```
variable DB_PASSWORD must be defined: DB_PASSWORD debe definirse
```

### Paso 4: Construcción y arranque de los microservicios
```bash
docker compose up -d --build
```

### Paso 5: Verificación de Salud
```bash
docker compose ps
curl -s http://localhost:8080/actuator/health
curl -s http://localhost:5176/actuator/health
```

---

## 5. Auditoría Automatizada de Fugas (Gitleaks)

Se incorporó `.gitleaks.toml` y el paso automatizado en el pipeline de CI (`.github/workflows/ci-cd.yml`) para verificar que ningún commit contenga credenciales en texto plano antes de permitir el merge a ramas protegidas.
