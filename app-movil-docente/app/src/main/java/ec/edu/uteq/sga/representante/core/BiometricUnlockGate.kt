package ec.edu.uteq.sga.representante.core

enum class BiometricResult { SUCCESS, FAILED, CANCELLED }

object BiometricUnlockGate {
    fun mayEnterHome(result: BiometricResult): Boolean = result == BiometricResult.SUCCESS
}
