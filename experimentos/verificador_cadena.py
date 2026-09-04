#!/usr/bin/env python3
"""
Módulo de Verificación Criptográfica y Detección de Manipulaciones (T1 a T5)
AcadTrace / SGA Escuela - Entrega 4
"""

import json
import hashlib
from dataclasses import dataclass
from typing import List, Dict, Any, Optional, Tuple

GENESIS_HASH = "0" * 64


@dataclass(frozen=True)
class ResultadoVerificacion:
    valido: bool
    registros_verificados: int
    primer_eslabon_roto: Optional[int] = None
    tipo_inconsistencia: Optional[str] = None
    tiempo_deteccion_us: float = 0.0


def calcular_sha256(anterior: str, contenido: str) -> str:
    raw = f"{anterior}|{contenido}"
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()


def verificar_cadena(eventos: List[Dict[str, Any]], hash_cabeza: Optional[str] = None, lamport_cabeza: Optional[int] = None) -> ResultadoVerificacion:
    anterior = GENESIS_HASH
    lamport_anterior = 0
    verificados = 0

    for ev in eventos:
        identificador = ev.get("id_evento", ev.get("id", verificados + 1))
        
        # Invariante 1: Encadenamiento por hash
        if ev.get("hash_anterior", ev.get("hash_previo")) != anterior:
            return ResultadoVerificacion(False, verificados, identificador, "HASH_ANTERIOR_INVALIDO")

        # Invariante 2: Monotonicidad estricta de Lamport
        lamport = int(ev.get("reloj_lamport", ev.get("lamport", 0)))
        if lamport <= lamport_anterior:
            return ResultadoVerificacion(False, verificados, identificador, "LAMPORT_NO_MONOTONICO")

        # Invariante 3: Integridad de resumen de contenido
        contenido_str = ev.get("payload_canonico", ev.get("payload", ""))
        calculado = calcular_sha256(anterior, contenido_str)
        if ev.get("hash_actual") != calculado:
            return ResultadoVerificacion(False, verificados, identificador, "HASH_ACTUAL_INVALIDO")

        anterior = calculado
        lamport_anterior = lamport
        verificados += 1

    if hash_cabeza is not None and anterior != hash_cabeza:
        return ResultadoVerificacion(False, verificados, None, "CABEZA_CADENA_INVALIDA")
    if lamport_cabeza is not None and lamport_anterior != int(lamport_cabeza):
        return ResultadoVerificacion(False, verificados, None, "CABEZA_LAMPORT_INVALIDA")

    return ResultadoVerificacion(True, verificados)


def verificar_estado_tabla_vs_bitacora(tabla_calificaciones: List[Dict[str, Any]], eventos_bitacora: List[Dict[str, Any]]) -> Tuple[bool, Optional[str], Optional[int]]:
    """
    Detección de Manipulación T1 (Direct Database Tampering):
    Reconstruye el estado verdadero acumulado a partir de la bitácora criptográfica
    verificada y lo contrasta contra los registros vigentes en la tabla física de calificaciones.
    Si una nota fue alterada directamente en SQL, la bitácora revela la discrepancia de estado.
    """
    # 1. Primero se valida la integridad de la cadena
    res_cadena = verificar_cadena(eventos_bitacora)
    if not res_cadena.valido:
        return False, f"BITACORA_ALTERADA_{res_cadena.tipo_inconsistencia}", res_cadena.primer_eslabon_roto

    # 2. Reconstrucción determinista del estado a partir de los eventos válidos
    estado_esperado = {}
    for ev in eventos_bitacora:
        est_id = ev.get("est_id", ev.get("estudiante_id"))
        nota = float(ev.get("nota_final", ev.get("nota", 0.0)))
        estado_esperado[est_id] = nota

    # 3. Comparación contra la tabla persistida
    for fila in tabla_calificaciones:
        est_id = fila.get("est_id", fila.get("id_estudiante"))
        nota_actual = float(fila.get("nota_final", fila.get("nota", 0.0)))
        if est_id in estado_esperado:
            nota_esperada = estado_esperado[est_id]
            if abs(nota_actual - nota_esperada) > 0.001:
                return False, "DISCREPANCIA_ESTADO_TABLA_VS_BITACORA", est_id

    return True, None, None
