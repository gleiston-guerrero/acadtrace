import os
import time
import json
import base64
import hmac
import hashlib
from locust import HttpUser, task, between, tag

# La clave secreta NO se hardcodea aquí: debe venir de la variable de entorno
# JWT_SECRET (la misma configurada en docker-compose/application.properties).
# Sin valor por defecto: si no está seteada, el script falla explícitamente
# en vez de firmar tokens con un secreto expuesto en el código fuente.
JWT_SECRET = os.getenv("JWT_SECRET")
if not JWT_SECRET:
    raise RuntimeError(
        "JWT_SECRET no está seteada. Exporta la variable de entorno antes de "
        "correr Locust, por ejemplo:\n"
        "  PowerShell:  $env:JWT_SECRET = \"<el-secreto-real>\"\n"
        "  bash/zsh:    export JWT_SECRET=\"<el-secreto-real>\""
    )


def generate_jwt_token(secret: str = JWT_SECRET, username: str = "soporte_loadtest", roles: list = None) -> str:
    """
    Genera un token JWT HMAC-SHA256 válido compatible con JwtService de microservicio-soporte
    sin requerir librerías externas adicionales.
    """
    if roles is None:
        roles = ["SOPORTE_TECNICO", "DIRECTOR"]

    header = {"alg": "HS256", "typ": "JWT"}
    payload = {
        "sub": username,
        "roles": roles,
        "iat": int(time.time()),
        "exp": int(time.time()) + 86400  # Válido por 24 horas
    }

    def b64url(data_bytes: bytes) -> str:
        return base64.urlsafe_b64encode(data_bytes).decode("utf-8").rstrip("=")

    header_b64 = b64url(json.dumps(header, separators=(",", ":")).encode("utf-8"))
    payload_b64 = b64url(json.dumps(payload, separators=(",", ":")).encode("utf-8"))
    signing_input = f"{header_b64}.{payload_b64}".encode("utf-8")
    signature = hmac.new(secret.encode("utf-8"), signing_input, hashlib.sha256).digest()
    signature_b64 = b64url(signature)

    return f"{header_b64}.{payload_b64}.{signature_b64}"


class SoporteUser(HttpUser):
    """
    Simula usuarios concurrentes accediendo a los endpoints existentes de microservicio-soporte.
    """
    # Tiempo de espera simulado entre peticiones sucesivas por usuario (entre 0.5 y 1.5 segundos)
    wait_time = between(0.5, 1.5)

    def on_start(self):
        """Inicializa el token JWT y los headers para peticiones autenticadas."""
        token = os.getenv("SOPORTE_JWT_TOKEN")
        if not token:
            token = generate_jwt_token()
        self.headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "Accept": "application/json"
        }

    @tag("health")
    @task(3)
    def test_health(self):
        """
        GET /health: Endpoint público de comprobación de salud del microservicio-soporte.
        Retorna JSON: {"service":"sga-soporte","status":"ok","timestamp":"..."}
        """
        with self.client.get("/health", name="[GET] /health", catch_response=True) as response:
            if response.status_code == 200:
                try:
                    data = response.json()
                    if data.get("status") == "ok":
                        response.success()
                    else:
                        response.failure(f"Respuesta inesperada: {response.text}")
                except Exception as e:
                    response.failure(f"Error parseando JSON: {e}")
            else:
                response.failure(f"Código HTTP inesperado: {response.status_code}")

    @tag("tickets")
    @task(2)
    def test_listar_tickets(self):
        """
        GET /api/soporte/tickets: Consulta el listado de tickets en la base de datos.
        Requiere autenticación JWT con rol SOPORTE_TECNICO / DIRECTOR.
        """
        with self.client.get("/api/soporte/tickets", headers=self.headers, name="[GET] /api/soporte/tickets", catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            elif response.status_code == 401:
                response.failure("Error 401: Token JWT no autorizado o no configurado")
            else:
                response.failure(f"Código HTTP: {response.status_code}")

    @tag("election")
    @task(1)
    def test_election_status(self):
        """
        GET /api/soporte/election/status: Consulta el estado de líder etcd de la réplica.
        Requiere autenticación JWT.
        """
        with self.client.get("/api/soporte/election/status", headers=self.headers, name="[GET] /api/soporte/election/status", catch_response=True) as response:
            if response.status_code in (200, 500):
                # Si etcd no está corriendo en pruebas aisladas puede dar 500, marcamos si responde
                if response.status_code == 200:
                    response.success()
                else:
                    response.failure(f"Error en estado de elección (posible etcd apagado): {response.status_code}")
            else:
                response.failure(f"Código HTTP: {response.status_code}")

    @tag("actuator")
    @task(1)
    def test_actuator_health(self):
        """
        GET /actuator/health: Endpoint de salud estándar de Spring Boot Actuator.
        """
        with self.client.get("/actuator/health", name="[GET] /actuator/health", catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Código HTTP Actuator: {response.status_code}")
