#!/usr/bin/env python3
"""Genera la tabla y el boxplot de latencias del punto 21.

Fuente unica: experimentos/resultados/exp1_concurrencia.csv.
La columna latencia_mediana_ms se interpreta y publica en milisegundos (ms),
sin multiplicaciones ni divisiones adicionales de unidad.
"""

from __future__ import annotations

import csv
from dataclasses import dataclass
import hashlib
import math
import os
from pathlib import Path
import random
import statistics


REPO_ROOT = Path(__file__).resolve().parents[1]
SOURCE_CSV = REPO_ROOT / "experimentos" / "resultados" / "exp1_concurrencia.csv"
REPORT_DIR = REPO_ROOT / "Informe-E4_BCEL"
OUTPUT_TEX = REPORT_DIR / "tabla_latencias_generada.tex"
OUTPUT_PNG = REPORT_DIR / "boxplot_latencia.png"

MECHANISMS = ("M0", "M1", "M2", "M3")
MECHANISM_LABELS = {
    "M0": "Base",
    "M1": "SQL",
    "M2": "SHA-256 + Lamport",
    "M3": "Vector Clocks",
}
EXPECTED_ROWS = 160
EXPECTED_ROWS_PER_MECHANISM = 40
BOOTSTRAP_REPLICATES = 10_000
BOOTSTRAP_SEED = 20260831
REQUIRED_COLUMNS = {
    "concurrencia_docentes",
    "mecanismo",
    "repeticion",
    "transacciones_totales",
    "throughput_tps",
    "latencia_media_ms",
    "latencia_mediana_ms",
    "latencia_p95_ms",
    "latencia_p99_ms",
    "desviacion_std_ms",
}


@dataclass(frozen=True)
class MechanismStatistics:
    mechanism: str
    count: int
    median_ms: float
    ci_low_ms: float
    ci_high_ms: float
    p95_ms: float
    effect_vs_m0_ms: float
    effect_ci_low_ms: float
    effect_ci_high_ms: float


def percentile(values: list[float], percentage: float) -> float:
    """Percentil lineal equivalente al usado por el banco experimental."""
    if not values:
        raise ValueError("No se puede calcular un percentil de una muestra vacia")

    ordered = sorted(values)
    position = (len(ordered) - 1) * (percentage / 100.0)
    lower = math.floor(position)
    upper = math.ceil(position)
    if lower == upper:
        return ordered[lower]
    return (
        ordered[lower] * (upper - position)
        + ordered[upper] * (position - lower)
    )


def load_latency_samples() -> dict[str, list[float]]:
    if not SOURCE_CSV.is_file():
        raise FileNotFoundError(f"No existe el archivo fuente: {SOURCE_CSV}")

    with SOURCE_CSV.open("r", encoding="utf-8-sig", newline="") as stream:
        reader = csv.DictReader(stream)
        columns = set(reader.fieldnames or ())
        missing = sorted(REQUIRED_COLUMNS - columns)
        if missing:
            raise ValueError(
                "Faltan columnas requeridas en exp1_concurrencia.csv: "
                + ", ".join(missing)
            )
        rows = list(reader)

    if len(rows) != EXPECTED_ROWS:
        raise ValueError(
            f"Se esperaban exactamente {EXPECTED_ROWS} registros y se encontraron {len(rows)}"
        )

    mechanisms_found = {row["mecanismo"] for row in rows}
    if mechanisms_found != set(MECHANISMS):
        raise ValueError(
            "Los mecanismos deben ser exactamente M0, M1, M2 y M3; encontrados: "
            + ", ".join(sorted(mechanisms_found))
        )

    samples = {mechanism: [] for mechanism in MECHANISMS}
    for row_number, row in enumerate(rows, start=2):
        mechanism = row["mecanismo"]
        raw_value = row["latencia_mediana_ms"]
        try:
            value_ms = float(raw_value)
        except (TypeError, ValueError) as exc:
            raise ValueError(
                f"latencia_mediana_ms invalida en la fila {row_number}: {raw_value!r}"
            ) from exc
        if not math.isfinite(value_ms) or value_ms < 0:
            raise ValueError(
                f"latencia_mediana_ms debe ser finita y no negativa en la fila {row_number}"
            )
        samples[mechanism].append(value_ms)

    invalid_counts = {
        mechanism: len(values)
        for mechanism, values in samples.items()
        if len(values) != EXPECTED_ROWS_PER_MECHANISM
    }
    if invalid_counts:
        details = ", ".join(
            f"{mechanism}={count}" for mechanism, count in invalid_counts.items()
        )
        raise ValueError(
            "Cada mecanismo debe tener exactamente "
            f"{EXPECTED_ROWS_PER_MECHANISM} registros; encontrados: {details}"
        )

    return samples


def bootstrap_median_ci(
    values: list[float], rng: random.Random
) -> tuple[float, float]:
    sample_size = len(values)
    bootstrap_medians = [
        statistics.median(rng.choices(values, k=sample_size))
        for _ in range(BOOTSTRAP_REPLICATES)
    ]
    return (
        percentile(bootstrap_medians, 2.5),
        percentile(bootstrap_medians, 97.5),
    )


def bootstrap_median_difference_ci(
    reference: list[float],
    comparison: list[float],
    rng: random.Random,
) -> tuple[float, float]:
    if len(reference) != len(comparison):
        raise ValueError(
            "La referencia M0 y el mecanismo comparado deben tener el mismo n"
        )

    sample_size = len(reference)
    bootstrap_differences = []

    for _ in range(BOOTSTRAP_REPLICATES):
        reference_sample = rng.choices(reference, k=sample_size)
        comparison_sample = rng.choices(comparison, k=sample_size)
        bootstrap_differences.append(
            statistics.median(comparison_sample)
            - statistics.median(reference_sample)
        )

    return (
        percentile(bootstrap_differences, 2.5),
        percentile(bootstrap_differences, 97.5),
    )


def calculate_statistics(
    samples: dict[str, list[float]],
) -> list[MechanismStatistics]:
    rng = random.Random(BOOTSTRAP_SEED)
    results = []
    reference = samples["M0"]
    reference_median = statistics.median(reference)

    for mechanism in MECHANISMS:
        values = samples[mechanism]
        ci_low_ms, ci_high_ms = bootstrap_median_ci(values, rng)

        if mechanism == "M0":
            effect_vs_m0_ms = 0.0
            effect_ci_low_ms = 0.0
            effect_ci_high_ms = 0.0
        else:
            effect_vs_m0_ms = (
                statistics.median(values) - reference_median
            )
            effect_ci_low_ms, effect_ci_high_ms = (
                bootstrap_median_difference_ci(
                    reference,
                    values,
                    rng,
                )
            )

        results.append(
            MechanismStatistics(
                mechanism=mechanism,
                count=len(values),
                median_ms=statistics.median(values),
                ci_low_ms=ci_low_ms,
                ci_high_ms=ci_high_ms,
                p95_ms=percentile(values, 95),
                effect_vs_m0_ms=effect_vs_m0_ms,
                effect_ci_low_ms=effect_ci_low_ms,
                effect_ci_high_ms=effect_ci_high_ms,
            )
        )

    return results


def format_ms(value: float) -> str:
    return f"{value:.6f}"


def build_latex_table(results: list[MechanismStatistics]) -> str:
    lines = [
        "% Archivo generado automaticamente por experimentos/generar_tabla_latencias.py.",
        "% No editar manualmente: la fuente unica es experimentos/resultados/exp1_concurrencia.csv.",
        "\\begin{table}[H]",
        "\\centering",
        "\\caption{Resumen reproducible de latencia mediana por mecanismo "
        "($B = 10{,}000$; semilla 20260831).}",
        "\\label{tab:estadistica-inferencial}",
        "\\begin{tabular}{|l|c|c|c|c|c|}",
        "\\hline",
        "\\textbf{Mecanismo} & \\textbf{$n$} & \\textbf{Mediana (ms)} & "
        "\\textbf{IC 95\\% bootstrap (ms)} & \\textbf{$P_{95}$ (ms)} & "
        "\\textbf{$\\Delta$ mediana vs. $M_0$ (ms), IC 95\\%} \\\\",
        "\\hline",
    ]
    for result in results:
        mechanism_number = result.mechanism[1]
        label = MECHANISM_LABELS[result.mechanism]
        lines.extend(
            [
                f"$M_{mechanism_number}$ ({label}) & {result.count} & "
                f"{format_ms(result.median_ms)} & "
                f"[{format_ms(result.ci_low_ms)}, {format_ms(result.ci_high_ms)}] & "
                f"{format_ms(result.p95_ms)} & "
                f"{format_ms(result.effect_vs_m0_ms)} "
                f"[{format_ms(result.effect_ci_low_ms)}, "
                f"{format_ms(result.effect_ci_high_ms)}] \\\\",
                "\\hline",
            ]
        )
    lines.extend(
        [
            "\\end{tabular}",
            "\\vspace{0.35em}",
            "\\begin{minipage}{0.98\\columnwidth}",
            "\\footnotesize Fuente: \\texttt{experimentos/resultados/exp1\\_concurrencia.csv}, "
            "columna \\texttt{latencia\\_mediana\\_ms}. Entrada y salida expresadas "
            "directamente en milisegundos (ms), sin conversi\\'on adicional. "
            "Cada mecanismo contiene $n=40$ observaciones. El IC 95\\% de la mediana "
            "corresponde al bootstrap no param\\'etrico con $B=10{,}000$ y semilla "
            "fija 20260831. La magnitud $\\Delta$ mediana vs. $M_0$ se define como "
            "la mediana del mecanismo menos la mediana de $M_0$; su IC 95\\% se "
            "obtiene mediante bootstrap no param\\'etrico independiente con "
            "$B=10{,}000$ y la misma semilla fija. No se reportan contrastes de "
            "hip\\'otesis ni valores $p$.",
            "\\end{minipage}",
            "\\end{table}",
            "",
        ]
    )
    return "\n".join(lines)


def build_boxplot(
    samples: dict[str, list[float]], results: list[MechanismStatistics], output: Path
) -> None:
    import matplotlib

    matplotlib.use("Agg", force=True)
    import matplotlib.pyplot as plt
    from matplotlib.ticker import FormatStrFormatter

    with plt.rc_context(
        {
            "font.family": "DejaVu Sans",
            "axes.edgecolor": "#94a3b8",
            "axes.linewidth": 0.8,
        }
    ):
        figure, axis = plt.subplots(figsize=(9, 5.5), dpi=300)
        boxplot = axis.boxplot(
            [samples[mechanism] for mechanism in MECHANISMS],
            patch_artist=True,
            tick_labels=[f"{m}\n{MECHANISM_LABELS[m]}" for m in MECHANISMS],
            showmeans=True,
            meanline=True,
            widths=0.55,
            medianprops={"color": "#b91c1c", "linewidth": 2.0},
            meanprops={"color": "#1e40af", "linestyle": "--", "linewidth": 1.5},
            whiskerprops={"color": "#475569", "linewidth": 1.2},
            capprops={"color": "#475569", "linewidth": 1.2},
            flierprops={
                "marker": "o",
                "markerfacecolor": "#64748b",
                "markeredgecolor": "#64748b",
                "markersize": 4,
                "alpha": 0.6,
            },
        )
        colors = ("#f1f5f9", "#dbeafe", "#ffedd5", "#ede9fe")
        edges = ("#64748b", "#2563eb", "#ea580c", "#7c3aed")
        for patch, color, edge in zip(boxplot["boxes"], colors, edges):
            patch.set_facecolor(color)
            patch.set_edgecolor(edge)
            patch.set_linewidth(1.3)

        axis.set_title(
            "Latencia mediana por mecanismo de auditoria",
            fontsize=12,
            fontweight="bold",
            pad=14,
            color="#0f172a",
        )
        axis.set_xlabel("Mecanismo de auditoria", fontsize=11, fontweight="semibold")
        axis.set_ylabel(
            "latencia_mediana_ms (ms)", fontsize=11, fontweight="semibold"
        )
        axis.yaxis.set_major_formatter(FormatStrFormatter("%.6f"))
        axis.grid(axis="y", linestyle=":", alpha=0.6, color="#cbd5e1")

        vertical_span = max(
            value for values in samples.values() for value in values
        ) - min(value for values in samples.values() for value in values)
        annotation_offset = max(vertical_span * 0.035, 0.00025)
        for position, result in enumerate(results, start=1):
            axis.text(
                position,
                result.median_ms + annotation_offset,
                f"Mediana={format_ms(result.median_ms)} ms",
                horizontalalignment="center",
                fontsize=8,
                color="#1e293b",
                fontweight="bold",
                bbox={
                    "boxstyle": "round,pad=0.2",
                    "facecolor": "white",
                    "alpha": 0.85,
                    "edgecolor": "#cbd5e1",
                },
            )

        figure.text(
            0.5,
            0.01,
            "Fuente: experimentos/resultados/exp1_concurrencia.csv | "
            "40 observaciones por mecanismo | Unidad: ms (sin conversion adicional)",
            ha="center",
            fontsize=8,
            color="#475569",
        )
        figure.tight_layout(rect=(0, 0.04, 1, 1))
        figure.savefig(
            output,
            format="png",
            dpi=300,
            metadata={"Software": "AcadTrace punto 21"},
        )
        plt.close(figure)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(65_536), b""):
            digest.update(block)
    return digest.hexdigest()


def main() -> None:
    samples = load_latency_samples()
    results = calculate_statistics(samples)
    REPORT_DIR.mkdir(parents=True, exist_ok=True)

    temporary_tex = OUTPUT_TEX.with_suffix(".tex.tmp")
    temporary_png = OUTPUT_PNG.with_suffix(".png.tmp")
    try:
        temporary_tex.write_text(build_latex_table(results), encoding="utf-8", newline="\n")
        build_boxplot(samples, results, temporary_png)
        os.replace(temporary_tex, OUTPUT_TEX)
        os.replace(temporary_png, OUTPUT_PNG)
    finally:
        temporary_tex.unlink(missing_ok=True)
        temporary_png.unlink(missing_ok=True)

    print(f"Fuente unica: {SOURCE_CSV.relative_to(REPO_ROOT).as_posix()}")
    print("Unidad de entrada y salida: ms (sin conversion adicional)")
    print(
        f"Validacion: {EXPECTED_ROWS} registros; "
        f"{EXPECTED_ROWS_PER_MECHANISM} por mecanismo"
    )
    print(f"Bootstrap de la mediana: B={BOOTSTRAP_REPLICATES}; semilla={BOOTSTRAP_SEED}")
    for result in results:
        print(
            f"{result.mechanism}: n={result.count}; "
            f"mediana={format_ms(result.median_ms)} ms; "
            f"IC95=[{format_ms(result.ci_low_ms)}, {format_ms(result.ci_high_ms)}] ms; "
            f"P95={format_ms(result.p95_ms)} ms"
        )
    print(f"SHA-256 {OUTPUT_TEX.name}: {sha256(OUTPUT_TEX)}")
    print(f"SHA-256 {OUTPUT_PNG.name}: {sha256(OUTPUT_PNG)}")


if __name__ == "__main__":
    main()
