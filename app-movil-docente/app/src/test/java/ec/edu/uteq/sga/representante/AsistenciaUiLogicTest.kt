package ec.edu.uteq.sga.representante

import ec.edu.uteq.sga.representante.ui.screens.representante.TrimestreAsistencia
import ec.edu.uteq.sga.representante.ui.screens.representante.formatoFechaAsistencia
import ec.edu.uteq.sga.representante.ui.screens.representante.trimestreAsistencia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AsistenciaUiLogicTest {
    @Test fun cadaNombreRealConAnoLectivoCorrespondeASuTrimestre() {
        assertEquals(TrimestreAsistencia.T1, "Primer Trimestre 2026-2027".trimestreAsistencia())
        assertEquals(TrimestreAsistencia.T2, "Segundo Trimestre 2026-2027".trimestreAsistencia())
        assertEquals(TrimestreAsistencia.T3, "Tercer Trimestre 2026-2027".trimestreAsistencia())
    }

    @Test fun tambienAceptaNombresSinAnoLectivo() {
        assertEquals(TrimestreAsistencia.T1, "Primer Trimestre".trimestreAsistencia())
        assertEquals(TrimestreAsistencia.T2, "Segundo Trimestre".trimestreAsistencia())
        assertEquals(TrimestreAsistencia.T3, "Tercer Trimestre".trimestreAsistencia())
    }

    @Test fun cadaFiltroExcluyeLosOtrosTrimestres() {
        val periodos = listOf(
            "Primer Trimestre 2026-2027",
            "Segundo Trimestre 2026-2027",
            "Tercer Trimestre 2026-2027"
        )

        assertEquals(listOf(periodos[0]), periodos.filter { it.trimestreAsistencia() == TrimestreAsistencia.T1 })
        assertEquals(listOf(periodos[1]), periodos.filter { it.trimestreAsistencia() == TrimestreAsistencia.T2 })
        assertEquals(listOf(periodos[2]), periodos.filter { it.trimestreAsistencia() == TrimestreAsistencia.T3 })
    }

    @Test fun normalizaMayusculasYEspaciosSinAceptarTextoAmbiguo() {
        assertEquals(TrimestreAsistencia.T3, "  tercer   trimestre  2026-2027 ".trimestreAsistencia())
        assertNull("Segundo y Tercer Trimestre 2026-2027".trimestreAsistencia())
    }

    @Test fun fechaUsaDiaMesYAnoDelRegistro() {
        assertEquals("15 SEP 2026", "2026-09-15".formatoFechaAsistencia())
        assertEquals("18 FEB 2027", "2027-02-18".formatoFechaAsistencia())
        assertEquals("10 ENE 2028", "2028-01-10".formatoFechaAsistencia())
    }
}
