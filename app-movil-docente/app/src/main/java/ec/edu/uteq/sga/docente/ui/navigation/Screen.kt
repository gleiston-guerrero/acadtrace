package ec.edu.uteq.sga.docente.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object Cursos : Screen("cursos")
    object DetalleCurso : Screen("detalle_curso/{idAsignacion}") {
        fun createRoute(idAsignacion: Long) = "detalle_curso/$idAsignacion"
    }
    object Actividades : Screen("actividades/{idAsignacion}") {
        fun createRoute(idAsignacion: Long) = "actividades/$idAsignacion"
    }
    object FormActividad : Screen("form_actividad/{idAsignacion}?idActividad={idActividad}") {
        fun createRoute(idAsignacion: Long, idActividad: Long? = null) =
            if (idActividad != null) "form_actividad/$idAsignacion?idActividad=$idActividad"
            else "form_actividad/$idAsignacion"
    }
    object Calificaciones : Screen("calificaciones/{idActividad}/{idAsignacion}?nombre={nombre}&max={max}") {
        fun createRoute(idActividad: Long, idAsignacion: Long, nombre: String, max: Double) =
            "calificaciones/$idActividad/$idAsignacion?nombre=$nombre&max=$max"
    }
    object Asistencia : Screen("asistencia/{idAsignacion}") {
        fun createRoute(idAsignacion: Long) = "asistencia/$idAsignacion"
    }
    object ResumenAsistencia : Screen("resumen_asistencia/{idAsignacion}") {
        fun createRoute(idAsignacion: Long) = "resumen_asistencia/$idAsignacion"
    }
    object AulaVirtualSemanas : Screen("aula_virtual_semanas/{idAsignacion}") {
        fun createRoute(idAsignacion: Long) = "aula_virtual_semanas/$idAsignacion"
    }
    object Horario : Screen("horario")
    object Seguimiento : Screen("seguimiento?idMatricula={idMatricula}") {
        fun createRoute(idMatricula: Long? = null) =
            if (idMatricula != null) "seguimiento?idMatricula=$idMatricula" else "seguimiento"
    }
    object FormSeguimiento : Screen("form_seguimiento/{idMatricula}?nombre={nombre}") {
        fun createRoute(idMatricula: Long, nombre: String) =
            "form_seguimiento/$idMatricula?nombre=$nombre"
    }
    object Anuncios : Screen("anuncios/{idAsignacion}") {
        fun createRoute(idAsignacion: Long) = "anuncios/$idAsignacion"
    }
    object Materiales : Screen("materiales/{idAsignacion}") {
        fun createRoute(idAsignacion: Long) = "materiales/$idAsignacion"
    }
    object ReportesPromedios : Screen("reportes_promedios/{idAsignacion}") {
        fun createRoute(idAsignacion: Long) = "reportes_promedios/$idAsignacion"
    }
    object SyncStatus : Screen("sync_status")
    object Settings : Screen("settings")
}
