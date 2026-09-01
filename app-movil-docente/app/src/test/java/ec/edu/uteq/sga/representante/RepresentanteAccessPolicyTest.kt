package ec.edu.uteq.sga.representante

import ec.edu.uteq.sga.representante.data.repository.RepresentanteAccessPolicy
import org.junit.Assert.*
import org.junit.Test

class RepresentanteAccessPolicyTest {
    @Test fun loginAceptaRepresentante() {
        assertTrue(RepresentanteAccessPolicy.isAllowed(listOf("REPRESENTANTE")))
        assertTrue(RepresentanteAccessPolicy.isAllowed(listOf("DIRECTOR", "REPRESENTANTE")))
        assertFalse(RepresentanteAccessPolicy.isAllowed(emptyList()))
    }
    @Test fun loginRechazaDocenteYOtrosRoles() {
        assertFalse(RepresentanteAccessPolicy.isAllowed(listOf("DOCENTE")))
        assertFalse(RepresentanteAccessPolicy.isAllowed(listOf("SECRETARIA")))
        assertFalse(RepresentanteAccessPolicy.isAllowed(listOf("ROLE_REPRESENTANTE")))
        assertFalse(RepresentanteAccessPolicy.isAllowed(listOf("SUPER_REPRESENTANTE")))
        assertFalse(RepresentanteAccessPolicy.isAllowed(listOf("representante")))
    }
}
