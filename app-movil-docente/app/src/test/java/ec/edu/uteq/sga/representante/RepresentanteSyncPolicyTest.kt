package ec.edu.uteq.sga.representante

import ec.edu.uteq.sga.representante.data.sync.RepresentanteSyncPolicy
import org.junit.Assert.*
import org.junit.Test

class RepresentanteSyncPolicyTest {
    @Test fun modoAvionProgramaReintentoDeLectura() = assertTrue(RepresentanteSyncPolicy.shouldRetry(false, false))
    @Test fun reconexionSinRefreshCompletoReintenta() = assertTrue(RepresentanteSyncPolicy.shouldRetry(true, false))
    @Test fun reconexionYCacheActualizadaFinaliza() = assertFalse(RepresentanteSyncPolicy.shouldRetry(true, true))
    @Test fun representanteNoEscribeNotas() = assertFalse(RepresentanteSyncPolicy.acceptsAcademicWrite("CALIFICACION"))
    @Test fun representanteNoEscribeAsistenciaNiActividades() {
        assertFalse(RepresentanteSyncPolicy.acceptsAcademicWrite("ASISTENCIA"))
        assertFalse(RepresentanteSyncPolicy.acceptsAcademicWrite("ACTIVIDAD"))
    }
}
