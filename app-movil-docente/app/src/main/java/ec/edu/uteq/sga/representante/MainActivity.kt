package ec.edu.uteq.sga.representante

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import ec.edu.uteq.sga.representante.core.BiometricAccessPolicy
import ec.edu.uteq.sga.representante.core.BiometricStartDecision
import ec.edu.uteq.sga.representante.ui.navigation.RepresentanteNavGraph
import ec.edu.uteq.sga.representante.ui.navigation.Screen
import ec.edu.uteq.sga.representante.ui.theme.SgaRepresentanteAppTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as SgaRepresentanteApp
        val sessionValid = app.authRepository.isUserLoggedIn() && app.sessionManager.isRepresentante()
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        val biometricAvailable = BiometricManager.from(this).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
        val start = when (BiometricAccessPolicy.decide(sessionValid, app.sessionManager.isBiometricEnabled(), biometricAvailable)) {
            BiometricStartDecision.LOGIN -> Screen.Login.route
            BiometricStartDecision.HOME -> Screen.Home.route
            BiometricStartDecision.BIOMETRIC_UNLOCK -> Screen.BiometricUnlock.route
            BiometricStartDecision.BIOMETRIC_FALLBACK -> Screen.BiometricFallback.route
        }
        setContent { SgaRepresentanteAppTheme { RepresentanteNavGraph(rememberNavController(), app, start) } }
    }
}
