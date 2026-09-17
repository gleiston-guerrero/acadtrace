@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package ec.edu.uteq.sga.representante.ui.screens.representante

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ec.edu.uteq.sga.representante.domain.model.*
import ec.edu.uteq.sga.representante.ui.components.OfflineBanner
import ec.edu.uteq.sga.representante.notifications.AttendanceNotificationContext

@Composable fun HomeRepresentante(onRepresentados: () -> Unit, onComunicados: () -> Unit, onSecurity: () -> Unit, onLogout: () -> Unit) = Scaffold(
    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
    topBar = {
        TopAppBar(
            title = { Text("AcadTrace", fontWeight = FontWeight.Bold) },
            actions = { IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "Cerrar sesión") } },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )
    }
) { padding ->
    Column(
        Modifier.padding(padding).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Portal del Representante", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Acceda a la información académica y gestione sus preferencias.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OptionCard("Mis representados", "Consulte la información académica de sus hijos", Icons.Default.People, onRepresentados, MaterialTheme.colorScheme.primary)
        OptionCard("Comunicados", "Avisos institucionales para representantes", Icons.Default.Campaign, onComunicados, Color(0xFFD97706))
        OptionCard("Seguridad", "Biometría y notificaciones locales", Icons.Default.Security, onSecurity, Color(0xFF0D9488))
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                Text("Calificaciones y asistencia se consultan desde cada representado.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable fun ComunicadosRepresentanteScreen(vm: RepresentanteViewModel, back: () -> Unit) {
    val state by vm.comunicados.collectAsState()
    LaunchedEffect(Unit) { vm.cargarComunicados() }
    Page("Comunicados", back) { StateContent(state, vm::cargarComunicados) { items ->
        if (items.isEmpty()) Text("No existen comunicados disponibles") else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(items, key = { it.id }) { item -> ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconContainer(Icons.Default.Campaign, Color(0xFFD97706))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(item.titulo, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(Icons.Default.CalendarToday, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(item.fecha, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                    Text(item.contenido, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } }
        }
    } }
}

@Composable fun MisRepresentadosScreen(vm: RepresentanteViewModel, back: () -> Unit, select: (Representado) -> Unit) {
    val state by vm.representados.collectAsState()
    Page("Mis representados", back) { StateContent(state, vm::cargarRepresentados) { items ->
        if (items.isEmpty()) Text("No hay representados asociados") else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(items) { item -> ElevatedCard(Modifier.fillMaxWidth().clickable { select(item) }, shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    IconContainer(Icons.Default.School, MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(item.nombreCompleto, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        val course = listOfNotNull(item.curso, item.paralelo).joinToString(" · ")
                        if (course.isNotBlank()) Text(course, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, "Ver resumen", tint = MaterialTheme.colorScheme.primary)
                }
            } }
        }
    } }
}

@Composable fun ResumenRepresentadoScreen(nombre: String, back: () -> Unit, notas: () -> Unit, asistencia: () -> Unit) = Page("Resumen académico", back) {
    Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconContainer(Icons.Default.School, MaterialTheme.colorScheme.onPrimary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(nombre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                Text("Información académica", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f))
            }
        }
    }
    Text("Consulta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    OptionCard("Calificaciones", "Periodos, actividades y promedios", Icons.Default.Grade, notas, Color(0xFF7C3AED))
    OptionCard("Asistencia", "Registros y resumen de asistencia", Icons.Default.EventAvailable, asistencia, Color(0xFF16803A))
}

@Composable fun ConsultaCalificacionesRepresentante(vm: RepresentanteViewModel, back: () -> Unit, retry: () -> Unit) {
    val state by vm.calificaciones.collectAsState()
    val selected by vm.periodoSeleccionado.collectAsState()
    Page("Calificaciones", back) { StateContent(state, retry) { data ->
        var expandedSubjectId by rememberSaveable(selected) { mutableStateOf<Long?>(null) }
        PeriodSelector(data.periodos, selected, vm::seleccionarPeriodo)
        val blocks = selected?.let(data::asignaturas).orEmpty()
        if (blocks.isEmpty()) Text("No existen calificaciones para este período")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(blocks, key = { it.idAsignacion }) { block ->
                val expanded = expandedSubjectId == block.idAsignacion
                ElevatedCard(
                    Modifier.fillMaxWidth().animateContentSize(animationSpec = tween(250)).clickable {
                        expandedSubjectId = if (expanded) null else block.idAsignacion
                    },
                    shape = RoundedCornerShape(18.dp)
                ) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    IconContainer(Icons.Default.MenuBook, MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text(block.asignatura, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(block.promedio?.periodo ?: block.actividades.firstOrNull()?.periodo.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    block.promedio?.let { p ->
                        Column(horizontalAlignment = Alignment.End) {
                            Text(p.promedioTrimestral.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(p.notaCualitativa.etiquetaCualitativa(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (expanded) "Contraer materia" else "Expandir materia", tint = MaterialTheme.colorScheme.primary)
                }

                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(tween(180)) + expandVertically(tween(250)),
                    exit = fadeOut(tween(140)) + shrinkVertically(tween(220))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                        block.promedio?.let { p ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GradeMetric("Formativo", p.promedioFormativo.toString(), Modifier.weight(1f))
                                GradeMetric("Sumativo", p.notaSumativa.toString(), Modifier.weight(1f))
                                GradeMetric("Promedio", "${p.promedioTrimestral} · ${p.notaCualitativa.etiquetaCualitativa()}", Modifier.weight(1f), emphasized = true)
                            }
                        }
                        if (block.actividades.isNotEmpty()) Text("Actividades", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        block.actividades.forEachIndexed { index, n ->
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(n.actividad, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                GradeBadge(n.nota?.toString() ?: "Pendiente")
                            }
                        }
                    }
                }
            } } }
            if (data.mostrarPromediosAnuales && data.promediosAnuales.isNotEmpty()) {
                item { Text("Promedios finales", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                items(data.promediosAnuales, key = { it.idAsignacion }) { p -> ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(p.asignatura, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        GradeBadge("${p.promedioAnual} · ${p.notaCualitativa.etiquetaCualitativa()}")
                    }
                } }
            }
        }
    } }
}

@Composable
private fun PeriodSelector(periods: List<PeriodoCalificaciones>, selected: Long?, onSelect: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        periods.forEach { period ->
            val active = selected == period.idPeriodo
            val label = compactPeriodLabel(period.nombre)
            Surface(
                modifier = Modifier.weight(1f).clickable { onSelect(period.idPeriodo) },
                shape = RoundedCornerShape(14.dp),
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = if (active) 2.dp else 0.dp
            ) {
                Column(
                    Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(label.first, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                    label.second?.let { Text(it, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                }
            }
        }
    }
}

private fun compactPeriodLabel(name: String): Pair<String, String?> {
    val normalized = name.trim()
    val title = when {
        normalized.contains("primer", ignoreCase = true) -> "1° Trimestre"
        normalized.contains("segundo", ignoreCase = true) -> "2° Trimestre"
        normalized.contains("tercer", ignoreCase = true) -> "3° Trimestre"
        else -> normalized
    }
    val schoolYear = Regex("\\b\\d{4}\\s*[-–]\\s*\\d{4}\\b").find(normalized)?.value
    return title to schoolYear
}

@Composable fun AsistenciaHijoScreen(vm: RepresentanteViewModel, back: () -> Unit, notificationContext: AttendanceNotificationContext? = null, retry: () -> Unit) {
    val state by vm.asistencia.collectAsState()
    var trimestre by rememberSaveable { mutableStateOf(TrimestreAsistencia.T1) }
    var detalle by rememberSaveable { mutableStateOf<FiltroAsistencia?>(null) }
    val closeOrBack = { if (detalle != null) detalle = null else back() }

    Scaffold(
        containerColor = Color(0xFFF5F7FC),
        topBar = {
            TopAppBar(
                title = { Text(detalle?.let { "Asistencias - ${it.label}" } ?: "Asistencia", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = closeOrBack) { Icon(Icons.Default.ArrowBack, "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (detalle == null) notificationContext?.let { AttendanceNotificationCard(it) }
            StateContent(state, retry) { data ->
                val registrosTrimestre = remember(data.asistencias, trimestre) {
                    data.asistencias.filter { it.periodo.trimestreAsistencia() == trimestre }
                }
                val resumen = remember(registrosTrimestre) { registrosTrimestre.resumenAsistencia() }

                if (detalle == null) {
                    SelectorTrimestreAsistencia(seleccionado = trimestre, onSelect = { trimestre = it })
                    ResumenAsistenciaCard(resumen)
                    Text("Consultar registros", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(FiltroAsistencia.entries.filter { it != FiltroAsistencia.PRESENTES }) { filtro ->
                            CategoriaAsistenciaCard(filtro) { detalle = filtro }
                        }
                    }
                } else {
                    val filtro = requireNotNull(detalle)
                    val registrosVisibles = remember(registrosTrimestre, filtro) {
                        if (filtro.estado == null) registrosTrimestre
                        else registrosTrimestre.filter { it.estado.equals(filtro.estado, ignoreCase = true) }
                    }
                    DetalleAsistencia(
                        filtro = filtro,
                        periodo = registrosTrimestre.firstOrNull()?.periodo ?: trimestre.nombreVisual,
                        registros = registrosVisibles
                    )
                }
            }
        }
    }
}

@Composable private fun AttendanceNotificationCard(context: AttendanceNotificationContext) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(context.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(context.message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

internal enum class TrimestreAsistencia(val label: String, val nombreVisual: String) {
    T1("T1", "1° Trimestre"), T2("T2", "2° Trimestre"), T3("T3", "3° Trimestre")
}

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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TrimestreAsistencia.entries.forEach { trimestre ->
            val selected = seleccionado == trimestre
            Surface(
                modifier = Modifier.weight(1f).clickable { onSelect(trimestre) },
                shape = RoundedCornerShape(14.dp),
                color = if (selected) Color(0xFF2563EB) else Color.White,
                contentColor = if (selected) Color.White else MaterialTheme.colorScheme.primary,
                border = BorderStroke(1.dp, if (selected) Color(0xFF2563EB) else Color(0xFFD8E0EE)),
                shadowElevation = if (selected) 2.dp else 0.dp
            ) {
                Column(Modifier.padding(horizontal = 3.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(18.dp))
                    Text(trimestre.nombreVisual, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}

@Composable private fun ResumenAsistenciaCard(resumen: ResumenAsistenciaLocal) {
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ResumenDato("Presentes", resumen.presentes, EstadoColor.PRESENTE, Color(0xFFEAF8EE), Icons.Default.CheckCircle, Modifier.weight(1f))
                ResumenDato("Ausentes", resumen.ausentes, EstadoColor.AUSENTE, Color(0xFFFFECEF), Icons.Default.Cancel, Modifier.weight(1f))
                ResumenDato("Atrasos", resumen.atrasos, EstadoColor.ATRASO, Color(0xFFFFF5DD), Icons.Default.Schedule, Modifier.weight(1f))
                ResumenDato("Justificados", resumen.justificados, EstadoColor.JUSTIFICADO, Color(0xFFEAF2FF), Icons.Default.Verified, Modifier.weight(1f))
            }
        }
    }
}

@Composable private fun ResumenDato(label: String, cantidad: Int, color: Color, background: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(modifier, color = background, shape = RoundedCornerShape(13.dp)) {
        Column(Modifier.padding(horizontal = 5.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Text(cantidad.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable private fun CategoriaAsistenciaCard(filtro: FiltroAsistencia, onClick: () -> Unit) {
    val visual = categoriaVisual(filtro)
    ElevatedCard(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(17.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Surface(color = visual.background, shape = RoundedCornerShape(13.dp), modifier = Modifier.size(46.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(visual.icon, null, tint = visual.color, modifier = Modifier.size(24.dp)) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(filtro.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(visual.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, "Ver ${filtro.label.lowercase()}", tint = visual.color)
        }
    }
}

private data class CategoriaVisual(val subtitle: String, val icon: ImageVector, val color: Color, val background: Color)

private fun categoriaVisual(filtro: FiltroAsistencia) = when (filtro) {
    FiltroAsistencia.TODOS -> CategoriaVisual("Ver todas las asistencias", Icons.Default.FormatListBulleted, Color(0xFF7C3AED), Color(0xFFF1EAFF))
    FiltroAsistencia.AUSENTES -> CategoriaVisual("Ver solo ausencias", Icons.Default.Cancel, EstadoColor.AUSENTE, Color(0xFFFFECEF))
    FiltroAsistencia.ATRASOS -> CategoriaVisual("Ver solo atrasos", Icons.Default.Schedule, EstadoColor.ATRASO, Color(0xFFFFF5DD))
    FiltroAsistencia.JUSTIFICADOS -> CategoriaVisual("Ver solo asistencias justificadas", Icons.Default.Verified, EstadoColor.JUSTIFICADO, Color(0xFFEAF2FF))
    FiltroAsistencia.PRESENTES -> CategoriaVisual("Ver solo asistencias presentes", Icons.Default.CheckCircle, EstadoColor.PRESENTE, Color(0xFFEAF8EE))
}

@Composable private fun ColumnScope.DetalleAsistencia(filtro: FiltroAsistencia, periodo: String, registros: List<AsistenciaHijo>) {
    val visual = categoriaVisual(filtro)
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(color = visual.background, shape = RoundedCornerShape(14.dp), modifier = Modifier.size(50.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(visual.icon, null, tint = visual.color, modifier = Modifier.size(26.dp)) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(if (filtro == FiltroAsistencia.TODOS) "Todos los registros" else visual.subtitle.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(periodo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(registros.size.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = visual.color)
        }
    }

    Text("Registros", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    if (registros.isEmpty()) {
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Text("No hay registros para ${filtro.label.lowercase()}", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    } else {
        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(registros) { registro -> RegistroAsistenciaCard(registro) }
        }
    }
}

@Composable private fun RegistroAsistenciaCard(registro: AsistenciaHijo) {
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(registro.fecha.formatoFechaAsistencia(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Registro de asistencia", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
    topBar = { TopAppBar(title = { Text(title, fontWeight = FontWeight.SemiBold) }, navigationIcon = { IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Volver") } }) }
) { padding -> Column(Modifier.padding(padding).padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content) }

@Composable private fun OptionCard(title: String, subtitle: String, icon: ImageVector, click: () -> Unit, accent: Color = MaterialTheme.colorScheme.primary) = ElevatedCard(
    Modifier.fillMaxWidth().clickable(onClick = click), shape = RoundedCornerShape(18.dp)
) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        IconContainer(icon, accent)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, null, tint = accent)
    }
}

@Composable private fun IconContainer(icon: ImageVector, accent: Color) = Surface(
    color = accent.copy(alpha = 0.12f), shape = RoundedCornerShape(14.dp), modifier = Modifier.size(48.dp)
) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent, modifier = Modifier.size(25.dp)) } }

@Composable private fun GradeMetric(label: String, value: String, modifier: Modifier = Modifier, emphasized: Boolean = false) {
    val accent = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(modifier, color = accent.copy(alpha = if (emphasized) 0.10f else 0.06f), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable private fun GradeBadge(value: String) = Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), shape = RoundedCornerShape(50)) {
    Text(value, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
}
