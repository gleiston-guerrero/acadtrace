package ec.edu.uteq.sga.docente.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ec.edu.uteq.sga.docente.core.Constants
import ec.edu.uteq.sga.docente.core.SessionManager
import ec.edu.uteq.sga.docente.ui.components.SgaTopAppBar
import ec.edu.uteq.sga.docente.ui.theme.AccentGreen
import ec.edu.uteq.sga.docente.ui.theme.PrimaryBlue

@Composable
fun SettingsScreen(
    sessionManager: SessionManager,
    onBackClick: () -> Unit
) {
    var gatewayUrl by remember { mutableStateOf(sessionManager.getGatewayUrl()) }
    var docenteUrl by remember { mutableStateOf(sessionManager.getDocenteUrl()) }
    var savedMessage by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SgaTopAppBar(
                title = "Configuración de Servidor",
                showBackButton = true,
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Direcciones de Red de la API",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Configura la IP o dominio del backend distribuido para pruebas en emulador Android o dispositivo físico en red local.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 13.sp
            )

            // Presets rápidos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = gatewayUrl.contains("10.0.2.2"),
                    onClick = {
                        gatewayUrl = "http://10.0.2.2:8080/api/"
                        docenteUrl = "http://10.0.2.2:8081/api/"
                    },
                    label = { Text("Emulador (10.0.2.2)") }
                )
                FilterChip(
                    selected = gatewayUrl.contains("localhost") || gatewayUrl.contains("127.0.0.1"),
                    onClick = {
                        gatewayUrl = "http://localhost:8080/api/"
                        docenteUrl = "http://localhost:8081/api/"
                    },
                    label = { Text("Localhost") }
                )
            }

            OutlinedTextField(
                value = gatewayUrl,
                onValueChange = {
                    gatewayUrl = it
                    savedMessage = false
                },
                label = { Text("URL SGA Principal / Gateway") },
                leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = docenteUrl,
                onValueChange = {
                    docenteUrl = it
                    savedMessage = false
                },
                label = { Text("URL Microservicio Docente") },
                leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (savedMessage) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AccentGreen.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Configuración guardada correctamente.",
                        color = AccentGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Button(
                onClick = {
                    sessionManager.setGatewayUrl(gatewayUrl.trim())
                    sessionManager.setDocenteUrl(docenteUrl.trim())
                    savedMessage = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardar Cambios", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Información de la sesión
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Información de Aplicación", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Versión: 1.0.0 (Módulo Docente)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text("Base de Datos Local: ${Constants.DATABASE_NAME}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    if (sessionManager.getUsername() != null) {
                        Text("Usuario Conectado: ${sessionManager.getUsername()}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}
