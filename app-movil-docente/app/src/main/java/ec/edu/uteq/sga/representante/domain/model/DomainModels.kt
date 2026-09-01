package ec.edu.uteq.sga.representante.domain.model

data class UserSession(
    val token: String,
    val idUsuario: Long,
    val username: String,
    val correo: String?,
    val roles: List<String>,
    val primerIngreso: Boolean = false
)

data class Representado(val idEstudiante: Long, val nombres: String, val apellidos: String,
    val curso: String?, val paralelo: String?, val matriculas: List<Long>) {
    val nombreCompleto get() = "$nombres $apellidos"
}
data class NotaRepresentado(val actividad: String, val periodo: String, val nota: Double, val notaCualitativa: String?)
data class PromedioRepresentado(val periodo: String, val promedioFormativo: Double,
    val notaSumativa: Double, val promedioTrimestral: Double, val notaCualitativa: String)
data class CalificacionesRepresentado(val calificaciones: List<NotaRepresentado>, val promedios: List<PromedioRepresentado>)
data class AsistenciaHijo(val fecha: String, val periodo: String, val estado: String)
data class ResumenAsistenciaHijo(val total: Int, val presentes: Int, val ausentes: Int,
    val justificados: Int, val atrasos: Int, val porcentajeAsistencia: Double)
data class AsistenciaRepresentado(val asistencias: List<AsistenciaHijo>, val resumen: ResumenAsistenciaHijo)

data class Asignacion(
    val idAsignacion: Long,
    val asignaturaNombre: String,
    val gradoNombre: String,
    val paraleloLetra: String,
    val anoLectivoNombre: String,
    val cantidadEstudiantes: Int = 0,
    val porcentajeAsistencia: Double? = null,
    val promedioCalificaciones: Double? = null
)

data class Estudiante(
    val idMatricula: Long,
    val idAsignacion: Long,
    val estudianteId: Long,
    val nombres: String,
    val apellidos: String,
    val cedula: String = "",
    val estadoMatricula: String = "ACTIVA"
) {
    val nombreCompleto: String get() = "$apellidos $nombres"
}

data class PeriodoEvaluacion(
    val idPeriodo: Long,
    val idAnoLectivo: Long,
    val tipo: String,
    val nombre: String,
    val fechaInicio: String,
    val fechaFin: String,
    val activo: Boolean = true
)

data class ActividadAcademica(
    val idActividad: Long,
    val idAsignacion: Long,
    val idPeriodo: Long,
    val tipo: String,
    val nombre: String,
    val descripcion: String? = null,
    val fechaEntrega: String,
    val ponderacion: Double = 0.0,
    val notaMaxima: Double = 10.0,
    val esSumativa: Boolean = false,
    val isPendingSync: Boolean = false
)

data class CalificacionEstudiante(
    val idCalificacion: Long,
    val idActividad: Long,
    val idMatricula: Long,
    val nota: Double,
    val notaCualitativa: String? = null,
    val observacion: String? = null,
    val isPendingSync: Boolean = false
)

enum class EstadoAsistenciaEnum(val label: String, val code: String) {
    PRESENTE("Presente", "PRESENTE"),
    AUSENTE("Ausente", "AUSENTE"),
    JUSTIFICADO("Justificado", "JUSTIFICADO"),
    ATRASO("Atraso", "ATRASO")
}

data class AsistenciaRegistro(
    val idAsistencia: Long,
    val idMatricula: Long,
    val idAsignacion: Long,
    val idPeriodo: Long,
    val fecha: String,
    val estado: String,
    val justificacion: String? = null,
    val isPendingSync: Boolean = false
)

data class ResumenAsistencia(
    val idResumen: Long,
    val idMatricula: Long,
    val idAsignacion: Long,
    val idPeriodo: Long,
    val totalPresentes: Int,
    val totalAusentes: Int,
    val totalJustificados: Int,
    val totalAtrasos: Int
) {
    val totalClases: Int get() = totalPresentes + totalAusentes + totalJustificados + totalAtrasos
    val porcentajeAsistencia: Double get() = if (totalClases > 0) {
        ((totalPresentes + totalAtrasos).toDouble() / totalClases) * 100.0
    } else 0.0
}

data class PromedioTrimestral(
    val idPromedio: Long,
    val idMatricula: Long,
    val idAsignacion: Long,
    val idPeriodo: Long,
    val promedioFormativo: Double,
    val notaSumativa: Double,
    val promedioTrimestral: Double,
    val notaCualitativa: String
)

data class PromedioAnual(
    val idPromedioAnual: Long,
    val idMatricula: Long,
    val idAsignacion: Long,
    val idAnoLectivo: Long,
    val promedioAnual: Double,
    val notaCualitativa: String
)

data class SeguimientoItem(
    val idSeguimiento: Long,
    val idMatricula: Long,
    val idPeriodo: Long,
    val categoria: String,
    val descripcion: String,
    val accionesTomadas: String? = null,
    val requiereFollowup: Boolean = false,
    val fechaEvento: String,
    val isPendingSync: Boolean = false
)

data class AnuncioCurso(
    val idAnuncio: Long,
    val idAsignacion: Long,
    val titulo: String?,
    val contenido: String?,
    val fecha: String?,
    val fijado: Boolean = false,
    val isPendingSync: Boolean = false
)

data class MaterialCurso(
    val idMaterial: Long,
    val idAsignacion: Long,
    val tipo: String?,
    val titulo: String?,
    val descripcion: String?,
    val url: String,
    val tamanoBytes: Long?,
    val fecha: String?,
    val isPendingSync: Boolean = false
)

data class HorarioItem(
    val idHorario: Long,
    val idAsignacion: Long,
    val diaSemana: Int, // 1=Lunes .. 5=Viernes
    val idPeriodo: Int,
    val horaInicio: String,
    val horaFin: String,
    val aula: String?,
    val asignatura: String,
    val docente: String,
    val grado: String,
    val paralelo: String
)
