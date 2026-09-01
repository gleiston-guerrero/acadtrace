package ec.edu.uteq.sga.representante.data.local.dao

import androidx.room.*
import ec.edu.uteq.sga.representante.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RepresentanteCacheDao {
    @Query("SELECT * FROM representados_cache ORDER BY idEstudiante")
    suspend fun getRepresentados(): List<RepresentadoCacheEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putRepresentados(items: List<RepresentadoCacheEntity>)
    @Query("SELECT * FROM calificaciones_representado_cache WHERE idEstudiante = :id")
    suspend fun getCalificaciones(id: Long): CalificacionesRepresentadoCacheEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCalificaciones(item: CalificacionesRepresentadoCacheEntity)
    @Query("SELECT * FROM asistencia_hijo_cache WHERE idEstudiante = :id")
    suspend fun getAsistencia(id: Long): AsistenciaHijoCacheEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAsistencia(item: AsistenciaHijoCacheEntity)
}

@Dao
interface AsignacionDao {
    @Query("SELECT * FROM asignaciones ORDER BY gradoNombre, paraleloLetra, asignaturaNombre")
    fun getAllAsignaciones(): Flow<List<AsignacionEntity>>

    @Query("SELECT * FROM asignaciones WHERE idAsignacion = :id")
    suspend fun getAsignacionById(id: Long): AsignacionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsignaciones(asignaciones: List<AsignacionEntity>)

    @Query("DELETE FROM asignaciones")
    suspend fun clearAsignaciones()
}

@Dao
interface EstudianteDao {
    @Query("SELECT * FROM estudiantes WHERE idAsignacion = :idAsignacion ORDER BY apellidos, nombres")
    fun getEstudiantesByAsignacion(idAsignacion: Long): Flow<List<EstudianteEntity>>

    @Query("SELECT * FROM estudiantes WHERE idMatricula = :idMatricula")
    suspend fun getEstudianteByMatricula(idMatricula: Long): EstudianteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEstudiantes(estudiantes: List<EstudianteEntity>)

    @Query("DELETE FROM estudiantes WHERE idAsignacion = :idAsignacion")
    suspend fun clearEstudiantesByAsignacion(idAsignacion: Long)
}

@Dao
interface PeriodoDao {
    @Query("SELECT * FROM periodos WHERE activo = 1 ORDER BY fechaInicio")
    fun getPeriodosActivos(): Flow<List<PeriodoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriodos(periodos: List<PeriodoEntity>)
}

@Dao
interface ActividadDao {
    @Query("SELECT * FROM actividades WHERE idAsignacion = :idAsignacion AND (:idPeriodo IS NULL OR idPeriodo = :idPeriodo) ORDER BY fechaEntrega DESC")
    fun getActividades(idAsignacion: Long, idPeriodo: Long?): Flow<List<ActividadEntity>>

    @Query("SELECT * FROM actividades WHERE idActividad = :id")
    suspend fun getActividadById(id: Long): ActividadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActividades(actividades: List<ActividadEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActividad(actividad: ActividadEntity)

    @Delete
    suspend fun deleteActividad(actividad: ActividadEntity)

    @Query("DELETE FROM actividades WHERE idActividad = :id")
    suspend fun deleteActividadById(id: Long)

    @Query("UPDATE actividades SET idActividad = :newRemoteId, isPendingSync = 0 WHERE idActividad = :localTempId")
    suspend fun updateActividadRemoteId(localTempId: Long, newRemoteId: Long)
}

@Dao
interface CalificacionDao {
    @Query("SELECT * FROM calificaciones WHERE idActividad = :idActividad")
    fun getCalificacionesByActividad(idActividad: Long): Flow<List<CalificacionEntity>>

    @Query("SELECT * FROM calificaciones WHERE idActividad = :idActividad AND idMatricula = :idMatricula")
    suspend fun getCalificacion(idActividad: Long, idMatricula: Long): CalificacionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalificaciones(calificaciones: List<CalificacionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalificacion(calificacion: CalificacionEntity)

    @Query("UPDATE calificaciones SET idCalificacion = :newRemoteId, isPendingSync = 0 WHERE idCalificacion = :localTempId")
    suspend fun updateCalificacionRemoteId(localTempId: Long, newRemoteId: Long)
}

@Dao
interface AsistenciaDao {
    @Query("SELECT * FROM asistencias WHERE idAsignacion = :idAsignacion AND fecha = :fecha")
    fun getAsistenciasPorFecha(idAsignacion: Long, fecha: String): Flow<List<AsistenciaEntity>>

    @Query("SELECT * FROM asistencias WHERE idMatricula = :idMatricula AND idAsignacion = :idAsignacion AND fecha = :fecha")
    suspend fun getAsistenciaAlumno(idMatricula: Long, idAsignacion: Long, fecha: String): AsistenciaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsistencias(asistencias: List<AsistenciaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsistencia(asistencia: AsistenciaEntity)

    @Query("UPDATE asistencias SET idAsistencia = :newRemoteId, isPendingSync = 0 WHERE idAsistencia = :localTempId")
    suspend fun updateAsistenciaRemoteId(localTempId: Long, newRemoteId: Long)
}

@Dao
interface ResumenAsistenciaDao {
    @Query("SELECT * FROM resumenes_asistencia WHERE idAsignacion = :idAsignacion AND (:idPeriodo IS NULL OR idPeriodo = :idPeriodo)")
    fun getResumenesAsistencia(idAsignacion: Long, idPeriodo: Long?): Flow<List<ResumenAsistenciaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResumenes(resumenes: List<ResumenAsistenciaEntity>)
}

@Dao
interface PromediosDao {
    @Query("SELECT * FROM promedios_trimestrales WHERE idAsignacion = :idAsignacion AND (:idPeriodo IS NULL OR idPeriodo = :idPeriodo)")
    fun getPromediosTrimestrales(idAsignacion: Long, idPeriodo: Long?): Flow<List<PromedioTrimestralEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromediosTrimestrales(promedios: List<PromedioTrimestralEntity>)

    @Query("SELECT * FROM promedios_anuales WHERE idAsignacion = :idAsignacion")
    fun getPromediosAnuales(idAsignacion: Long): Flow<List<PromedioAnualEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromediosAnuales(promedios: List<PromedioAnualEntity>)
}

@Dao
interface SeguimientoDao {
    @Query("SELECT * FROM seguimiento_academico ORDER BY fechaEvento DESC")
    fun getAllSeguimientos(): Flow<List<SeguimientoEntity>>

    @Query("SELECT * FROM seguimiento_academico WHERE idMatricula = :idMatricula ORDER BY fechaEvento DESC")
    fun getSeguimientosByMatricula(idMatricula: Long): Flow<List<SeguimientoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeguimientos(seguimientos: List<SeguimientoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeguimiento(seguimiento: SeguimientoEntity)

    @Query("UPDATE seguimiento_academico SET idSeguimiento = :newRemoteId, isPendingSync = 0 WHERE idSeguimiento = :localTempId")
    suspend fun updateSeguimientoRemoteId(localTempId: Long, newRemoteId: Long)
}

@Dao
interface AnuncioDao {
    @Query("SELECT * FROM anuncios WHERE idAsignacion = :idAsignacion ORDER BY fijado DESC, fecha DESC")
    fun getAnunciosByAsignacion(idAsignacion: Long): Flow<List<AnuncioEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnuncios(anuncios: List<AnuncioEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnuncio(anuncio: AnuncioEntity)

    @Query("DELETE FROM anuncios WHERE idAnuncio = :id")
    suspend fun deleteAnuncioById(id: Long)
}

@Dao
interface MaterialDao {
    @Query("SELECT * FROM materiales WHERE idAsignacion = :idAsignacion ORDER BY fecha DESC")
    fun getMaterialesByAsignacion(idAsignacion: Long): Flow<List<MaterialEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMateriales(materiales: List<MaterialEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: MaterialEntity)

    @Query("DELETE FROM materiales WHERE idMaterial = :id")
    suspend fun deleteMaterialById(id: Long)
}

@Dao
interface HorarioDao {
    @Query("SELECT * FROM horarios ORDER BY diaSemana, horaInicio")
    fun getHorarioCompleto(): Flow<List<HorarioEntity>>

    @Query("SELECT * FROM horarios WHERE idAsignacion = :idAsignacion ORDER BY diaSemana, horaInicio")
    fun getHorarioByAsignacion(idAsignacion: Long): Flow<List<HorarioEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHorarios(horarios: List<HorarioEntity>)

    @Query("DELETE FROM horarios")
    suspend fun clearHorarios()
}

@Dao
interface PendingSyncDao {
    @Query("SELECT * FROM pending_sync ORDER BY createdAt ASC")
    fun getPendingOperationsFlow(): Flow<List<PendingSyncEntity>>

    @Query("SELECT * FROM pending_sync ORDER BY createdAt ASC")
    suspend fun getAllPendingOperations(): List<PendingSyncEntity>

    @Query("SELECT COUNT(*) FROM pending_sync")
    fun getPendingCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingOperation(operation: PendingSyncEntity): Long

    @Delete
    suspend fun deletePendingOperation(operation: PendingSyncEntity)

    @Query("DELETE FROM pending_sync WHERE id = :id")
    suspend fun deletePendingById(id: Long)

    @Query("DELETE FROM pending_sync")
    suspend fun clearAll()
}
