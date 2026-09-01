package ec.edu.uteq.sga.representante.ui.screens.calificaciones

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ec.edu.uteq.sga.representante.domain.model.Estudiante
import ec.edu.uteq.sga.representante.domain.rules.AcademicRules
import ec.edu.uteq.sga.representante.ui.components.*
import ec.edu.uteq.sga.representante.ui.theme.*

@Composable
fun CalificacionesScreen(
    idActividad: Long,
    idAsignacion: Long,
    actividadNombre: String,
    notaMaxima: Double,
    viewModel: CalificacionesViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var estudianteAEditar by remember { mutableStateOf<Pair<Estudiante, Double?>?>(null) }

    LaunchedEffect(idActividad, idAsignacion) {
        viewModel.init(idActividad, idAsignacion, actividadNombre, notaMaxima)
    }

    Scaffold(
        topBar = {
            SgaTopAppBar(
                title = "Registro de Calificaciones",
                subtitle = "$actividadNombre • Nota Máx: ${notaMaxima} pts",
                showBackButton = true,
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundSlate)
        ) {
            OfflineBanner(isOffline = state.isOffline)

            // Header de la Actividad
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardSurface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = actividadNombre,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Nota Máxima: $notaMaxima pts",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AccentGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Promedio: ${String.format("%.2f", state.promedioActividad)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (state.isLoading && state.items.isEmpty()) {
                LoadingView("Cargando lista de estudiantes...")
            } else if (state.items.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Grade,
                    title = "No hay estudiantes en este curso",
                    subtitle = "Verifica las asignaciones o contacta al administrador."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.items) { item ->
                        CalificacionItemCard(
                            item = item,
                            onClick = {
                                estudianteAEditar = Pair(item.estudiante, item.calificacion?.nota)
                            }
                        )
                    }
                }
            }
        }
    }

    estudianteAEditar?.let { (est, notaActual) ->
        DialogoIngresarNota(
            estudiante = est,
            notaActual = notaActual,
            notaMaxima = notaMaxima,
            onDismiss = { estudianteAEditar = null },
            onGuardar = { nota, obs ->
                viewModel.guardarNota(
                    idMatricula = est.idMatricula,
                    nota = nota,
                    observacion = obs,
                    onSuccess = { estudianteAEditar = null },
                    onError = { }
                )
            }
        )
    }
}

@Composable
fun CalificacionItemCard(
    item: EstudianteCalificacionItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(1.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ModuloCalificacionesBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.estudiante.apellidos.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = ModuloCalificacionesText,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = item.estudiante.nombreCompleto,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (item.calificacion?.isPendingSync == true) {
                        Text(
                            text = "Pendiente de sincronizar",
                            fontSize = 11.sp,
                            color = WarningAmber,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (item.calificacion != null) {
                GradeBadge(
                    nota = item.calificacion.nota,
                    notaCualitativa = item.calificacion.notaCualitativa
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BackgroundSlate,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                ) {
                    Text(
                        text = "Sin Calificar",
                        fontSize = 12.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DialogoIngresarNota(
    estudiante: Estudiante,
    notaActual: Double?,
    notaMaxima: Double,
    onDismiss: () -> Unit,
    onGuardar: (Double, String?) -> Unit
) {
    var notaTexto by remember { mutableStateOf(notaActual?.toString() ?: "") }
    var observacion by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val notaNum = notaTexto.toDoubleOrNull()
    val cualitativaPreview = if (notaNum != null && notaNum >= 0.0 && notaNum <= notaMaxima) {
        AcademicRules.getEtiquetaCualitativa(AcademicRules.convertirNotaCualitativa(notaNum))
    } else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = estudiante.nombreCompleto,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Asentar Calificación (Máximo: $notaMaxima puntos)",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = notaTexto,
                    onValueChange = {
                        notaTexto = it
                        error = null
                    },
                    label = { Text("Nota Cuantitativa") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryNavy
                    )
                )

                if (cualitativaPreview != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AccentGreen.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Escala: $cualitativaPreview",
                            color = AccentGreen,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = observacion,
                    onValueChange = { observacion = it },
                    label = { Text("Observación (Opcional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryNavy
                    )
                )

                if (error != null) {
                    Text(text = error!!, color = DangerRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val valor = notaTexto.toDoubleOrNull()
                    if (valor == null || valor < 0.0 || valor > notaMaxima) {
                        error = "Ingresa una nota válida entre 0 y $notaMaxima"
                        return@Button
                    }
                    onGuardar(valor, observacion.trim().ifBlank { null })
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
            ) {
                Text("Guardar Nota", fontWeight = FontWeight.Bold, color = Color.White)
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
