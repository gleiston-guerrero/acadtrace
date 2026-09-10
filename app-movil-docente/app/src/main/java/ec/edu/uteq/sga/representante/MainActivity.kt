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
import ec.edu.uteq.sga.representante.notifications.NotificationDestination

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
        val notificationRoute = NotificationDestination.route(intent.extras?.keySet()?.associateWith { intent.extras?.get(it)?.toString().orEmpty() }.orEmpty())
        val effectiveStart = NotificationDestination.startRoute(sessionValid, start, intent.extras?.keySet()?.associateWith { intent.extras?.get(it)?.toString().orEmpty() }.orEmpty())
        setContent { SgaRepresentanteAppTheme { RepresentanteNavGraph(rememberNavController(), app, effectiveStart, notificationRoute) } }
    }
}
