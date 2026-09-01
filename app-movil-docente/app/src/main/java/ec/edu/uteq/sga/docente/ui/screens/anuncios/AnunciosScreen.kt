package ec.edu.uteq.sga.docente.ui.screens.anuncios

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ec.edu.uteq.sga.docente.domain.model.AnuncioCurso
import ec.edu.uteq.sga.docente.ui.components.*
import ec.edu.uteq.sga.docente.ui.theme.*

@Composable
fun AnunciosScreen(
    idAsignacion: Long,
    viewModel: AnunciosViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(idAsignacion) {
        viewModel.init(idAsignacion)
    }

    Scaffold(
        topBar = {
            SgaTopAppBar(
                title = "Anuncios y Comunicados",
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
                Icon(Icons.Default.Add, contentDescription = "Nuevo Anuncio")
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
                                        selectedContainerColor = ModuloAnunciosBg,
                                        selectedLabelColor = ModuloAnunciosText
                                    )
                                )
                            }
                        }
                    }
                }
            }

            if (state.isLoading && state.anuncios.isEmpty()) {
                LoadingView("Cargando anuncios...")
            } else if (state.anuncios.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Campaign,
                    title = "No hay anuncios publicados",
                    subtitle = "Comparte comunicados oficiales y avisos con este paralelo.",
                    actionButtonText = "Publicar Primer Anuncio",
                    onActionClick = { showCreateDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.anuncios) { anuncio ->
                        AnuncioCard(
                            anuncio = anuncio,
                            onDelete = { viewModel.eliminarAnuncio(anuncio.idAnuncio) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        DialogoNuevoAnuncio(
            onDismiss = { showCreateDialog = false },
            onPublicar = { titulo, contenido, fijado ->
                viewModel.publicarAnuncio(titulo, contenido, fijado)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun AnuncioCard(
    anuncio: AnuncioCurso,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (anuncio.fijado) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ModuloAnunciosBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PushPin, contentDescription = null, tint = ModuloAnunciosText, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fijado", color = ModuloAnunciosText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BackgroundSlate
                    ) {
                        Text(
                            text = "Aviso General",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar", tint = DangerRed)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = anuncio.titulo ?: "Aviso del Curso",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = anuncio.contenido ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 20.sp
            )

            if (!anuncio.fecha.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = SlateBorder.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Publicado: ${anuncio.fecha}",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun DialogoNuevoAnuncio(
    onDismiss: () -> Unit,
    onPublicar: (String, String, Boolean) -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var contenido by remember { mutableStateOf("") }
    var fijado by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Publicar Comunicado", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título del Comunicado") },
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
                    value = contenido,
                    onValueChange = { contenido = it },
                    label = { Text("Contenido / Mensaje") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryNavy
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fijar en la parte superior", fontSize = 13.sp, color = TextPrimary)
                    Switch(checked = fijado, onCheckedChange = { fijado = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titulo.isNotBlank() && contenido.isNotBlank()) {
                        onPublicar(titulo.trim(), contenido.trim(), fijado)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
            ) {
                Text("Publicar", fontWeight = FontWeight.Bold, color = Color.White)
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
