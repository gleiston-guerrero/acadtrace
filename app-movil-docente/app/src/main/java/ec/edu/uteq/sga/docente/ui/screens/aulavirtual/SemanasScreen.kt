package ec.edu.uteq.sga.docente.ui.screens.aulavirtual

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import ec.edu.uteq.sga.docente.data.remote.dto.SemanaDTO
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
        viewModel.init(idAsignacion)
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
                .background(BackgroundSlate)
        ) {
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
                                        selectedContainerColor = ModuloAulaVirtualBg,
                                        selectedLabelColor = ModuloAulaVirtualText
                                    )
                                )
                            }
                        }
                    }
                }
            }

            if (state.isLoading) {
                LoadingView("Cargando agenda académica por semanas...")
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
                                color = PrimaryNavy,
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
                Text(
                    text = "Semana ${semana.numero}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryNavy
                )
                Text(
                    text = "${semana.fechaInicio} al ${semana.fechaFin}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
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
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    bgColor = ModuloActividadesBg,
                    color = ModuloActividadesText
                )
                ContenidoChip(
                    count = semana.materiales.size,
                    label = "Recursos",
                    icon = Icons.Default.Folder,
                    bgColor = ModuloMaterialBg,
                    color = ModuloMaterialText
                )
                ContenidoChip(
                    count = semana.anuncios.size,
                    label = "Avisos",
                    icon = Icons.Default.Campaign,
                    bgColor = ModuloAnunciosBg,
                    color = ModuloAnunciosText
                )
            }

            if (semana.actividades.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = SlateBorder.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Actividades planificadas:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                semana.actividades.forEach { act ->
                    Text(
                        text = "• ${act.nombre} (${act.tipo})",
                        fontSize = 12.sp,
                        color = TextSecondary
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
    bgColor: Color,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
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
