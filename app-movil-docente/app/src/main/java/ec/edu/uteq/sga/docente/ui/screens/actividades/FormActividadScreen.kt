package ec.edu.uteq.sga.docente.ui.screens.actividades

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import ec.edu.uteq.sga.docente.ui.theme.*
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Trimestre / Período
            ExposedDropdownMenuBox(
                expanded = expandedPeriodo,
                onExpandedChange = { expandedPeriodo = !expandedPeriodo }
            ) {
                OutlinedTextField(
                    value = state.periodos.find { it.idPeriodo == selectedPeriodoId }?.nombre ?: "Seleccionar Trimestre",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Período de Evaluación") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPeriodo) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryNavy
                    )
                )
                ExposedDropdownMenu(
                    expanded = expandedPeriodo,
                    onDismissRequest = { expandedPeriodo = false }
                ) {
                    state.periodos.forEach { periodo ->
                        DropdownMenuItem(
                            text = { Text(periodo.nombre) },
                            onClick = {
                                selectedPeriodoId = periodo.idPeriodo
                                expandedPeriodo = false
                            }
                        )
                    }
                }
            }

            // Tipo de Actividad
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
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryNavy
                    )
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
                                expandedTipo = false
                            }
                        )
                    }
                }
            }

            // Nombre
            OutlinedTextField(
                value = nombre,
                onValueChange = {
                    nombre = it
                    errorMessage = null
                },
                label = { Text("Nombre de la Actividad *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PrimaryNavy
                )
            )

            // Descripción
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Instrucciones / Descripción (Opcional)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PrimaryNavy
                )
            )

            // Fecha de Entrega
            OutlinedTextField(
                value = fechaEntrega,
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha de Entrega") },
                trailingIcon = {
                    IconButton(onClick = { datePicker.show() }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Seleccionar fecha", tint = PrimaryNavy)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePicker.show() },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PrimaryNavy
                )
            )

            // Ponderación y Nota Máxima
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = ponderacion,
                    onValueChange = { ponderacion = it },
                    label = { Text("Ponderación (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryNavy
                    )
                )

                OutlinedTextField(
                    value = notaMaxima,
                    onValueChange = { notaMaxima = it },
                    label = { Text("Nota Máxima") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryNavy
                    )
                )
            }

            // Sumativa vs Formativa Switch
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("¿Es Actividad Sumativa?", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            text = if (esSumativa) "Cuenta para el 30% sumativo del trimestre" else "Cuenta para el 70% formativo del trimestre",
                            fontSize = 12.sp,
                            color = TextSecondary
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
                    color = DangerRed,
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryNavy,
                    contentColor = Color.White,
                    disabledContainerColor = PrimaryNavy.copy(alpha = 0.75f),
                    disabledContentColor = Color.White
                ),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardando...", fontWeight = FontWeight.Bold, color = Color.White)
                } else {
                    Text(
                        text = if (idActividad != null) "Guardar Cambios" else "Crear Actividad",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
