"""Pruebas E7 sobre copias temporales; nunca altera resultados versionados."""

import contextlib
import io
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from verificar_reproducibilidad import ARTIFACTS, CERTIFICATE, verify, write_certificate
from run_experimentos import LiveBackendClient


class CertificateTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.directory = Path(self.temp.name)
        for name in ARTIFACTS:
            (self.directory / name).write_bytes(b"fixture\n")
        write_certificate(self.directory)

    def check(self, expected):
        with contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(verify(self.directory), expected)

    def test_tamper_and_restore(self):
        self.check(0)
        path = self.directory / ARTIFACTS[0]
        original = path.read_bytes()
        try:
            path.write_bytes(original + b"alteracion\n")
            self.check(1)
        finally:
            path.write_bytes(original)
        self.check(0)

    def test_missing_artifact(self):
        (self.directory / ARTIFACTS[0]).unlink()
        self.check(1)

    def test_missing_certificate(self):
        (self.directory / CERTIFICATE).unlink()
        self.check(1)

    def test_empty_artifact(self):
        (self.directory / ARTIFACTS[0]).write_bytes(b"")
        self.check(1)

    def test_malformed_partial_duplicate_and_extra(self):
        path = self.directory / CERTIFICATE
        original = path.read_text(encoding="utf-8")
        variants = (
            "", "no es un certificado\n", original.replace("  ", " ", 1),
            "\n".join(original.splitlines()[1:]),
            original + original.splitlines()[0] + "\n",
            original + "0" * 64 + "  ../ajeno.csv\n",
        )
        for text in variants:
            with self.subTest(text=text):
                path.write_text(text, encoding="utf-8")
                self.check(1)

    def test_crlf_rejected_before_signing(self):
        (self.directory / ARTIFACTS[0]).write_bytes(b"fixture\r\n")
        with self.assertRaises(ValueError):
            write_certificate(self.directory)
        self.check(1)


class ModeTests(unittest.TestCase):
    def test_local_never_checks_network(self):
        with patch.object(LiveBackendClient, "verificar_conexion") as check:
            self.assertFalse(LiveBackendClient(mode="local").is_live)
            check.assert_not_called()

    def test_http_does_not_fall_back(self):
        with patch.object(LiveBackendClient, "verificar_conexion", return_value=False):
            with self.assertRaises(RuntimeError):
                LiveBackendClient(mode="http")

    def test_invalid_mode(self):
        with self.assertRaises(ValueError):
            LiveBackendClient(mode="automatico")


if __name__ == "__main__":
    unittest.main()
