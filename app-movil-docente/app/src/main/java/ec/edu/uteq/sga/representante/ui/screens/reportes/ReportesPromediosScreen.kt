package ec.edu.uteq.sga.representante.ui.screens.reportes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ec.edu.uteq.sga.representante.ui.components.*
import ec.edu.uteq.sga.representante.ui.theme.*

@Composable
fun ReportesPromediosScreen(
    idAsignacion: Long,
    viewModel: ReportesViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(idAsignacion) {
        viewModel.init(idAsignacion)
    }

    Scaffold(
        topBar = {
            SgaTopAppBar(
                title = "Reportes y Promedios",
                subtitle = state.selectedAsignacion?.let { "${it.asignaturaNombre} • ${it.gradoNombre} \"${it.paraleloLetra}\"" },
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
                                        selectedContainerColor = ModuloReportesBg,
                                        selectedLabelColor = ModuloReportesText
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ─── SELECTOR TRIMESTRE / ANUAL ───────────────────────────────────
            Surface(
                color = CardSurface,
                shadowElevation = 1.dp
            ) {
                Column {
                    TabRow(
                        selectedTabIndex = if (state.vistaAnual) 1 else 0,
                        containerColor = CardSurface,
                        contentColor = PrimaryNavy
                    ) {
                        Tab(
                            selected = !state.vistaAnual,
                            onClick = { viewModel.setVistaAnual(false) },
                            text = { Text("Promedios Trimestrales", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = state.vistaAnual,
                            onClick = { viewModel.setVistaAnual(true) },
                            text = { Text("Promedio Anual", fontWeight = FontWeight.Bold) }
                        )
                    }

                    if (!state.vistaAnual && state.periodos.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
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
            }

            if (state.isLoading && state.items.isEmpty()) {
                LoadingView("Calculando promedios ponderados...")
            } else if (state.items.isEmpty()) {
                EmptyStateView(
                    title = "Sin registros de promedios",
                    subtitle = "Ingresa notas en las actividades para calcular los promedios."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.items) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(1.dp, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.estudiante.nombreCompleto,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (!state.vistaAnual) {
                                        IconButton(
                                            onClick = { viewModel.recalcularPromedioTrimestral(item.estudiante.idMatricula) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Calculate,
                                                contentDescription = "Recalcular",
                                                tint = PrimaryNavy
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                if (!state.vistaAnual) {
                                    val trim = item.promedioTrimestral
                                    if (trim != null) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Formativo (70%): ${String.format("%.2f", trim.promedioFormativo)}",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                            Text(
                                                text = "Sumativo (30%): ${String.format("%.2f", trim.notaSumativa)}",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Nota Trimestral:",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                            GradeBadge(
                                                nota = trim.promedioTrimestral,
                                                notaCualitativa = trim.notaCualitativa
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "Promedio no calculado aún.",
                                            fontSize = 12.sp,
                                            color = TextMuted
                                        )
                                    }
                                } else {
                                    // Vista Anual
                                    val anual = item.promedioAnual
                                    if (anual != null) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Promedio Final Anual:",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                            GradeBadge(
                                                nota = anual.promedioAnual,
                                                notaCualitativa = anual.notaCualitativa
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "Promedio anual no asentado.",
                                            fontSize = 12.sp,
                                            color = TextMuted
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
