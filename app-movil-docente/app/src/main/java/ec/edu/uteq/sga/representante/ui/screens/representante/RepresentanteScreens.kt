@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package ec.edu.uteq.sga.representante.ui.screens.representante

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ec.edu.uteq.sga.representante.domain.model.*
import ec.edu.uteq.sga.representante.ui.components.OfflineBanner

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
    Page("Calificaciones", back) { StateContent(state, retry) { data ->
        if (data.calificaciones.isEmpty() && data.promedios.isEmpty()) Text("No existen calificaciones disponibles")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(data.promedios) { p -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) {
                Text(p.periodo, fontWeight = FontWeight.Bold); Text("Formativo: ${p.promedioFormativo} · Sumativo: ${p.notaSumativa}"); Text("Promedio: ${p.promedioTrimestral} (${p.notaCualitativa})")
            } } }
            items(data.calificaciones) { n -> ListItem(headlineContent = { Text(n.actividad) }, supportingContent = { Text(n.periodo) }, trailingContent = { Text(n.nota.toString(), fontWeight = FontWeight.Bold) }) }
        }
    } }
}

@Composable fun AsistenciaHijoScreen(vm: RepresentanteViewModel, back: () -> Unit, retry: () -> Unit) {
    val state by vm.asistencia.collectAsState()
    Page("Asistencia", back) { StateContent(state, retry) { data ->
        val r = data.resumen
        Text("Asistencia: ${r.porcentajeAsistencia}% · Presentes ${r.presentes} · Ausentes ${r.ausentes} · Justificados ${r.justificados} · Atrasos ${r.atrasos}")
        if (data.asistencias.isEmpty()) Text("No existen registros de asistencia") else LazyColumn { items(data.asistencias) { a ->
            ListItem(headlineContent = { Text(a.estado) }, supportingContent = { Text("${a.fecha} · ${a.periodo}") })
        } }
    } }
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
