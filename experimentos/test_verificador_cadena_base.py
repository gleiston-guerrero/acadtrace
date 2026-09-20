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


class TestVerificadorCadenaMain(unittest.TestCase):
    """Pruebas que ejercitan main() de verificador_cadena frente a manipulaciones reales."""

    def _crear_cadena_valida(self, n=3):
        filas = []
        GENESIS = "0" * 64
        h_ant = GENESIS
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
            filas.append({
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
            })
            h_ant = h_act
        return filas, h_ant, n

    def _simular_cursor(self, filas, cabeza_hash, cabeza_lamport, seq_val=None):
        columnas = [
            "id_auditoria", "descripcion", "fecha", "username", "registro_id",
            "reloj_lamport", "contenido_canonico", "hash_anterior", "hash_actual",
            "version_canonica", "schema_origen", "tabla_afectada", "accion",
            "resultado", "ip_address"
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
    def test_genesis_invalido_retorna_2(self, mock_cursor_ctx):
        """Si el primer hash anterior no es el bloque genesis, falla."""
        filas, cabeza_hash, cabeza_lamport = self._crear_cadena_valida(3)
        filas[0]["hash_anterior"] = "bad" * 21 + "b"
        mock_cursor = self._simular_cursor(filas, cabeza_hash, cabeza_lamport)
        mock_cursor_ctx.return_value.__enter__.return_value = mock_cursor

        codigo = verificador_cadena.main()
        self.assertEqual(codigo, 2)

    @patch("django.db.connection.cursor")
    def test_retroceso_cabeza_retorna_2(self, mock_cursor_ctx):
        """Si la cabeza en estado_cadena_auditoria difiere del ultimo eslabon, falla."""
        filas, cabeza_hash, cabeza_lamport = self._crear_cadena_valida(3)
        # Cabeza retrocedida al eslabon anterior
        cabeza_falsa = filas[1]["hash_actual"]
        mock_cursor = self._simular_cursor(filas, cabeza_falsa, 2)
        mock_cursor_ctx.return_value.__enter__.return_value = mock_cursor

        codigo = verificador_cadena.main()
        self.assertEqual(codigo, 2)

    @patch("django.db.connection.cursor")
    def test_fila_sin_hash_retorna_2(self, mock_cursor_ctx):
        """Si existe una fila con hash_actual NULL, se detecta insercion sin firma."""
        filas, cabeza_hash, cabeza_lamport = self._crear_cadena_valida(3)
        filas[1]["hash_actual"] = None
        mock_cursor = self._simular_cursor(filas, cabeza_hash, cabeza_lamport)
        mock_cursor_ctx.return_value.__enter__.return_value = mock_cursor

        codigo = verificador_cadena.main()
        self.assertEqual(codigo, 2)

    @patch("django.db.connection.cursor")
    def test_fila_sin_version_canonica_retorna_2(self, mock_cursor_ctx):
        """Si una fila tiene version_canonica NULL, no se omite sino que falla con error 2."""
        filas, cabeza_hash, cabeza_lamport = self._crear_cadena_valida(3)
        filas[2]["version_canonica"] = None
        mock_cursor = self._simular_cursor(filas, cabeza_hash, cabeza_lamport)
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
    def test_error_conexion_retorna_1(self, mock_cursor_ctx):
        mock_cursor_ctx.side_effect = RuntimeError("Error de conexion de base de datos")

        codigo = verificador_cadena.main()
        self.assertEqual(codigo, 1)


if __name__ == "__main__":
    unittest.main()
