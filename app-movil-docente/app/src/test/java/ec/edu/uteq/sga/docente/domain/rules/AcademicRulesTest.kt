package ec.edu.uteq.sga.docente.domain.rules

import org.junit.Assert.*
import org.junit.Test

class AcademicRulesTest {

    @Test
    fun `convertirNotaCualitativa devuelve A_MAS para notas mayores o iguales a 9`() {
        assertEquals("A_MAS", AcademicRules.convertirNotaCualitativa(10.0))
        assertEquals("A_MAS", AcademicRules.convertirNotaCualitativa(9.5))
        assertEquals("A_MAS", AcademicRules.convertirNotaCualitativa(9.0))
    }

    @Test
    fun `convertirNotaCualitativa devuelve B_MAS para notas entre 7 y 8,99`() {
        assertEquals("B_MAS", AcademicRules.convertirNotaCualitativa(8.99))
        assertEquals("B_MAS", AcademicRules.convertirNotaCualitativa(7.5))
        assertEquals("B_MAS", AcademicRules.convertirNotaCualitativa(7.0))
    }

    @Test
    fun `convertirNotaCualitativa devuelve C_MAS para notas entre 4,01 y 6,99`() {
        assertEquals("C_MAS", AcademicRules.convertirNotaCualitativa(6.99))
        assertEquals("C_MAS", AcademicRules.convertirNotaCualitativa(5.0))
        assertEquals("C_MAS", AcademicRules.convertirNotaCualitativa(4.01))
    }

    @Test
    fun `convertirNotaCualitativa devuelve D para notas menores o iguales a 4`() {
        assertEquals("D", AcademicRules.convertirNotaCualitativa(4.0))
        assertEquals("D", AcademicRules.convertirNotaCualitativa(2.5))
        assertEquals("D", AcademicRules.convertirNotaCualitativa(0.0))
    }

    @Test
    fun `calcularPromedioTrimestral aplica formula 70 por ciento formativo y 30 por ciento sumativo`() {
        // Formativo: 8.0 * 0.70 = 5.60
        // Sumativo:  9.0 * 0.30 = 2.70
        // Total: 8.30
        val promedio = AcademicRules.calcularPromedioTrimestral(promedioFormativo = 8.0, notaSumativa = 9.0)
        assertEquals(8.30, promedio, 0.001)
    }

    @Test
    fun `calcularPromedioTrimestral con notas perfectas devuelve 10`() {
        val promedio = AcademicRules.calcularPromedioTrimestral(promedioFormativo = 10.0, notaSumativa = 10.0)
        assertEquals(10.00, promedio, 0.001)
    }

    @Test
    fun `calcularPromedioAnual calcula la media exacta de tres trimestres`() {
        val notasTrimestres = listOf(8.50, 9.00, 8.00)
        val anual = AcademicRules.calcularPromedioAnual(notasTrimestres)
        assertEquals(8.50, anual, 0.001)
    }

    @Test
    fun `calcularPromedioAnual con lista vacia devuelve 0`() {
        val anual = AcademicRules.calcularPromedioAnual(emptyList())
        assertEquals(0.0, anual, 0.001)
    }

    @Test
    fun `validarPonderacion formativa permite maximo 70 por ciento`() {
        val (valido1, error1) = AcademicRules.validarPonderacion(
            ponderacionActual = 50.0,
            nuevaPonderacion = 20.0,
            esSumativa = false
        )
        assertTrue(valido1)
        assertNull(error1)

        val (valido2, error2) = AcademicRules.validarPonderacion(
            ponderacionActual = 50.0,
            nuevaPonderacion = 25.0,
            esSumativa = false
        )
        assertFalse(valido2)
        assertNotNull(error2)
        assertTrue(error2!!.contains("70.0%"))
    }

    @Test
    fun `validarPonderacion sumativa permite maximo 30 por ciento`() {
        val (valido1, error1) = AcademicRules.validarPonderacion(
            ponderacionActual = 15.0,
            nuevaPonderacion = 15.0,
            esSumativa = true
        )
        assertTrue(valido1)
        assertNull(error1)

        val (valido2, error2) = AcademicRules.validarPonderacion(
            ponderacionActual = 20.0,
            nuevaPonderacion = 15.0,
            esSumativa = true
        )
        assertFalse(valido2)
        assertNotNull(error2)
        assertTrue(error2!!.contains("30.0%"))
    }

    @Test
    fun `getEtiquetaCualitativa y etiqueta corta devuelven representaciones correctas`() {
        assertEquals("A+ (Domina los aprendizajes)", AcademicRules.getEtiquetaCualitativa("A_MAS"))
        assertEquals("A+", AcademicRules.getEtiquetaCortaCualitativa("A_MAS"))
        assertEquals("B+ (Alcanza los aprendizajes)", AcademicRules.getEtiquetaCualitativa("B_MAS"))
        assertEquals("B+", AcademicRules.getEtiquetaCortaCualitativa("B_MAS"))
        assertEquals("D (No alcanza aprendizajes)", AcademicRules.getEtiquetaCualitativa("D"))
        assertEquals("D", AcademicRules.getEtiquetaCortaCualitativa("D"))
    }
}
