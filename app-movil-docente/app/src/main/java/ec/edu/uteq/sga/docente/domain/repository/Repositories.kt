package ec.edu.uteq.sga.docente.domain.repository

import ec.edu.uteq.sga.docente.core.Resource
import ec.edu.uteq.sga.docente.data.remote.dto.*
import ec.edu.uteq.sga.docente.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(username: String, password: String): Resource<UserSession>
    fun logout()
    fun isUserLoggedIn(): Boolean
    fun isDocente(): Boolean
    fun getTeacherUserId(): Long
}

interface DocenteRepository {
    fun getAsignaciones(): Flow<Resource<List<Asignacion>>>
    fun getEstudiantesPorAsignacion(idAsignacion: Long): Flow<Resource<List<Estudiante>>>
    fun getPeriodosEvaluacion(): Flow<Resource<List<PeriodoEvaluacion>>>
    suspend fun refreshDocenteContext(): Resource<Unit>
}

interface ActividadesRepository {
    fun getActividades(idAsignacion: Long, idPeriodo: Long?): Flow<Resource<List<ActividadAcademica>>>
    suspend fun createActividad(actividad: ActividadCreateDTO): Resource<ActividadAcademica>
    suspend fun updateActividad(id: Long, actividad: ActividadCreateDTO): Resource<ActividadAcademica>
    suspend fun deleteActividad(id: Long): Resource<Unit>
}

interface CalificacionesRepository {
    fun getCalificaciones(idActividad: Long): Flow<Resource<List<CalificacionEstudiante>>>
    suspend fun saveCalificacion(calificacion: CalificacionCreateDTO, idCalificacion: Long? = null): Resource<CalificacionEstudiante>
    suspend fun getPromedioFormativo(idMatricula: Long, idAsignacion: Long, idPeriodo: Long): Resource<PromedioFormativoResponseDTO>
}

interface AsistenciasRepository {
    fun getAsistenciasPorFecha(idAsignacion: Long, fecha: String): Flow<Resource<List<AsistenciaRegistro>>>
    suspend fun saveAsistencia(asistencia: AsistenciaCreateDTO, idAsistencia: Long? = null): Resource<AsistenciaRegistro>
    fun getResumenAsistencia(idAsignacion: Long, idPeriodo: Long?): Flow<Resource<List<ResumenAsistencia>>>
    suspend fun calcularResumen(idMatricula: Long, idAsignacion: Long, idPeriodo: Long): Resource<ResumenAsistencia>
}

interface PromediosRepository {
    fun getPromediosTrimestrales(idAsignacion: Long, idPeriodo: Long?): Flow<Resource<List<PromedioTrimestral>>>
    suspend fun calcularPromedioTrimestral(idMatricula: Long, idAsignacion: Long, idPeriodo: Long): Resource<PromedioTrimestral>
    fun getPromediosAnuales(idAsignacion: Long): Flow<Resource<List<PromedioAnual>>>
    suspend fun calcularPromedioAnual(idMatricula: Long, idAsignacion: Long, idAnoLectivo: Long): Resource<PromedioAnual>
}

interface SeguimientoRepository {
    fun getSeguimientos(idMatricula: Long? = null): Flow<Resource<List<SeguimientoItem>>>
    suspend fun createSeguimiento(seguimiento: SeguimientoCreateDTO): Resource<SeguimientoItem>
}

interface AnunciosRepository {
    fun getAnuncios(idAsignacion: Long): Flow<Resource<List<AnuncioCurso>>>
    suspend fun createAnuncio(anuncio: AnuncioCreateDTO): Resource<AnuncioCurso>
    suspend fun deleteAnuncio(id: Long): Resource<Unit>
}

interface MaterialesRepository {
    fun getMateriales(idAsignacion: Long): Flow<Resource<List<MaterialCurso>>>
    suspend fun createMaterial(material: MaterialCreateDTO): Resource<MaterialCurso>
    suspend fun deleteMaterial(id: Long): Resource<Unit>
}

interface AulaVirtualRepository {
    suspend fun getResumenCursos(idsAsignacion: List<Long>): Resource<AulaVirtualResumenResponseDTO>
    suspend fun getAgendaSemanas(idAsignacion: Long): Resource<AulaVirtualSemanasResponseDTO>
}

interface HorarioRepository {
    fun getHorarios(idPersona: Long? = null, idAsignacion: Long? = null): Flow<Resource<List<HorarioItem>>>
    suspend fun refreshHorarios(idPersona: Long): Resource<Unit>
}
