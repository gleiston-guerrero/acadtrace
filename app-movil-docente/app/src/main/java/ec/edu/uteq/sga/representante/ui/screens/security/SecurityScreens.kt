@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package ec.edu.uteq.sga.representante.ui.screens.security

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ec.edu.uteq.sga.representante.core.SessionManager
import ec.edu.uteq.sga.representante.core.BiometricResult
import ec.edu.uteq.sga.representante.core.BiometricUnlockGate
import ec.edu.uteq.sga.representante.notifications.NotificationSupport

private const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG or
    BiometricManager.Authenticators.BIOMETRIC_WEAK

fun isBiometricAvailable(activity: FragmentActivity): Boolean =
    BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

private fun authenticate(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            if (BiometricUnlockGate.mayEnterHome(BiometricResult.SUCCESS)) onSuccess()
        }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onError(errString.toString())
        override fun onAuthenticationFailed() = onError("No se reconoció la biometría. Intenta nuevamente.")
    })
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Acceder a SGA Representante")
        .setSubtitle("Confirma tu identidad para proteger la sesión guardada")
        .setAllowedAuthenticators(AUTHENTICATORS)
        .setNegativeButtonText("Cancelar")
        .build()
    prompt.authenticate(info)
}

@Composable
fun BiometricUnlockScreen(session: SessionManager, onUnlocked: () -> Unit, onUseLogin: () -> Unit) {
    val activity = LocalContext.current as FragmentActivity
    var error by remember { mutableStateOf<String?>(null) }
    val launch = { authenticate(activity, onUnlocked) { error = it } }
    LaunchedEffect(Unit) { launch() }

    Scaffold(topBar = { TopAppBar(title = { Text("Sesión protegida") }) }) { padding ->
        Column(Modifier.padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Usa la biometría del dispositivo para desbloquear tu sesión válida.")
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = launch) { Text("Desbloquear con biometría") }
            TextButton(onClick = { session.clearSession(); onUseLogin() }) { Text("Usar usuario y contraseña") }
        }
    }
}

@Composable
fun BiometricFallbackScreen(session: SessionManager, onUseLogin: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Biometría no disponible") }) }) { padding ->
        Column(Modifier.padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("La biometría configurada no está disponible. Inicia sesión nuevamente con tus credenciales.")
            Button(onClick = { session.clearSession(); onUseLogin() }) { Text("Ir al inicio de sesión") }
        }
    }
}

@Composable
fun SecurityScreen(session: SessionManager, back: () -> Unit) {
    val activity = LocalContext.current as FragmentActivity
    var biometricEnabled by remember { mutableStateOf(session.isBiometricEnabled()) }
    var notificationsEnabled by remember { mutableStateOf(session.areNotificationsEnabled()) }
    var message by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsEnabled = granted
        session.setNotificationsEnabled(granted)
        if (granted) NotificationSupport.schedule(activity) else NotificationSupport.cancel(activity)
        if (!granted) message = "El permiso de notificaciones no fue concedido."
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Seguridad y notificaciones") }, navigationIcon = {
        TextButton(onClick = back) { Text("Volver") }
    }) }) { padding ->
        Column(Modifier.padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            SettingSwitch("Usar biometría para desbloquear", biometricEnabled) { enabled ->
                if (!enabled) {
                    biometricEnabled = false
                    session.setBiometricEnabled(false)
                } else if (!isBiometricAvailable(activity)) {
                    message = "No hay biometría disponible o registrada en este dispositivo."
                } else {
                    authenticate(activity, {
                        biometricEnabled = true
                        session.setBiometricEnabled(true)
                        message = "Desbloqueo biométrico activado."
                    }) { message = it }
                }
            }
            Text("La biometría solo desbloquea una sesión JWT local que siga vigente.")

            SettingSwitch("Avisarme cuando la sesión expire", notificationsEnabled) { enabled ->
                if (!enabled) {
                    notificationsEnabled = false
                    session.setNotificationsEnabled(false)
                    NotificationSupport.cancel(activity)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationSupport.hasPermission(activity)) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    notificationsEnabled = true
                    session.setNotificationsEnabled(true)
                    NotificationSupport.schedule(activity)
                }
            }
            Text("Son notificaciones locales de seguridad. FCM no está configurado en este proyecto.")
            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
