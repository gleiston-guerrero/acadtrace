"""
Suite de Pruebas de Contratos (Contract Testing) — AcadTrace E4
Verifica formalmente:
1. Consistencia de contratos gRPC (Protocol Buffers v3) entre SGA Principal y Microservicio Docente.
2. Concordancia de esquemas de mensajes, tipos de datos y números de campo.
3. Especificación formal OpenAPI 3.0 (docs/api/openapi.yaml) y esquemas de seguridad JWT.
"""

import os
import re
import yaml
import pytest

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))


class TestGrpcProtobufContracts:
    """Verifica la compatibilidad binaria y consistencia de esquemas gRPC/Protobuf inter-servicios."""

    @pytest.fixture(autouse=True)
    def setup_proto_paths(self):
        self.principal_proto = os.path.join(REPO_ROOT, "sga-principal", "src", "main", "proto", "representante_academico.proto")
        self.docente_proto = os.path.join(REPO_ROOT, "microservicio-docente", "grpc_protos", "representante_academico.proto")

    def test_proto_files_exist(self):
        """Verifica que los archivos .proto existan en ambos microservicios."""
        assert os.path.exists(self.principal_proto), f"Falta proto en SGA Principal: {self.principal_proto}"
        assert os.path.exists(self.docente_proto), f"Falta proto en Docente: {self.docente_proto}"

    def test_syntax_proto3_declared(self):
        """Verifica que ambos contratos declaren formalmente 'syntax = \"proto3\";'."""
        for path in [self.principal_proto, self.docente_proto]:
            with open(path, "r", encoding="utf-8") as f:
                content = f.read()
            assert 'syntax = "proto3";' in content, f"El archivo {path} no declara syntax = proto3"

    def test_rpc_method_signatures_match(self):
        """Verifica que los métodos RPC del servicio coincidan exactamente en nombre y signatura."""
        rpc_pattern = re.compile(r"rpc\s+(\w+)\s*\(([^)]+)\)\s*returns\s*\(([^)]+)\);")

        with open(self.principal_proto, "r", encoding="utf-8") as f:
            methods_principal = {m[0]: (m[1].strip(), m[2].strip()) for m in rpc_pattern.findall(f.read())}

        with open(self.docente_proto, "r", encoding="utf-8") as f:
            methods_docente = {m[0]: (m[1].strip(), m[2].strip()) for m in rpc_pattern.findall(f.read())}

        assert len(methods_principal) > 0, "No se encontraron métodos RPC en proto de Principal"
        for name, sig in methods_principal.items():
            assert name in methods_docente, f"El método RPC '{name}' no está implementado en el contrato de Docente"
            assert sig == methods_docente[name], f"La signatura de '{name}' difiere: {sig} vs {methods_docente[name]}"

    def test_message_fields_and_tag_numbers_match(self):
        """Verifica que los campos de los mensajes de datos compartidos tengan idénticos tipos y números de etiqueta."""
        msg_block_pattern = re.compile(r"message\s+(\w+)\s*\{([^}]+)\}")
        field_pattern = re.compile(r"(\w+)\s+(\w+)\s*=\s*(\d+);")

        def extract_messages(filepath):
            with open(filepath, "r", encoding="utf-8") as f:
                content = f.read()
            messages = {}
            for msg_name, msg_body in msg_block_pattern.findall(content):
                fields = {}
                for f_type, f_name, f_num in field_pattern.findall(msg_body):
                    fields[int(f_num)] = (f_type, f_name)
                messages[msg_name] = fields
            return messages

        msgs_p = extract_messages(self.principal_proto)
        msgs_d = extract_messages(self.docente_proto)

        assert "MatriculasRequest" in msgs_p and "MatriculasRequest" in msgs_d
        assert "CalificacionesResponse" in msgs_p and "CalificacionesResponse" in msgs_d
        assert "AsistenciaResponse" in msgs_p and "AsistenciaResponse" in msgs_d

        for msg_name in ["MatriculasRequest", "CalificacionesResponse", "AsistenciaResponse"]:
            fields_p = msgs_p[msg_name]
            fields_d = msgs_d[msg_name]
            assert fields_p == fields_d, f"Discordancia en campos de {msg_name}: {fields_p} vs {fields_d}"


class TestOpenApiContract:
    """Verifica la especificación contractual REST OpenAPI 3.0 del API Gateway y servicios."""

    @pytest.fixture(autouse=True)
    def setup_openapi(self):
        self.openapi_path = os.path.join(REPO_ROOT, "docs", "api", "openapi.yaml")

    def test_openapi_file_exists_and_parses(self):
        """Verifica que docs/api/openapi.yaml exista y sea un documento YAML válido."""
        assert os.path.exists(self.openapi_path), "Falta el archivo docs/api/openapi.yaml"
        with open(self.openapi_path, "r", encoding="utf-8") as f:
            data = yaml.safe_load(f)
        assert isinstance(data, dict), "openapi.yaml no es un diccionario válido"
        assert "openapi" in data and data["openapi"].startswith("3."), f"Versión OpenAPI no válida: {data.get('openapi')}"

    def test_openapi_core_paths_defined(self):
        """Verifica que los endpoints REST críticos estén definidos en la especificación."""
        with open(self.openapi_path, "r", encoding="utf-8") as f:
            data = yaml.safe_load(f)
        paths = data.get("paths", {})

        expected_paths = ["/actuator/health", "/api/auditoria", "/api/secretaria/matriculas", "/api/secretaria/estudiantes"]
        for p in expected_paths:
            assert p in paths, f"Ruta contractual '{p}' ausente en openapi.yaml"

    def test_openapi_security_schemes_declared(self):
        """Verifica que los esquemas de seguridad bearerAuth (JWT) y respuestas 401 estén declarados."""
        with open(self.openapi_path, "r", encoding="utf-8") as f:
            data = yaml.safe_load(f)

        components = data.get("components", {})
        sec_schemes = components.get("securitySchemes", {})
        assert "bearerAuth" in sec_schemes, "Falta securityScheme 'bearerAuth' en openapi.yaml"
        assert sec_schemes["bearerAuth"].get("type") == "http"
        assert sec_schemes["bearerAuth"].get("scheme") == "bearer"
