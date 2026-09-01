package ec.edu.uteq.sga.representante.ui.screens.asistencia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ec.edu.uteq.sga.representante.ui.components.*
import ec.edu.uteq.sga.representante.ui.theme.*

@Composable
fun ResumenAsistenciaScreen(
    idAsignacion: Long,
    viewModel: AsistenciaViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(idAsignacion) {
        viewModel.loadResumenAsistencias(idAsignacion)
    }

    Scaffold(
        topBar = {
            SgaTopAppBar(
                title = "Resumen de Asistencia",
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

            if (state.resumenes.isEmpty()) {
                EmptyStateView(
                    title = "No hay registros estadísticos",
                    subtitle = "Toma asistencia en varios días para ver los resúmenes acumulados."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.resumenes) { res ->
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
                                        text = "Matrícula #${res.idMatricula}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (res.porcentajeAsistencia >= 75.0) ModuloAsistenciaBg else DangerRed.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "${String.format("%.1f", res.porcentajeAsistencia)}%",
                                            fontWeight = FontWeight.Bold,
                                            color = if (res.porcentajeAsistencia >= 75.0) AccentGreen else DangerRed,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = SlateBorder.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Presentes: ${res.totalPresentes}", color = ChipPresente, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Ausentes: ${res.totalAusentes}", color = ChipAusente, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Justificados: ${res.totalJustificados}", color = ChipJustificado, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Atrasos: ${res.totalAtrasos}", color = ChipAtraso, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
