package ec.edu.uteq.sga.representante.data.remote.dto

import com.google.gson.annotations.SerializedName

// ─── PERIODOS EVALUACIÓN ───────────────────────────────────────────────
data class PeriodoEvaluacionDTO(
    @SerializedName("id_periodo") val idPeriodo: Long,
    @SerializedName("id_ano_lectivo") val idAnoLectivo: Long,
    @SerializedName("tipo") val tipo: String, // PRIMER_TRIMESTRE, SEGUNDO_TRIMESTRE, TERCER_TRIMESTRE
    @SerializedName("nombre") val nombre: String,
    @SerializedName("fecha_inicio") val fechaInicio: String,
    @SerializedName("fecha_fin") val fechaFin: String,
    @SerializedName("activo") val activo: Boolean = true
)

// ─── ACTIVIDADES ───────────────────────────────────────────────────────
data class ActividadDTO(
    @SerializedName("id_actividad") val idActividad: Long,
    @SerializedName("id_asignacion") val idAsignacion: Long,
    @SerializedName("id_periodo") val idPeriodo: Long,
    @SerializedName("tipo") val tipo: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("fecha_entrega") val fechaEntrega: String,
    @SerializedName("ponderacion") val ponderacion: Double = 0.0,
    @SerializedName("nota_maxima") val notaMaxima: Double = 10.0,
    @SerializedName("es_sumativa") val esSumativa: Boolean = false,
    @SerializedName("fecha_creacion") val fechaCreacion: String? = null
)

data class ActividadCreateDTO(
    @SerializedName("id_asignacion") val idAsignacion: Long,
    @SerializedName("id_periodo") val idPeriodo: Long,
    @SerializedName("tipo") val tipo: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("fecha_entrega") val fechaEntrega: String,
    @SerializedName("ponderacion") val ponderacion: Double = 0.0,
    @SerializedName("nota_maxima") val notaMaxima: Double = 10.0,
    @SerializedName("es_sumativa") val esSumativa: Boolean = false
)

// ─── CALIFICACIONES ────────────────────────────────────────────────────
data class CalificacionDTO(
    @SerializedName("id_calificacion") val idCalificacion: Long,
    @SerializedName("id_actividad") val idActividad: Long,
    @SerializedName("id_matricula") val idMatricula: Long,
    @SerializedName("nota") val nota: Double,
    @SerializedName("nota_cualitativa") val notaCualitativa: String? = null,
    @SerializedName("observacion") val observacion: String? = null,
    @SerializedName("registrado_por") val registradoPor: Long? = null,
    @SerializedName("fecha_registro") val fechaRegistro: String? = null,
    @SerializedName("fecha_actualizacion") val fechaActualizacion: String? = null
)

data class CalificacionCreateDTO(
    @SerializedName("id_actividad") val idActividad: Long,
    @SerializedName("id_matricula") val idMatricula: Long,
    @SerializedName("nota") val nota: Double,
    @SerializedName("observacion") val observacion: String? = null,
    @SerializedName("registrado_por") val registradoPor: Long? = null,
    @SerializedName("nivel") val nivel: String = "EGB"
)

data class PromedioFormativoResponseDTO(
    @SerializedName("id_matricula") val idMatricula: Long,
    @SerializedName("id_asignacion") val idAsignacion: Long,
    @SerializedName("id_periodo") val idPeriodo: Long,
    @SerializedName("promedio_formativo") val promedioFormativo: Double,
    @SerializedName("nota_cualitativa") val notaCualitativa: String
)

// ─── ASISTENCIAS ───────────────────────────────────────────────────────
data class AsistenciaDTO(
    @SerializedName("id_asistencia") val idAsistencia: Long,
    @SerializedName("id_matricula") val idMatricula: Long,
    @SerializedName("id_asignacion") val idAsignacion: Long,
    @SerializedName("id_periodo") val idPeriodo: Long,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("estado") val estado: String, // PRESENTE, AUSENTE, JUSTIFICADO, ATRASO
    @SerializedName("justificacion") val justificacion: String? = null,
    @SerializedName("registrado_por") val registradoPor: Long? = null,
    @SerializedName("fecha_registro") val fechaRegistro: String? = null,
    @SerializedName("fecha_actualizacion") val fechaActualizacion: String? = null
)

data class AsistenciaCreateDTO(
    @SerializedName("id_matricula") val idMatricula: Long,
    @SerializedName("id_asignacion") val idAsignacion: Long,
    @SerializedName("id_periodo") val idPeriodo: Long,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("justificacion") val justificacion: String? = null,
    @SerializedName("registrado_por") val registradoPor: Long? = null
)

data class ResumenAsistenciaDTO(
    @SerializedName("id_resumen") val idResumen: Long = 0,
    @SerializedName("id_matricula") val idMatricula: Long,
    @SerializedName("id_asignacion") val idAsignacion: Long,
    @SerializedName("id_periodo") val idPeriodo: Long,
    @SerializedName("total_presentes") val totalPresentes: Int = 0,
    @SerializedName("total_ausentes") val totalAusentes: Int = 0,
    @SerializedName("total_justificados") val totalJustificados: Int = 0,
    @SerializedName("total_atrasos") val totalAtrasos: Int = 0,
    @SerializedName("calculado_en") val calculadoEn: String? = null
)

// ─── PROMEDIOS ─────────────────────────────────────────────────────────
data class PromedioTrimestralDTO(
    @SerializedName("id_promedio") val idPromedio: Long = 0,
    @SerializedName("id_matricula") val idMatricula: Long,
    @SerializedName("id_asignacion") val idAsignacion: Long,
    @SerializedName("id_periodo") val idPeriodo: Long,
    @SerializedName("promedio_formativo") val promedioFormativo: Double = 0.0,
    @SerializedName("nota_sumativa") val notaSumativa: Double = 0.0,
    @SerializedName("promedio_trimestral") val promedioTrimestral: Double = 0.0,
    @SerializedName("nota_cualitativa") val notaCualitativa: String = "D",
    @SerializedName("calculado_en") val calculadoEn: String? = null
)

data class PromedioAnualDTO(
    @SerializedName("id_promedio_anual") val idPromedioAnual: Long = 0,
    @SerializedName("id_matricula") val idMatricula: Long,
    @SerializedName("id_asignacion") val idAsignacion: Long,
    @SerializedName("id_ano_lectivo") val idAnoLectivo: Long,
    @SerializedName("promedio_anual") val promedioAnual: Double = 0.0,
    @SerializedName("nota_cualitativa") val notaCualitativa: String = "D",
    @SerializedName("registrado_por") val registradoPor: Long? = null,
    @SerializedName("calculado_en") val calculadoEn: String? = null
)

data class PromedioAnualDetalleDTO(
    @SerializedName("id_detalle") val idDetalle: Long = 0,
    @SerializedName("id_promedio_anual") val idPromedioAnual: Long,
    @SerializedName("id_promedio_trim") val idPromedioTrim: Long
)

// ─── SEGUIMIENTO ACADÉMICO ─────────────────────────────────────────────
data class SeguimientoAcademicoDTO(
    @SerializedName("id_seguimiento") val idSeguimiento: Long,
    @SerializedName("id_matricula") val idMatricula: Long,
    @SerializedName("id_periodo") val idPeriodo: Long,
    @SerializedName("categoria") val categoria: String, // ACADEMICO, CONDUCTUAL, DECE, MEDICO, FAMILIAR, OTRO
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("acciones_tomadas") val accionesTomadas: String? = null,
    @SerializedName("requiere_followup") val requiereFollowup: Boolean = false,
    @SerializedName("fecha_evento") val fechaEvento: String,
    @SerializedName("registrado_por") val registradoPor: Long? = null,
    @SerializedName("fecha_registro") val fechaRegistro: String? = null
)

data class SeguimientoCreateDTO(
    @SerializedName("id_matricula") val idMatricula: Long,
    @SerializedName("id_periodo") val idPeriodo: Long,
    @SerializedName("categoria") val categoria: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("acciones_tomadas") val accionesTomadas: String? = null,
    @SerializedName("requiere_followup") val requiereFollowup: Boolean = false,
    @SerializedName("fecha_evento") val fechaEvento: String,
    @SerializedName("registrado_por") val registradoPor: Long
)

// ─── ANUNCIOS ──────────────────────────────────────────────────────────
data class AnuncioDTO(
    @SerializedName("id_anuncio") val idAnuncio: Long,
    @SerializedName("id_asignacion") val idAsignacion: Long,
    @SerializedName("titulo") val titulo: String?,
    @SerializedName("contenido") val contenido: String?,
    @SerializedName("autor_id") val autorId: Long? = null,
    @SerializedName("fecha") val fecha: String? = null,
    @SerializedName("fijado") val fijado: Boolean = false
)

data class AnuncioCreateDTO(
    @SerializedName("id_asignacion") val idAsignacion: Long,
    @SerializedName("titulo") val titulo: String,
    @SerializedName("contenido") val contenido: String,
    @SerializedName("autor_id") val autorId: Long? = null,
    @SerializedName("fijado") val fijado: Boolean = false
)

// ─── MATERIALES ────────────────────────────────────────────────────────
data class MaterialDTO(
    @SerializedName("id_material") val idMaterial: Long,
    @SerializedName("id_asignacion") val idAsignacion: Long,
    @SerializedName("tipo") val tipo: String? = null,
    @SerializedName("titulo") val titulo: String?,
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("url") val url: String,
    @SerializedName("tamano_bytes") val tamanoBytes: Long? = null,
    @SerializedName("fecha") val fecha: String? = null
)

data class MaterialCreateDTO(
    @SerializedName("id_asignacion") val idAsignacion: Long,
    @SerializedName("tipo") val tipo: String? = "ENLACE",
    @SerializedName("titulo") val titulo: String,
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("url") val url: String
)

// ─── AULA VIRTUAL ──────────────────────────────────────────────────────
data class PeriodoSimpleDTO(
    @SerializedName("id_periodo") val idPeriodo: Long,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("fecha_inicio") val fechaInicio: String,
    @SerializedName("fecha_fin") val fechaFin: String
)

data class CursoResumenDTO(
    @SerializedName("id_asignacion") val idAsignacion: Long,
    @SerializedName("id_ano_lectivo") val idAnoLectivo: Long,
    @SerializedName("promedio_curso") val promedioCurso: Double? = null,
    @SerializedName("porcentaje_asistencia") val porcentajeAsistencia: Double? = null,
    @SerializedName("registros_asistencia") val registrosAsistencia: Int = 0,
    @SerializedName("indicador_minimos") val indicadorMinimos: String? = null,
    @SerializedName("fecha_inicio") val fechaInicio: String? = null,
    @SerializedName("fecha_fin") val fechaFin: String? = null,
    @SerializedName("periodos") val periodos: List<PeriodoSimpleDTO> = emptyList()
)

data class AulaVirtualResumenResponseDTO(
    @SerializedName("cursos") val cursos: List<CursoResumenDTO> = emptyList()
)

data class ActividadResumenDTO(
    @SerializedName("id_actividad") val idActividad: Long,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("tipo") val tipo: String,
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("fecha_entrega") val fechaEntrega: String,
    @SerializedName("ponderacion") val ponderacion: Double = 0.0,
    @SerializedName("nota_maxima") val notaMaxima: Double = 10.0,
    @SerializedName("es_sumativa") val esSumativa: Boolean = false
)

data class AsistenciaResumenDTO(
    @SerializedName("id_asistencia") val idAsistencia: Long,
    @SerializedName("id_matricula") val idMatricula: Long,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("justificacion") val justificacion: String? = null
)

data class CalificacionResumenDTO(
    @SerializedName("id_calificacion") val idCalificacion: Long,
    @SerializedName("id_matricula") val idMatricula: Long,
    @SerializedName("id_actividad") val idActividad: Long,
    @SerializedName("actividad") val actividad: String? = null,
    @SerializedName("nota") val nota: Double,
    @SerializedName("nota_cualitativa") val notaCualitativa: String? = null,
    @SerializedName("fecha_registro") val fechaRegistro: String? = null
)

data class SemanaDTO(
    @SerializedName("numero") val numero: Int,
    @SerializedName("fecha_inicio") val fechaInicio: String,
    @SerializedName("fecha_fin") val fechaFin: String,
    @SerializedName("actividades") val actividades: List<ActividadResumenDTO> = emptyList(),
    @SerializedName("asistencias") val asistencias: List<AsistenciaResumenDTO> = emptyList(),
    @SerializedName("materiales") val materiales: List<MaterialDTO> = emptyList(),
    @SerializedName("anuncios") val anuncios: List<AnuncioDTO> = emptyList(),
    @SerializedName("calificaciones") val calificaciones: List<CalificacionResumenDTO> = emptyList()
)

data class TrimestreSemanasDTO(
    @SerializedName("id_periodo") val idPeriodo: Long,
    @SerializedName("trimestre") val trimestre: String,
    @SerializedName("tipo") val tipo: String,
    @SerializedName("fecha_inicio") val fechaInicio: String,
    @SerializedName("fecha_fin") val fechaFin: String,
    @SerializedName("semanas") val semanas: List<SemanaDTO> = emptyList()
)

data class PendienteDTO(
    @SerializedName("tipo") val tipo: String,
    @SerializedName("titulo") val titulo: String,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("dias_restantes") val diasRestantes: Int? = null
)

data class AulaVirtualSemanasResponseDTO(
    @SerializedName("trimestres") val trimestres: List<TrimestreSemanasDTO> = emptyList(),
    @SerializedName("pendientes") val pendientes: List<PendienteDTO> = emptyList()
)

// ─── HORARIOS ──────────────────────────────────────────────────────────
data class PeriodoHorarioDTO(
    @SerializedName("idPeriodo") val idPeriodo: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("horaInicio") val horaInicio: String,
    @SerializedName("horaFin") val horaFin: String,
    @SerializedName("orden") val orden: Int,
    @SerializedName("activo") val activo: Boolean = true
)

data class HorarioSlotDTO(
    @SerializedName("idHorario") val idHorario: Long,
    @SerializedName("idAsignacion") val idAsignacion: Long,
    @SerializedName("diaSemana") val diaSemana: Int, // 1=Lunes .. 5=Viernes
    @SerializedName("idPeriodo") val idPeriodo: Int,
    @SerializedName("horaInicio") val horaInicio: String,
    @SerializedName("horaFin") val horaFin: String,
    @SerializedName("aula") val aula: String? = null,
    @SerializedName("asignatura") val asignatura: String,
    @SerializedName("docente") val docente: String,
    @SerializedName("grado") val grado: String,
    @SerializedName("paralelo") val paralelo: String
)

data class HorarioGrillaResponseDTO(
    @SerializedName("periodos") val periodos: List<PeriodoHorarioDTO> = emptyList(),
    @SerializedName("slots") val slots: List<HorarioSlotDTO> = emptyList(),
    @SerializedName("totalHoras") val totalHoras: Int? = null
)
