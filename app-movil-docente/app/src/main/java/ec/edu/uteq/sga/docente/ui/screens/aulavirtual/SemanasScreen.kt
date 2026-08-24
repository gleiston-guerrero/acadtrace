package ec.edu.uteq.sga.docente.ui.screens.aulavirtual

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ec.edu.uteq.sga.docente.data.remote.dto.SemanaDTO
import ec.edu.uteq.sga.docente.data.remote.dto.TrimestreSemanasDTO
import ec.edu.uteq.sga.docente.ui.components.*
import ec.edu.uteq.sga.docente.ui.theme.*

@Composable
fun SemanasScreen(
    idAsignacion: Long,
    viewModel: AulaVirtualViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(idAsignacion) {
        viewModel.loadAgenda(idAsignacion)
    }

    Scaffold(
        topBar = {
            SgaTopAppBar(
                title = "Aula Virtual por Semanas",
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
            if (state.isLoading) {
                LoadingView("Cargando agenda académica...")
            } else if (state.errorMessage != null) {
                ErrorView(
                    message = state.errorMessage!!,
                    onRetry = { viewModel.loadAgenda(idAsignacion) }
                )
            } else if (state.agenda?.trimestres.isNullOrEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.DateRange,
                    title = "Sin contenido en el aula virtual",
                    subtitle = "No hay semanas configuradas para este curso."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    state.agenda!!.trimestres.forEach { trimestre ->
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PrimaryBlue,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.School, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = trimestre.trimestre,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        items(trimestre.semanas) { semana ->
                            SemanaCard(semana = semana)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SemanaCard(semana: SemanaDTO) {
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
                Text(
                    text = "Semana ${semana.numero}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
                Text(
                    text = "${semana.fechaInicio} al ${semana.fechaFin}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Resumen de elementos de la semana
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ContenidoChip(
                    count = semana.actividades.size,
                    label = "Actividades",
                    icon = Icons.Default.Assignment,
                    color = PrimaryBlue
                )
                ContenidoChip(
                    count = semana.materiales.size,
                    label = "Recursos",
                    icon = Icons.Default.Folder,
                    color = SecondarySky
                )
                ContenidoChip(
                    count = semana.anuncios.size,
                    label = "Avisos",
                    icon = Icons.Default.Campaign,
                    color = WarningAmber
                )
            }

            if (semana.actividades.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Actividades planificadas:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                semana.actividades.forEach { act ->
                    Text(
                        text = "• ${act.nombre} (${act.tipo})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun ContenidoChip(
    count: Int,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$count $label",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}
