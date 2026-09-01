package ec.edu.uteq.sga.representante.core

enum class BiometricStartDecision {
    LOGIN,
    HOME,
    BIOMETRIC_UNLOCK,
    BIOMETRIC_FALLBACK
}

object BiometricAccessPolicy {
    fun decide(sessionValid: Boolean, biometricEnabled: Boolean, biometricAvailable: Boolean): BiometricStartDecision = when {
        !sessionValid -> BiometricStartDecision.LOGIN
        !biometricEnabled -> BiometricStartDecision.HOME
        biometricAvailable -> BiometricStartDecision.BIOMETRIC_UNLOCK
        else -> BiometricStartDecision.BIOMETRIC_FALLBACK
    }
}
