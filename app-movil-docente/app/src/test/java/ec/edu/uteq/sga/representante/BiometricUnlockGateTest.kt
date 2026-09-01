package ec.edu.uteq.sga.representante

import ec.edu.uteq.sga.representante.core.BiometricResult
import ec.edu.uteq.sga.representante.core.BiometricUnlockGate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricUnlockGateTest {
    @Test fun exitoBiometricoPermiteHome() =
        assertTrue(BiometricUnlockGate.mayEnterHome(BiometricResult.SUCCESS))

    @Test fun falloBiometricoNoPermiteHome() =
        assertFalse(BiometricUnlockGate.mayEnterHome(BiometricResult.FAILED))

    @Test fun cancelacionBiometricaNoPermiteHome() =
        assertFalse(BiometricUnlockGate.mayEnterHome(BiometricResult.CANCELLED))
}
