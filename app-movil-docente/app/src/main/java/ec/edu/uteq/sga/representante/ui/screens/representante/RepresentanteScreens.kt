@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package ec.edu.uteq.sga.representante.ui.screens.representante

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ec.edu.uteq.sga.representante.domain.model.*
import ec.edu.uteq.sga.representante.ui.components.OfflineBanner
import ec.edu.uteq.sga.representante.notifications.AttendanceNotificationContext

@Composable fun HomeRepresentante(onRepresentados: () -> Unit, onComunicados: () -> Unit, onSecurity: () -> Unit, onLogout: () -> Unit) = Scaffold(
    topBar = { TopAppBar(title = { Text("Portal del Representante") }, actions = { IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "Cerrar sesión") } }) }
) { padding -> Column(Modifier.padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text("Bienvenido", style = MaterialTheme.typography.headlineMedium)
    OptionCard("Mis representados", "Consulte la información académica de sus hijos", Icons.Default.People, onRepresentados)
    OptionCard("Comunicados", "Avisos institucionales para representantes", Icons.Default.Campaign, onComunicados)
    OptionCard("Seguridad", "Biometría y notificaciones locales", Icons.Default.Security, onSecurity)
    Text("Calificaciones y asistencia se consultan desde cada representado.")
} }

@Composable fun ComunicadosRepresentanteScreen(vm: RepresentanteViewModel, back: () -> Unit) {
    val state by vm.comunicados.collectAsState()
    LaunchedEffect(Unit) { vm.cargarComunicados() }
    Page("Comunicados", back) { StateContent(state, vm::cargarComunicados) { items ->
        if (items.isEmpty()) Text("No existen comunicados disponibles") else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.id }) { item -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) {
                Text(item.titulo, fontWeight = FontWeight.Bold)
                Text(item.contenido)
                Text(item.fecha, style = MaterialTheme.typography.bodySmall)
            } } }
        }
    } }
}

@Composable fun MisRepresentadosScreen(vm: RepresentanteViewModel, back: () -> Unit, select: (Representado) -> Unit) {
    val state by vm.representados.collectAsState()
    Page("Mis representados", back) { StateContent(state, vm::cargarRepresentados) { items ->
        if (items.isEmpty()) Text("No hay representados asociados") else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items) { item -> Card(Modifier.fillMaxWidth().clickable { select(item) }) { Column(Modifier.padding(16.dp)) {
                Text(item.nombreCompleto, fontWeight = FontWeight.Bold); Text(listOfNotNull(item.curso, item.paralelo).joinToString(" · "))
            } } }
        }
    } }
}

@Composable fun ResumenRepresentadoScreen(nombre: String, back: () -> Unit, notas: () -> Unit, asistencia: () -> Unit) = Page(nombre, back) {
    Text("Resumen del representado", style = MaterialTheme.typography.headlineSmall)
    OptionCard("Calificaciones", "Periodos, actividades y promedios", Icons.Default.Grade, notas)
    OptionCard("Asistencia", "Registros y resumen de asistencia", Icons.Default.EventAvailable, asistencia)
}

@Composable fun ConsultaCalificacionesRepresentante(vm: RepresentanteViewModel, back: () -> Unit, retry: () -> Unit) {
    val state by vm.calificaciones.collectAsState()
    val selected by vm.periodoSeleccionado.collectAsState()
    Page("Calificaciones", back) { StateContent(state, retry) { data ->
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(data.periodos, key = { it.idPeriodo }) { p -> FilterChip(
                selected = selected == p.idPeriodo, onClick = { vm.seleccionarPeriodo(p.idPeriodo) }, label = { Text(p.nombre) }) }
        }
        val blocks = selected?.let(data::asignaturas).orEmpty()
        if (blocks.isEmpty()) Text("No existen calificaciones para este período")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(blocks, key = { it.idAsignacion }) { block -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(block.asignatura, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(block.promedio?.periodo ?: block.actividades.firstOrNull()?.periodo.orEmpty(), style = MaterialTheme.typography.bodySmall)
                block.promedio?.let { p ->
                    Text("Formativo: ${p.promedioFormativo} · Sumativo: ${p.notaSumativa}")
                    Text("Promedio: ${p.promedioTrimestral} (${p.notaCualitativa.etiquetaCualitativa()})")
                }
                block.actividades.forEach { n -> ListItem(
                    headlineContent = { Text(n.actividad) },
                    trailingContent = { Text(n.nota?.toString() ?: "Pendiente de calificación", fontWeight = FontWeight.Bold) }) }
            } } }
            if (data.mostrarPromediosAnuales && data.promediosAnuales.isNotEmpty()) {
                item { Text("Promedios finales", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                items(data.promediosAnuales, key = { it.idAsignacion }) { p -> ListItem(
                    headlineContent = { Text(p.asignatura) },
                    trailingContent = { Text("${p.promedioAnual} (${p.notaCualitativa.etiquetaCualitativa()})", fontWeight = FontWeight.Bold) }) }
            }
        }
    } }
}

@Composable fun AsistenciaHijoScreen(vm: RepresentanteViewModel, back: () -> Unit, notificationContext: AttendanceNotificationContext? = null, retry: () -> Unit) {
    val state by vm.asistencia.collectAsState()
    Page("Asistencia", back) {
        notificationContext?.let { AttendanceNotificationCard(it) }
        StateContent(state, retry) { data ->
        var trimestre by rememberSaveable { mutableStateOf(TrimestreAsistencia.T1) }
        var filtro by rememberSaveable { mutableStateOf(FiltroAsistencia.TODOS) }
        val registrosTrimestre = remember(data.asistencias, trimestre) {
            data.asistencias.filter { it.periodo.trimestreAsistencia() == trimestre }
        }
        val resumen = remember(registrosTrimestre) { registrosTrimestre.resumenAsistencia() }
        val registrosVisibles = remember(registrosTrimestre, filtro) {
            if (filtro.estado == null) registrosTrimestre
            else registrosTrimestre.filter { it.estado.equals(filtro.estado, ignoreCase = true) }
        }
        val grupos = remember(registrosVisibles) { registrosVisibles.groupBy { it.fecha } }

        SelectorTrimestreAsistencia(seleccionado = trimestre, onSelect = { trimestre = it })
        ResumenAsistenciaCard(resumen)
        FiltrosAsistencia(seleccionado = filtro, onSelect = { filtro = it })

        if (registrosVisibles.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    if (registrosTrimestre.isEmpty()) "No existen registros de asistencia en ${trimestre.label}"
                    else "No hay registros para el filtro ${filtro.label.lowercase()}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                grupos.forEach { (fecha, registros) ->
                    item(key = "fecha-$fecha") {
                        Text(
                            text = fecha.formatoFechaAsistencia(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                    }
                    itemsIndexed(registros) { _, registro -> RegistroAsistenciaCard(registro) }
                }
            }
        }
    } }
}

@Composable private fun AttendanceNotificationCard(context: AttendanceNotificationContext) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(context.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(context.message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

internal enum class TrimestreAsistencia(val label: String) { T1("T1"), T2("T2"), T3("T3") }

private enum class FiltroAsistencia(val label: String, val estado: String?) {
    TODOS("Todos", null), AUSENTES("Ausentes", "AUSENTE"), ATRASOS("Atrasos", "ATRASO"),
    JUSTIFICADOS("Justificados", "JUSTIFICADO"), PRESENTES("Presentes", "PRESENTE")
}

private data class ResumenAsistenciaLocal(
    val total: Int,
    val presentes: Int,
    val ausentes: Int,
    val atrasos: Int,
    val justificados: Int
) {
    val porcentaje: Double get() = if (total == 0) 0.0 else presentes * 100.0 / total
}

internal fun String.trimestreAsistencia(): TrimestreAsistencia? {
    val value = trim().uppercase().replace(Regex("\\s+"), " ")
    return when {
        value == "T1" || value.startsWith("PRIMER TRIMESTRE") -> TrimestreAsistencia.T1
        value == "T2" || value.startsWith("SEGUNDO TRIMESTRE") -> TrimestreAsistencia.T2
        value == "T3" || value.startsWith("TERCER TRIMESTRE") -> TrimestreAsistencia.T3
        else -> null
    }
}

private fun List<AsistenciaHijo>.resumenAsistencia() = ResumenAsistenciaLocal(
    total = size,
    presentes = count { it.estado.equals("PRESENTE", ignoreCase = true) },
    ausentes = count { it.estado.equals("AUSENTE", ignoreCase = true) },
    atrasos = count { it.estado.equals("ATRASO", ignoreCase = true) },
    justificados = count { it.estado.equals("JUSTIFICADO", ignoreCase = true) }
)

@Composable private fun SelectorTrimestreAsistencia(
    seleccionado: TrimestreAsistencia,
    onSelect: (TrimestreAsistencia) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Trimestre", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TrimestreAsistencia.entries.forEach { trimestre ->
                FilterChip(
                    selected = seleccionado == trimestre,
                    onClick = { onSelect(trimestre) },
                    label = { Text(trimestre.label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable private fun ResumenAsistenciaCard(resumen: ResumenAsistenciaLocal) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Asistencia", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        String.format("%.2f%%", resumen.porcentaje),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text("${resumen.total} registros", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ResumenDato("Presentes", resumen.presentes, EstadoColor.PRESENTE, Modifier.weight(1f))
                ResumenDato("Ausentes", resumen.ausentes, EstadoColor.AUSENTE, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ResumenDato("Atrasos", resumen.atrasos, EstadoColor.ATRASO, Modifier.weight(1f))
                ResumenDato("Justificados", resumen.justificados, EstadoColor.JUSTIFICADO, Modifier.weight(1f))
            }
        }
    }
}

@Composable private fun ResumenDato(label: String, cantidad: Int, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(9.dp).background(color, RoundedCornerShape(50)))
        Column {
            Text(cantidad.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun FiltrosAsistencia(seleccionado: FiltroAsistencia, onSelect: (FiltroAsistencia) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FiltroAsistencia.entries.forEach { filtro ->
            FilterChip(selected = seleccionado == filtro, onClick = { onSelect(filtro) }, label = { Text(filtro.label) })
        }
    }
}

@Composable private fun RegistroAsistenciaCard(registro: AsistenciaHijo) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Registro de asistencia", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(registro.periodo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            EstadoAsistenciaBadge(registro.estado)
        }
    }
}

private object EstadoColor {
    val PRESENTE = Color(0xFF16803A)
    val AUSENTE = Color(0xFFC62828)
    val ATRASO = Color(0xFFE56A00)
    val JUSTIFICADO = Color(0xFF1565C0)
}

@Composable private fun EstadoAsistenciaBadge(estado: String) {
    val color = when (estado.uppercase()) {
        "PRESENTE" -> EstadoColor.PRESENTE
        "AUSENTE" -> EstadoColor.AUSENTE
        "ATRASO" -> EstadoColor.ATRASO
        "JUSTIFICADO" -> EstadoColor.JUSTIFICADO
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = color.copy(alpha = 0.14f), shape = RoundedCornerShape(50)) {
        Text(
            estado.uppercase(),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

internal fun String.formatoFechaAsistencia(): String {
    val parts = split('-')
    if (parts.size != 3) return uppercase()
    val month = parts[1].toIntOrNull()?.let { listOf("ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC").getOrNull(it - 1) }
        ?: return uppercase()
    return "${parts[2].trimStart('0').ifEmpty { "0" }} $month ${parts[0]}"
}

@Composable private fun <T> StateContent(state: ConsultaUiState<T>, retry: () -> Unit, content: @Composable (T) -> Unit) = when {
    state.loading -> CircularProgressIndicator()
    state.error != null -> Column { Text(state.error); Button(onClick = retry) { Text("Reintentar") } }
    state.data != null -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OfflineBanner(isOffline = state.isOffline)
        content(state.data)
    }
    else -> Text("Sin información")
}

@Composable private fun Page(title: String, back: () -> Unit, content: @Composable ColumnScope.() -> Unit) = Scaffold(
    topBar = { TopAppBar(title = { Text(title) }, navigationIcon = { IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Volver") } }) }
) { padding -> Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content) }

@Composable private fun OptionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, click: () -> Unit) = Card(Modifier.fillMaxWidth().clickable(onClick = click)) {
    ListItem(leadingContent = { Icon(icon, null) }, headlineContent = { Text(title, fontWeight = FontWeight.Bold) }, supportingContent = { Text(subtitle) })
}
