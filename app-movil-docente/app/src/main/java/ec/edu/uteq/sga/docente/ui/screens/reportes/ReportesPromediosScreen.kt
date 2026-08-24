package ec.edu.uteq.sga.docente.ui.screens.reportes

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ec.edu.uteq.sga.docente.ui.components.*
import ec.edu.uteq.sga.docente.ui.theme.*

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
                title = "Promedios y Calificaciones",
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

            // Selector Trimestre / Anual
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column {
                    TabRow(
                        selectedTabIndex = if (state.vistaAnual) 1 else 0,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = PrimaryBlue
                    ) {
                        Tab(
                            selected = !state.vistaAnual,
                            onClick = { viewModel.setVistaAnual(false) },
                            text = { Text("Promedios Trimestrales", fontWeight = FontWeight.SemiBold) }
                        )
                        Tab(
                            selected = state.vistaAnual,
                            onClick = { viewModel.setVistaAnual(true) },
                            text = { Text("Promedio Anual", fontWeight = FontWeight.SemiBold) }
                        )
                    }

                    if (!state.vistaAnual && state.periodos.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
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
            }

            if (state.isLoading && state.items.isEmpty()) {
                LoadingView("Calculando promedios...")
            } else if (state.items.isEmpty()) {
                EmptyStateView(
                    title = "Sin registros de promedios",
                    subtitle = "Ingresa notas en las actividades para calcular promedios."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.items) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (!state.vistaAnual) {
                                        IconButton(
                                            onClick = { viewModel.recalcularPromedioTrimestral(item.estudiante.idMatricula) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Calculate,
                                                contentDescription = "Recalcular",
                                                tint = PrimaryBlue
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

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
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                            Text(
                                                text = "Sumativo (30%): ${String.format("%.2f", trim.notaSumativa)}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
                                                fontSize = 13.sp
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
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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
                                                fontSize = 13.sp
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
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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
