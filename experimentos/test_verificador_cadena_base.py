"""Pruebas unitarias completas para el verificador de cadena de auditoria (E2/E3/E43).

Ejercita:
  1. cotejar_columnas_visibles:
     - Ausencia de falsos positivos (Docente con actor_id entero, Instant con 0 ms).
     - Deteccion de alteraciones en descripcion (Principal y Docente), fecha, actor,
       registro_id, reloj_lamport, schema_origen, tabla_afectada, accion, resultado e ip_address.
  2. main():
     - Ejecucion con base de datos simulada (SQLite / cursor mockeado).
     - Cadena legitima devuelve 0.
     - Deteccion de manipulaciones (codigo 2):
       * Genesis roto o primer eslabon borrado.
       * Divergencia de cabeza (estado_cadena_auditoria retrocedida o adulterada).
       * Fila sin hash_actual o sin sellar.
       * Fila desversionada (version_canonica != 'v1' o NULL).
       * Truncamiento final / borrado de ultimos eslabones.
       * Columnas visibles manipuladas.
       * Inconsistencia criptografica o Lamport no monotonico.
     - Fallo de conexion devuelve 1.
"""

import datetime
import hashlib
import json
import unittest
from unittest.mock import MagicMock, patch

from experimentos import verificador_cadena


def _sha256(s: str) -> str:
    return hashlib.sha256(s.encode("utf-8")).hexdigest()


class TestCotejoColumnas(unittest.TestCase):

    def _canonico(self, **overrides):
        base = {
            "actor_id": "usuario1",
            "entidad": "calificacion",
            "entidad_id": "101",
            "estado_reconciliacion": "APLICADO",
            "modo": "m2",
            "operacion": "CREAR",
            "payload": {
                "descripcion": "registro original",
                "nota": "8.50",
                "schema_origen": "PRINCIPAL",
                "resultado": "EXITO",
                "ip_address": "192.168.1.50",
            },
            "reloj_lamport": 1,
            "reloj_vectorial": {"P": 1},
            "timestamp": "2026-09-17T12:00:00Z",
            "tipo_evento": "AUDITORIA",
        }
        base.update(overrides)
        return json.dumps(base)

    def _fila(self, **overrides):
        base = {
            "id_auditoria": 1,
            "descripcion": "registro original",
            "fecha": datetime.datetime(2026, 9, 17, 12, 0, 0, tzinfo=datetime.timezone.utc),
            "username": "usuario1",
            "registro_id": 101,
            "reloj_lamport": 1,
            "schema_origen": "PRINCIPAL",
            "tabla_afectada": "calificacion",
            "accion": "CREAR",
            "resultado": "EXITO",
            "ip_address": "192.168.1.50",
            "contenido_canonico": self._canonico(),
            "hash_anterior": "0" * 64,
            "hash_actual": "a" * 64,
            "version_canonica": "v1",
        }
        base.update(overrides)
        return base

    def test_fila_coherente_no_reporta_discrepancias(self):
        self.assertEqual(
            verificador_cadena.cotejar_columnas_visibles(self._fila()),
            [],
        )

    def test_sin_falso_positivo_docente_actor_entero(self):
        """Docente almacena actor_id como entero 7 y username como string '7'."""
        canonico_docente = json.dumps({
            "actor_id": 7,
            "entidad": "asistencia",
            "entidad_id": "20",
            "estado_reconciliacion": "NO_APLICA",
            "modo": "m2",
            "operacion": "REGISTRAR",
            "payload": {},
            "reloj_lamport": 5,
            "reloj_vectorial": {"docente": 1},
            "timestamp": "2026-09-17T12:00:00Z",
            "tipo_evento": "ASISTENCIA_REGISTRADA",
        })
        fila_docente = {
            "id_auditoria": 5,
            "descripcion": "ASISTENCIA_REGISTRADA",
            "fecha": "2026-09-17T12:00:00Z",
            "username": "7",  # String
            "registro_id": 20,
            "reloj_lamport": 5,
            "schema_origen": "DOCENTE",
            "tabla_afectada": "asistencia",
            "accion": "AUDITAR",
            "resultado": "EXITO",
            "contenido_canonico": canonico_docente,
        }
        self.assertEqual(
            verificador_cadena.cotejar_columnas_visibles(fila_docente),
            [],
        )

    def test_sin_falso_positivo_java_instant_0_ms(self):
        """Java Instant con 0 ms emite ...:01Z mientras Postgres retorna ...:01+00:00."""
        canonico = json.dumps({
            "actor_id": "admin",
            "entidad": "curso",
            "entidad_id": "1",
            "estado_reconciliacion": "APLICADO",
            "modo": "m2",
            "operacion": "EDITAR",
            "payload": {"descripcion": "test"},
            "reloj_lamport": 1,
            "reloj_vectorial": {},
            "timestamp": "2026-09-17T12:00:01Z",
            "tipo_evento": "AUDITORIA",
        })
        fila = {
            "id_auditoria": 1,
            "descripcion": "test",
            # Postgres timestamptz con 0 microsegundos
            "fecha": datetime.datetime(2026, 9, 17, 12, 0, 1, tzinfo=datetime.timezone.utc),
            "username": "admin",
            "registro_id": 1,
            "reloj_lamport": 1,
            "contenido_canonico": canonico,
        }
        self.assertEqual(
            verificador_cadena.cotejar_columnas_visibles(fila),
            [],
        )

    def test_descripcion_docente_alterada_se_detecta(self):
        """En Docente la descripcion visible debe coincidir con tipo_evento."""
        canonico_docente = json.dumps({
            "actor_id": "1",
            "entidad": "nota",
            "entidad_id": "5",
            "estado_reconciliacion": "NO_APLICA",
            "modo": "m2",
            "operacion": "CALIFICAR",
            "payload": {},
            "reloj_lamport": 2,
            "reloj_vectorial": {},
            "timestamp": "2026-09-17T12:00:00Z",
            "tipo_evento": "CALIFICACION_REGISTRADA",
        })
        fila = {
            "id_auditoria": 2,
            "descripcion": "DESCRIPCION_MANIPULADA",
            "fecha": "2026-09-17T12:00:00Z",
            "username": "1",
            "registro_id": 5,
            "reloj_lamport": 2,
            "contenido_canonico": canonico_docente,
        }
        self.assertIn("descripcion", verificador_cadena.cotejar_columnas_visibles(fila))

    def test_descripcion_alterada_se_detecta(self):
        fila = self._fila(descripcion="alterada")
        self.assertIn("descripcion", verificador_cadena.cotejar_columnas_visibles(fila))

    def test_fecha_alterada_se_detecta(self):
        fila = self._fila(fecha=datetime.datetime(2026, 9, 17, 15, 30, 0, tzinfo=datetime.timezone.utc))
        self.assertIn("fecha", verificador_cadena.cotejar_columnas_visibles(fila))

    def test_username_alterado_se_detecta(self):
        fila = self._fila(username="otro_usuario")
        self.assertIn("username", verificador_cadena.cotejar_columnas_visibles(fila))

    def test_registro_id_alterado_se_detecta(self):
        fila = self._fila(registro_id=999)
        self.assertIn("registro_id", verificador_cadena.cotejar_columnas_visibles(fila))

    def test_reloj_lamport_alterado_se_detecta(self):
        fila = self._fila(reloj_lamport=60)
        self.assertIn("reloj_lamport", verificador_cadena.cotejar_columnas_visibles(fila))

    def test_schema_origen_alterado_se_detecta(self):
        fila = self._fila(schema_origen="DOCENTE")
        self.assertIn("schema_origen", verificador_cadena.cotejar_columnas_visibles(fila))

    def test_tabla_afectada_alterada_se_detecta(self):
        fila = self._fila(tabla_afectada="otra_tabla")
        self.assertIn("tabla_afectada", verificador_cadena.cotejar_columnas_visibles(fila))

    def test_accion_alterada_se_detecta(self):
        fila = self._fila(accion="ELIMINAR")
        self.assertIn("accion", verificador_cadena.cotejar_columnas_visibles(fila))

    def test_resultado_alterado_se_detecta(self):
        fila = self._fila(resultado="FALLO")
        self.assertIn("resultado", verificador_cadena.cotejar_columnas_visibles(fila))

    def test_ip_address_alterada_se_detecta(self):
        fila = self._fila(ip_address="10.0.0.99")
        self.assertIn("ip_address", verificador_cadena.cotejar_columnas_visibles(fila))


SECRET_TEST_DEFAULT = "clave-secreta-institucional-test"


@patch.dict("os.environ", {"JWT_SECRET": SECRET_TEST_DEFAULT})
class TestVerificadorCadenaMain(unittest.TestCase):
    """Pruebas que ejercitan main() de verificador_cadena frente a manipulaciones reales.

    JWT_SECRET esta disponible por defecto (como en un entorno real bien
    configurado) para que las cadenas validas tengan HMAC verificable de
    verdad, en vez de un placeholder no criptografico ("a"*64) que antes
    dejaba pasar cualquier fila por falta de secreto, no porque fuera
    autentica. Las pruebas que necesitan probar la ausencia de secreto lo
    quitan explicitamente.
    """

    def _crear_cadena_valida(self, n=3, inicio_hash=None, secret=None):
        secret = secret or SECRET_TEST_DEFAULT
        filas = []
        GENESIS = "0" * 64
        h_ant = inicio_hash if inicio_hash is not None else GENESIS
        for i in range(1, n + 1):
            can_dict = {
                "actor_id": f"user{i}",
                "entidad": "estudiante",
                "entidad_id": str(i),
                "estado_reconciliacion": "APLICADO",
                "modo": "m2",
                "operacion": "CREAR",
                "payload": {
                    "descripcion": f"evento {i}",
                    "resultado": "EXITO",
                    "schema_origen": "PRINCIPAL",
                },
                "reloj_lamport": i,
                "reloj_vectorial": {},
                "timestamp": f"2026-09-17T10:0{i}:00Z",
                "tipo_evento": "AUDITORIA",
            }
            can_str = json.dumps(can_dict, sort_keys=True, separators=(",", ":"))
            h_act = _sha256(h_ant + can_str)
            fila = {
                "id_auditoria": i,
                "descripcion": f"evento {i}",
                "fecha": f"2026-09-17T10:0{i}:00Z",
                "username": f"user{i}",
                "registro_id": i,
                "reloj_lamport": i,
                "contenido_canonico": can_str,
                "hash_anterior": h_ant,
                "hash_actual": h_act,
                "version_canonica": "v1",
                "schema_origen": "PRINCIPAL",
                "tabla_afectada": "estudiante",
                "accion": "CREAR",
                "resultado": "EXITO",
                "ip_address": None,
                "trace_id": f"00000000-0000-0000-0000-00000000000{i}",
                "hmac": "a" * 64,
            }
            if secret:
                fila["hmac"] = verificador_cadena.calcular_hmac(secret, fila)
            filas.append(fila)
            h_ant = h_act
        return filas, h_ant, n

    def _simular_cursor(self, filas, cabeza_hash, cabeza_lamport, seq_val=None):
        columnas = [
            "id_auditoria", "descripcion", "fecha", "username", "registro_id",
            "reloj_lamport", "contenido_canonico", "hash_anterior", "hash_actual",
            "version_canonica", "schema_origen", "tabla_afectada", "accion",
            "resultado", "ip_address", "trace_id", "hmac"
        ]
        cursor_mock = MagicMock()

        def execute_side_effect(sql, *args, **kwargs):
            sql_clean = " ".join(sql.split()).lower()
            if "count(*)" in sql_clean:
                cursor_mock.fetchone.return_value = [len(filas)]
            elif "select last_value" in sql_clean:
                s_val = seq_val if seq_val is not None else (len(filas) if filas else 1)
                cursor_mock.fetchone.return_value = [s_val, True]
            elif "estado_cadena_auditoria" in sql_clean:
                cursor_mock.fetchone.return_value = [cabeza_hash, cabeza_lamport]
            elif "select id_auditoria" in sql_clean:
                cursor_mock.description = [(c,) for c in columnas]
                cursor_mock.fetchall.return_value = [
                    [f.get(c) for c in columnas] for f in filas
                ]
            else:
                cursor_mock.fetchall.return_value = []
                cursor_mock.fetchone.return_value = None

        cursor_mock.execute.side_effect = execute_side_effect
        return cursor_mock

    @patch("django.db.connection.cursor")
    def test_cadena_valida_retorna_0(self, mock_cursor_ctx):
        filas, cabeza_hash, cabeza_lamport = self._crear_cadena_valida(3)
        mock_cursor = self._simular_cursor(filas, cabeza_hash, cabeza_lamport, seq_val=3)
        mock_cursor_ctx.return_value.__enter__.return_value = mock_cursor

        codigo = verificador_cadena.main()
        self.assertEqual(codigo, 0)

    @patch("django.db.connection.cursor")
    def test_m1_legitimo_retorna_0(self, mock_cursor_ctx):
        """Filas en modo m1 (sin hash ni version) son legitimas y no deben dar falso positivo."""
        filas_m1 = [
            {
                "id_auditoria": 1,
                "descripcion": "Creacion de usuario m1",
                "fecha": "2026-09-17T10:00:00Z",
                "username": "admin",
                "registro_id": 1,
                "reloj_lamport": None,
                "contenido_canonico": None,
                "hash_anterior": None,
                "hash_actual": None,
                "version_canonica": None,
                "schema_origen": "PRINCIPAL",
                "tabla_afectada": "usuario",
                "accion": "CREAR",
                "resultado": "EXITO",
                "ip_address": "127.0.0.1",
                "trace_id": "00000000-0000-0000-0000-000000000001",
                "hmac": None,
            }
        ]
        mock_cursor = self._simular_cursor(filas_m1, None, None, seq_val=1)
        mock_cursor_ctx.return_value.__enter__.return_value = mock_cursor

        codigo = verificador_cadena.main()
        self.assertEqual(codigo, 0)

    @patch("django.db.connection.cursor")
    def test_genesis_invalido_retorna_2(self, mock_cursor_ctx):
        """Si el primer hash anterior no es el bloque genesis, falla de forma aislada."""
        # Cadena criptograficamente valida en si misma pero anclada en un genesis corrupto
        hash_no_genesis = "bad" * 21 + "b"
        filas, cabeza_hash, cabeza_lamport = self._crear_cadena_valida(3, inicio_hash=hash_no_genesis)
        mock_cursor = self._simular_cursor(filas, cabeza_hash, cabeza_lamport, seq_val=3)
        mock_cursor_ctx.return_value.__enter__.return_value = mock_cursor

        codigo = verificador_cadena.main()
        self.assertEqual(codigo, 2)

    @patch("django.db.connection.cursor")
    def test_retroceso_cabeza_retorna_2(self, mock_cursor_ctx):
        """Si la cabeza en estado_cadena_auditoria difiere del ultimo eslabon, falla de forma aislada."""
        filas, cabeza_hash, cabeza_lamport = self._crear_cadena_valida(3)
        # Cabeza retrocedida al eslabon anterior
        cabeza_falsa = filas[1]["hash_actual"]
        mock_cursor = self._simular_cursor(filas, cabeza_falsa, 2, seq_val=3)
        mock_cursor_ctx.return_value.__enter__.return_value = mock_cursor

        codigo = verificador_cadena.main()
        self.assertEqual(codigo, 2)

    @patch("django.db.connection.cursor")
    def test_fila_sin_hash_retorna_2(self, mock_cursor_ctx):
        """Si existe una fila con hash_actual NULL, se detecta de forma aislada."""
        filas, cabeza_hash, cabeza_lamport = self._crear_cadena_valida(2)
        # Fila no sellada agregada a la lista
        fila_incompleta = {
            "id_auditoria": 3,
            "descripcion": "evento no sellado",
            "fecha": "2026-09-17T10:03:00Z",
            "username": "user3",
            "registro_id": 3,
            "reloj_lamport": 3,
            "contenido_canonico": "{}",
            "hash_anterior": cabeza_hash,
            "hash_actual": None,
            "version_canonica": "v1",
            "schema_origen": "PRINCIPAL",
            "tabla_afectada": "estudiante",
            "accion": "CREAR",
            "resultado": "EXITO",
            "ip_address": None,
            "trace_id": "00000000-0000-0000-0000-000000000003",
            "hmac": "a" * 64,
        }
        todas = filas + [fila_incompleta]
        mock_cursor = self._simular_cursor(todas, cabeza_hash, cabeza_lamport, seq_val=2)
        mock_cursor_ctx.return_value.__enter__.return_value = mock_cursor

        codigo = verificador_cadena.main()
        self.assertEqual(codigo, 2)

    @patch("django.db.connection.cursor")
    def test_fila_sin_version_canonica_retorna_2(self, mock_cursor_ctx):
        """Si una fila con hashes tiene version_canonica NULL, se detecta manipulacion."""
        filas, cabeza_hash, cabeza_lamport = self._crear_cadena_valida(2)
        fila_desversionada = {
            "id_auditoria": 3,
            "descripcion": "evento desversionado",
            "fecha": "2026-09-17T10:03:00Z",
            "username": "user3",
            "registro_id": 3,
            "reloj_lamport": 3,
            "contenido_canonico": "{}",
            "hash_anterior": cabeza_hash,
            "hash_actual": "f" * 64,
            "version_canonica": None,  # Forzada a NULL
            "schema_origen": "PRINCIPAL",
            "tabla_afectada": "estudiante",
            "accion": "CREAR",
            "resultado": "EXITO",
            "ip_address": None,
            "trace_id": "00000000-0000-0000-0000-000000000003",
            "hmac": "a" * 64,
        }
        todas = filas + [fila_desversionada]
        mock_cursor = self._simular_cursor(todas, cabeza_hash, cabeza_lamport, seq_val=2)
        mock_cursor_ctx.return_value.__enter__.return_value = mock_cursor

        codigo = verificador_cadena.main()
        self.assertEqual(codigo, 2)

    @patch("django.db.connection.cursor")
    def test_truncamiento_cola_detectado_por_secuencia_retorna_2(self, mock_cursor_ctx):
        """Si se borro el ultimo eslabon y la secuencia de Postgres continua alta, falla."""
        filas, cabeza_hash, cabeza_lamport = self._crear_cadena_valida(3)
        # Borrar el ultimo eslabon y ajustar la cabeza para fingir validez
        filas_truncadas = filas[:2]
        cabeza_ajustada = filas_truncadas[-1]["hash_actual"]
        # Pero la secuencia PostgreSQL alcanzo 3
        mock_cursor = self._simular_cursor(filas_truncadas, cabeza_ajustada, 2, seq_val=3)
        mock_cursor_ctx.return_value.__enter__.return_value = mock_cursor

        codigo = verificador_cadena.main()
        self.assertEqual(codigo, 2)

    @patch("django.db.connection.cursor")
    def test_columna_visible_alterada_en_bd_retorna_2(self, mock_cursor_ctx):
        """Si una columna visible fue modificada en la base sin tocar el canonico, falla."""
        filas, cabeza_hash, cabeza_lamport = self._crear_cadena_valida(3)
        filas[0]["descripcion"] = "descripcion_adulterada"
        mock_cursor = self._simular_cursor(filas, cabeza_hash, cabeza_lamport)
        mock_cursor_ctx.return_value.__enter__.return_value = mock_cursor

        codigo = verificador_cadena.main()
        self.assertEqual(codigo, 2)

    @patch("django.db.connection.cursor")
    def test_hmac_ausente_en_principal_retorna_2(self, mock_cursor_ctx):
        """Si una fila de PRINCIPAL/SECRETARIA no tiene HMAC (ej. insertada directo con sga_app), falla."""
        filas, cabeza_hash, cabeza_lamport = self._crear_cadena_valida(3)
        filas[0]["hmac"] = None
        mock_cursor = self._simular_cursor(filas, cabeza_hash, cabeza_lamport, seq_val=3)
        mock_cursor_ctx.return_value.__enter__.return_value = mock_cursor

        codigo = verificador_cadena.main()
        self.assertEqual(codigo, 2)

    @patch.dict("os.environ", {"JWT_SECRET": "clave-secreta-institucional-test"})
    @patch("django.db.connection.cursor")
    def test_hmac_alterado_con_secreto_retorna_2(self, mock_cursor_ctx):
        """Si el HMAC almacenado no coincide con el calculado con el secreto institucional, falla."""
        filas, cabeza_hash, cabeza_lamport = self._crear_cadena_valida(3, secret="clave-secreta-institucional-test")
        filas[1]["hmac"] = "b" * 64  # HMAC falso
        mock_cursor = self._simular_cursor(filas, cabeza_hash, cabeza_lamport, seq_val=3)
        mock_cursor_ctx.return_value.__enter__.return_value = mock_cursor

        codigo = verificador_cadena.main()
        self.assertEqual(codigo, 2)

    @patch.dict("os.environ", {"JWT_SECRET": "clave-secreta-institucional-test"})
    @patch("django.db.connection.cursor")
    def test_hmac_valido_con_secreto_retorna_0(self, mock_cursor_ctx):
        """Si el HMAC almacenado coincide con el secreto institucional, pasa con codigo 0."""
        filas, cabeza_hash, cabeza_lamport = self._crear_cadena_valida(3, secret="clave-secreta-institucional-test")
        mock_cursor = self._simular_cursor(filas, cabeza_hash, cabeza_lamport, seq_val=3)
        mock_cursor_ctx.return_value.__enter__.return_value = mock_cursor

        codigo = verificador_cadena.main()
        self.assertEqual(codigo, 0)

    @patch("django.db.connection.cursor")
    def test_cadena_rota_hash_actual_invalido_retorna_2(self, mock_cursor_ctx):
        """Si hash_actual no corresponde a sha256(hash_anterior + contenido_canonico),
        verificar_cadena_global declara la cadena rota y main() debe propagar codigo 2
        (antes nada probaba este camino extremo a extremo)."""
        filas, cabeza_hash, cabeza_lamport = self._crear_cadena_valida(2, secret="clave-secreta-institucional-test")
        filas[1]["hash_actual"] = "f" * 64  # no es sha256(hash_anterior + contenido_canonico)
        filas[1]["hmac"] = verificador_cadena.calcular_hmac("clave-secreta-institucional-test", filas[1])
        mock_cursor = self._simular_cursor(filas, "f" * 64, cabeza_lamport, seq_val=2)
        mock_cursor_ctx.return_value.__enter__.return_value = mock_cursor

        codigo = verificador_cadena.main()
        self.assertEqual(codigo, 2)

    @patch("django.db.connection.cursor")
    def test_lamport_no_monotonico_retorna_2(self, mock_cursor_ctx):
        """Si el reloj de Lamport no es estrictamente creciente, retorna 2."""
        filas, cabeza_hash, cabeza_lamport = self._crear_cadena_valida(3)
        filas[1]["reloj_lamport"] = filas[0]["reloj_lamport"]  # no creciente
        mock_cursor = self._simular_cursor(filas, cabeza_hash, cabeza_lamport, seq_val=3)
        mock_cursor_ctx.return_value.__enter__.return_value = mock_cursor

        codigo = verificador_cadena.main()
        self.assertEqual(codigo, 2)

    @patch("django.db.connection.cursor")
    def test_orden_ids_inconsistente_retorna_2(self, mock_cursor_ctx):
        """Si el orden de IDs es inconsistente (ej. id decreciente), retorna 2."""
        filas, cabeza_hash, cabeza_lamport = self._crear_cadena_valida(3)
        filas[1]["id_auditoria"] = filas[0]["id_auditoria"]  # no estrictamente creciente
        mock_cursor = self._simular_cursor(filas, cabeza_hash, cabeza_lamport, seq_val=3)
        mock_cursor_ctx.return_value.__enter__.return_value = mock_cursor

        codigo = verificador_cadena.main()
        self.assertEqual(codigo, 2)

    @patch("django.db.connection.cursor")
    def test_m1_mezclado_con_v1_retorna_2(self, mock_cursor_ctx):
        """Si se insertan filas no selladas (m1) mezcladas en una cadena v1, retorna 2."""
        filas, cabeza_hash, cabeza_lamport = self._crear_cadena_valida(2)
        fila_m1_falsa = {
            "id_auditoria": 99,
            "descripcion": "insercion directa sin hash",
            "fecha": "2026-09-17T10:05:00Z",
            "username": "sga_app",
            "registro_id": 99,
            "reloj_lamport": None,
            "contenido_canonico": None,
            "hash_anterior": None,
            "hash_actual": None,
            "version_canonica": None,
            "schema_origen": "PRINCIPAL",
            "tabla_afectada": "estudiante",
            "accion": "CREAR",
            "resultado": "EXITO",
            "ip_address": "127.0.0.1",
            "trace_id": "trace-m1",
            "hmac": None,
        }
        filas_mixtas = [filas[0], fila_m1_falsa, filas[1]]
        mock_cursor = self._simular_cursor(filas_mixtas, cabeza_hash, cabeza_lamport, seq_val=99)
        mock_cursor_ctx.return_value.__enter__.return_value = mock_cursor

        codigo = verificador_cadena.main()
        self.assertEqual(codigo, 2)

    @patch("django.db.connection.cursor")
    def test_m1_con_cabeza_activa_retorna_2(self, mock_cursor_ctx):
        """Si solo hay filas m1 pero estado_cadena_auditoria registra una cadena activa, retorna 2."""
        fila_m1 = {
            "id_auditoria": 1,
            "descripcion": "evento m1",
            "fecha": "2026-09-17T10:00:00Z",
            "username": "admin",
            "registro_id": 1,
            "reloj_lamport": None,
            "contenido_canonico": None,
            "hash_anterior": None,
            "hash_actual": None,
            "version_canonica": None,
            "schema_origen": "PRINCIPAL",
            "tabla_afectada": "usuario",
            "accion": "CREAR",
            "resultado": "EXITO",
            "ip_address": "127.0.0.1",
            "trace_id": "trace-1",
            "hmac": None,
        }
        # Cabeza activa con hash no génesis y lamport > 0
        mock_cursor = self._simular_cursor([fila_m1], "a" * 64, 5, seq_val=1)
        mock_cursor_ctx.return_value.__enter__.return_value = mock_cursor

        codigo = verificador_cadena.main()
        self.assertEqual(codigo, 2)

    @patch("django.db.connection.cursor")
    def test_m1_historico_antes_de_v1_retorna_0(self, mock_cursor_ctx):
        """Filas m1 cuyo id_auditoria es anterior a toda la cadena v1 son una
        transicion historica legitima de modo (el sistema empezo sin
        encadenamiento y luego lo activo), no una manipulacion."""
        filas_v1, cabeza_hash, cabeza_lamport = self._crear_cadena_valida(2)
        fila_m1_historica = {
            "id_auditoria": 0,
            "descripcion": "evento previo a la cadena v1",
            "fecha": "2026-09-16T09:00:00Z",
            "username": "admin",
            "registro_id": 0,
            "reloj_lamport": None,
            "contenido_canonico": None,
            "hash_anterior": None,
            "hash_actual": None,
            "version_canonica": None,
            "schema_origen": "PRINCIPAL",
            "tabla_afectada": "usuario",
            "accion": "CREAR",
            "resultado": "EXITO",
            "ip_address": "127.0.0.1",
            "trace_id": "trace-historico",
            "hmac": None,
        }
        filas_mixtas = [fila_m1_historica] + filas_v1
        mock_cursor = self._simular_cursor(filas_mixtas, cabeza_hash, cabeza_lamport, seq_val=2)
        mock_cursor_ctx.return_value.__enter__.return_value = mock_cursor

        codigo = verificador_cadena.main()
        self.assertEqual(codigo, 0)

    @patch.dict("os.environ", {"JWT_SECRET": ""})
    @patch("django.db.connection.cursor")
    def test_v1_sin_secreto_disponible_retorna_2(self, mock_cursor_ctx):
        """Sin JWT_SECRET no se puede distinguir un HMAC real de uno inventado
        con forma valida: para PRINCIPAL/SECRETARIA eso falla cerrado, no pasa
        en silencio como si estuviera verificado."""
        filas, cabeza_hash, cabeza_lamport = self._crear_cadena_valida(2, secret="clave-secreta-institucional-test")
        mock_cursor = self._simular_cursor(filas, cabeza_hash, cabeza_lamport, seq_val=2)
        mock_cursor_ctx.return_value.__enter__.return_value = mock_cursor

        codigo = verificador_cadena.main()
        self.assertEqual(codigo, 2)

    @patch.dict("os.environ", {"JWT_SECRET": "clave-secreta-institucional-test"})
    @patch("django.db.connection.cursor")
    def test_hmac_registro_id_null_valido_retorna_0(self, mock_cursor_ctx):
        """Evento legitimo con registro_id null y HMAC calculado con String.valueOf(null) pasa con codigo 0."""
        filas, cabeza_hash, cabeza_lamport = self._crear_cadena_valida(1, secret="clave-secreta-institucional-test")
        filas[0]["registro_id"] = None
        # Recalcular HMAC con registro_id = None
        filas[0]["hmac"] = verificador_cadena.calcular_hmac("clave-secreta-institucional-test", filas[0])
        mock_cursor = self._simular_cursor(filas, cabeza_hash, cabeza_lamport, seq_val=1)
        mock_cursor_ctx.return_value.__enter__.return_value = mock_cursor

        codigo = verificador_cadena.main()
        self.assertEqual(codigo, 0)

    @patch("django.db.connection.cursor")
    def test_docente_eslabon_falso_no_en_bitacora_local_retorna_2(self, mock_cursor_ctx):
        """Eslabon DOCENTE sin fila correspondiente (hash_actual+entidad_id) en la
        bitacora local sga_docente.eventos_auditoria retorna 2.

        El hack anterior de este test (un fetchone personalizado que dependia de
        un atributo mock_cursor.last_query que _simular_cursor nunca establece)
        hacia que "x" in MagicMock() devolviera False siempre, cayendo a un
        fetchone original nunca configurado (un MagicMock generico, veraz por
        defecto) en vez de None: el test pasaba, pero por el cotejo de HMAC que
        rompia la mutacion de schema_origen, no por el cotejo Docente que decia
        probar. _simular_cursor ya devuelve None por defecto para cualquier
        query no reconocida (como la de sga_docente.eventos_auditoria), asi que
        no hace falta ningun hack.
        """
        fila_docente = {
            "id_auditoria": 1,
            "descripcion": "evento docente",
            "fecha": "2026-09-17T10:00:00Z",
            "username": "1",
            "registro_id": 99999,
            "reloj_lamport": 1,
            "contenido_canonico": json.dumps({
                "actor_id": 1, "entidad": "calificacion", "entidad_id": "99999",
                "estado_reconciliacion": "APLICADO", "modo": "m2", "operacion": "CREAR",
                "payload": {"descripcion": "evento docente", "resultado": "EXITO",
                            "schema_origen": "DOCENTE"},
                "reloj_lamport": 1, "reloj_vectorial": {},
                "timestamp": "2026-09-17T10:00:00Z", "tipo_evento": "AUDITORIA",
            }, sort_keys=True, separators=(",", ":")),
            "hash_anterior": "0" * 64,
            "hash_actual": "d" * 64,
            "version_canonica": "v1",
            "schema_origen": "DOCENTE",
            "tabla_afectada": "calificacion",
            "accion": "AUDITAR",
            "resultado": "EXITO",
            "ip_address": None,
            "trace_id": "trace-docente-falso",
            "hmac": None,
        }
        mock_cursor = self._simular_cursor([fila_docente], "d" * 64, 1, seq_val=1)
        mock_cursor_ctx.return_value.__enter__.return_value = mock_cursor

        codigo = verificador_cadena.main()
        self.assertEqual(codigo, 2)

    @patch("django.db.connection.cursor")
    def test_docente_eslabon_legitimo_en_bitacora_local_retorna_0(self, mock_cursor_ctx):
        """Eslabon DOCENTE con fila correspondiente (mismo hash_actual y entidad_id)
        en sga_docente.eventos_auditoria pasa con codigo 0."""
        can_dict = {
            "actor_id": 1, "entidad": "calificacion", "entidad_id": "42",
            "estado_reconciliacion": "APLICADO", "modo": "m2", "operacion": "CREAR",
            "payload": {"descripcion": "evento docente", "resultado": "EXITO",
                        "schema_origen": "DOCENTE"},
            "reloj_lamport": 1, "reloj_vectorial": {},
            "timestamp": "2026-09-17T10:00:00Z", "tipo_evento": "AUDITORIA",
        }
        can_str = json.dumps(can_dict, sort_keys=True, separators=(",", ":"))
        hash_anterior = "0" * 64
        hash_actual = _sha256(hash_anterior + can_str)
        fila_docente = {
            "id_auditoria": 1,
            "descripcion": "evento docente",
            "fecha": "2026-09-17T10:00:00Z",
            "username": "1",
            "registro_id": 42,
            "reloj_lamport": 1,
            "contenido_canonico": can_str,
            "hash_anterior": hash_anterior,
            "hash_actual": hash_actual,
            "version_canonica": "v1",
            "schema_origen": "DOCENTE",
            "tabla_afectada": "calificacion",
            "accion": "AUDITAR",
            "resultado": "EXITO",
            "ip_address": None,
            "trace_id": "trace-docente-legitimo",
            "hmac": None,
        }
        mock_cursor = self._simular_cursor([fila_docente], hash_actual, 1, seq_val=1)

        def execute_side_effect(sql, *args, **kwargs):
            sql_clean = " ".join(sql.split()).lower()
            if "sga_docente.eventos_auditoria" in sql_clean:
                mock_cursor.fetchone.return_value = [1]
            elif "count(*)" in sql_clean:
                mock_cursor.fetchone.return_value = [1]
            elif "select last_value" in sql_clean:
                mock_cursor.fetchone.return_value = [1, True]
            elif "estado_cadena_auditoria" in sql_clean:
                mock_cursor.fetchone.return_value = [hash_actual, 1]
            elif "select id_auditoria" in sql_clean:
                columnas = [
                    "id_auditoria", "descripcion", "fecha", "username", "registro_id",
                    "reloj_lamport", "contenido_canonico", "hash_anterior", "hash_actual",
                    "version_canonica", "schema_origen", "tabla_afectada", "accion",
                    "resultado", "ip_address", "trace_id", "hmac",
                ]
                mock_cursor.description = [(c,) for c in columnas]
                mock_cursor.fetchall.return_value = [[fila_docente.get(c) for c in columnas]]
            else:
                mock_cursor.fetchall.return_value = []
                mock_cursor.fetchone.return_value = None

        mock_cursor.execute.side_effect = execute_side_effect
        mock_cursor_ctx.return_value.__enter__.return_value = mock_cursor

        codigo = verificador_cadena.main()
        self.assertEqual(codigo, 0)


    @patch("django.db.connection.cursor")
    def test_error_conexion_retorna_1(self, mock_cursor_ctx):
        mock_cursor_ctx.side_effect = RuntimeError("Error de conexion de base de datos")

        codigo = verificador_cadena.main()
        self.assertEqual(codigo, 1)


if __name__ == "__main__":
    unittest.main()
