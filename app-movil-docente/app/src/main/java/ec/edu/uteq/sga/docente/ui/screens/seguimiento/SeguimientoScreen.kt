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
import androidx.compose.ui.draw.shadow
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

    val alertasCriticas = remember(state.estudiantesAlerta) {
        state.estudiantesAlerta.filter { it.esRiesgoAsistencia }
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
                .background(BackgroundSlate)
        ) {
            OfflineBanner(isOffline = state.isOffline)

            // Selector de Pestañas: 0 = Observaciones / Bitácora, 1 = Rendimiento y Alertas
            TabRow(
                selectedTabIndex = state.selectedTab,
                containerColor = CardSurface,
                contentColor = PrimaryNavy
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
                Surface(
                    color = CardSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
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
                                label = { Text("Todas", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryNavy.copy(alpha = 0.12f),
                                    selectedLabelColor = PrimaryNavy
                                )
                            )
                        }
                        items(CATEGORIAS_SEGUIMIENTO) { (key, label) ->
                            val isSelected = state.selectedCategoriaFiltro == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setCategoriaFiltro(key) },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryNavy.copy(alpha = 0.12f),
                                    selectedLabelColor = PrimaryNavy
                                )
                            )
                        }
                    }
                }

                if (state.isLoading && state.items.isEmpty()) {
                    LoadingView("Cargando bitácora de seguimiento...")
                } else if (itemsFiltrados.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.AssignmentTurnedIn,
                        title = "No hay observaciones registradas",
                        subtitle = "Crea registros de acompañamiento desde la lista de estudiantes."
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
                if (state.isLoading && state.estudiantesAlerta.isEmpty()) {
                    LoadingView("Analizando alertas y rendimiento...")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Sección: Alertas de Asistencia Crítica (< 80%)
                        item {
                            Text(
                                text = "Alertas de Inasistencias Críticas (< 80%)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DangerRed
                            )
                        }

                        if (alertasCriticas.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = ModuloAsistenciaBg)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "No se registran alumnos con asistencia crítica.",
                                            fontWeight = FontWeight.SemiBold,
                                            color = AccentGreen,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            items(alertasCriticas) { alerta ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = alerta.estudiante.nombreCompleto,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFB91C1C)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Ausencias: ${alerta.resumenAsistencia?.totalAusentes ?: 0} • Atrasos: ${alerta.resumenAsistencia?.totalAtrasos ?: 0}",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = DangerRed
                                        ) {
                                            Text(
                                                text = "${String.format("%.1f", alerta.resumenAsistencia?.porcentajeAsistencia ?: 0.0)}%",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Sección: Estudiantes Registrados en Seguimiento
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nómina de Estudiantes y Seguimiento",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryNavy
                            )
                        }

                        if (state.estudiantesAlerta.isEmpty()) {
                            item {
                                EmptyStateView(
                                    icon = Icons.Default.SearchOff,
                                    title = "Sin casos activos",
                                    subtitle = "Todos los estudiantes se encuentran con seguimiento al día."
                                )
                            }
                        } else {
                            items(state.estudiantesAlerta) { alertaItem ->
                                val est = alertaItem.estudiante
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(1.dp, RoundedCornerShape(12.dp)),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(ModuloSeguimientoBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = est.apellidos.take(1).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                color = ModuloSeguimientoText,
                                                fontSize = 15.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = est.nombreCompleto,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            if (est.cedula.isNotBlank()) {
                                                Text(
                                                    text = "C.I. ${est.cedula}",
                                                    fontSize = 12.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (alertaItem.totalObservaciones > 0) ModuloSeguimientoBg else BackgroundSlate
                                        ) {
                                            Text(
                                                text = if (alertaItem.totalObservaciones > 0) "${alertaItem.totalObservaciones} Observ." else "Al día",
                                                color = if (alertaItem.totalObservaciones > 0) ModuloSeguimientoText else TextMuted,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
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
                    color = ModuloSeguimientoBg
                ) {
                    Text(
                        text = item.categoria,
                        color = ModuloSeguimientoText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (item.requiereFollowup) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = WarningAmber.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Requiere Seguimiento",
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
                text = item.descripcion,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                lineHeight = 20.sp
            )

            if (!item.accionesTomadas.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BackgroundSlate,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Acciones tomadas:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.accionesTomadas,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = SlateBorder.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Fecha: ${item.fechaEvento}",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Text(
                    text = "Matrícula #${item.idMatricula}",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}
