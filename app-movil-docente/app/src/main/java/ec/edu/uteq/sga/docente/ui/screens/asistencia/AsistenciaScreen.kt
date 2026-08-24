package ec.edu.uteq.sga.docente.ui.screens.asistencia

import android.app.DatePickerDialog
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
                title = "Toma de Asistencia",
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
                .background(MaterialTheme.colorScheme.background)
        ) {
            OfflineBanner(isOffline = state.isOffline)

            // Selector de fecha y botón "Marcar Todos Presentes"
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                            tint = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { datePicker.show() }) {
                            Text(
                                text = "Fecha: ${state.selectedFecha}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = PrimaryBlue
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = { viewModel.marcarTodosPresentes() },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = AccentGreen.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Todos P", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            if (state.isLoading && state.items.isEmpty()) {
                LoadingView("Cargando lista de asistencia...")
            } else if (state.items.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.PersonOff,
                    title = "No hay estudiantes matriculados"
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
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (estadoActual != null) {
                    AttendanceBadge(estado = estadoActual)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botones rápidos de marcado
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
