"""Pruebas E7 sobre copias temporales; nunca altera resultados versionados."""

import contextlib
import io
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from verificar_reproducibilidad import ARTIFACTS, CERTIFICATE, verify, write_certificate
from run_experimentos import LiveBackendClient
from generar_tabla_latencias import (
    EXPECTED_REPETITIONS,
    LOAD_LEVELS,
    MECHANISMS,
    calculate_statistics,
    load_latency_samples,
    vargha_delaney_a12,
)


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


class DocumentationMirrorTests(unittest.TestCase):
    def test_copias_documentales_coinciden_con_resultados_oficiales(self):
        """E7: la evidencia publicada debe ser id?ntica al resultado certificado."""
        raiz = Path(__file__).resolve().parents[1]
        oficiales = raiz / "experimentos" / "resultados"
        documentados = raiz / "docs" / "experimentos" / "resultados"

        archivos = tuple(dict.fromkeys(
            (*ARTIFACTS, CERTIFICATE, "falsos_positivos.csv")
        ))

        for nombre in archivos:
            with self.subTest(archivo=nombre):
                origen = oficiales / nombre
                copia = documentados / nombre

                self.assertTrue(
                    origen.is_file(),
                    f"Falta resultado oficial E7: {origen}",
                )
                self.assertTrue(
                    copia.is_file(),
                    f"Falta copia documental E7: {copia}",
                )
                self.assertEqual(
                    origen.read_bytes(),
                    copia.read_bytes(),
                    f"Copia documental E7 desactualizada: {nombre}",
                )


class LatencyAnalysisTests(unittest.TestCase):
    """Punto 21: estadistica estratificada por mecanismo y nivel."""

    def test_segmenta_160_observaciones_en_16_celdas(self):
        samples = load_latency_samples()

        self.assertEqual(
            set(samples),
            set(MECHANISMS),
        )

        total = 0

        for mechanism in MECHANISMS:
            self.assertEqual(
                set(samples[mechanism]),
                set(LOAD_LEVELS),
            )

            for level in LOAD_LEVELS:
                values = samples[mechanism][level]

                self.assertEqual(
                    len(values),
                    EXPECTED_REPETITIONS,
                    (
                        f"{mechanism}/nivel={level} debe "
                        f"tener {EXPECTED_REPETITIONS} repeticiones"
                    ),
                )

                total += len(values)

        self.assertEqual(total, 160)


    def test_vargha_delaney_a12_dominancia_y_empates(self):
        self.assertAlmostEqual(
            vargha_delaney_a12(
                [2.0, 2.0],
                [1.0, 1.0],
            ),
            1.0,
        )

        self.assertAlmostEqual(
            vargha_delaney_a12(
                [1.0, 1.0],
                [1.0, 1.0],
            ),
            0.5,
        )

        self.assertAlmostEqual(
            vargha_delaney_a12(
                [1.0, 1.0],
                [2.0, 2.0],
            ),
            0.0,
        )


    def test_estadisticas_y_a12_se_calculan_dentro_del_mismo_nivel(self):
        samples = load_latency_samples()
        results = calculate_statistics(samples)

        expected_keys = {
            (mechanism, level)
            for level in LOAD_LEVELS
            for mechanism in MECHANISMS
        }

        actual_keys = {
            (result.mechanism, result.load_level)
            for result in results
        }

        self.assertEqual(
            len(results),
            16,
        )

        self.assertEqual(
            actual_keys,
            expected_keys,
        )

        for result in results:
            if result.mechanism == "M0":
                self.assertIsNone(result.a12_vs_m0)
                continue

            expected_a12 = vargha_delaney_a12(
                samples[result.mechanism][result.load_level],
                samples["M0"][result.load_level],
            )

            self.assertAlmostEqual(
                result.a12_vs_m0,
                expected_a12,
                places=12,
                msg=(
                    "A12 debe comparar el mecanismo con M0 "
                    f"dentro del nivel {result.load_level}"
                ),
            )


if __name__ == "__main__":
    unittest.main()
