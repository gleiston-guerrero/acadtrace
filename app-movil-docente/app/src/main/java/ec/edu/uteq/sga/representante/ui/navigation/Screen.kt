package ec.edu.uteq.sga.representante.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Home : Screen("home_representante")
    data object BiometricUnlock : Screen("biometric_unlock")
    data object BiometricFallback : Screen("biometric_fallback")
    data object Security : Screen("security")
    data object MisRepresentados : Screen("mis_representados")
    data object ResumenRepresentado : Screen("representado/{id}/{nombre}") {
        fun create(id: Long, nombre: String) = "representado/$id/${java.net.URLEncoder.encode(nombre, Charsets.UTF_8.name())}"
    }
    data object Calificaciones : Screen("representado/{id}/calificaciones") { fun create(id: Long) = "representado/$id/calificaciones" }
    data object Asistencia : Screen("representado/{id}/asistencia") { fun create(id: Long) = "representado/$id/asistencia" }
}
