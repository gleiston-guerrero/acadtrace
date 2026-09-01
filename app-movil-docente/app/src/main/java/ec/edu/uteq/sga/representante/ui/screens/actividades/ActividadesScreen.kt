package ec.edu.uteq.sga.representante.ui.screens.actividades

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ec.edu.uteq.sga.representante.domain.model.ActividadAcademica
import ec.edu.uteq.sga.representante.ui.components.*
import ec.edu.uteq.sga.representante.ui.theme.*

@Composable
fun ActividadesScreen(
    idAsignacion: Long,
    viewModel: ActividadesViewModel,
    onBackClick: () -> Unit,
    onCreateActividadClick: () -> Unit,
    onEditActividadClick: (Long) -> Unit,
    onCalificarClick: (Long, String, Double) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var actividadAEliminar by remember { mutableStateOf<ActividadAcademica?>(null) }

    LaunchedEffect(idAsignacion) {
        viewModel.init(idAsignacion)
    }

    Scaffold(
        topBar = {
            SgaTopAppBar(
                title = "Actividades Académicas",
                subtitle = state.selectedAsignacion?.let { "${it.asignaturaNombre} • ${it.gradoNombre} \"${it.paraleloLetra}\"" },
                showBackButton = true,
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateActividadClick,
                containerColor = PrimaryNavy,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Actividad")
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
                                        selectedContainerColor = ModuloActividadesBg,
                                        selectedLabelColor = PrimaryNavy
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ─── SELECTOR DE TRIMESTRE / PERÍODO ──────────────────────────────
            if (state.periodos.isNotEmpty()) {
                Surface(
                    color = CardSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.periodos) { periodo ->
                            val isSelected = state.selectedPeriodo?.idPeriodo == periodo.idPeriodo
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectPeriodo(periodo) },
                                label = { Text(periodo.nombre, fontSize = 12.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryNavy) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryNavy.copy(alpha = 0.12f),
                                    selectedLabelColor = PrimaryNavy
                                )
                            )
                        }
                    }
                }
            }

            // ─── BARRA DE PONDERACIÓN (70% FORMATIVA / 30% SUMATIVA) ───────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = CardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Formativa (Máx 70%)", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                        Text(
                            text = "${String.format("%.1f", state.totalFormativa)}%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (state.totalFormativa <= 70.0) PrimaryNavy else DangerRed
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Sumativa (Máx 30%)", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                        Text(
                            text = "${String.format("%.1f", state.totalSumativa)}%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (state.totalSumativa <= 30.0) AccentGreen else DangerRed
                        )
                    }
                }
            }

            if (state.isLoading && state.actividades.isEmpty()) {
                LoadingView("Cargando actividades académicas...")
            } else if (state.actividades.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.AssignmentLate,
                    title = "No hay actividades registradas",
                    subtitle = "Crea tareas, lecciones o exámenes para este curso y trimestre.",
                    actionButtonText = "Crear Nueva Actividad",
                    onActionClick = onCreateActividadClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.actividades) { actividad ->
                        ActividadCard(
                            actividad = actividad,
                            onCalificar = {
                                onCalificarClick(
                                    actividad.idActividad,
                                    actividad.nombre,
                                    actividad.notaMaxima
                                )
                            },
                            onEdit = { onEditActividadClick(actividad.idActividad) },
                            onDelete = { actividadAEliminar = actividad }
                        )
                    }
                }
            }
        }
    }

    actividadAEliminar?.let { act ->
        AlertDialog(
            onDismissRequest = { actividadAEliminar = null },
            title = { Text("Eliminar Actividad", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("¿Deseas eliminar la actividad '${act.nombre}'?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteActividad(act.idActividad)
                        actividadAEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { actividadAEliminar = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = CardSurface
        )
    }
}

@Composable
fun ActividadCard(
    actividad: ActividadAcademica,
    onCalificar: () -> Unit,
    onEdit: () -> Unit,
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
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (actividad.esSumativa) AccentGreen.copy(alpha = 0.15f) else ModuloActividadesBg
                ) {
                    Text(
                        text = if (actividad.esSumativa) "SUMATIVA (30%)" else "FORMATIVA (70%)",
                        color = if (actividad.esSumativa) AccentGreen else PrimaryNavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (actividad.isPendingSync) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = WarningAmber.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Pendiente Sync",
                            color = WarningAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = actividad.nombre,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            if (!actividad.descripcion.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = actividad.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tipo: ${actividad.tipo.replace("_", " ")}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = "Entrega: ${actividad.fechaEntrega}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ponderación: ${actividad.ponderacion}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryNavy
                )
                Text(
                    text = "Nota Máxima: ${actividad.notaMaxima}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SlateBorder.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = PrimaryNavy)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = DangerRed)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = onCalificar,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Grade, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Calificar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
