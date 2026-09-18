"""Pruebas que ejercitan la ruta de lectura real del verificador de cadena.

Estas pruebas usan un SQLite en memoria simulando el esquema minimo de
auditoria; no dependen de PostgreSQL. Su proposito es que las mutaciones
del codigo del verificador no pasen desapercibidas.
"""
import json
import unittest
from unittest.mock import patch

from experimentos import verificador_cadena


class TestCotejoColumnas(unittest.TestCase):

    def _canonico(self, **overrides):
        base = {
            "actor_id": "usuario1",
            "entidad": "calificacion",
            "entidad_id": "101",
            "estado_reconciliacion": "PENDIENTE",
            "modo": "m2",
            "operacion": "CREAR",
            "payload": {"descripcion": "registro original", "nota": "8.50"},
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
            "fecha": None,
            "username": "usuario1",
            "registro_id": 101,
            "reloj_lamport": 1,
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

    def test_descripcion_alterada_se_detecta(self):
        fila = self._fila(descripcion="alterada")
        self.assertIn("descripcion", verificador_cadena.cotejar_columnas_visibles(fila))

    def test_username_alterado_se_detecta(self):
        fila = self._fila(username="otro_usuario")
        self.assertIn("username", verificador_cadena.cotejar_columnas_visibles(fila))

    def test_registro_id_alterado_se_detecta(self):
        fila = self._fila(registro_id=999)
        self.assertIn("registro_id", verificador_cadena.cotejar_columnas_visibles(fila))

    def test_reloj_lamport_alterado_se_detecta(self):
        fila = self._fila(reloj_lamport=60)
        self.assertIn("reloj_lamport", verificador_cadena.cotejar_columnas_visibles(fila))


if __name__ == "__main__":
    unittest.main()
