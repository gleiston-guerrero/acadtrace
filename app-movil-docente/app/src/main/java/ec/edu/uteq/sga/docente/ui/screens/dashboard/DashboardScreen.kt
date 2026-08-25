package ec.edu.uteq.sga.docente.ui.screens.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ec.edu.uteq.sga.docente.R
import ec.edu.uteq.sga.docente.domain.model.Asignacion
import ec.edu.uteq.sga.docente.ui.components.*
import ec.edu.uteq.sga.docente.ui.theme.*

data class ModuloItem(
    val id: String,
    val label: String,
    val desc: String,
    val icon: ImageVector,
    val bgColor: Color,
    val iconColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onCourseClick: (Long) -> Unit,
    onHorarioClick: () -> Unit,
    onSeguimientoClick: () -> Unit,
    onSyncStatusClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Cursos, 1 = Módulos
    var searchQuery by remember { mutableStateOf("") }

    val modulosList = remember {
        listOf(
            ModuloItem("aula-virtual", "Aula Virtual", "Organice sus cursos, semanas y novedades", Icons.AutoMirrored.Filled.MenuBook, ModuloAulaVirtualBg, ModuloAulaVirtualText),
            ModuloItem("actividades", "Actividades", "Cree y evalúe tareas, exámenes y proyectos", Icons.AutoMirrored.Filled.Assignment, ModuloActividadesBg, ModuloActividadesText),
            ModuloItem("asistencia", "Asistencia", "Registre y controle la asistencia diaria", Icons.Default.CheckCircle, ModuloAsistenciaBg, ModuloAsistenciaText),
            ModuloItem("calificaciones", "Calificaciones", "Asiente y consulte las calificaciones", Icons.AutoMirrored.Filled.Grading, ModuloCalificacionesBg, ModuloCalificacionesText),
            ModuloItem("seguimiento", "Seguimiento", "Monitoree el desempeño académico y DECE", Icons.AutoMirrored.Filled.TrendingUp, ModuloSeguimientoBg, ModuloSeguimientoText),
            ModuloItem("reportes", "Reportes", "Genere reportes de gestión académica", Icons.Default.Assessment, ModuloReportesBg, ModuloReportesText),
            ModuloItem("anuncios", "Anuncios", "Publique avisos para sus cursos", Icons.Default.Campaign, ModuloAnunciosBg, ModuloAnunciosText),
            ModuloItem("material", "Material", "Comparta archivos y enlaces de clase", Icons.Default.Folder, ModuloMaterialBg, ModuloMaterialText),
            ModuloItem("horario", "Horarios", "Consulte su distribución semanal de clases", Icons.Default.CalendarMonth, ModuloHorarioBg, ModuloHorarioText)
        )
    }

    val asignacionesFiltradas = remember(state.asignaciones, searchQuery) {
        if (searchQuery.isBlank()) state.asignaciones
        else state.asignaciones.filter {
            it.asignaturaNombre.contains(searchQuery, ignoreCase = true) ||
            it.gradoNombre.contains(searchQuery, ignoreCase = true) ||
            it.paraleloLetra.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.logo_escuela),
                                contentDescription = "Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Text(
                            text = "SGA | Portal Docente",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    // Sincronización
                    IconButton(onClick = onSyncStatusClick) {
                        BadgedBox(
                            badge = {
                                if (state.pendingSyncCount > 0) {
                                    Badge(containerColor = WarningAmber) {
                                        Text(state.pendingSyncCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sincronización",
                                tint = Color.White
                            )
                        }
                    }

                    // Avatar de usuario con inicial
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.teacherUsername.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Cerrar sesión
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryNavy
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundSlate)
        ) {
            OfflineBanner(
                isOffline = state.isOffline,
                pendingCount = state.pendingSyncCount,
                onSyncClick = { viewModel.syncNow() }
            )

            // ─── HEADER DE BIENVENIDA ──────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardSurface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Bienvenido, ",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = state.teacherUsername,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNavy
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Año lectivo: 2026 - 2027 (Actual)",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        // Badge de cursos activos
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ModuloActividadesBg
                        ) {
                            Text(
                                text = "${state.asignaciones.size} cursos",
                                color = PrimaryNavy,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Buscador
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar asignatura o curso...", fontSize = 13.sp, color = TextMuted) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Limpiar",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = BackgroundSlate,
                            unfocusedContainerColor = BackgroundSlate,
                            focusedBorderColor = PrimaryNavy,
                            unfocusedBorderColor = SlateBorder
                        )
                    )
                }
            }

            // ─── TABS DE NAVEGACIÓN ───────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CardSurface,
                contentColor = PrimaryNavy
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Mis Cursos Asignados", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.School, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Módulos del Sistema", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.GridView, contentDescription = null) }
                )
            }

            if (state.isLoading && state.asignaciones.isEmpty()) {
                LoadingView("Cargando información del docente...")
            } else if (state.errorMessage != null && state.asignaciones.isEmpty()) {
                ErrorView(
                    message = state.errorMessage!!,
                    onRetry = { viewModel.loadData() }
                )
            } else if (selectedTab == 0) {
                // ─── VISTA 1: LISTADO DE CURSOS ASIGNADOS ───────────────────────
                if (asignacionesFiltradas.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Class,
                        title = "No se encontraron cursos",
                        subtitle = if (searchQuery.isNotEmpty()) "Intenta con otro término de búsqueda" else "No tienes asignaturas activas en este período."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(asignacionesFiltradas) { curso ->
                            CursoCardEstiloWeb(
                                curso = curso,
                                onClick = { onCourseClick(curso.idAsignacion) }
                            )
                        }
                    }
                }
            } else {
                // ─── VISTA 2: MÓDULOS DEL SISTEMA (GRID ESTILO WEB) ──────────────
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(modulosList) { mod ->
                        ModuloCardEstiloWeb(
                            item = mod,
                            onClick = {
                                when (mod.id) {
                                    "horario" -> onHorarioClick()
                                    "seguimiento" -> onSeguimientoClick()
                                    else -> {
                                        if (state.asignaciones.isNotEmpty()) {
                                            onCourseClick(state.asignaciones.first().idAsignacion)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar Sesión", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("¿Estás seguro de que deseas salir del Portal Docente?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        onLogoutClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Cerrar Sesión", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = CardSurface
        )
    }
}

@Composable
fun CursoCardEstiloWeb(
    curso: Asignacion,
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
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = curso.asignaturaNombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${curso.gradoNombre} • Paralelo \"${curso.paraleloLetra}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ModuloAulaVirtualBg
                ) {
                    Text(
                        text = "ID #${curso.idAsignacion}",
                        color = ModuloAulaVirtualText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SlateBorder.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cantidad de estudiantes
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${curso.cantidadEstudiantes} Estudiantes",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // Asistencia promedio
                curso.porcentajeAsistencia?.let { asis ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (asis >= 80.0) AccentGreen else DangerRed,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${String.format("%.1f", asis)}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (asis >= 80.0) AccentGreen else DangerRed
                        )
                    }
                }

                // Flecha de navegación
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ver Curso",
                        color = PrimaryNavy,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = PrimaryNavy,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ModuloCardEstiloWeb(
    item: ModuloItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() }
            .shadow(2.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(item.bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = item.iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.desc,
                fontSize = 10.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp,
                maxLines = 2
            )
        }
    }
}
