package ec.edu.uteq.sga.docente.ui.screens.horario

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import ec.edu.uteq.sga.docente.domain.model.HorarioItem
import ec.edu.uteq.sga.docente.ui.components.*
import ec.edu.uteq.sga.docente.ui.theme.*

val DIAS_SEMANA = listOf(
    1 to "Lunes",
    2 to "Martes",
    3 to "Miércoles",
    4 to "Jueves",
    5 to "Viernes"
)

@Composable
fun HorarioScreen(
    viewModel: HorarioViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val slotsFiltrados = state.slots.filter { it.diaSemana == state.selectedDia }

    Scaffold(
        topBar = {
            SgaTopAppBar(
                title = "Mi Horario Semanal",
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

            // Selector de Días
            TabRow(
                selectedTabIndex = state.selectedDia - 1,
                containerColor = CardSurface,
                contentColor = PrimaryNavy
            ) {
                DIAS_SEMANA.forEach { (diaNum, diaNom) ->
                    Tab(
                        selected = state.selectedDia == diaNum,
                        onClick = { viewModel.setDia(diaNum) },
                        text = {
                            Text(
                                text = diaNom.take(3),
                                fontWeight = if (state.selectedDia == diaNum) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            if (state.isLoading && state.slots.isEmpty()) {
                LoadingView("Cargando horario semanal...")
            } else if (!state.errorMessage.isNullOrBlank() && state.slots.isEmpty()) {
                ErrorView(
                    message = state.errorMessage ?: "Error al cargar los horarios del servidor",
                    onRetry = { viewModel.loadHorario() }
                )
            } else if (slotsFiltrados.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.EventBusy,
                    title = "Sin clases programadas",
                    subtitle = "No tienes horas de clase registradas para este día."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(slotsFiltrados) { slot ->
                        SlotHorarioCard(slot = slot)
                    }
                }
            }
        }
    }
}

@Composable
fun SlotHorarioCard(slot: HorarioItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
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
            // Franja de hora
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ModuloHorarioBg
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = slot.horaInicio.take(5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ModuloHorarioText
                    )
                    Text(
                        text = "a",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                    Text(
                        text = slot.horaFin.take(5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ModuloHorarioText
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slot.asignatura,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${slot.grado} • Paralelo \"${slot.paralelo}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryNavy,
                    fontWeight = FontWeight.SemiBold
                )
                if (!slot.aula.isNullOrBlank()) {
                    Text(
                        text = "Aula: ${slot.aula}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
