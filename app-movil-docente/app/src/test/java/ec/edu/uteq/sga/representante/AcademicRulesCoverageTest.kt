package ec.edu.uteq.sga.representante

import ec.edu.uteq.sga.representante.domain.rules.AcademicRules
import org.junit.Assert.*
import org.junit.Test

class AcademicRulesCoverageTest {

    @Test
    fun convertirNotaCualitativaCubreTodosLosRangos() {
        assertEquals("A_MAS", AcademicRules.convertirNotaCualitativa(10.0))
        assertEquals("A_MAS", AcademicRules.convertirNotaCualitativa(9.0))
        assertEquals("B_MAS", AcademicRules.convertirNotaCualitativa(8.0))
        assertEquals("B_MAS", AcademicRules.convertirNotaCualitativa(7.0))
        assertEquals("C_MAS", AcademicRules.convertirNotaCualitativa(6.0))
        assertEquals("C_MAS", AcademicRules.convertirNotaCualitativa(4.01))
        assertEquals("D", AcademicRules.convertirNotaCualitativa(4.0))
        assertEquals("D", AcademicRules.convertirNotaCualitativa(0.0))
    }

    @Test
    fun etiquetasCompletasCubrenCodigosYValoresDesconocidos() {
        assertEquals("A+ (Domina los aprendizajes)", AcademicRules.getEtiquetaCualitativa("A_MAS"))
        assertEquals("A- (Domina aprendizajes)", AcademicRules.getEtiquetaCualitativa("A_MENOS"))
        assertEquals("B+ (Alcanza los aprendizajes)", AcademicRules.getEtiquetaCualitativa("B_MAS"))
        assertEquals("B- (Alcanza aprendizajes)", AcademicRules.getEtiquetaCualitativa("B_MENOS"))
        assertEquals("C+ (Próximo a alcanzar)", AcademicRules.getEtiquetaCualitativa("C_MAS"))
        assertEquals("C- (Próximo a alcanzar)", AcademicRules.getEtiquetaCualitativa("C_MENOS"))
        assertEquals("D (No alcanza aprendizajes)", AcademicRules.getEtiquetaCualitativa("D"))
        assertEquals("OTRO", AcademicRules.getEtiquetaCualitativa("OTRO"))
        assertEquals("-", AcademicRules.getEtiquetaCualitativa(null))
    }

    @Test
    fun etiquetasCortasCubrenCodigosYValoresDesconocidos() {
        assertEquals("A+", AcademicRules.getEtiquetaCortaCualitativa("A_MAS"))
        assertEquals("A-", AcademicRules.getEtiquetaCortaCualitativa("A_MENOS"))
        assertEquals("B+", AcademicRules.getEtiquetaCortaCualitativa("B_MAS"))
        assertEquals("B-", AcademicRules.getEtiquetaCortaCualitativa("B_MENOS"))
        assertEquals("C+", AcademicRules.getEtiquetaCortaCualitativa("C_MAS"))
        assertEquals("C-", AcademicRules.getEtiquetaCortaCualitativa("C_MENOS"))
        assertEquals("D", AcademicRules.getEtiquetaCortaCualitativa("D"))
        assertEquals("X", AcademicRules.getEtiquetaCortaCualitativa("X"))
        assertEquals("-", AcademicRules.getEtiquetaCortaCualitativa(null))
    }

    @Test
    fun calculaPromedioTrimestral7030() {
        assertEquals(
            8.30,
            AcademicRules.calcularPromedioTrimestral(8.0, 9.0),
            0.001
        )
    }

    @Test
    fun promedioAnualVacioEsCero() {
        assertEquals(
            0.0,
            AcademicRules.calcularPromedioAnual(emptyList()),
            0.001
        )
    }

    @Test
    fun promedioAnualCalculaYRedondea() {
        assertEquals(
            8.0,
            AcademicRules.calcularPromedioAnual(listOf(9.0, 8.0, 7.0)),
            0.001
        )
    }

    @Test
    fun ponderacionFormativaValidaYExcedida() {
        val valido = AcademicRules.validarPonderacion(50.0, 20.0, false)
        assertTrue(valido.first)
        assertNull(valido.second)

        val invalido = AcademicRules.validarPonderacion(65.0, 10.0, false)
        assertFalse(invalido.first)
        assertNotNull(invalido.second)
        assertTrue(invalido.second!!.contains("formativas"))
    }

    @Test
    fun ponderacionSumativaValidaYExcedida() {
        val valido = AcademicRules.validarPonderacion(20.0, 10.0, true)
        assertTrue(valido.first)
        assertNull(valido.second)

        val invalido = AcademicRules.validarPonderacion(25.0, 10.0, true)
        assertFalse(invalido.first)
        assertNotNull(invalido.second)
        assertTrue(invalido.second!!.contains("sumativas"))
    }
}
