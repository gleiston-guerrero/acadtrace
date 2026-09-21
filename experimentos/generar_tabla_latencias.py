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
    "M1": "Relacional simple",
    "M2": "SHA-256 + Lamport",
    "M3": "Vector Clocks",
}
LOAD_LEVELS = (1, 5, 10, 14)
EXPECTED_REPETITIONS = 10
EXPECTED_ROWS = (
    len(MECHANISMS)
    * len(LOAD_LEVELS)
    * EXPECTED_REPETITIONS
)
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
class ConditionStatistics:
    mechanism: str
    load_level: int
    count: int
    median_ms: float
    ci_low_ms: float
    ci_high_ms: float
    ci_informative: bool
    p95_ms: float
    effect_vs_m0_ms: float | None
    effect_ci_low_ms: float | None
    effect_ci_high_ms: float | None
    effect_ci_informative: bool
    a12_vs_m0: float | None

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


def load_latency_samples() -> dict[str, dict[int, list[float]]]:
    """Carga las 160 observaciones sin mezclar niveles de carga."""
    if not SOURCE_CSV.is_file():
        raise FileNotFoundError(
            f"No existe el archivo fuente: {SOURCE_CSV}"
        )

    with SOURCE_CSV.open(
        "r",
        encoding="utf-8-sig",
        newline="",
    ) as stream:
        reader = csv.DictReader(stream)
        columns = set(reader.fieldnames or ())
        missing = sorted(REQUIRED_COLUMNS - columns)

        if missing:
            raise ValueError(
                "Faltan columnas requeridas en "
                "exp1_concurrencia.csv: "
                + ", ".join(missing)
            )

        rows = list(reader)

    if len(rows) != EXPECTED_ROWS:
        raise ValueError(
            f"Se esperaban exactamente {EXPECTED_ROWS} "
            f"registros y se encontraron {len(rows)}"
        )

    mechanisms_found = {
        row["mecanismo"]
        for row in rows
    }

    if mechanisms_found != set(MECHANISMS):
        raise ValueError(
            "Los mecanismos deben ser exactamente "
            "M0, M1, M2 y M3; encontrados: "
            + ", ".join(sorted(mechanisms_found))
        )

    try:
        levels_found = {
            int(row["concurrencia_docentes"])
            for row in rows
        }
    except ValueError as exc:
        raise ValueError(
            "concurrencia_docentes debe contener enteros"
        ) from exc

    if levels_found != set(LOAD_LEVELS):
        raise ValueError(
            "Los niveles deben ser exactamente "
            f"{LOAD_LEVELS}; encontrados: "
            f"{sorted(levels_found)}"
        )

    samples = {
        mechanism: {
            level: []
            for level in LOAD_LEVELS
        }
        for mechanism in MECHANISMS
    }

    repetitions = {
        (mechanism, level): set()
        for mechanism in MECHANISMS
        for level in LOAD_LEVELS
    }

    for row_number, row in enumerate(rows, start=2):
        mechanism = row["mecanismo"]

        try:
            level = int(row["concurrencia_docentes"])
            repetition = int(row["repeticion"])
            value_ms = float(row["latencia_mediana_ms"])
        except (TypeError, ValueError) as exc:
            raise ValueError(
                f"Valor invalido en la fila {row_number}"
            ) from exc

        if mechanism not in MECHANISMS:
            raise ValueError(
                f"Mecanismo invalido en fila {row_number}: "
                f"{mechanism}"
            )

        if level not in LOAD_LEVELS:
            raise ValueError(
                f"Nivel invalido en fila {row_number}: {level}"
            )

        if not math.isfinite(value_ms) or value_ms < 0:
            raise ValueError(
                "latencia_mediana_ms debe ser finita "
                f"y no negativa en fila {row_number}"
            )

        key = (mechanism, level)

        if repetition in repetitions[key]:
            raise ValueError(
                "Repeticion duplicada para "
                f"{mechanism}, nivel {level}: {repetition}"
            )

        repetitions[key].add(repetition)
        samples[mechanism][level].append(value_ms)

    expected_repetitions = set(
        range(1, EXPECTED_REPETITIONS + 1)
    )

    for mechanism in MECHANISMS:
        for level in LOAD_LEVELS:
            values = samples[mechanism][level]
            key = (mechanism, level)

            if len(values) != EXPECTED_REPETITIONS:
                raise ValueError(
                    f"{mechanism}, nivel {level}: "
                    f"se esperaban {EXPECTED_REPETITIONS} "
                    f"observaciones y hay {len(values)}"
                )

            if repetitions[key] != expected_repetitions:
                raise ValueError(
                    f"{mechanism}, nivel {level}: "
                    "las repeticiones deben ser exactamente "
                    f"1..{EXPECTED_REPETITIONS}"
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


def vargha_delaney_a12(
    comparison: list[float],
    reference: list[float],
) -> float:
    """Vargha-Delaney A12: P(X>Y) + 0.5*P(X=Y)."""
    if not comparison or not reference:
        raise ValueError(
            "A12 requiere dos muestras no vacias"
        )

    score = 0.0

    for x in comparison:
        for y in reference:
            if x > y:
                score += 1.0
            elif x == y:
                score += 0.5

    return score / (
        len(comparison) * len(reference)
    )


def calculate_statistics(
    samples: dict[str, dict[int, list[float]]],
) -> list[ConditionStatistics]:
    """Calcula estadistica dentro de cada nivel de carga."""
    rng = random.Random(BOOTSTRAP_SEED)
    results: list[ConditionStatistics] = []

    for level in LOAD_LEVELS:
        reference = samples["M0"][level]
        reference_median = statistics.median(reference)

        for mechanism in MECHANISMS:
            values = samples[mechanism][level]

            ci_low_ms, ci_high_ms = bootstrap_median_ci(
                values,
                rng,
            )

            ci_informative = not math.isclose(
                ci_low_ms,
                ci_high_ms,
                rel_tol=0.0,
                abs_tol=1e-15,
            )

            if mechanism == "M0":
                effect_vs_m0_ms = None
                effect_ci_low_ms = None
                effect_ci_high_ms = None
                effect_ci_informative = False
                a12_vs_m0 = None

            else:
                effect_vs_m0_ms = (
                    statistics.median(values)
                    - reference_median
                )

                (
                    effect_ci_low_ms,
                    effect_ci_high_ms,
                ) = bootstrap_median_difference_ci(
                    reference,
                    values,
                    rng,
                )

                effect_ci_informative = not math.isclose(
                    effect_ci_low_ms,
                    effect_ci_high_ms,
                    rel_tol=0.0,
                    abs_tol=1e-15,
                )

                a12_vs_m0 = vargha_delaney_a12(
                    values,
                    reference,
                )

            results.append(
                ConditionStatistics(
                    mechanism=mechanism,
                    load_level=level,
                    count=len(values),
                    median_ms=statistics.median(values),
                    ci_low_ms=ci_low_ms,
                    ci_high_ms=ci_high_ms,
                    ci_informative=ci_informative,
                    p95_ms=percentile(values, 95),
                    effect_vs_m0_ms=effect_vs_m0_ms,
                    effect_ci_low_ms=effect_ci_low_ms,
                    effect_ci_high_ms=effect_ci_high_ms,
                    effect_ci_informative=effect_ci_informative,
                    a12_vs_m0=a12_vs_m0,
                )
            )

    return results

def format_ms(value: float) -> str:
    return f"{value:.6f}"


def format_ci(
    low: float,
    high: float,
    informative: bool,
) -> str:
    if not informative:
        return "\\emph{no informativo}"

    return (
        f"[{format_ms(low)}, "
        f"{format_ms(high)}]"
    )


def build_latex_table(
    results: list[ConditionStatistics],
) -> str:
    lines = [
        "% Archivo generado automaticamente por "
        "experimentos/generar_tabla_latencias.py.",
        "% Fuente unica: "
        "experimentos/resultados/exp1_concurrencia.csv.",
        "\\begin{table}[H]",
        "\\centering",
        "\\scriptsize",
        "\\caption{Latencia segmentada por mecanismo "
        "y nivel de carga "
        "($B=10{,}000$; semilla 20260831).}",
        "\\label{tab:estadistica-inferencial}",
        "\\begin{tabular}{|l|c|c|c|c|c|c|}",
        "\\hline",
        "\\textbf{Mec.} & "
        "\\textbf{Nivel} & "
        "\\textbf{$n$} & "
        "\\textbf{Mediana (ms)} & "
        "\\textbf{IC 95\\% med.} & "
        "\\textbf{$\\Delta$ vs. $M_0$} & "
        "\\textbf{$A_{12}$} \\\\",
        "\\hline",
    ]

    for result in results:
        mechanism_number = result.mechanism[1]
        label = MECHANISM_LABELS[result.mechanism]

        median_ci = format_ci(
            result.ci_low_ms,
            result.ci_high_ms,
            result.ci_informative,
        )

        if result.mechanism == "M0":
            effect_text = "--"
            a12_text = "--"
        else:
            assert result.effect_vs_m0_ms is not None
            assert result.effect_ci_low_ms is not None
            assert result.effect_ci_high_ms is not None
            assert result.a12_vs_m0 is not None

            effect_ci = format_ci(
                result.effect_ci_low_ms,
                result.effect_ci_high_ms,
                result.effect_ci_informative,
            )

            effect_text = (
                f"{format_ms(result.effect_vs_m0_ms)} "
                f"{effect_ci}"
            )

            a12_text = f"{result.a12_vs_m0:.4f}"

        lines.extend(
            [
                f"$M_{mechanism_number}$ ({label}) & "
                f"{result.load_level} & "
                f"{result.count} & "
                f"{format_ms(result.median_ms)} & "
                f"{median_ci} & "
                f"{effect_text} & "
                f"{a12_text} \\\\",
                "\\hline",
            ]
        )

    lines.extend(
        [
            "\\end{tabular}",
            "\\vspace{0.35em}",
            "\\begin{minipage}{0.98\\columnwidth}",
            "\\footnotesize "
            "Fuente: \\texttt{experimentos/resultados/"
            "exp1\\_concurrencia.csv}, columna "
            "\\texttt{latencia\\_mediana\\_ms}. "
            "Cada fila contiene $n=10$ repeticiones "
            "de una unica combinacion mecanismo--nivel; "
            "no se mezclan los niveles 1, 5, 10 y 14. "
            "Los IC 95\\% corresponden al bootstrap "
            "no parametrico de la mediana con "
            "$B=10{,}000$ y semilla fija 20260831. "
            "Cuando los cuantiles bootstrap coinciden "
            "por la resolucion de los datos, el IC se "
            "marca como \\emph{no informativo} y no se "
            "interpreta como precision infinita. "
            "$A_{12}$ es el tamano de efecto "
            "Vargha--Delaney calculado contra $M_0$ "
            "dentro del mismo nivel: "
            "$A_{12}=P(X>M_0)+0.5P(X=M_0)$. "
            "No se reportan valores $p$ ni contrastes "
            "entre niveles distintos.",
            "\\end{minipage}",
            "\\end{table}",
            "",
        ]
    )

    return "\n".join(lines)

def build_boxplot(
    samples: dict[str, dict[int, list[float]]],
    results: list[ConditionStatistics],
    output: Path,
) -> None:
    import matplotlib

    matplotlib.use("Agg", force=True)
    import matplotlib.pyplot as plt
    from matplotlib.ticker import FormatStrFormatter

    data = []
    labels = []

    for level in LOAD_LEVELS:
        for mechanism in MECHANISMS:
            data.append(samples[mechanism][level])
            labels.append(f"{mechanism}\\nL{level}")

    with plt.rc_context(
        {
            "font.family": "DejaVu Sans",
            "axes.edgecolor": "#94a3b8",
            "axes.linewidth": 0.8,
        }
    ):
        figure, axis = plt.subplots(
            figsize=(12, 6),
            dpi=300,
        )

        boxplot = axis.boxplot(
            data,
            patch_artist=True,
            tick_labels=labels,
            showmeans=True,
            meanline=True,
            widths=0.55,
            medianprops={
                "color": "#b91c1c",
                "linewidth": 1.8,
            },
            meanprops={
                "color": "#1e40af",
                "linestyle": "--",
                "linewidth": 1.2,
            },
        )

        colors = {
            "M0": "#f1f5f9",
            "M1": "#dbeafe",
            "M2": "#ffedd5",
            "M3": "#ede9fe",
        }

        edges = {
            "M0": "#64748b",
            "M1": "#2563eb",
            "M2": "#ea580c",
            "M3": "#7c3aed",
        }

        mechanisms_by_position = [
            mechanism
            for _level in LOAD_LEVELS
            for mechanism in MECHANISMS
        ]

        for patch, mechanism in zip(
            boxplot["boxes"],
            mechanisms_by_position,
        ):
            patch.set_facecolor(colors[mechanism])
            patch.set_edgecolor(edges[mechanism])
            patch.set_linewidth(1.2)

        axis.set_title(
            "Latencia mediana por mecanismo "
            "y nivel de carga",
            fontsize=12,
            fontweight="bold",
            pad=14,
        )

        axis.set_xlabel(
            "Mecanismo y nivel de carga",
            fontsize=10,
            fontweight="bold",
        )

        axis.set_ylabel(
            "latencia_mediana_ms (ms)",
            fontsize=10,
            fontweight="bold",
        )

        axis.yaxis.set_major_formatter(
            FormatStrFormatter("%.6f")
        )

        axis.grid(
            axis="y",
            linestyle=":",
            alpha=0.6,
        )

        figure.text(
            0.5,
            0.01,
            "Fuente: exp1_concurrencia.csv | "
            "n=10 por combinacion mecanismo--nivel | "
            "niveles no mezclados",
            ha="center",
            fontsize=8,
        )

        figure.tight_layout(
            rect=(0, 0.04, 1, 1)
        )

        figure.savefig(
            output,
            format="png",
            dpi=300,
            metadata={
                "Software": "AcadTrace punto 21"
            },
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

    REPORT_DIR.mkdir(
        parents=True,
        exist_ok=True,
    )

    temporary_tex = OUTPUT_TEX.with_suffix(
        ".tex.tmp"
    )

    temporary_png = OUTPUT_PNG.with_suffix(
        ".png.tmp"
    )

    try:
        temporary_tex.write_text(
            build_latex_table(results),
            encoding="utf-8",
            newline="\n",
        )

        build_boxplot(
            samples,
            results,
            temporary_png,
        )

        os.replace(
            temporary_tex,
            OUTPUT_TEX,
        )

        os.replace(
            temporary_png,
            OUTPUT_PNG,
        )

    finally:
        temporary_tex.unlink(
            missing_ok=True
        )

        temporary_png.unlink(
            missing_ok=True
        )

    print(
        "Fuente unica: "
        f"{SOURCE_CSV.relative_to(REPO_ROOT).as_posix()}"
    )

    print(
        "Unidad de entrada y salida: "
        "ms (sin conversion adicional)"
    )

    print(
        "Validacion: "
        f"{len(MECHANISMS)} mecanismos x "
        f"{len(LOAD_LEVELS)} niveles x "
        f"{EXPECTED_REPETITIONS} repeticiones "
        f"= {EXPECTED_ROWS} observaciones"
    )

    print(
        "Bootstrap de la mediana: "
        f"B={BOOTSTRAP_REPLICATES}; "
        f"semilla={BOOTSTRAP_SEED}"
    )

    print(
        "A12 Vargha-Delaney: "
        "cada M1/M2/M3 se compara con M0 "
        "del mismo nivel"
    )

    for result in results:
        ci_text = format_ci(
            result.ci_low_ms,
            result.ci_high_ms,
            result.ci_informative,
        )

        line = (
            f"{result.mechanism}/"
            f"nivel={result.load_level}: "
            f"n={result.count}; "
            f"mediana={format_ms(result.median_ms)} ms; "
            f"IC95={ci_text}; "
            f"P95={format_ms(result.p95_ms)} ms"
        )

        if result.a12_vs_m0 is not None:
            line += (
                f"; A12_vs_M0="
                f"{result.a12_vs_m0:.4f}"
            )

        print(line)

    print(
        f"SHA-256 {OUTPUT_TEX.name}: "
        f"{sha256(OUTPUT_TEX)}"
    )

    print(
        f"SHA-256 {OUTPUT_PNG.name}: "
        f"{sha256(OUTPUT_PNG)}"
    )

if __name__ == "__main__":
    main()
