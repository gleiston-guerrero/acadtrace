package ec.edu.uteq.sga.representante.domain.rules

import java.math.BigDecimal
import java.math.RoundingMode

object AcademicRules {

    const val MAX_PONDERACION_FORMATIVA = 70.0
    const val MAX_PONDERACION_SUMATIVA = 30.0
    const val NOTA_MAXIMA_DEFECTO = 10.0

    /**
     * Convierte una calificación numérica en su escala cualitativa equivalente (A+, B+, C+, D).
     */
    fun convertirNotaCualitativa(nota: Double, nivel: String = "EGB"): String {
        val bdNota = BigDecimal.valueOf(nota).setScale(2, RoundingMode.HALF_UP)
        return when {
            bdNota >= BigDecimal("9.00") -> "A_MAS"
            bdNota >= BigDecimal("7.00") -> "B_MAS"
            bdNota > BigDecimal("4.00") -> "C_MAS"
            else -> "D"
        }
    }

    fun getEtiquetaCualitativa(codigo: String?): String {
        return when (codigo) {
            "A_MAS" -> "A+ (Domina los aprendizajes)"
            "A_MENOS" -> "A- (Domina aprendizajes)"
            "B_MAS" -> "B+ (Alcanza los aprendizajes)"
            "B_MENOS" -> "B- (Alcanza aprendizajes)"
            "C_MAS" -> "C+ (Próximo a alcanzar)"
            "C_MENOS" -> "C- (Próximo a alcanzar)"
            "D" -> "D (No alcanza aprendizajes)"
            else -> codigo ?: "-"
        }
    }

    fun getEtiquetaCortaCualitativa(codigo: String?): String {
        return when (codigo) {
            "A_MAS" -> "A+"
            "A_MENOS" -> "A-"
            "B_MAS" -> "B+"
            "B_MENOS" -> "B-"
            "C_MAS" -> "C+"
            "C_MENOS" -> "C-"
            "D" -> "D"
            else -> codigo ?: "-"
        }
    }

    /**
     * Calcula el promedio trimestral aplicando la fórmula del sistema:
     * 70% Promedio Formativo + 30% Nota Sumativa
     */
    fun calcularPromedioTrimestral(promedioFormativo: Double, notaSumativa: Double): Double {
        val f = BigDecimal.valueOf(promedioFormativo).multiply(BigDecimal("0.70"))
        val s = BigDecimal.valueOf(notaSumativa).multiply(BigDecimal("0.30"))
        return f.add(s).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Calcula el promedio anual como el promedio simple de los trimestres cursados.
     */
    fun calcularPromedioAnual(notasTrimestrales: List<Double>): Double {
        if (notasTrimestrales.isEmpty()) return 0.0
        val suma = notasTrimestrales.map { BigDecimal.valueOf(it) }
            .fold(BigDecimal.ZERO) { acc, d -> acc.add(d) }
        return suma.divide(BigDecimal.valueOf(notasTrimestrales.size.toLong()), 2, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Valida si una ponderación propuesta excede los límites (70% para formativas o 30% para sumativas).
     */
    fun validarPonderacion(
        ponderacionActual: Double,
        nuevaPonderacion: Double,
        esSumativa: Boolean
    ): Pair<Boolean, String?> {
        val limite = if (esSumativa) MAX_PONDERACION_SUMATIVA else MAX_PONDERACION_FORMATIVA
        val tipo = if (esSumativa) "sumativas" else "formativas"
        val total = ponderacionActual + nuevaPonderacion
        return if (total > limite) {
            Pair(false, "La ponderación total para actividades $tipo no puede superar el $limite%. (Actual: $ponderacionActual%)")
        } else {
            Pair(true, null)
        }
    }
}
