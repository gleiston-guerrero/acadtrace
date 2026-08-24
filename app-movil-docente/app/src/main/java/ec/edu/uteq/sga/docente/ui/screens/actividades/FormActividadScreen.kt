package ec.edu.uteq.sga.docente.ui.screens.actividades

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ec.edu.uteq.sga.docente.ui.components.SgaTopAppBar
import ec.edu.uteq.sga.docente.ui.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.util.*

val TIPOS_ACTIVIDAD = listOf(
    "TAREA" to "Tarea",
    "LECCION_ORAL" to "Lección Oral",
    "LECCION_ESCRITA" to "Lección Escrita",
    "TALLER" to "Taller",
    "CUADERNO" to "Cuaderno",
    "TRABAJO_INDIVIDUAL" to "Trabajo Individual",
    "EXPOSICION" to "Exposición",
    "PROYECTO_INTERDISCIPLINARIO" to "Proyecto Interdisciplinario",
    "EXAMEN_TRIMESTRAL" to "Examen Trimestral"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormActividadScreen(
    idAsignacion: Long,
    idActividad: Long?,
    viewModel: ActividadesViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val existingActividad = remember(state.actividades, idActividad) {
        if (idActividad != null) state.actividades.find { it.idActividad == idActividad } else null
    }

    var tipo by remember(existingActividad) { mutableStateOf(existingActividad?.tipo ?: "TAREA") }
    var nombre by remember(existingActividad) { mutableStateOf(existingActividad?.nombre ?: "") }
    var descripcion by remember(existingActividad) { mutableStateOf(existingActividad?.descripcion ?: "") }
    var fechaEntrega by remember(existingActividad) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        mutableStateOf(existingActividad?.fechaEntrega ?: sdf.format(Date()))
    }
    var ponderacion by remember(existingActividad) { mutableStateOf(existingActividad?.ponderacion?.toString() ?: "10") }
    var notaMaxima by remember(existingActividad) { mutableStateOf(existingActividad?.notaMaxima?.toString() ?: "10") }
    var esSumativa by remember(existingActividad) { mutableStateOf(existingActividad?.esSumativa ?: false) }
    var selectedPeriodoId by remember(existingActividad, state.selectedPeriodo) {
        mutableStateOf(existingActividad?.idPeriodo ?: state.selectedPeriodo?.idPeriodo ?: 1L)
    }

    var expandedTipo by remember { mutableStateOf(false) }
    var expandedPeriodo by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val datePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formatted = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            fechaEntrega = formatted
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            SgaTopAppBar(
                title = if (idActividad != null) "Editar Actividad" else "Nueva Actividad",
                showBackButton = true,
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Período
            ExposedDropdownMenuBox(
                expanded = expandedPeriodo,
                onExpandedChange = { expandedPeriodo = !expandedPeriodo }
            ) {
                OutlinedTextField(
                    value = state.periodos.find { it.idPeriodo == selectedPeriodoId }?.nombre ?: "Seleccionar Período",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Período de Evaluación") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPeriodo) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expandedPeriodo,
                    onDismissRequest = { expandedPeriodo = false }
                ) {
                    state.periodos.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.nombre) },
                            onClick = {
                                selectedPeriodoId = p.idPeriodo
                                expandedPeriodo = false
                            }
                        )
                    }
                }
            }

            // Tipo de actividad
            ExposedDropdownMenuBox(
                expanded = expandedTipo,
                onExpandedChange = { expandedTipo = !expandedTipo }
            ) {
                OutlinedTextField(
                    value = TIPOS_ACTIVIDAD.find { it.first == tipo }?.second ?: tipo,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de Actividad") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expandedTipo,
                    onDismissRequest = { expandedTipo = false }
                ) {
                    TIPOS_ACTIVIDAD.forEach { (key, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                tipo = key
                                if (key == "EXAMEN_TRIMESTRAL") esSumativa = true
                                expandedTipo = false
                            }
                        )
                    }
                }
            }

            // Nombre
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre de la Actividad *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Descripción
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción / Indicaciones") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Fecha de Entrega
            OutlinedTextField(
                value = fechaEntrega,
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha de Entrega (YYYY-MM-DD) *") },
                trailingIcon = {
                    IconButton(onClick = { datePicker.show() }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Seleccionar Fecha", tint = PrimaryBlue)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Ponderación
                OutlinedTextField(
                    value = ponderacion,
                    onValueChange = { ponderacion = it },
                    label = { Text("Ponderación (%) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                // Nota Máxima
                OutlinedTextField(
                    value = notaMaxima,
                    onValueChange = { notaMaxima = it },
                    label = { Text("Nota Máxima *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Switch Sumativa
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("¿Es Actividad Sumativa?", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (esSumativa) "Cuenta para el 30% sumativo del trimestre" else "Cuenta para el 70% formativo del trimestre",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = esSumativa,
                        onCheckedChange = { esSumativa = it }
                    )
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (nombre.isBlank()) {
                        errorMessage = "El nombre de la actividad es obligatorio."
                        return@Button
                    }
                    val pond = ponderacion.toDoubleOrNull() ?: 0.0
                    val maxScore = notaMaxima.toDoubleOrNull() ?: 10.0

                    isSaving = true
                    errorMessage = null

                    viewModel.saveActividad(
                        idActividad = idActividad,
                        idAsignacion = idAsignacion,
                        idPeriodo = selectedPeriodoId,
                        tipo = tipo,
                        nombre = nombre.trim(),
                        descripcion = descripcion.trim().ifBlank { null },
                        fechaEntrega = fechaEntrega,
                        ponderacion = pond,
                        notaMaxima = maxScore,
                        esSumativa = esSumativa,
                        onSuccess = {
                            isSaving = false
                            onBackClick()
                        },
                        onError = {
                            isSaving = false
                            errorMessage = it
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = if (idActividad != null) "Guardar Cambios" else "Crear Actividad",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
