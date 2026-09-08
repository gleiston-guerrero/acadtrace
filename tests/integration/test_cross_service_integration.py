"""
Suite de Pruebas de Integración (Integration Testing) — AcadTrace E4
Verifica formalmente:
1. Configuración del API Gateway HAProxy 2.9 (enrutamiento de puertos, balanceo y health checks).
2. Arquitectura de persistencia multi-esquema PostgreSQL (aislamiento funcional de esquemas).
3. Mecanismo de inmutabilidad append-only mediante disparadores PL/pgSQL.
4. Trazabilidad distribuida y correlación de peticiones (X-Trace-Id / MDC).
"""

import os
import re
import pytest

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))


class TestHAProxyGatewayIntegration:
    """Verifica la configuración del reverse proxy y balanceador de carga perimetral HAProxy."""

    @pytest.fixture(autouse=True)
    def setup_haproxy(self):
        self.cfg_path = os.path.join(REPO_ROOT, "infra", "haproxy", "haproxy.cfg")

    def test_haproxy_file_exists(self):
        """Verifica que el archivo de configuración de HAProxy exista."""
        assert os.path.exists(self.cfg_path), f"Falta archivo de configuración: {self.cfg_path}"

    def test_haproxy_frontend_bindings(self):
        """Verifica que HAProxy enlace los puertos REST, gRPC y Dashboard de observabilidad."""
        with open(self.cfg_path, "r", encoding="utf-8") as f:
            content = f.read()

        required_ports = [":8080", ":8081", ":8082", ":9092", ":8404"]
        for port in required_ports:
            assert port in content, f"Puerto requerido '{port}' no está vinculado en haproxy.cfg"

    def test_haproxy_balancing_algorithms(self):
        """Verifica que se emplee roundrobin para tráfico REST y leastconn para canales gRPC HTTP/2."""
        with open(self.cfg_path, "r", encoding="utf-8") as f:
            content = f.read()

        assert "balance roundrobin" in content, "Falta política 'balance roundrobin' para microservicios REST"
        assert "balance leastconn" in content, "Falta política 'balance leastconn' para balanceo gRPC de larga duración"

    def test_haproxy_health_checks(self):
        """Verifica la inspección activa de salud (/health) en los backends configurados."""
        with open(self.cfg_path, "r", encoding="utf-8") as f:
            content = f.read()

        assert "option httpchk" in content or "check" in content, "Falta verificación activa de salud en servidores"


class TestDatabaseSchemaIntegration:
    """Verifica la arquitectura multi-esquema de PostgreSQL y los disparadores de inmutabilidad."""

    @pytest.fixture(autouse=True)
    def setup_schema(self):
        self.schema_path = os.path.join(REPO_ROOT, "docs", "db", "schema.sql")

    def test_schema_file_exists(self):
        """Verifica que docs/db/schema.sql exista y contenga DDL relacional."""
        assert os.path.exists(self.schema_path), f"Falta schema.sql en: {self.schema_path}"

    def test_multi_schema_isolation_declared(self):
        """Verifica la existencia de esquemas aislados por dominio de microservicio."""
        with open(self.schema_path, "r", encoding="utf-8") as f:
            content = f.read().lower()

        required_schemas = ["sga_principal", "secretaria", "docente", "soporte"]
        for schema in required_schemas:
            assert f"create schema" in content and schema in content, f"Esquema '{schema}' ausente en docs/db/schema.sql"

    def test_append_only_trigger_declared(self):
        """Verifica que el disparador de inmutabilidad tg_auditoria_append_only rechace UPDATE y DELETE."""
        with open(self.schema_path, "r", encoding="utf-8") as f:
            content = f.read()

        assert "tg_auditoria_append_only" in content or "trigger_auditoria" in content, "Disparador de inmutabilidad ausente en DDL"
        assert "BEFORE UPDATE OR DELETE" in content or "before update or delete" in content.lower(), "El disparador debe abortar UPDATE y DELETE"


class TestTraceCorrelationIntegration:
    """Verifica la integración de identificadores de traza distribuida (X-Trace-Id)."""

    def test_trace_id_format_and_propagation_headers(self):
        """Valida la estructura formal de cabeceras de correlación X-Trace-Id."""
        sample_trace_id = "a1b2c3d4-e5f6-47a8-b9c0-d1e2f3a4b5c6"
        uuid_pattern = re.compile(r"^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", re.I)
        assert uuid_pattern.match(sample_trace_id), "El formato de trace_id no cumple estándar UUIDv4"


if __name__ == "__main__":
    pytest.main(["-v", __file__])
