package ec.edu.uteq.sga.docente.ui.screens.seguimiento

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ec.edu.uteq.sga.docente.ui.components.SgaTopAppBar
import ec.edu.uteq.sga.docente.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

val CATEGORIAS_SEGUIMIENTO = listOf(
    "ACADEMICO" to "Académico",
    "CONDUCTUAL" to "Conductual",
    "DECE" to "DECE (Psicología)",
    "MEDICO" to "Médico / Salud",
    "FAMILIAR" to "Familiar",
    "OTRO" to "Otro"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormSeguimientoScreen(
    idMatricula: Long,
    estudianteNombre: String,
    viewModel: SeguimientoViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var fechaEvento by remember { mutableStateOf(sdf.format(Date())) }
    var categoria by remember { mutableStateOf("CONDUCTUAL") }
    var descripcion by remember { mutableStateOf("") }
    var accionesTomadas by remember { mutableStateOf("") }
    var requiereFollowup by remember { mutableStateOf(false) }
    var selectedPeriodoId by remember(state.selectedPeriodo) {
        mutableStateOf(state.selectedPeriodo?.idPeriodo ?: 1L)
    }

    var expandedCat by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val datePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formatted = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            fechaEvento = formatted
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            SgaTopAppBar(
                title = "Registrar Seguimiento",
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
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Estudiante:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = estudianteNombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy
                    )
                    Text(
                        text = "Matrícula #$idMatricula",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            // Categoría
            ExposedDropdownMenuBox(
                expanded = expandedCat,
                onExpandedChange = { expandedCat = !expandedCat }
            ) {
                OutlinedTextField(
                    value = CATEGORIAS_SEGUIMIENTO.find { it.first == categoria }?.second ?: categoria,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría de Observación *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCat) },
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
                    expanded = expandedCat,
                    onDismissRequest = { expandedCat = false }
                ) {
                    CATEGORIAS_SEGUIMIENTO.forEach { (key, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                categoria = key
                                expandedCat = false
                            }
                        )
                    }
                }
            }

            // Fecha
            OutlinedTextField(
                value = fechaEvento,
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha del Suceso *") },
                trailingIcon = {
                    IconButton(onClick = { datePicker.show() }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Seleccionar Fecha", tint = PrimaryNavy)
                    }
                },
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
                label = { Text("Descripción de la observación / hecho *") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PrimaryNavy
                )
            )

            // Acciones tomadas
            OutlinedTextField(
                value = accionesTomadas,
                onValueChange = { accionesTomadas = it },
                label = { Text("Acciones o Acuerdos Tomados") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PrimaryNavy
                )
            )

            // Switch de seguimiento
            Card(
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
                        Text("¿Requiere Seguimiento Especial?", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            text = "Marca si este estudiante necesita monitoreo adicional o reunión con representante / DECE.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = requiereFollowup,
                        onCheckedChange = { requiereFollowup = it }
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
                    if (descripcion.isBlank()) {
                        errorMessage = "La descripción de la observación es obligatoria."
                        return@Button
                    }
                    isSaving = true
                    errorMessage = null

                    viewModel.guardarSeguimiento(
                        idMatricula = idMatricula,
                        idPeriodo = selectedPeriodoId,
                        categoria = categoria,
                        descripcion = descripcion.trim(),
                        accionesTomadas = accionesTomadas.trim().ifBlank { null },
                        requiereFollowup = requiereFollowup,
                        fechaEvento = fechaEvento,
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
                    Text("Registrando...", fontWeight = FontWeight.Bold, color = Color.White)
                } else {
                    Text("Registrar Observación", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
