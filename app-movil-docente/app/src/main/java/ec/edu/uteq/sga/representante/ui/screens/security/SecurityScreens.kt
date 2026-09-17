@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package ec.edu.uteq.sga.representante.ui.screens.security

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        topBar = { TopAppBar(title = { Text("Seguridad", fontWeight = FontWeight.SemiBold) }, navigationIcon = {
            IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Volver") }
        }) }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(horizontal = 16.dp, vertical = 14.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Protección y alertas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Administre cómo protege su acceso y recibe avisos.", color = MaterialTheme.colorScheme.onSurfaceVariant)

            SecuritySection("Biometría", Icons.Default.Fingerprint, MaterialTheme.colorScheme.primary) {
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
                Text("La biometría solo desbloquea una sesión JWT local que siga vigente.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            SecuritySection("Sesión", Icons.Default.Security, Color(0xFF0D9488)) {
                Text("Su acceso permanece protegido mientras la sesión local siga vigente.", style = MaterialTheme.typography.bodyMedium)
            }

            SecuritySection("Notificaciones", Icons.Default.Notifications, Color(0xFFD97706)) {
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
                Text("Las alertas de expiración son locales. Las notificaciones académicas push se reciben mediante Firebase Cloud Messaging (FCM).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            message?.let {
                Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SecuritySection(title: String, icon: ImageVector, accent: Color, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = accent.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent, modifier = Modifier.size(23.dp)) }
                }
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            content()
        }
    }
}
