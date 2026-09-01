"""
Locust Load Testing Suite — Proyecto AcadTrace / SGA Sistema Distribuido
Cátedra: Aplicaciones Distribuidas (Entrega 4)
Responsable de Calidad y Gateway: Ernesto Gregory Luna Mora

Endpoints evaluados a través del API Gateway (HAProxy / Spring Boot / Uvicorn):
  - [GET]  /health
  - [GET]  /actuator/health
  - [POST] /api/v1/auth/login (o /api/auth/login)
  - [GET]  /api/secretario/estudiantes
  - [GET]  /api/secretario/matriculas
  - [GET]  /api/soporte/tickets
  - [GET]  /api/soporte/election/status
"""

import os
import time
import json
import base64
import hmac
import hashlib
from locust import HttpUser, task, between, tag

JWT_SECRET = os.getenv("JWT_SECRET", "super-secret-key-sga-distribuido-2026-acadtrace-production-security")


def generate_jwt_token(secret: str = JWT_SECRET, username: str = "locust_loadtest", roles: list = None) -> str:
    """
    Genera un token JWT HMAC-SHA256 válido para autenticación interservicios
    conforme a los estándares de microservicio-secretaria y sga-principal.
    """
    if roles is None:
        roles = ["DIRECTOR", "SECRETARIA", "DOCENTE", "SOPORTE_TECNICO"]

    header = {"alg": "HS256", "typ": "JWT"}
    payload = {
        "sub": username,
        "username": username,
        "roles": roles,
        "iat": int(time.time()),
        "exp": int(time.time()) + 86400
    }

    def b64url(data_bytes: bytes) -> str:
        return base64.urlsafe_b64encode(data_bytes).decode("utf-8").rstrip("=")

    header_b64 = b64url(json.dumps(header, separators=(",", ":")).encode("utf-8"))
    payload_b64 = b64url(json.dumps(payload, separators=(",", ":")).encode("utf-8"))
    signing_input = f"{header_b64}.{payload_b64}".encode("utf-8")
    signature = hmac.new(secret.encode("utf-8"), signing_input, hashlib.sha256).digest()
    signature_b64 = b64url(signature)

    return f"{header_b64}.{payload_b64}.{signature_b64}"


class SgaBaseUser(HttpUser):
    """Usuario base concurrente que simula tráfico hacia el API Gateway y microservicios."""
    wait_time = between(0.3, 1.2)

    def on_start(self):
        token = generate_jwt_token()
        self.headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "Accept": "application/json",
            "X-Trace-Id": f"locust-trace-{int(time.time() * 1000)}"
        }

    @tag("health")
    @task(4)
    def test_health(self):
        """Verifica el endpoint de salud perimetral."""
        with self.client.get("/health", name="[GET] /health", catch_response=True) as res:
            if res.status_code in (200, 204):
                res.success()
            else:
                res.failure(f"Status HTTP {res.status_code}")

    @tag("actuator")
    @task(3)
    def test_actuator_health(self):
        """Verifica el endpoint de Spring Boot Actuator."""
        with self.client.get("/actuator/health", name="[GET] /actuator/health", catch_response=True) as res:
            if res.status_code in (200, 204):
                res.success()
            else:
                res.failure(f"Status HTTP {res.status_code}")

    @tag("secretaria", "estudiantes")
    @task(3)
    def test_listar_estudiantes(self):
        """Consulta el listado paginado de estudiantes en secretaría."""
        with self.client.get("/api/secretario/estudiantes?page=1&limit=10", headers=self.headers, name="[GET] /api/secretario/estudiantes", catch_response=True) as res:
            if res.status_code in (200, 204):
                res.success()
            elif res.status_code == 401:
                res.failure("Error 401: Token JWT inválido")
            else:
                res.failure(f"Status HTTP {res.status_code}")

    @tag("secretaria", "matriculas")
    @task(2)
    def test_listar_matriculas(self):
        """Consulta matrículas activas en secretaría."""
        with self.client.get("/api/secretario/matriculas", headers=self.headers, name="[GET] /api/secretario/matriculas", catch_response=True) as res:
            if res.status_code in (200, 204):
                res.success()
            elif res.status_code == 401:
                res.failure("Error 401: No autorizado")
            else:
                res.failure(f"Status HTTP {res.status_code}")

    @tag("soporte", "tickets")
    @task(2)
    def test_listar_tickets(self):
        """Consulta tickets de soporte técnico."""
        with self.client.get("/api/soporte/tickets", headers=self.headers, name="[GET] /api/soporte/tickets", catch_response=True) as res:
            if res.status_code in (200, 204):
                res.success()
            elif res.status_code == 401:
                res.failure("Error 401: No autorizado")
            else:
                res.failure(f"Status HTTP {res.status_code}")

    @tag("soporte", "election")
    @task(1)
    def test_election_status(self):
        """Consulta estado de elección de líder distribuido etcd."""
        with self.client.get("/api/soporte/election/status", headers=self.headers, name="[GET] /api/soporte/election/status", catch_response=True) as res:
            if res.status_code in (200, 500):
                res.success()
            else:
                res.failure(f"Status HTTP {res.status_code}")
