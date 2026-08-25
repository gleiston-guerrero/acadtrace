package ec.edu.uteq.sga.docente.ui.screens.seguimiento

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ec.edu.uteq.sga.docente.domain.model.SeguimientoItem
import ec.edu.uteq.sga.docente.ui.components.*
import ec.edu.uteq.sga.docente.ui.theme.*

@Composable
fun SeguimientoScreen(
    idMatricula: Long?,
    viewModel: SeguimientoViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(idMatricula) {
        viewModel.init(idMatricula)
    }

    val itemsFiltrados = if (state.selectedCategoriaFiltro == null) {
        state.items
    } else {
        state.items.filter { it.categoria == state.selectedCategoriaFiltro }
    }

    Scaffold(
        topBar = {
            SgaTopAppBar(
                title = "Seguimiento y Rendimiento",
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
        ) {
            OfflineBanner(isOffline = state.isOffline)

            // Selector de Pestañas: 0 = Observaciones / Bitácora, 1 = Rendimiento y Alertas
            TabRow(
                selectedTabIndex = state.selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryBlue
            ) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.setTab(0) },
                    text = { Text("Bitácora (${state.items.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.setTab(1) },
                    text = { Text("Rendimiento y Alertas", fontWeight = FontWeight.Bold) }
                )
            }

            if (state.selectedTab == 0) {
                // ─── PESTAÑA BITÁCORA / OBSERVACIONES ───────────────────────────
                // Filtros de categoría
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = state.selectedCategoriaFiltro == null,
                            onClick = { viewModel.setCategoriaFiltro(null) },
                            label = { Text("Todas") }
                        )
                    }
                    items(CATEGORIAS_SEGUIMIENTO) { (key, label) ->
                        FilterChip(
                            selected = state.selectedCategoriaFiltro == key,
                            onClick = { viewModel.setCategoriaFiltro(key) },
                            label = { Text(label) }
                        )
                    }
                }

                if (state.isLoading && state.items.isEmpty()) {
                    LoadingView("Cargando bitácora...")
                } else if (itemsFiltrados.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.AssignmentLate,
                        title = "Sin observaciones registradas",
                        subtitle = "No hay registros conductuales o académicos asentados."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(itemsFiltrados) { item ->
                            SeguimientoCard(item = item)
                        }
                    }
                }
            } else {
                // ─── PESTAÑA RENDIMIENTO Y ALERTAS ──────────────────────────────
                // Selector de curso si hay múltiples
                if (state.asignaciones.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.asignaciones) { asig ->
                            FilterChip(
                                selected = state.selectedAsignacion?.idAsignacion == asig.idAsignacion,
                                onClick = { viewModel.selectAsignacion(asig) },
                                label = { Text("${asig.asignaturaNombre} - ${asig.gradoNombre} ${asig.paraleloLetra}") }
                            )
                        }
                    }
                }

                if (state.estudiantesAlerta.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.People,
                        title = "Cargando estudiantes...",
                        subtitle = "Obteniendo métricas de rendimiento y asistencia."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.estudiantesAlerta) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (item.esRiesgoAsistencia) DangerRed.copy(alpha = 0.15f) else AccentGreen.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (item.esRiesgoAsistencia) Icons.Default.Warning else Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (item.esRiesgoAsistencia) DangerRed else AccentGreen,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.estudiante.nombreCompleto,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val pct = item.resumenAsistencia?.porcentajeAsistencia ?: 100.0
                                        Text(
                                            text = "Asistencia: ${String.format("%.1f", pct)}% • Observaciones: ${item.totalObservaciones}",
                                            fontSize = 12.sp,
                                            color = if (pct < 80.0) DangerRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            fontWeight = if (pct < 80.0) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }

                                    if (item.esRiesgoAsistencia) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = DangerRed.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "EN RIESGO",
                                                color = DangerRed,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeguimientoCard(item: SeguimientoItem) {
    val (catColor, catText) = when (item.categoria) {
        "ACADEMICO" -> Pair(PrimaryBlue, "Académico")
        "CONDUCTUAL" -> Pair(DangerRed, "Conductual")
        "DECE" -> Pair(Color(0xFF8B5CF6), "DECE")
        "MEDICO" -> Pair(AccentGreen, "Médico")
        "FAMILIAR" -> Pair(SecondarySky, "Familiar")
        else -> Pair(WarningAmber, item.categoria)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
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
                    color = catColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = catText,
                        color = catColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = item.fechaEvento,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.descripcion,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            if (!item.accionesTomadas.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Acciones Tomadas:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = item.accionesTomadas,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            if (item.requiereFollowup) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PriorityHigh,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Requiere Seguimiento Continuo",
                        color = WarningAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
