package ec.edu.uteq.sga.docente.ui.screens.materiales

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ec.edu.uteq.sga.docente.domain.model.MaterialCurso
import ec.edu.uteq.sga.docente.ui.components.*
import ec.edu.uteq.sga.docente.ui.theme.*

@Composable
fun MaterialesScreen(
    idAsignacion: Long,
    viewModel: MaterialesViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(idAsignacion) {
        viewModel.loadMateriales(idAsignacion)
    }

    Scaffold(
        topBar = {
            SgaTopAppBar(
                title = "Materiales de Estudio",
                showBackButton = true,
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Subir Material")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            OfflineBanner(isOffline = state.isOffline)

            if (state.isLoading && state.materiales.isEmpty()) {
                LoadingView("Cargando materiales...")
            } else if (state.materiales.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.FolderOpen,
                    title = "No hay materiales subidos",
                    subtitle = "Comparte enlaces, guías de estudio o documentos con el curso.",
                    actionButtonText = "Agregar Primer Material",
                    onActionClick = { showCreateDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.materiales) { material ->
                        MaterialCard(
                            material = material,
                            onOpen = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(material.url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Error abriendo enlace
                                }
                            },
                            onDelete = { viewModel.eliminarMaterial(material.idMaterial) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        DialogoNuevoMaterial(
            onDismiss = { showCreateDialog = false },
            onGuardar = { titulo, descripcion, tipo, url ->
                viewModel.subirMaterial(titulo, descripcion, tipo, url)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun MaterialCard(
    material: MaterialCurso,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SecondarySky.copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = SecondarySky,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = material.titulo ?: "Recurso de Estudio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!material.descripcion.isNullOrBlank()) {
                    Text(
                        text = material.descripcion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = material.url,
                    fontSize = 11.sp,
                    color = PrimaryBlue,
                    maxLines = 1
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar", tint = DangerRed)
            }
        }
    }
}

@Composable
fun DialogoNuevoMaterial(
    onDismiss: () -> Unit,
    onGuardar: (String, String?, String, String) -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("ENLACE") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Recurso o Guía", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título del Documento *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL o Enlace *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (Opcional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titulo.isNotBlank() && url.isNotBlank()) {
                        onGuardar(titulo.trim(), descripcion.trim().ifBlank { null }, tipo, url.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
