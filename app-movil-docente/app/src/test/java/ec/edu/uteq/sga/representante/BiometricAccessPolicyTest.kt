package ec.edu.uteq.sga.representante

import ec.edu.uteq.sga.representante.core.BiometricAccessPolicy
import ec.edu.uteq.sga.representante.core.BiometricStartDecision
import org.junit.Assert.assertEquals
import org.junit.Test

class BiometricAccessPolicyTest {
    @Test fun sesionValidaYBiometriaHabilitadaSolicitaDesbloqueo() {
        assertEquals(
            BiometricStartDecision.BIOMETRIC_UNLOCK,
            BiometricAccessPolicy.decide(sessionValid = true, biometricEnabled = true, biometricAvailable = true)
        )
    }

    @Test fun sesionVencidaSiempreVuelveAlLogin() {
        assertEquals(
            BiometricStartDecision.LOGIN,
            BiometricAccessPolicy.decide(sessionValid = false, biometricEnabled = true, biometricAvailable = true)
        )
    }

    @Test fun biometriaDeshabilitadaMantieneSesionValida() {
        assertEquals(
            BiometricStartDecision.HOME,
            BiometricAccessPolicy.decide(sessionValid = true, biometricEnabled = false, biometricAvailable = true)
        )
    }

    @Test fun biometriaNoDisponibleUsaFallbackSeguro() {
        assertEquals(
            BiometricStartDecision.BIOMETRIC_FALLBACK,
            BiometricAccessPolicy.decide(sessionValid = true, biometricEnabled = true, biometricAvailable = false)
        )
    }
}
