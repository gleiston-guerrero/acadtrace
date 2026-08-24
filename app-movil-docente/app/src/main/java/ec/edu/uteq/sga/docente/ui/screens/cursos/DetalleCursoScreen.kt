package ec.edu.uteq.sga.docente.ui.screens.cursos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ec.edu.uteq.sga.docente.ui.components.*
import ec.edu.uteq.sga.docente.ui.theme.*

@Composable
fun DetalleCursoScreen(
    idAsignacion: Long,
    viewModel: CursosViewModel,
    onBackClick: () -> Unit,
    onActividadesClick: (Long) -> Unit,
    onAsistenciaClick: (Long) -> Unit,
    onAulaVirtualClick: (Long) -> Unit,
    onAnunciosClick: (Long) -> Unit,
    onMaterialesClick: (Long) -> Unit,
    onReportesClick: (Long) -> Unit,
    onSeguimientoEstudianteClick: (Long, String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Módulos del Curso, 1: Nómina de Estudiantes

    LaunchedEffect(idAsignacion) {
        viewModel.loadCurso(idAsignacion)
    }

    Scaffold(
        topBar = {
            SgaTopAppBar(
                title = state.asignacion?.asignaturaNombre ?: "Detalle del Curso",
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
            OfflineBanner(isOffline = state.isOffline)

            // Encabezado del curso
            state.asignacion?.let { curso ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${curso.gradoNombre} - Paralelo \"${curso.paraleloLetra}\"",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SecondarySky.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = curso.anoLectivoNombre,
                                    color = SecondarySky,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Pestañas
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryBlue
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Gestión Académica", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.DashboardCustomize, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Estudiantes (${state.estudiantes.size})", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.People, contentDescription = null) }
                )
            }

            if (selectedTab == 0) {
                // Módulos del Curso
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        ModuloCursoCard(
                            title = "Actividades y Calificaciones",
                            description = "Crear tareas, lecciones, exámenes y asentar calificaciones.",
                            icon = Icons.Default.Assignment,
                            color = PrimaryBlue,
                            onClick = { onActividadesClick(idAsignacion) }
                        )
                    }
                    item {
                        ModuloCursoCard(
                            title = "Control de Asistencia",
                            description = "Registrar lista diaria y ver resumen de inasistencias.",
                            icon = Icons.Default.CheckCircle,
                            color = AccentGreen,
                            onClick = { onAsistenciaClick(idAsignacion) }
                        )
                    }
                    item {
                        ModuloCursoCard(
                            title = "Aula Virtual por Semanas",
                            description = "Agenda organizada de actividades Lunes-Viernes por trimestres.",
                            icon = Icons.Default.DateRange,
                            color = SecondarySky,
                            onClick = { onAulaVirtualClick(idAsignacion) }
                        )
                    }
                    item {
                        ModuloCursoCard(
                            title = "Anuncios y Comunicados",
                            description = "Publicar avisos importantes para los estudiantes de este curso.",
                            icon = Icons.Default.Campaign,
                            color = WarningAmber,
                            onClick = { onAnunciosClick(idAsignacion) }
                        )
                    }
                    item {
                        ModuloCursoCard(
                            title = "Materiales de Estudio",
                            description = "Compartir guías, diapositivas, documentos y enlaces.",
                            icon = Icons.Default.Folder,
                            color = Color(0xFF8B5CF6),
                            onClick = { onMaterialesClick(idAsignacion) }
                        )
                    }
                    item {
                        ModuloCursoCard(
                            title = "Reportes y Promedios Anuales",
                            description = "Desglose de notas trimestrales (70/30) y notas finales anuales.",
                            icon = Icons.Default.BarChart,
                            color = Color(0xFFEC4899),
                            onClick = { onReportesClick(idAsignacion) }
                        )
                    }
                }
            } else {
                // Lista de Estudiantes
                if (state.isLoading && state.estudiantes.isEmpty()) {
                    LoadingView("Cargando nómina de estudiantes...")
                } else if (state.estudiantes.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.PeopleOutline,
                        title = "No hay estudiantes registrados",
                        subtitle = "No se encontraron matriculados para esta asignatura."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.estudiantes) { estudiante ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSeguimientoEstudianteClick(
                                            estudiante.idMatricula,
                                            estudiante.nombreCompleto
                                        )
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryBlue.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = estudiante.apellidos.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryBlue
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = estudiante.nombreCompleto,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (estudiante.cedula.isNotBlank()) {
                                            Text(
                                                text = "Cédula: ${estudiante.cedula}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            onSeguimientoEstudianteClick(
                                                estudiante.idMatricula,
                                                estudiante.nombreCompleto
                                            )
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AssignmentLate,
                                            contentDescription = "Registrar Seguimiento",
                                            tint = WarningAmber
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModuloCursoCard(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}
