package ec.edu.uteq.sga.representante.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RepresentadoDTO(val idEstudiante: Long, val nombres: String, val apellidos: String,
    val curso: String?, val paralelo: String?, val matriculas: List<Long> = emptyList())
data class CalificacionRepresentadoDTO(@SerializedName("id_calificacion") val idCalificacion: Long,
    @SerializedName("id_matricula") val idMatricula: Long, @SerializedName("id_actividad") val idActividad: Long,
    val actividad: String, @SerializedName("id_asignacion") val idAsignacion: Long,
    @SerializedName("id_periodo") val idPeriodo: Long, val periodo: String, val nota: Double,
    @SerializedName("nota_cualitativa") val notaCualitativa: String?)
data class PromedioRepresentadoDTO(@SerializedName("id_matricula") val idMatricula: Long,
    @SerializedName("id_asignacion") val idAsignacion: Long, @SerializedName("id_periodo") val idPeriodo: Long,
    val periodo: String, @SerializedName("promedio_formativo") val promedioFormativo: Double,
    @SerializedName("nota_sumativa") val notaSumativa: Double,
    @SerializedName("promedio_trimestral") val promedioTrimestral: Double,
    @SerializedName("nota_cualitativa") val notaCualitativa: String)
data class CalificacionesRepresentadoDTO(val calificaciones: List<CalificacionRepresentadoDTO>,
    val promedios: List<PromedioRepresentadoDTO>)
data class AsistenciaHijoDTO(@SerializedName("id_asistencia") val idAsistencia: Long,
    @SerializedName("id_matricula") val idMatricula: Long, @SerializedName("id_asignacion") val idAsignacion: Long,
    @SerializedName("id_periodo") val idPeriodo: Long, val periodo: String, val fecha: String, val estado: String)
data class ResumenAsistenciaHijoDTO(val total: Int, val presentes: Int, val ausentes: Int,
    val justificados: Int, val atrasos: Int, @SerializedName("porcentaje_asistencia") val porcentajeAsistencia: Double)
data class AsistenciaRepresentadoDTO(val asistencias: List<AsistenciaHijoDTO>, val resumen: ResumenAsistenciaHijoDTO)
data class ComunicadoDTO(val id: Long, val titulo: String, val contenido: String, val fecha: String, val fijado: Boolean)
