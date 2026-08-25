package ec.edu.uteq.sga.docente.ui.screens.cursos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Módulos del Curso, 1: Nómina de Estudiantes

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
                .background(BackgroundSlate)
        ) {
            OfflineBanner(isOffline = state.isOffline)

            // Encabezado del curso (estilo web)
            state.asignacion?.let { curso ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CardSurface,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${curso.gradoNombre} • Paralelo \"${curso.paraleloLetra}\"",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNavy
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Año lectivo: ${curso.anoLectivoNombre}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ModuloAulaVirtualBg
                            ) {
                                Text(
                                    text = "ID #${curso.idAsignacion}",
                                    color = ModuloAulaVirtualText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Pestañas
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CardSurface,
                contentColor = PrimaryNavy
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Gestión Académica", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.DashboardCustomize, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Estudiantes (${state.estudiantes.size})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.People, contentDescription = null) }
                )
            }

            if (selectedTab == 0) {
                // ─── MÓDULOS DEL CURSO ──────────────────────────────────────────
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        ModuloCursoCard(
                            title = "Actividades y Calificaciones",
                            description = "Cree tareas, lecciones, exámenes y asiente notas formativas/sumativas.",
                            icon = Icons.AutoMirrored.Filled.Assignment,
                            bgColor = ModuloActividadesBg,
                            iconColor = ModuloActividadesText,
                            onClick = { onActividadesClick(idAsignacion) }
                        )
                    }
                    item {
                        ModuloCursoCard(
                            title = "Control de Asistencia",
                            description = "Registre la lista diaria (Presente, Ausente, Justificado, Atraso) y vea resúmenes.",
                            icon = Icons.Default.CheckCircle,
                            bgColor = ModuloAsistenciaBg,
                            iconColor = ModuloAsistenciaText,
                            onClick = { onAsistenciaClick(idAsignacion) }
                        )
                    }
                    item {
                        ModuloCursoCard(
                            title = "Aula Virtual por Semanas",
                            description = "Agenda organizada de actividades Lunes a Viernes por trimestres escolares.",
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            bgColor = ModuloAulaVirtualBg,
                            iconColor = ModuloAulaVirtualText,
                            onClick = { onAulaVirtualClick(idAsignacion) }
                        )
                    }
                    item {
                        ModuloCursoCard(
                            title = "Anuncios y Comunicados",
                            description = "Publique avisos y novedades fijadas para los estudiantes de este paralelo.",
                            icon = Icons.Default.Campaign,
                            bgColor = ModuloAnunciosBg,
                            iconColor = ModuloAnunciosText,
                            onClick = { onAnunciosClick(idAsignacion) }
                        )
                    }
                    item {
                        ModuloCursoCard(
                            title = "Materiales de Estudio",
                            description = "Comparta guías, diapositivas, documentos PDF y enlaces de apoyo.",
                            icon = Icons.Default.Folder,
                            bgColor = ModuloMaterialBg,
                            iconColor = ModuloMaterialText,
                            onClick = { onMaterialesClick(idAsignacion) }
                        )
                    }
                    item {
                        ModuloCursoCard(
                            title = "Reportes y Promedios Anuales",
                            description = "Consulte ponderaciones trimestrales (70% + 30%) y cuadro de calificaciones finales.",
                            icon = Icons.Default.Assessment,
                            bgColor = ModuloReportesBg,
                            iconColor = ModuloReportesText,
                            onClick = { onReportesClick(idAsignacion) }
                        )
                    }
                }
            } else {
                // ─── NÓMINA DE ESTUDIANTES ───────────────────────────────────────
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
                                    }
                                    .shadow(1.dp, RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = CardSurface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Avatar con inicial
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(ModuloAulaVirtualBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = estudiante.apellidos.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryNavy,
                                            fontSize = 16.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = estudiante.nombreCompleto,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (estudiante.cedula.isNotBlank()) "Cédula: ${estudiante.cedula}" else "Matrícula activa",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
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
                                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                            contentDescription = "Ver Seguimiento",
                                            tint = PrimaryNavy
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
    bgColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(2.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
