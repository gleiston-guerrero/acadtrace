package ec.edu.uteq.sga.representante.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import ec.edu.uteq.sga.representante.SgaRepresentanteApp
import ec.edu.uteq.sga.representante.ui.screens.login.LoginScreen
import ec.edu.uteq.sga.representante.ui.screens.login.LoginViewModel
import ec.edu.uteq.sga.representante.ui.screens.representante.*
import ec.edu.uteq.sga.representante.ui.screens.security.*
import ec.edu.uteq.sga.representante.data.sync.SyncWorker

@Composable
fun RepresentanteNavGraph(nav: NavHostController, app: SgaRepresentanteApp, startDestination: String) {
    NavHost(nav, startDestination) {
        composable(Screen.Login.route) {
            val vm: LoginViewModel = factory { LoginViewModel(app.authRepository) }
            LoginScreen(vm, onLoginSuccess = { nav.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } } })
        }
        composable(Screen.BiometricUnlock.route) {
            BiometricUnlockScreen(app.sessionManager, onUnlocked = {
                nav.navigate(Screen.Home.route) { popUpTo(Screen.BiometricUnlock.route) { inclusive = true } }
            }, onUseLogin = {
                nav.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
            })
        }
        composable(Screen.BiometricFallback.route) {
            BiometricFallbackScreen(app.sessionManager) {
                nav.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
            }
        }
        composable(Screen.Home.route) {
            HomeRepresentante(onRepresentados = { nav.navigate(Screen.MisRepresentados.route) }, onComunicados = {
                nav.navigate(Screen.Comunicados.route)
            }, onSecurity = {
                nav.navigate(Screen.Security.route)
            }, onLogout = {
                app.authRepository.logout(); nav.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
            })
        }
        composable(Screen.Security.route) {
            SecurityScreen(app.sessionManager) { nav.popBackStack() }
        }
        composable(Screen.Comunicados.route) {
            val vm: RepresentanteViewModel = factory("comunicados") { RepresentanteViewModel(app.representanteRepository) }
            ComunicadosRepresentanteScreen(vm) { nav.popBackStack() }
        }
        composable(Screen.MisRepresentados.route) {
            val vm: RepresentanteViewModel = factory { RepresentanteViewModel(app.representanteRepository) }
            MisRepresentadosScreen(vm, { nav.popBackStack() }) { nav.navigate(Screen.ResumenRepresentado.create(it.idEstudiante, it.nombreCompleto)) }
        }
        composable(Screen.ResumenRepresentado.route, listOf(navArgument("id") { type = NavType.LongType }, navArgument("nombre") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            val nombre = java.net.URLDecoder.decode(entry.arguments?.getString("nombre").orEmpty(), Charsets.UTF_8.name())
            ResumenRepresentadoScreen(nombre, { nav.popBackStack() },
                { nav.navigate(Screen.Calificaciones.create(id)) }, { nav.navigate(Screen.Asistencia.create(id)) })
        }
        composable(Screen.Calificaciones.route, listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            val vm: RepresentanteViewModel = factory("calificaciones_$id") { RepresentanteViewModel(app.representanteRepository) }
            LaunchedEffect(id) { vm.cargarCalificaciones(id) }
            ConsultaCalificacionesRepresentante(vm, { nav.popBackStack() }) {
                vm.cargarCalificaciones(id)
                SyncWorker.triggerImmediateSync(app)
            }
        }
        composable(Screen.Asistencia.route, listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            val vm: RepresentanteViewModel = factory("asistencia_$id") { RepresentanteViewModel(app.representanteRepository) }
            LaunchedEffect(id) { vm.cargarAsistencia(id) }
            AsistenciaHijoScreen(vm, { nav.popBackStack() }) {
                vm.cargarAsistencia(id)
                SyncWorker.triggerImmediateSync(app)
            }
        }
    }
}

@Composable private inline fun <reified T : ViewModel> factory(key: String? = null, crossinline create: () -> T): T =
    viewModel(key = key, factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <R : ViewModel> create(modelClass: Class<R>): R = create() as R
    })
