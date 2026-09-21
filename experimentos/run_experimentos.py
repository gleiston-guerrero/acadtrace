#!/usr/bin/env python3
"""
Modulo Banco Experimental y Evaluación Cuantitativa de Carga y Cripto-Auditoría
Proyecto AcadTrace / SGA Escuela - Entrega 4
Responsable de Calidad y Gateway / Documentación

Genera experimentos locales/sintéticos con mediciones temporales variables:
  - experimentos/resultados/deteccion.csv
  - experimentos/resultados/manipulaciones.csv
  - experimentos/resultados/iso25010.csv
  - experimentos/resultados/exp1_concurrencia.csv
  - experimentos/resultados/exp3_reconciliacion.csv
  - experimentos/resultados/boxplot_latencia.png
"""

import argparse
from pathlib import Path
import shutil
import tempfile
if __package__:
    from .verificar_reproducibilidad import (
        ARTIFACTS,
        CERTIFICATE,
        write_certificate,
        verify,
    )
else:
    from verificar_reproducibilidad import (
        ARTIFACTS,
        CERTIFICATE,
        write_certificate,
        verify,
    )
import os
import sys
import time
import math
import random
import csv
import json
import statistics
import urllib.request
import urllib.error

from pathlib import Path
from types import SimpleNamespace
from typing import List, Dict, Any, Tuple, Optional

REPO_ROOT = Path(__file__).resolve().parents[1]
DOCENTE_DIR = REPO_ROOT / "microservicio-docente"

if str(DOCENTE_DIR) not in sys.path:
    sys.path.insert(0, str(DOCENTE_DIR))

os.environ.setdefault(
    "DJANGO_SETTINGS_MODULE",
    "micro_docente.settings",
)

import django

django.setup()

from docentes.auditoria.clocks import (  # noqa: E402
    RelacionVectorial,
    incrementar_lamport,
    incrementar_vector,
    reconciliar_vectores,
)
from docentes.auditoria.hashing import (  # noqa: E402
    GENESIS_HASH,
    calcular_hash,
    contenido_evento,
    json_canonico,
    calcular_hash_canonico,

)
from docentes.auditoria.verifier import (  # noqa: E402
    verificar_cadena,
    verificar_cadena_global,
    verificar_estado_academico,
)

# Semilla fija para reproducibilidad de secuencias factoriales
SEED = 20260831
random.seed(SEED)

OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "resultados")


NUM_ESTUDIANTES = 344
NUM_DOCENTES = 14
REPETICIONES_FACTORIALES = 30
BACKEND_URL = os.environ.get("BACKEND_URL", "http://localhost:8080")

# Referencias históricas conservadas para trazabilidad documental; no son
# métricas vigentes ni se usan para calcular los resultados experimentales.
JACOCO_SGA_PRINCIPAL_GLOBAL_PCT = 0.51   # Histórico: instrucciones de SGA Principal
JACOCO_CRIPTO_AUDITORIA_CORE_PCT = 100.0  # Histórico: núcleo de cripto-auditoría
JACOCO_SECRETARIA_GLOBAL_PCT = 34.52    # Histórico: CSV agregado de Secretaría, no XML global

# =============================================================================
# 1. CLIENTE HTTP REAL PARA MEDICIÃ“N DE LATENCIA CONTRA BACKEND VIVO
# =============================================================================

class LiveBackendClient:
    def __init__(self, base_url: str = BACKEND_URL, mode: str = "local"):
        self.base_url = base_url.rstrip("/")
        self.token: Optional[str] = None
        if mode not in ("local", "http"):
            raise ValueError("EXPERIMENT_MODE debe ser local o http")
        self.is_live = mode == "http"
        if self.is_live and not self.verificar_conexion():
            raise RuntimeError("Modo HTTP solicitado: backend no disponible; no se cambia a local")

    def verificar_conexion(self) -> bool:
        """Verifica si el backend estÃ¡ activo en el puerto configurado."""
        try:
            req = urllib.request.Request(f"{self.base_url}/actuator/health", headers={"User-Agent": "AcadTrace-Benchmark/1.0"})
            with urllib.request.urlopen(req, timeout=1.5) as resp:
                return resp.status in (200, 204)
        except Exception:
            return False

    def conmutar_modo_auditoria(self, modo: str) -> bool:
        """Invoca el endpoint PUT /api/auditoria/modo/{modo} implementado en AuditoriaService."""
        if not self.is_live:
            return False
        try:
            url = f"{self.base_url}/api/auditoria/modo/{modo.lower()}"
            req = urllib.request.Request(url, method="PUT", headers={"User-Agent": "AcadTrace-Benchmark/1.0"})
            with urllib.request.urlopen(req, timeout=3.0) as resp:
                return resp.status in (200, 204)
        except Exception:
            return False

    def enviar_calificacion_http(self, estudiante_id: int, docente_id: int, nota: float) -> Tuple[bool, float, int]:
        """EnvÃ­a una calificaciÃ³n real por HTTP POST y mide la latencia de ida y vuelta."""
        if not self.is_live:
            return False, 0.0, 0
        url = f"{self.base_url}/api/calificaciones"
        payload_dict = {
            "estudianteId": estudiante_id,
            "docenteId": docente_id,
            "nota": nota,
            "asignaturaId": 1
        }
        data = json.dumps(payload_dict).encode("utf-8")
        headers = {
            "Content-Type": "application/json",
            "User-Agent": "AcadTrace-Benchmark/1.0"
        }
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"

        req = urllib.request.Request(url, data=data, headers=headers, method="POST")
        t0 = time.perf_counter()
        try:
            with urllib.request.urlopen(req, timeout=5.0) as resp:
                t1 = time.perf_counter()
                lat_ms = (t1 - t0) * 1000.0
                return True, lat_ms, resp.status
        except urllib.error.HTTPError as e:
            t1 = time.perf_counter()
            lat_ms = (t1 - t0) * 1000.0
            return False, lat_ms, e.code
        except Exception:
            t1 = time.perf_counter()
            lat_ms = (t1 - t0) * 1000.0
            return False, lat_ms, 599


# =============================================================================
# 2. MODELO DE RELOJES LÓGICOS Y CRIPTOGRAFÍA
# =============================================================================

def construir_evento_productivo(
    *,
    identificador,
    anterior,
    lamport,
    vector,
    est_id,
    doc_id,
    nota_final,
    timestamp,
    modo,
):
    payload = {
        "est_id": est_id,
        "nota_final": nota_final,
    }

    contenido = contenido_evento(
        tipo_evento="CALIFICACION_ACTUALIZADA",
        entidad="Calificacion",
        entidad_id=est_id,
        operacion="ACTUALIZAR",
        actor_id=doc_id,
        timestamp=timestamp,
        payload=payload,
        modo=modo,
        reloj_lamport=lamport,
        reloj_vectorial=vector,
        estado_reconciliacion="APLICADO" if modo == "m3" else "NO_APLICA",
    )

    contenido_canonico = json_canonico(contenido)

    actual = calcular_hash_canonico(
        anterior,
        contenido_canonico,
    )

    return {
        "id_auditoria": identificador,
        "version_canonica": "v1",
        "contenido_canonico": contenido_canonico,

        "id_evento": identificador,
        "hash_anterior": anterior,
        "hash_actual": actual,
        "payload_canonico": json_canonico(payload),

        **{
            clave: valor
            for clave, valor in contenido.items()
            if clave != "payload"
        },
    }


# =============================================================================
# 3. MOTOR DE VERIFICACIÃ“N DE INTEGRIDAD Y ESTADO
# =============================================================================

def medir_verificacion_productiva(eventos: List[Dict[str, Any]], mec: str) -> Tuple[bool, str, int, float]:
    if mec in ["M0", "M1"]:
        return True, "SIN_CRIPTOGRAFIA", -1, 0.0

    t0 = time.perf_counter_ns()
    resultado = verificar_cadena_global(eventos)
    t_us = (time.perf_counter_ns() - t0) / 1000.0
    return (
        resultado.valido, resultado.tipo_inconsistencia or "CADENA_VALIDA",
        resultado.primer_eslabon_roto or -1, t_us,
    )


def verificar_estado_tabla_vs_bitacora(tabla_notas: Dict[int, float], eventos: List[Dict[str, Any]], mec: str) -> Tuple[bool, str, int, float]:
    if mec in ["M0", "M1"]:
        return True, "SIN_PROTECCION_AUDITORIA", -1, 0.0

    t0 = time.perf_counter_ns()
    for est_id, nota_tabla in tabla_notas.items():
        evidencia = next(
            (evento for evento in reversed(eventos)
             if json.loads(evento["payload_canonico"])["est_id"] == est_id),
            None,
        )
        if evidencia is not None:
            instancia = SimpleNamespace(est_id=est_id, nota_final=nota_tabla)
            resultado = verificar_estado_academico(instancia, [evidencia])
            if not resultado.valido:
                t_us = (time.perf_counter_ns() - t0) / 1000.0
                return False, resultado.tipo_inconsistencia, est_id, t_us

    t_us = (time.perf_counter_ns() - t0) / 1000.0
    return True, "ESTADO_CONSISTENTE", -1, t_us


# =============================================================================
# 4. EXPERIMENTO 1: CONCURRENCIA Y CARGA FACTORIAL (24 CONDICIONES)
# =============================================================================

def percentile(data: List[float], p: float) -> float:
    if not data:
        return 0.0
    sorted_data = sorted(data)
    k = (len(sorted_data) - 1) * (p / 100.0)
    f = math.floor(k)
    c = math.ceil(k)
    if f == c:
        return sorted_data[int(k)]
    d0 = sorted_data[int(f)] * (c - k)
    d1 = sorted_data[int(c)] * (k - f)
    return d0 + d1


def ejecutar_experimento_1_concurrencia(
    client: LiveBackendClient,
    *,
    escribir_resultados: bool = True,
) -> List[Dict[str, Any]]:
    # En modo local este experimento es un microbenchmark secuencial.
    # Los valores 1, 5, 10 y 14 controlan el tamaño del lote
    # (20 operaciones por unidad); no representan usuarios simultáneos.
    # Se conserva el nombre histórico concurrencia_docentes en el CSV
    # para mantener compatibilidad con el generador reproducible.
    print(f"[1/5] Ejecutando Experimento 1 ({'HTTP' if client.is_live else 'microbenchmark secuencial'}) — Modo: {'HTTP EN VIVO' if client.is_live else 'CRIPTO-ENGINE LOCAL'}...")
    concurrencias = [1, 5, 10, 14]
    mecanismos = ["M0", "M1", "M2", "M3"]
    repeticiones = 10

    filas_exp1 = []

    for conc in concurrencias:
        for mec in mecanismos:
            if client.is_live:
                if not client.conmutar_modo_auditoria(mec):
                    raise RuntimeError(f"No se pudo configurar auditoría HTTP: {mec}")

            for rep in range(1, repeticiones + 1):
                t_inicio = time.perf_counter()
                # En local, conc es únicamente el factor histórico de tamaño
                # de lote. La ejecución de estas operaciones es secuencial.
                transacciones = conc * 20
                latencias_op = []

                lamport = 0
                vector = {}
                hash_p = GENESIS_HASH

                for i in range(transacciones):
                    est_id = (i % NUM_ESTUDIANTES) + 1
                    doc_id = (i % NUM_DOCENTES) + 1
                    nota = 8.5

                    if client.is_live:
                        ok, lat_ms, status = client.enviar_calificacion_http(
                            est_id,
                            doc_id,
                            nota,
                        )
                        if not ok:
                            raise RuntimeError(
                                f"Falló la transacción HTTP: estado {status}"
                            )
                        latencias_op.append(lat_ms)
                    else:
                        t_op0 = time.perf_counter_ns()

                        if mec == "M0":
                            payload = f"{est_id}|{doc_id}|{nota}"

                        elif mec == "M1":
                            payload = f"{est_id}|{doc_id}|{nota}|{time.time()}"

                        elif mec == "M2":
                            lamport = incrementar_lamport(lamport)
                            evento = construir_evento_productivo(
                                identificador=i + 1,
                                anterior=hash_p,
                                lamport=lamport,
                                vector=None,
                                est_id=est_id,
                                doc_id=doc_id,
                                nota_final=nota,
                                timestamp=time.time(),
                                modo="m2",
                            )
                            hash_p = evento["hash_actual"]

                        elif mec == "M3":
                            lamport = incrementar_lamport(lamport)
                            vector = incrementar_vector(vector, "docente-0")
                            evento = construir_evento_productivo(
                                identificador=i + 1,
                                anterior=hash_p,
                                lamport=lamport,
                                vector=vector,
                                est_id=est_id,
                                doc_id=doc_id,
                                nota_final=nota,
                                timestamp=time.time(),
                                modo="m3",
                            )
                            hash_p = evento["hash_actual"]

                        t_op1 = time.perf_counter_ns()

                        # Microbenchmark local: únicamente tiempo observado.
                        # No se añaden latencias, sobrecargas ni ruido sintético.
                        lat_op = (t_op1 - t_op0) / 1e6
                        latencias_op.append(lat_op)


                t_fin = time.perf_counter()
                duracion_total = t_fin - t_inicio
                throughput = round(transacciones / max(duracion_total, 0.001), 2)

                filas_exp1.append({
                    "concurrencia_docentes": conc,
                    "mecanismo": mec,
                    "repeticion": rep,
                    "transacciones_totales": transacciones,
                    "throughput_tps": throughput,
                    "latencia_media_ms": round(statistics.mean(latencias_op), 6),
                    "latencia_mediana_ms": round(statistics.median(latencias_op), 6),
                    "latencia_p95_ms": round(percentile(latencias_op, 95), 6),
                    "latencia_p99_ms": round(percentile(latencias_op, 99), 6),
                    "desviacion_std_ms": round(statistics.stdev(latencias_op) if len(latencias_op) > 1 else 0.0, 6)
                })

    if escribir_resultados:
        filepath = os.path.join(OUTPUT_DIR, "exp1_concurrencia.csv")
        with open(filepath, "w", newline="", encoding="utf-8") as f:
            writer = csv.DictWriter(
                f,
                fieldnames=list(filas_exp1[0].keys()),
                lineterminator="\n",
            )
            writer.writeheader()
            writer.writerows(filas_exp1)
        print(
            f"  -> Guardado: exp1_concurrencia.csv "
            f"({len(filas_exp1)} filas)"
        )

    return filas_exp1


# =============================================================================
# 5. EXPERIMENTO 2: INYECCIÃ“N DE MANIPULACIONES (T1 A T5) Y DETECCIÃ“N
# =============================================================================

def ejecutar_experimento_2_deteccion(
    *,
    escribir_resultados: bool = True,
) -> Tuple[List[Dict[str, Any]], List[Dict[str, Any]]]:
    print(
    "[2/5] Ejecutando Experimento 2: 600 corridas factoriales con inyeccion de manipulaciones (T1-T5)...")
    mecanismos = ["M0", "M1", "M2", "M3"]
    tipos_tampering = ["T1", "T2", "T3", "T4", "T5"]

    deteccion_rows = []
    manipulaciones_rows = []
    corrida_id = 1

    for mec in mecanismos:
        for rep in range(1, REPETICIONES_FACTORIALES + 1):
            for t_type in tipos_tampering:
                num_eventos = random.randint(45, 65)
                eventos = []
                tabla_calificaciones = {}
                hash_previo = GENESIS_HASH
                lamport = 0
                vector = {}

                t_reg_inicio = time.perf_counter()
                for i in range(num_eventos):
                    est_id = (i % NUM_ESTUDIANTES) + 1
                    doc_id = (i % NUM_DOCENTES) + 1
                    nota_formativa = 7.5 + ((i % 5) * 0.5)
                    nota_sumativa = 8.0 + ((i % 4) * 0.5)
                    nota_final = round(nota_formativa * 0.70 + nota_sumativa * 0.30, 2)
                    tabla_calificaciones[est_id] = nota_final

                    t_stamp = time.time() + (i * 0.05)
                    lamport = incrementar_lamport(lamport)
                    if mec == "M3":
                        vector = incrementar_vector(vector, f"docente-{rep % NUM_DOCENTES}")
                    modo = mec.lower()
                    evento = construir_evento_productivo(
                        identificador=i + 1, anterior=hash_previo,
                        lamport=lamport, vector=vector if mec == "M3" else None,
                        est_id=est_id, doc_id=doc_id, nota_final=nota_final,
                        timestamp=t_stamp, modo=modo,
                    )
                    if mec in ["M0", "M1"]:
                        evento["hash_anterior"] = None
                        evento["hash_actual"] = None
                    else:
                        hash_previo = evento["hash_actual"]
                    eventos.append(evento)
                t_reg_fin = time.perf_counter()
                latencia_registro = ((t_reg_fin - t_reg_inicio) / num_eventos) * 1000.0

                idx_tamper = num_eventos // 2
                ev_tamper = eventos[idx_tamper]
                payload_tamper = json.loads(ev_tamper["payload_canonico"])
                val_orig = str(payload_tamper["nota_final"])

                if t_type == "T1":
                    est_afectado = payload_tamper["est_id"]
                    tabla_calificaciones[est_afectado] = round(min(10.0, payload_tamper["nota_final"] + 1.5), 2)
                    val_adul = str(tabla_calificaciones[est_afectado])
                    campo_alterado = "calificacion_tabla_bd"
                elif t_type == "T2":
                    payload_tamper["nota_final"] = 10.00
                    ev_tamper["payload_canonico"] = json_canonico(payload_tamper)

                    contenido_tamper = json.loads(ev_tamper["contenido_canonico"])
                    contenido_tamper["payload"]["nota_final"] = 10.00
                    ev_tamper["contenido_canonico"] = json_canonico(contenido_tamper)

                    val_adul = "PAYLOAD_ALTERADO_10.00"
                    campo_alterado = "payload_bitacora"
                elif t_type == "T3":
                    ev_tamper["reloj_lamport"] = ev_tamper["reloj_lamport"] - 10
                    val_adul = f"LAMPORT_{ev_tamper['reloj_lamport']}"
                    campo_alterado = "reloj_lamport"
                elif t_type == "T4":
                    val_adul = "EVENTO_ELIMINADO"
                    eventos.pop(idx_tamper)
                    campo_alterado = "cadena_hash_truncada"
                elif t_type == "T5":
                    ev_tamper["timestamp"] -= 86400

                    contenido_tamper = json.loads(ev_tamper["contenido_canonico"])
                    contenido_tamper["timestamp"] = ev_tamper["timestamp"]
                    ev_tamper["contenido_canonico"] = json_canonico(contenido_tamper)

                    val_adul = "TIMESTAMP_RETROACTIVO"
                    campo_alterado = "timestamp_evento"

                t_v0 = time.perf_counter()
                valido_cadena, regla_cadena, id_cadena, t_det_us_cadena = medir_verificacion_productiva(eventos, mec)
                valido_tabla, regla_tabla, id_tabla, t_det_us_tabla = verificar_estado_tabla_vs_bitacora(tabla_calificaciones, eventos, mec)
                t_v1 = time.perf_counter()
                t_verif_ms = round((t_v1 - t_v0) * 1000.0, 3)

                if t_type == "T1":
                    detectado = not valido_tabla
                    regla_violada = regla_tabla if detectado else "NO_DETECTADO"
                    t_det_us = t_det_us_tabla
                else:
                    detectado = not valido_cadena
                    regla_violada = regla_cadena if detectado else "NO_DETECTADO"
                    t_det_us = t_det_us_cadena

                if mec in ["M0", "M1"]:
                    detectado = False
                    regla_violada = "SIN_PROTECCION_CRIPTOGRAFICA"
                    t_det_us = 0.0

                deteccion_rows.append({
                    "id_corrida": corrida_id,
                    "mecanismo": mec,
                    "repeticion": rep,
                    "tipo_manipulacion": t_type,
                    "eventos_totales": num_eventos,
                    "manipulaciones_inyectadas": 1,
                    "manipulaciones_detectadas": 1 if detectado else 0,
                    "tasa_deteccion_pct": 100.0 if detectado else 0.0,
                    "tiempo_verificacion_ms": t_verif_ms,
                    "latencia_registro_ms": round(latencia_registro, 3),
                    "estado": "DETECTADO" if detectado else "NO_DETECTADO"
                })

                manipulaciones_rows.append({
                    "id_corrida": corrida_id,
                    "id_evento": ev_tamper.get("id_evento", idx_tamper),
                    "docente_id": ev_tamper.get("actor_id", 1),
                    "estudiante_id": payload_tamper.get("est_id", 1),
                    "mecanismo": mec,
                    "tipo_ataque": t_type,
                    "campo_alterado": campo_alterado,
                    "valor_original": val_orig,
                    "valor_adulterado": val_adul,
                    "detectado": detectado,
                    "regla_violada": regla_violada,
                    "tiempo_deteccion_us": round(t_det_us, 1)
                })

                corrida_id += 1

    if escribir_resultados:
        f_det = os.path.join(OUTPUT_DIR, "deteccion.csv")
        with open(f_det, "w", newline="", encoding="utf-8") as f:
            writer = csv.DictWriter(
                f,
                fieldnames=list(deteccion_rows[0].keys()),
                lineterminator="\n",
            )
            writer.writeheader()
            writer.writerows(deteccion_rows)

        f_man = os.path.join(OUTPUT_DIR, "manipulaciones.csv")
        with open(f_man, "w", newline="", encoding="utf-8") as f:
            writer = csv.DictWriter(
                f,
                fieldnames=list(manipulaciones_rows[0].keys()),
                lineterminator="\n",
            )
            writer.writeheader()
            writer.writerows(manipulaciones_rows)

        print(
            f"  -> Guardados: deteccion.csv ({len(deteccion_rows)} filas), "
            f"manipulaciones.csv ({len(manipulaciones_rows)} filas)"
        )

    return deteccion_rows, manipulaciones_rows


# =============================================================================
# 6. EXPERIMENTO 3: RECONCILIACIÃ“N OFFLINE M2 VS M3 (RELOJES VECTORIALES)
# =============================================================================

def ejecutar_experimento_3_reconciliacion() -> List[Dict[str, Any]]:
    print(
        "[3/5] Ejecutando Experimento 3: "
        "Reconciliacion de ediciones offline concurrentes (M2 vs M3)..."
    )

    repeticiones = 30
    filas_exp3 = []

    for rep in range(1, repeticiones + 1):
        lamport_base = 5

        lamport_doc_a = incrementar_lamport(lamport_base)
        lamport_doc_b = incrementar_lamport(lamport_base)

        vector_base = {"docente-A": 1}

        vector_doc_a = incrementar_vector(
            vector_base,
            "docente-A",
        )
        vector_doc_b = incrementar_vector(
            vector_base,
            "docente-B",
        )

        conflicto_m2 = lamport_doc_a == lamport_doc_b

        reconciliacion = reconciliar_vectores(
            vector_doc_a,
            vector_doc_b,
        )

        conflicto_m3 = (
            reconciliacion["relacion"]
            == RelacionVectorial.CONCURRENTE
        )

        filas_exp3.append({
            "repeticion": rep,
            "mecanismo": "M2",
            "tipo_evento": "EDICION_CONCURRENTE_OFFLINE",
            "lamport_docA": lamport_doc_a,
            "lamport_docB": lamport_doc_b,
            "vector_docA": "N/A",
            "vector_docB": "N/A",
            "conflicto_detectado": conflicto_m2,
            "reconciliacion_automatica": False,
            "estrategia_resolucion":
                "BLOQUEO_INTERVENCION_MANUAL",
        })

        filas_exp3.append({
            "repeticion": rep,
            "mecanismo": "M3",
            "tipo_evento": "EDICION_CONCURRENTE_OFFLINE",
            "lamport_docA": lamport_doc_a,
            "lamport_docB": lamport_doc_b,
            "vector_docA": json_canonico(vector_doc_a),
            "vector_docB": json_canonico(vector_doc_b),
            "conflicto_detectado": conflicto_m3,
            "reconciliacion_automatica": False,
            "estrategia_resolucion":
                reconciliacion["politica"],
        })

    filepath = os.path.join(OUTPUT_DIR, "exp3_reconciliacion.csv")

    with open(filepath, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(
            f,
            fieldnames=list(filas_exp3[0].keys()),
            lineterminator="\n",
       )
        writer.writeheader()
        writer.writerows(filas_exp3)

    print(
        "  -> Guardado: exp3_reconciliacion.csv "
        f"({len(filas_exp3)} filas)"
    )

    return filas_exp3


# =============================================================================
# 7. MÉTRICAS DE CALIDAD ISO/IEC 25010 Y ANÁLISIS DE FALSOS POSITIVOS (FPR)
# =============================================================================

def obtener_cobertura_jacoco_real() -> Dict[str, float]:
    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    jacoco_principal = os.path.join(repo_root, "docs", "cobertura", "sga-principal", "jacoco.csv")
    jacoco_sec = os.path.join(repo_root, "docs", "cobertura", "secretaria", "jacoco.csv")

    pct_principal = "No disponible"
    if os.path.exists(jacoco_principal):
        try:
            with open(jacoco_principal, "r", encoding="utf-8") as f:
                reader = csv.DictReader(f)
                l_miss = sum(int(r.get("LINE_MISSED", 0)) for r in reader)
                f.seek(0)
                next(reader)
                l_cov = sum(int(r.get("LINE_COVERED", 0)) for r in reader)
                if (l_cov + l_miss) > 0:
                    pct_principal = round((l_cov / (l_cov + l_miss)) * 100.0, 2)
        except Exception:
            pass

    pct_sec = "No disponible"
    if os.path.exists(jacoco_sec):
        try:
            with open(jacoco_sec, "r", encoding="utf-8") as f:
                reader = csv.DictReader(f)
                l_miss = sum(int(r.get("LINE_MISSED", 0)) for r in reader)
                f.seek(0)
                next(reader)
                l_cov = sum(int(r.get("LINE_COVERED", 0)) for r in reader)
                if (l_cov + l_miss) > 0:
                    pct_sec = round((l_cov / (l_cov + l_miss)) * 100.0, 2)
        except Exception:
            pass

    return {
        "sga_principal_global_pct": pct_principal,
        "secretaria_global_pct": pct_sec,
        "core_cripto_auditoria_pct": "No disponible"
    }


def ejecutar_metricas_iso25010() -> List[Dict[str, Any]]:
    print("[4/5] Registrando metricas de calidad ISO/IEC 25010 (Derivación de evidencia histórica local de Locust y JaCoCo)...")
    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    h1 = os.path.join(repo_root, "experimentos", "resultados", "locust_esc1_stats_history.csv")
    h2 = os.path.join(repo_root, "experimentos", "resultados", "locust_esc3_stats_history.csv")

    coberturas = obtener_cobertura_jacoco_real()
    jacoco_global = coberturas["sga_principal_global_pct"]

    def parse_history_windows(filepath: str, escenario_nombre: str, default_users: int, duracion_total: int, num_windows: int = 10):
        if not os.path.exists(filepath):
            raise ValueError(f"Historial requerido ausente o sin muestras: {filepath}")
        with open(filepath, "r", encoding="utf-8") as f:
            rows = [r for r in csv.DictReader(f) if r.get("Name") == "Aggregated"]
        valid_rows = [r for r in rows if r.get("50%") not in ("N/A", "", None)]
        if not valid_rows:
            raise ValueError(f"Historial requerido ausente o sin muestras: {filepath}")
        step = max(1, len(valid_rows) // num_windows)
        out = []
        for i in range(num_windows):
            chunk = valid_rows[i*step:(i+1)*step]
            if not chunk:
                continue
            avg_rps = sum(float(r["Requests/s"]) for r in chunk) / len(chunk)
            med_lat = sum(float(r["50%"]) for r in chunk) / len(chunk)
            p95_lat = sum(float(r["95%"]) for r in chunk) / len(chunk)
            p99_lat = sum(float(r["99%"]) for r in chunk) / len(chunk)
            fail_s = sum(float(r["Failures/s"]) for r in chunk) / len(chunk)
            err_pct = (fail_s / avg_rps * 100.0) if avg_rps > 0 else 0.0
            disp_pct = max(0.0, 100.0 - err_pct)
            users = int(chunk[-1]["User Count"]) if "User Count" in chunk[-1] and chunk[-1]["User Count"] != "" else default_users
            reqs_window = int(round(avg_rps * (duracion_total / num_windows)))
            out.append({
                "escenario": escenario_nombre,
                "ventana_derivada": i + 1,
                "usuarios_concurrentes": users,
                "duracion_configurada_por_ventana_s": int(duracion_total / num_windows),
                "peticiones_estimadas": reqs_window,
                "throughput_promedio_muestras_rps": round(avg_rps, 2),
                "latencia_media_ms": "No disponible",
                "promedio_p50_muestras_ms": round(med_lat, 2),
                "promedio_p95_muestras_ms": round(p95_lat, 2),
                "promedio_p99_muestras_ms": round(p99_lat, 2),
                "fallos_generales_derivados_pct": round(err_pct, 4),
                "errores_5xx_pct": "No disponible",
                "exito_peticiones_derivado_pct": round(disp_pct, 4),
                "disponibilidad_produccion_pct": "No disponible",
                "cobertura_jacoco_pct": jacoco_global,
                "rechazo_401_pct": "No disponible",
                "origen": os.path.relpath(filepath, repo_root).replace(os.sep, "/"),
                "alcance": "derivado de muestras históricas; no nueva carga ni agregado oficial"
            })
        return out

    iso_rows = []
    esc1_rows = parse_history_windows(h1, "Histórico locust_esc1", default_users=50, duracion_total=300, num_windows=10)
    esc2_rows = parse_history_windows(h2, "Histórico locust_esc3 (perfil no validado)", default_users=200, duracion_total=600, num_windows=10)
    iso_rows.extend(esc1_rows)
    iso_rows.extend(esc2_rows)

    filepath = os.path.join(OUTPUT_DIR, "iso25010.csv")
    with open(filepath, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=list(iso_rows[0].keys()), lineterminator="\n")
        writer.writeheader()
        writer.writerows(iso_rows)
    print(f"  -> Guardado: iso25010.csv ({len(iso_rows)} ventanas derivadas de archivos históricos de Locust y JaCoCo)")

    return iso_rows


def verificar_falsos_positivos() -> Tuple[float, List[Dict[str, Any]]]:
    """Verifica que la tasa de falsos positivos en cadenas limpias e Ã­ntegras sea exactamente 0.0% y exporta falsos_positivos.csv."""
    print(
        "  -> Evaluando Tasa de Falsos Positivos (FPR) "
        "en 30 cadenas validas sin manipulacion..."
    )

    falsos_positivos = 0
    total_pruebas = 30
    fpr_rows = []

    for c in range(1, total_pruebas + 1):
        num_eventos = 50
        eventos = []
        tabla = {}
        hash_p = GENESIS_HASH
        lamport_actual = 0

        for i in range(num_eventos):
            est_id = i + 1
            nota = 8.5

            tabla[est_id] = nota

            lamport_actual = incrementar_lamport(
                lamport_actual
            )

            evento = construir_evento_productivo(
                identificador=i + 1,
                anterior=hash_p,
                lamport=lamport_actual,
                vector=None,
                est_id=est_id,
                doc_id=1,
                nota_final=nota,
                timestamp=time.time(),
                modo="m2",
            )

            eventos.append(evento)
            hash_p = evento["hash_actual"]

        t0 = time.perf_counter_ns()

        valido_cad, _, _, _ = medir_verificacion_productiva(
            eventos,
            "M2",
        )

        valido_tab, _, _, _ = verificar_estado_tabla_vs_bitacora(
            tabla,
            eventos,
            "M2",
        )

        t_ms = (
            time.perf_counter_ns() - t0
        ) / 1_000_000.0

        es_falso = (not valido_cad) or (not valido_tab)

        if es_falso:
            falsos_positivos += 1

        fpr_rows.append({
            "corrida": c,
            "mecanismo": "M2",
            "num_eventos": num_eventos,
            "cadena_integra": 1 if valido_cad else 0,
            "estado_tabla_integro": 1 if valido_tab else 0,
            "falso_positivo": 1 if es_falso else 0,
            "tiempo_verificacion_ms": round(t_ms, 3),
        })

    fpr = (
        falsos_positivos / total_pruebas
    ) * 100.0

    filepath = os.path.join(OUTPUT_DIR, "falsos_positivos.csv")
    with open(filepath, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=list(fpr_rows[0].keys()), lineterminator="\n")
        writer.writeheader()
        writer.writerows(fpr_rows)

    print(
        "  -> Guardado: falsos_positivos.csv "
        f"({len(fpr_rows)} corridas limpias)"
    )

    # Artefacto auxiliar de esta ejecucion.
    # Se escribe exclusivamente en OUTPUT_DIR.
    # main() decide cuando publicarlo en el destino canonico.
    # E7 no modifica docs/experimentos/resultados.
    return fpr, fpr_rows


# =============================================================================
# =============================================================================
# 8. BOXPLOT DESCRIPTIVO DE LATENCIAS DE DETECCIÓN
# =============================================================================

def generar_graficos_y_estadistica(deteccion_rows: List[Dict[str, Any]]):
    """Genera únicamente el boxplot requerido por las evidencias E7.

    Las latencias del Experimento 2 no se usan para contrastes inferenciales
    ni tamaños de efecto del punto 21. El análisis estadístico reproducible
    de latencias usa exp1_concurrencia.csv mediante
    experimentos/generar_tabla_latencias.py.
    """
    print("[5/5] Generando gráfico descriptivo de latencias de detección...")

    lat_m0 = [
        r["latencia_registro_ms"]
        for r in deteccion_rows
        if r["mecanismo"] == "M0"
    ]
    lat_m1 = [
        r["latencia_registro_ms"]
        for r in deteccion_rows
        if r["mecanismo"] == "M1"
    ]
    lat_m2 = [
        r["latencia_registro_ms"]
        for r in deteccion_rows
        if r["mecanismo"] == "M2"
    ]
    lat_m3 = [
        r["latencia_registro_ms"]
        for r in deteccion_rows
        if r["mecanismo"] == "M3"
    ]

    try:
        import matplotlib
        matplotlib.use("Agg")
        import matplotlib.pyplot as plt

        plt.figure(figsize=(9, 5.5), dpi=300)
        data_plot = [lat_m0, lat_m1, lat_m2, lat_m3]
        box = plt.boxplot(
            data_plot,
            patch_artist=True,
            tick_labels=[
                "M0 (Base)",
                "M1 (Relacional)",
                "M2 (Cripto-Lamport)",
                "M3 (Vector-Reconcil)",
            ],
        )

        colors = ["#81c784", "#64b5f6", "#ffb74d", "#e57373"]
        for patch, color in zip(box["boxes"], colors):
            patch.set_facecolor(color)

        plt.title(
            "Latencia de detección por mecanismo de auditoría (AcadTrace)",
            fontsize=12,
            fontweight="bold",
        )
        plt.ylabel(
            "Latencia de registro de calificaciones (ms)",
            fontsize=11,
        )
        plt.grid(axis="y", linestyle="--", alpha=0.7)
        plt.tight_layout()

        plot_path = os.path.join(OUTPUT_DIR, "boxplot_latencia.png")
        plt.savefig(plot_path)
        plt.close()

        print(f"  -> Gráfico PNG generado en: {plot_path}")
    except Exception as exc:
        raise RuntimeError(
            "No se pudo generar boxplot_latencia.png en esta ejecución"
        ) from exc


def main():
    global OUTPUT_DIR
    parser = argparse.ArgumentParser(description="Generación y certificado E7 de una ejecución concreta")
    parser.add_argument("--mode", choices=("local", "http"), default=os.environ.get("EXPERIMENT_MODE", "local"))
    parser.add_argument("--output-dir", type=Path, default=Path(OUTPUT_DIR))
    args = parser.parse_args()
    if args.mode not in ("local", "http"):
        parser.error("EXPERIMENT_MODE debe ser local o http")
    # Fallar antes de generar si no está instalada la dependencia gráfica.
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot
    random.seed(SEED)
    client = LiveBackendClient(mode=args.mode)
    destination = args.output_dir.resolve()
    destination.mkdir(parents=True, exist_ok=True)
    print(f"Modo explícito: {args.mode}; Python {sys.version.split()[0]}; Matplotlib {matplotlib.__version__}; semilla {SEED}")
    print("Datos sintéticos; tiempos medidos variables. ISO deriva de archivos históricos locales.")
    original_output = OUTPUT_DIR
    # Directorio vacío: ningún artefacto anterior puede superar las comprobaciones.
    with tempfile.TemporaryDirectory(prefix="e7-", dir=destination.parent) as temporary:
        OUTPUT_DIR = temporary
        try:
            ejecutar_experimento_1_concurrencia(client)
            deteccion_rows, _ = ejecutar_experimento_2_deteccion()
            ejecutar_experimento_3_reconciliacion()
            ejecutar_metricas_iso25010()
            verificar_falsos_positivos()
            generar_graficos_y_estadistica(deteccion_rows)
            staged = Path(temporary)
            write_certificate(staged)  # Exige 6/6, no vacíos y CSV con LF.
            if verify(staged) != 0:
                raise RuntimeError("La verificación previa a publicación falló")
            # Preparar la publicacion de los seis artefactos E7, el certificado y el auxiliar de falsos positivos.
            publish_artifacts = (
                *ARTIFACTS,
                "falsos_positivos.csv",
            )

            # Conservar los archivos anteriores antes de publicar
            # la nueva corrida.
            existing = [
                name
                for name in (*publish_artifacts, CERTIFICATE)
                if (destination / name).exists()
            ]

            if existing:
                archive = Path(
                    tempfile.mkdtemp(
                        prefix="e7-anterior-",
                        dir=destination,
                    )
                )

                for name in existing:
                    shutil.copy2(
                        destination / name,
                        archive / name,
                    )

                print(
                    "Evidencia anterior conservada en: "
                    f"{archive}"
                )

            # Publicar primero los seis artefactos certificados
            # y el auxiliar falsos_positivos.csv de esta corrida.
            for name in publish_artifacts:
                os.replace(
                    staged / name,
                    destination / name,
                )

            # REPRODUCIBILIDAD.txt sigue certificando exclusivamente
            # los seis artefactos definidos por ARTIFACTS y se publica
            # al final para no validar un conjunto parcial.
            os.replace(
                staged / CERTIFICATE,
                destination / CERTIFICATE,
            )
        finally:
            OUTPUT_DIR = original_output
    if verify(destination) != 0:
        raise RuntimeError("Falló la verificación del conjunto publicado")
    print("[OK] E7: integridad de esta ejecución verificada; no se garantiza identidad de hashes entre ejecuciones.")


if __name__ == "__main__":
    main()
