package ec.edu.uteq.sga.docente.ui.screens.asistencia

import android.app.DatePickerDialog
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ec.edu.uteq.sga.docente.ui.components.*
import ec.edu.uteq.sga.docente.ui.theme.*
import java.util.*

@Composable
fun AsistenciaScreen(
    idAsignacion: Long,
    viewModel: AsistenciaViewModel,
    onBackClick: () -> Unit,
    onVerResumenClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(idAsignacion) {
        viewModel.init(idAsignacion)
    }

    val calendar = Calendar.getInstance()
    val datePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formatted = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            viewModel.changeFecha(formatted)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            SgaTopAppBar(
                title = "Control de Asistencia",
                subtitle = state.selectedAsignacion?.let { "${it.asignaturaNombre} • ${it.gradoNombre} \"${it.paraleloLetra}\"" },
                showBackButton = true,
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = onVerResumenClick) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Ver Resumen",
                            tint = Color.White
                        )
                    }
                }
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
                                        selectedContainerColor = ModuloAsistenciaBg,
                                        selectedLabelColor = ModuloAsistenciaText
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ─── BARRA DE FECHA Y MARCADO RÁPIDO ──────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardSurface,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = null,
                            tint = PrimaryNavy,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(
                            onClick = { datePicker.show() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Fecha: ${state.selectedFecha}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = PrimaryNavy
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = { viewModel.marcarTodosPresentes() },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ModuloAsistenciaBg,
                            contentColor = ModuloAsistenciaText
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = ModuloAsistenciaText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Todos Presentes", color = ModuloAsistenciaText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            if (state.isLoading && state.items.isEmpty()) {
                LoadingView("Cargando lista de asistencia...")
            } else if (state.items.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.PersonOff,
                    title = "No hay estudiantes matriculados",
                    subtitle = "Selecciona otra asignatura o sincroniza los datos."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.items) { item ->
                        ItemAsistenciaEstudiante(
                            item = item,
                            onSelectEstado = { nuevoEstado ->
                                viewModel.setEstadoAsistencia(item.estudiante.idMatricula, nuevoEstado)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ItemAsistenciaEstudiante(
    item: EstudianteAsistenciaItem,
    onSelectEstado: (String) -> Unit
) {
    val estadoActual = item.asistencia?.estado

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ModuloAsistenciaBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.estudiante.apellidos.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = PrimaryNavy,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.estudiante.nombreCompleto,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                        if (item.estudiante.cedula.isNotBlank()) {
                            Text(
                                text = "C.I. ${item.estudiante.cedula}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                if (estadoActual != null) {
                    AttendanceBadge(estado = estadoActual)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botones de marcado rápido (P, A, J, At)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EstadoBoton(
                    texto = "P",
                    label = "Presente",
                    color = ChipPresente,
                    isSelected = estadoActual == "PRESENTE",
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectEstado("PRESENTE") }
                )
                EstadoBoton(
                    texto = "A",
                    label = "Ausente",
                    color = ChipAusente,
                    isSelected = estadoActual == "AUSENTE",
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectEstado("AUSENTE") }
                )
                EstadoBoton(
                    texto = "J",
                    label = "Justif.",
                    color = ChipJustificado,
                    isSelected = estadoActual == "JUSTIFICADO",
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectEstado("JUSTIFICADO") }
                )
                EstadoBoton(
                    texto = "At",
                    label = "Atraso",
                    color = ChipAtraso,
                    isSelected = estadoActual == "ATRASO",
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectEstado("ATRASO") }
                )
            }
        }
    }
}

@Composable
fun EstadoBoton(
    texto: String,
    label: String,
    color: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) color else color.copy(alpha = 0.08f),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = texto,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isSelected) Color.White else color
            )
            Text(
                text = label,
                fontSize = 9.sp,
                color = if (isSelected) Color.White else color
            )
        }
    }
}
