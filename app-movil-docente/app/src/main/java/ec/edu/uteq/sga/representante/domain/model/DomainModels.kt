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
data class NotaRepresentado(val idActividad: Long, val idAsignacion: Long, val idPeriodo: Long,
    val asignatura: String, val actividad: String, val periodo: String, val nota: Double?, val notaCualitativa: String?)
data class PromedioRepresentado(val idAsignacion: Long, val idPeriodo: Long, val asignatura: String, val periodo: String, val promedioFormativo: Double,
    val notaSumativa: Double, val promedioTrimestral: Double, val notaCualitativa: String)
data class PeriodoCalificaciones(val idPeriodo: Long, val nombre: String, val activo: Boolean, val fechaInicio: String)
data class PromedioAnualRepresentado(val idAsignacion: Long, val asignatura: String, val promedioAnual: Double, val notaCualitativa: String)
data class AsignaturaCalificaciones(val idAsignacion: Long, val asignatura: String, val promedio: PromedioRepresentado?, val actividades: List<NotaRepresentado>)
data class CalificacionesRepresentado(val calificaciones: List<NotaRepresentado>, val promedios: List<PromedioRepresentado>,
    val periodos: List<PeriodoCalificaciones> = emptyList(), val promediosAnuales: List<PromedioAnualRepresentado> = emptyList(),
    val mostrarPromediosAnuales: Boolean = false) {
    fun periodoInicial(): Long? = periodos.firstOrNull { it.activo }?.idPeriodo
        ?: periodos.maxByOrNull { it.fechaInicio }?.idPeriodo
    fun asignaturas(idPeriodo: Long): List<AsignaturaCalificaciones> {
        val periodPromedios = promedios.filter { it.idPeriodo == idPeriodo }.associateBy { it.idAsignacion }
        val periodNotas = calificaciones.filter { it.idPeriodo == idPeriodo }.groupBy { it.idAsignacion }
        return (periodPromedios.keys + periodNotas.keys).distinct().map { id ->
            val promedio = periodPromedios[id]
            val notas = periodNotas[id].orEmpty()
            AsignaturaCalificaciones(id, promedio?.asignatura ?: notas.firstOrNull()?.asignatura ?: "Asignatura $id", promedio, notas)
        }.sortedBy { it.asignatura }
    }
}

fun String.etiquetaCualitativa(): String = when (this) {
    "A_MAS" -> "A+"; "A_MENOS" -> "A-"; "B_MAS" -> "B+"; "B_MENOS" -> "B-"
    "C_MAS" -> "C+"; "C_MENOS" -> "C-"; else -> this
}
data class AsistenciaHijo(val fecha: String, val periodo: String, val estado: String)
data class ResumenAsistenciaHijo(val total: Int, val presentes: Int, val ausentes: Int,
    val justificados: Int, val atrasos: Int, val porcentajeAsistencia: Double)
data class AsistenciaRepresentado(val asistencias: List<AsistenciaHijo>, val resumen: ResumenAsistenciaHijo)
data class Comunicado(val id: Long, val titulo: String, val contenido: String, val fecha: String, val fijado: Boolean)

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
