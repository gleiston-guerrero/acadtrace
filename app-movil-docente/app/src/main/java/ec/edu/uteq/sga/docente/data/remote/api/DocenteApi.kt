package ec.edu.uteq.sga.docente.data.remote.api

import ec.edu.uteq.sga.docente.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface DocenteApi {

    // ─── CONTEXTO DOCENTE (SGA Principal) ──────────────────────────────────
    @GET("docentes/mis-asignaciones")
    suspend fun getMisAsignaciones(): Response<List<AsignacionDocenteDTO>>

    @GET("docentes/asignaciones/{id}/estudiantes")
    suspend fun getEstudiantesPorAsignacion(
        @Path("id") idAsignacion: Long
    ): Response<List<MatriculaEstudianteDTO>>

    @GET("docentes/ano-actual")
    suspend fun getAnoActual(): Response<AnoLectivoDTO>

    // ─── PERIODOS EVALUACIÓN ───────────────────────────────────────────────
    @GET("periodos-evaluacion/")
    suspend fun getPeriodosEvaluacion(): Response<List<PeriodoEvaluacionDTO>>

    // ─── ACTIVIDADES ───────────────────────────────────────────────────────
    @GET("actividades/")
    suspend fun getActividades(
        @Query("id_asignacion") idAsignacion: Long? = null,
        @Query("id_periodo") idPeriodo: Long? = null,
        @Query("es_sumativa") esSumativa: Boolean? = null
    ): Response<List<ActividadDTO>>

    @POST("actividades/")
    suspend fun createActividad(
        @Body actividad: ActividadCreateDTO
    ): Response<ActividadDTO>

    @GET("actividades/{id}/")
    suspend fun getActividad(
        @Path("id") id: Long
    ): Response<ActividadDTO>

    @PUT("actividades/{id}/")
    suspend fun updateActividad(
        @Path("id") id: Long,
        @Body actividad: ActividadCreateDTO
    ): Response<ActividadDTO>

    @DELETE("actividades/{id}/")
    suspend fun deleteActividad(
        @Path("id") id: Long
    ): Response<Unit>

    // ─── CALIFICACIONES ────────────────────────────────────────────────────
    @GET("calificaciones/")
    suspend fun getCalificaciones(
        @Query("id_matricula") idMatricula: Long? = null,
        @Query("id_actividad") idActividad: Long? = null,
        @Query("id_asignacion") idAsignacion: Long? = null,
        @Query("id_periodo") idPeriodo: Long? = null
    ): Response<List<CalificacionDTO>>

    @POST("calificaciones/")
    suspend fun createCalificacion(
        @Body calificacion: CalificacionCreateDTO
    ): Response<CalificacionDTO>

    @PUT("calificaciones/{id}/")
    suspend fun updateCalificacion(
        @Path("id") id: Long,
        @Body calificacion: CalificacionCreateDTO
    ): Response<CalificacionDTO>

    @GET("calificaciones/promedio-formativo/")
    suspend fun getPromedioFormativo(
        @Query("id_matricula") idMatricula: Long,
        @Query("id_asignacion") idAsignacion: Long,
        @Query("id_periodo") idPeriodo: Long,
        @Query("nivel") nivel: String? = "EGB"
    ): Response<PromedioFormativoResponseDTO>

    // ─── ASISTENCIAS ───────────────────────────────────────────────────────
    @GET("asistencias/")
    suspend fun getAsistencias(
        @Query("id_matricula") idMatricula: Long? = null,
        @Query("id_asignacion") idAsignacion: Long? = null,
        @Query("id_periodo") idPeriodo: Long? = null,
        @Query("fecha") fecha: String? = null,
        @Query("estado") estado: String? = null
    ): Response<List<AsistenciaDTO>>

    @POST("asistencias/")
    suspend fun createAsistencia(
        @Body asistencia: AsistenciaCreateDTO
    ): Response<AsistenciaDTO>

    @PUT("asistencias/{id}/")
    suspend fun updateAsistencia(
        @Path("id") id: Long,
        @Body asistencia: AsistenciaCreateDTO
    ): Response<AsistenciaDTO>

    @GET("resumen-asistencia/")
    suspend fun getResumenAsistencia(
        @Query("id_matricula") idMatricula: Long? = null,
        @Query("id_asignacion") idAsignacion: Long? = null,
        @Query("id_periodo") idPeriodo: Long? = null
    ): Response<List<ResumenAsistenciaDTO>>

    @POST("resumen-asistencia/calcular/")
    suspend fun calcularResumenAsistencia(
        @Body body: Map<String, Long>
    ): Response<ResumenAsistenciaDTO>

    // ─── PROMEDIOS TRIMESTRALES Y ANUALES ──────────────────────────────────
    @GET("promedios-trimestrales/")
    suspend fun getPromediosTrimestrales(
        @Query("id_matricula") idMatricula: Long? = null,
        @Query("id_asignacion") idAsignacion: Long? = null,
        @Query("id_periodo") idPeriodo: Long? = null
    ): Response<List<PromedioTrimestralDTO>>

    @POST("promedios-trimestrales/calcular/")
    suspend fun calcularPromedioTrimestral(
        @Body body: Map<String, Any>
    ): Response<PromedioTrimestralDTO>

    @GET("promedios-anuales/")
    suspend fun getPromediosAnuales(
        @Query("id_matricula") idMatricula: Long? = null,
        @Query("id_asignacion") idAsignacion: Long? = null,
        @Query("id_ano_lectivo") idAnoLectivo: Long? = null
    ): Response<List<PromedioAnualDTO>>

    @POST("promedios-anuales/calcular/")
    suspend fun calcularPromedioAnual(
        @Body body: Map<String, Any>
    ): Response<PromedioAnualDTO>

    @GET("promedios-anuales-detalle/")
    suspend fun getPromediosAnualesDetalle(): Response<List<PromedioAnualDetalleDTO>>

    // ─── SEGUIMIENTO ACADÉMICO ─────────────────────────────────────────────
    @GET("seguimiento-academico/")
    suspend fun getSeguimientoAcademico(
        @Query("id_matricula") idMatricula: Long? = null,
        @Query("id_periodo") idPeriodo: Long? = null,
        @Query("categoria") categoria: String? = null,
        @Query("requiere_followup") requiereFollowup: Boolean? = null
    ): Response<List<SeguimientoAcademicoDTO>>

    @POST("seguimiento-academico/")
    suspend fun createSeguimientoAcademico(
        @Body seguimiento: SeguimientoCreateDTO
    ): Response<SeguimientoAcademicoDTO>

    // ─── ANUNCIOS ──────────────────────────────────────────────────────────
    @GET("anuncios/")
    suspend fun getAnuncios(
        @Query("id_asignacion") idAsignacion: Long? = null
    ): Response<List<AnuncioDTO>>

    @POST("anuncios/")
    suspend fun createAnuncio(
        @Body anuncio: AnuncioCreateDTO
    ): Response<AnuncioDTO>

    @DELETE("anuncios/{id}/")
    suspend fun deleteAnuncio(
        @Path("id") id: Long
    ): Response<Unit>

    // ─── MATERIALES ────────────────────────────────────────────────────────
    @GET("materiales/")
    suspend fun getMateriales(
        @Query("id_asignacion") idAsignacion: Long? = null
    ): Response<List<MaterialDTO>>

    @POST("materiales/")
    suspend fun createMaterial(
        @Body material: MaterialCreateDTO
    ): Response<MaterialDTO>

    @DELETE("materiales/{id}/")
    suspend fun deleteMaterial(
        @Path("id") id: Long
    ): Response<Unit>

    // ─── AULA VIRTUAL ──────────────────────────────────────────────────────
    @GET("aula-virtual/resumen/")
    suspend fun getAulaVirtualResumen(
        @Query("id_asignacion") idsAsignacion: List<Long>
    ): Response<AulaVirtualResumenResponseDTO>

    @GET("aula-virtual/{id_asignacion}/semanas/")
    suspend fun getAulaVirtualSemanas(
        @Path("id_asignacion") idAsignacion: Long
    ): Response<AulaVirtualSemanasResponseDTO>

    // ─── HORARIOS (SGA Principal) ──────────────────────────────────────────
    @GET("horarios/docente/{idPersona}/grilla")
    suspend fun getHorarioDocente(
        @Path("idPersona") idPersona: Long
    ): Response<HorarioGrillaResponseDTO>

    @GET("horarios/curso/{idAsignacion}/grilla")
    suspend fun getHorarioCurso(
        @Path("idAsignacion") idAsignacion: Long
    ): Response<HorarioGrillaResponseDTO>
}
