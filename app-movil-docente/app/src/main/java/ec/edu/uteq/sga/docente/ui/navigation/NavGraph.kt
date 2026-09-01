package ec.edu.uteq.sga.docente.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import ec.edu.uteq.sga.docente.SgaDocenteApp
import ec.edu.uteq.sga.docente.ui.screens.actividades.*
import ec.edu.uteq.sga.docente.ui.screens.anuncios.*
import ec.edu.uteq.sga.docente.ui.screens.asistencia.*
import ec.edu.uteq.sga.docente.ui.screens.aulavirtual.*
import ec.edu.uteq.sga.docente.ui.screens.calificaciones.*
import ec.edu.uteq.sga.docente.ui.screens.cursos.*
import ec.edu.uteq.sga.docente.ui.screens.dashboard.*
import ec.edu.uteq.sga.docente.ui.screens.horario.*
import ec.edu.uteq.sga.docente.ui.screens.login.*
import ec.edu.uteq.sga.docente.ui.screens.materiales.*
import ec.edu.uteq.sga.docente.ui.screens.reportes.*
import ec.edu.uteq.sga.docente.ui.screens.seguimiento.*
import ec.edu.uteq.sga.docente.ui.screens.settings.*
import ec.edu.uteq.sga.docente.ui.screens.sync.*

@Composable
inline fun <reified T : ViewModel> rememberCustomViewModel(
    key: String? = null,
    crossinline creator: () -> T
): T {
    return viewModel(
        key = key,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
                return creator() as VM
            }
        }
    )
}

@Composable
fun SgaNavGraph(
    navController: NavHostController,
    app: SgaDocenteApp,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ─── LOGIN ────────────────────────────────────────────────────────────
        composable(Screen.Login.route) {
            val viewModel = rememberCustomViewModel(key = "login") {
                LoginViewModel(app.authRepository)
            }
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // ─── DASHBOARD ────────────────────────────────────────────────────────
        composable(Screen.Dashboard.route) {
            val viewModel = rememberCustomViewModel(key = "dashboard") {
                DashboardViewModel(
                    authRepository = app.authRepository,
                    docenteRepository = app.docenteRepository,
                    syncManager = app.syncManager,
                    sessionManager = app.sessionManager
                )
            }
            DashboardScreen(
                viewModel = viewModel,
                onCourseClick = { idAsignacion ->
                    navController.navigate(Screen.DetalleCurso.createRoute(idAsignacion))
                },
                onActividadesClick = { idAsignacion ->
                    navController.navigate(Screen.Actividades.createRoute(idAsignacion))
                },
                onAsistenciaClick = { idAsignacion ->
                    navController.navigate(Screen.Asistencia.createRoute(idAsignacion))
                },
                onAulaVirtualClick = { idAsignacion ->
                    navController.navigate(Screen.AulaVirtualSemanas.createRoute(idAsignacion))
                },
                onAnunciosClick = { idAsignacion ->
                    navController.navigate(Screen.Anuncios.createRoute(idAsignacion))
                },
                onMaterialesClick = { idAsignacion ->
                    navController.navigate(Screen.Materiales.createRoute(idAsignacion))
                },
                onReportesClick = { idAsignacion ->
                    navController.navigate(Screen.ReportesPromedios.createRoute(idAsignacion))
                },
                onHorarioClick = {
                    navController.navigate(Screen.Horario.route)
                },
                onSeguimientoClick = {
                    navController.navigate(Screen.Seguimiento.createRoute(null))
                },
                onSyncStatusClick = {
                    navController.navigate(Screen.SyncStatus.route)
                },
                onLogoutClick = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ─── DETALLE DEL CURSO ────────────────────────────────────────────────
        composable(
            route = Screen.DetalleCurso.route,
            arguments = listOf(navArgument("idAsignacion") { type = NavType.LongType })
        ) { backStackEntry ->
            val idAsignacion = backStackEntry.arguments?.getLong("idAsignacion") ?: 0L
            val viewModel = rememberCustomViewModel(key = "curso_$idAsignacion") {
                CursosViewModel(app.docenteRepository)
            }
            DetalleCursoScreen(
                idAsignacion = idAsignacion,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onActividadesClick = { id ->
                    navController.navigate(Screen.Actividades.createRoute(id))
                },
                onAsistenciaClick = { id ->
                    navController.navigate(Screen.Asistencia.createRoute(id))
                },
                onAulaVirtualClick = { id ->
                    navController.navigate(Screen.AulaVirtualSemanas.createRoute(id))
                },
                onAnunciosClick = { id ->
                    navController.navigate(Screen.Anuncios.createRoute(id))
                },
                onMaterialesClick = { id ->
                    navController.navigate(Screen.Materiales.createRoute(id))
                },
                onReportesClick = { id ->
                    navController.navigate(Screen.ReportesPromedios.createRoute(id))
                },
                onSeguimientoEstudianteClick = { idMatricula, nombre ->
                    navController.navigate(Screen.FormSeguimiento.createRoute(idMatricula, nombre))
                }
            )
        }

        // ─── ACTIVIDADES ──────────────────────────────────────────────────────
        composable(
            route = Screen.Actividades.route,
            arguments = listOf(navArgument("idAsignacion") { type = NavType.LongType })
        ) { backStackEntry ->
            val idAsignacion = backStackEntry.arguments?.getLong("idAsignacion") ?: 0L
            val viewModel = rememberCustomViewModel(key = "actividades_$idAsignacion") {
                ActividadesViewModel(app.actividadesRepository, app.docenteRepository)
            }
            ActividadesScreen(
                idAsignacion = idAsignacion,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onCreateActividadClick = {
                    navController.navigate(Screen.FormActividad.createRoute(idAsignacion, null))
                },
                onEditActividadClick = { idActividad ->
                    navController.navigate(Screen.FormActividad.createRoute(idAsignacion, idActividad))
                },
                onCalificarClick = { idActividad, nombre, max ->
                    navController.navigate(Screen.Calificaciones.createRoute(idActividad, idAsignacion, nombre, max))
                }
            )
        }

        // ─── FORMULARIO ACTIVIDAD (Crear / Editar) ────────────────────────────
        composable(
            route = Screen.FormActividad.route,
            arguments = listOf(
                navArgument("idAsignacion") { type = NavType.LongType },
                navArgument("idActividad") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val idAsignacion = backStackEntry.arguments?.getLong("idAsignacion") ?: 0L
            val idActividadStr = backStackEntry.arguments?.getString("idActividad")
            val idActividad = idActividadStr?.toLongOrNull()
            val viewModel = rememberCustomViewModel(key = "form_actividad_${idAsignacion}_$idActividad") {
                ActividadesViewModel(app.actividadesRepository, app.docenteRepository)
            }
            FormActividadScreen(
                idAsignacion = idAsignacion,
                idActividad = idActividad,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // ─── CALIFICACIONES ───────────────────────────────────────────────────
        composable(
            route = Screen.Calificaciones.route,
            arguments = listOf(
                navArgument("idActividad") { type = NavType.LongType },
                navArgument("idAsignacion") { type = NavType.LongType },
                navArgument("nombre") { type = NavType.StringType; defaultValue = "Actividad" },
                navArgument("max") { type = NavType.FloatType; defaultValue = 10f }
            )
        ) { backStackEntry ->
            val idActividad = backStackEntry.arguments?.getLong("idActividad") ?: 0L
            val idAsignacion = backStackEntry.arguments?.getLong("idAsignacion") ?: 0L
            val nombre = backStackEntry.arguments?.getString("nombre") ?: "Actividad"
            val max = (backStackEntry.arguments?.getFloat("max") ?: 10f).toDouble()
            val viewModel = rememberCustomViewModel(key = "calificaciones_$idActividad") {
                CalificacionesViewModel(app.calificacionesRepository, app.docenteRepository)
            }
            CalificacionesScreen(
                idActividad = idActividad,
                idAsignacion = idAsignacion,
                actividadNombre = nombre,
                notaMaxima = max,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // ─── ASISTENCIA ───────────────────────────────────────────────────────
        composable(
            route = Screen.Asistencia.route,
            arguments = listOf(navArgument("idAsignacion") { type = NavType.LongType })
        ) { backStackEntry ->
            val idAsignacion = backStackEntry.arguments?.getLong("idAsignacion") ?: 0L
            val viewModel = rememberCustomViewModel(key = "asistencia_$idAsignacion") {
                AsistenciaViewModel(app.asistenciasRepository, app.docenteRepository)
            }
            AsistenciaScreen(
                idAsignacion = idAsignacion,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onVerResumenClick = {
                    navController.navigate(Screen.ResumenAsistencia.createRoute(idAsignacion))
                }
            )
        }

        // ─── RESUMEN DE ASISTENCIA ────────────────────────────────────────────
        composable(
            route = Screen.ResumenAsistencia.route,
            arguments = listOf(navArgument("idAsignacion") { type = NavType.LongType })
        ) { backStackEntry ->
            val idAsignacion = backStackEntry.arguments?.getLong("idAsignacion") ?: 0L
            val viewModel = rememberCustomViewModel(key = "resumen_asistencia_$idAsignacion") {
                AsistenciaViewModel(app.asistenciasRepository, app.docenteRepository)
            }
            ResumenAsistenciaScreen(
                idAsignacion = idAsignacion,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // ─── AULA VIRTUAL POR SEMANAS ─────────────────────────────────────────
        composable(
            route = Screen.AulaVirtualSemanas.route,
            arguments = listOf(navArgument("idAsignacion") { type = NavType.LongType })
        ) { backStackEntry ->
            val idAsignacion = backStackEntry.arguments?.getLong("idAsignacion") ?: 0L
            val viewModel = rememberCustomViewModel(key = "aulavirtual_$idAsignacion") {
                AulaVirtualViewModel(app.aulaVirtualRepository, app.docenteRepository)
            }
            SemanasScreen(
                idAsignacion = idAsignacion,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // ─── HORARIO ──────────────────────────────────────────────────────────
        composable(Screen.Horario.route) {
            val viewModel = rememberCustomViewModel(key = "horario") {
                HorarioViewModel(app.horarioRepository, app.sessionManager)
            }
            HorarioScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // ─── SEGUIMIENTO ACADÉMICO ─────────────────────────────────────────────
        composable(
            route = Screen.Seguimiento.route,
            arguments = listOf(
                navArgument("idMatricula") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val idMatriculaStr = backStackEntry.arguments?.getString("idMatricula")
            val idMatricula = idMatriculaStr?.toLongOrNull()
            val viewModel = rememberCustomViewModel(key = "seguimiento_$idMatricula") {
                SeguimientoViewModel(app.seguimientoRepository, app.docenteRepository, app.asistenciasRepository, app.sessionManager)
            }
            SeguimientoScreen(
                idMatricula = idMatricula,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // ─── FORMULARIO DE SEGUIMIENTO ─────────────────────────────────────────
        composable(
            route = Screen.FormSeguimiento.route,
            arguments = listOf(
                navArgument("idMatricula") { type = NavType.LongType },
                navArgument("nombre") { type = NavType.StringType; defaultValue = "Estudiante" }
            )
        ) { backStackEntry ->
            val idMatricula = backStackEntry.arguments?.getLong("idMatricula") ?: 0L
            val nombre = backStackEntry.arguments?.getString("nombre") ?: "Estudiante"
            val viewModel = rememberCustomViewModel(key = "form_seguimiento_$idMatricula") {
                SeguimientoViewModel(app.seguimientoRepository, app.docenteRepository, app.asistenciasRepository, app.sessionManager)
            }
            FormSeguimientoScreen(
                idMatricula = idMatricula,
                estudianteNombre = nombre,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // ─── ANUNCIOS ──────────────────────────────────────────────────────────
        composable(
            route = Screen.Anuncios.route,
            arguments = listOf(navArgument("idAsignacion") { type = NavType.LongType })
        ) { backStackEntry ->
            val idAsignacion = backStackEntry.arguments?.getLong("idAsignacion") ?: 0L
            val viewModel = rememberCustomViewModel(key = "anuncios_$idAsignacion") {
                AnunciosViewModel(app.anunciosRepository, app.docenteRepository, app.sessionManager)
            }
            AnunciosScreen(
                idAsignacion = idAsignacion,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // ─── MATERIALES ────────────────────────────────────────────────────────
        composable(
            route = Screen.Materiales.route,
            arguments = listOf(navArgument("idAsignacion") { type = NavType.LongType })
        ) { backStackEntry ->
            val idAsignacion = backStackEntry.arguments?.getLong("idAsignacion") ?: 0L
            val viewModel = rememberCustomViewModel(key = "materiales_$idAsignacion") {
                MaterialesViewModel(app.materialesRepository, app.docenteRepository)
            }
            MaterialesScreen(
                idAsignacion = idAsignacion,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // ─── REPORTES Y PROMEDIOS ──────────────────────────────────────────────
        composable(
            route = Screen.ReportesPromedios.route,
            arguments = listOf(navArgument("idAsignacion") { type = NavType.LongType })
        ) { backStackEntry ->
            val idAsignacion = backStackEntry.arguments?.getLong("idAsignacion") ?: 0L
            val viewModel = rememberCustomViewModel(key = "reportes_$idAsignacion") {
                ReportesViewModel(app.promediosRepository, app.docenteRepository)
            }
            ReportesPromediosScreen(
                idAsignacion = idAsignacion,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // ─── ESTADO DE SINCRONIZACIÓN ──────────────────────────────────────────
        composable(Screen.SyncStatus.route) {
            SyncStatusScreen(
                database = app.database,
                syncManager = app.syncManager,
                connectivityObserver = app.connectivityObserver,
                onBackClick = { navController.popBackStack() }
            )
        }

        // ─── AJUSTES / CONFIGURACIÓN DE SERVIDOR ──────────────────────────────
        composable(Screen.Settings.route) {
            SettingsScreen(
                sessionManager = app.sessionManager,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
