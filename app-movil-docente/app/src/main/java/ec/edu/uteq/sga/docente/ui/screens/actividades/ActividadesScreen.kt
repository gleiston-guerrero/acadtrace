package ec.edu.uteq.sga.docente.ui.screens.actividades

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ec.edu.uteq.sga.docente.domain.model.ActividadAcademica
import ec.edu.uteq.sga.docente.ui.components.*
import ec.edu.uteq.sga.docente.ui.theme.*

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
                showBackButton = true,
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateActividadClick,
                containerColor = PrimaryBlue,
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
                .background(MaterialTheme.colorScheme.background)
        ) {
            OfflineBanner(isOffline = state.isOffline)

            // Selector de Período / Trimestre
            if (state.periodos.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.periodos) { periodo ->
                            FilterChip(
                                selected = state.selectedPeriodo?.idPeriodo == periodo.idPeriodo,
                                onClick = { viewModel.selectPeriodo(periodo) },
                                label = { Text(periodo.nombre) },
                                leadingIcon = if (state.selectedPeriodo?.idPeriodo == periodo.idPeriodo) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            // Barra de progreso de ponderación (70% formativa / 30% sumativa)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Formativa (Máx 70%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(
                            text = "${String.format("%.1f", state.totalFormativa)}%",
                            fontWeight = FontWeight.Bold,
                            color = if (state.totalFormativa <= 70.0) PrimaryBlue else DangerRed
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Sumativa (Máx 30%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(
                            text = "${String.format("%.1f", state.totalSumativa)}%",
                            fontWeight = FontWeight.Bold,
                            color = if (state.totalSumativa <= 30.0) AccentGreen else DangerRed
                        )
                    }
                }
            }

            if (state.isLoading && state.actividades.isEmpty()) {
                LoadingView("Cargando actividades...")
            } else if (state.actividades.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.AssignmentLate,
                    title = "No hay actividades registradas",
                    subtitle = "Crea tareas, lecciones o exámenes para este trimestre.",
                    actionButtonText = "Crear Primera Actividad",
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
            title = { Text("Eliminar Actividad") },
            text = { Text("¿Deseas eliminar la actividad '${act.nombre}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteActividad(act.idActividad)
                        actividadAEliminar = null
                    }
                ) {
                    Text("Eliminar", color = DangerRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { actividadAEliminar = null }) {
                    Text("Cancelar")
                }
            }
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (actividad.esSumativa) AccentGreen.copy(alpha = 0.15f) else PrimaryBlue.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (actividad.esSumativa) "SUMATIVA" else "FORMATIVA",
                        color = if (actividad.esSumativa) AccentGreen else PrimaryBlue,
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

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = actividad.nombre,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (!actividad.descripcion.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = actividad.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "Entrega: ${actividad.fechaEntrega}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
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
                    color = PrimaryBlue
                )
                Text(
                    text = "Nota Máx: ${actividad.notaMaxima}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = PrimaryBlue)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = DangerRed)
                }
                Button(
                    onClick = onCalificar,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Grade, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Calificar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
