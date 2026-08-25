package ec.edu.uteq.sga.docente.ui.screens.materiales

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
        viewModel.init(idAsignacion)
    }

    Scaffold(
        topBar = {
            SgaTopAppBar(
                title = "Materiales de Estudio",
                subtitle = state.selectedAsignacion?.let { "${it.asignaturaNombre} • ${it.gradoNombre} \"${it.paraleloLetra}\"" },
                showBackButton = true,
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = PrimaryNavy,
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
                .background(BackgroundSlate)
        ) {
            OfflineBanner(isOffline = state.isOffline)

            // ─── SELECTOR RÁPIDO DE CURSO ─────────────────────────────────────
            if (state.asignaciones.isNotEmpty()) {
                Surface(
                    color = CardSurface,
                    shadowElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                        Text(
                            text = "Curso seleccionado:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.asignaciones) { asig ->
                                val isSelected = state.selectedAsignacion?.idAsignacion == asig.idAsignacion
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectAsignacion(asig) },
                                    label = {
                                        Text(
                                            text = "${asig.asignaturaNombre} (${asig.gradoNombre} ${asig.paraleloLetra})",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ModuloMaterialBg,
                                        selectedLabelColor = ModuloMaterialText
                                    )
                                )
                            }
                        }
                    }
                }
            }

            if (state.isLoading && state.materiales.isEmpty()) {
                LoadingView("Cargando materiales...")
            } else if (state.materiales.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.FolderOpen,
                    title = "No hay materiales subidos",
                    subtitle = "Comparte enlaces, guías de estudio o documentos con este paralelo.",
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
            .clickable(onClick = onOpen)
            .shadow(2.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ModuloMaterialBg
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = ModuloMaterialText,
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
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (!material.descripcion.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = material.descripcion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = material.url,
                    fontSize = 11.sp,
                    color = PrimaryNavy,
                    maxLines = 1,
                    fontWeight = FontWeight.Medium
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
        title = { Text("Agregar Recurso o Guía", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título del Documento *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryNavy
                    )
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL o Enlace *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryNavy
                    )
                )

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (Opcional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryNavy
                    )
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
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        },
        containerColor = CardSurface
    )
}
