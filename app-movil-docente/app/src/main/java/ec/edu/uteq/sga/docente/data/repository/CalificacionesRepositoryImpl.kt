package ec.edu.uteq.sga.docente.data.repository

import ec.edu.uteq.sga.docente.core.NetworkConnectivityObserver
import ec.edu.uteq.sga.docente.core.Resource
import ec.edu.uteq.sga.docente.data.local.AppDatabase
import ec.edu.uteq.sga.docente.data.local.entity.CalificacionEntity
import ec.edu.uteq.sga.docente.data.remote.RetrofitClient
import ec.edu.uteq.sga.docente.data.remote.dto.CalificacionCreateDTO
import ec.edu.uteq.sga.docente.data.remote.dto.PromedioFormativoResponseDTO
import ec.edu.uteq.sga.docente.data.sync.SyncManager
import ec.edu.uteq.sga.docente.domain.model.CalificacionEstudiante
import ec.edu.uteq.sga.docente.domain.repository.CalificacionesRepository
import ec.edu.uteq.sga.docente.domain.rules.AcademicRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CalificacionesRepositoryImpl(
    private val database: AppDatabase,
    private val retrofitClient: RetrofitClient,
    private val syncManager: SyncManager,
    private val connectivityObserver: NetworkConnectivityObserver
) : CalificacionesRepository {

    private val calificacionDao = database.calificacionDao()
    private val docenteApi get() = retrofitClient.getDocenteApi()

    override fun getCalificaciones(idActividad: Long): Flow<Resource<List<CalificacionEstudiante>>> = flow {
        emit(Resource.Loading)

        // 1. Emitir caché local de inmediato si existe
        val cached = calificacionDao.getCalificacionesByActividad(idActividad).firstOrNull() ?: emptyList()
        if (cached.isNotEmpty()) {
            val domainList = cached.map { e ->
                CalificacionEstudiante(
                    idCalificacion = e.idCalificacion,
                    idActividad = e.idActividad,
                    idMatricula = e.idMatricula,
                    nota = e.nota,
                    notaCualitativa = e.notaCualitativa,
                    observacion = e.observacion,
                    isPendingSync = e.isPendingSync
                )
            }
            emit(Resource.Success(domainList, isOffline = !connectivityObserver.isCurrentlyConnected()))
        }

        // 2. Consultar red si hay conexión
        if (connectivityObserver.isCurrentlyConnected()) {
            try {
                val resp = docenteApi.getCalificaciones(idActividad = idActividad)
                if (resp.isSuccessful && resp.body() != null) {
                    val entities = resp.body()!!.map { dto ->
                        CalificacionEntity(
                            idCalificacion = dto.idCalificacion,
                            idActividad = dto.idActividad,
                            idMatricula = dto.idMatricula,
                            nota = dto.nota,
                            notaCualitativa = dto.notaCualitativa ?: AcademicRules.convertirNotaCualitativa(dto.nota),
                            observacion = dto.observacion,
                            registradoPor = dto.registradoPor,
                            fechaRegistro = dto.fechaRegistro,
                            isPendingSync = false
                        )
                    }
                    calificacionDao.insertCalificaciones(entities)

                    val domainList = entities.map { e ->
                        CalificacionEstudiante(
                            idCalificacion = e.idCalificacion,
                            idActividad = e.idActividad,
                            idMatricula = e.idMatricula,
                            nota = e.nota,
                            notaCualitativa = e.notaCualitativa,
                            observacion = e.observacion,
                            isPendingSync = false
                        )
                    }
                    emit(Resource.Success(domainList, isOffline = false))
                }
            } catch (e: Exception) {
                if (cached.isEmpty()) {
                    emit(Resource.Error("No se pudieron cargar las calificaciones."))
                }
            }
        } else if (cached.isEmpty()) {
            emit(Resource.Error("Sin conexión a Internet."))
        }
    }

    override suspend fun saveCalificacion(
        calificacion: CalificacionCreateDTO,
        idCalificacion: Long?
    ): Resource<CalificacionEstudiante> = withContext(Dispatchers.IO) {
        val isOnline = connectivityObserver.isCurrentlyConnected()
        val tempId = idCalificacion ?: -System.currentTimeMillis()
        val cualitativa = AcademicRules.convertirNotaCualitativa(calificacion.nota, calificacion.nivel)

        val localEntity = CalificacionEntity(
            idCalificacion = tempId,
            idActividad = calificacion.idActividad,
            idMatricula = calificacion.idMatricula,
            nota = calificacion.nota,
            notaCualitativa = cualitativa,
            observacion = calificacion.observacion,
            registradoPor = calificacion.registradoPor,
            isPendingSync = !isOnline
        )

        calificacionDao.insertCalificacion(localEntity)

        if (isOnline) {
            try {
                val resp = if (idCalificacion != null && idCalificacion > 0) {
                    docenteApi.updateCalificacion(idCalificacion, calificacion)
                } else {
                    docenteApi.createCalificacion(calificacion)
                }

                if (resp.isSuccessful && resp.body() != null) {
                    val serverDto = resp.body()!!
                    val serverEntity = CalificacionEntity(
                        idCalificacion = serverDto.idCalificacion,
                        idActividad = serverDto.idActividad,
                        idMatricula = serverDto.idMatricula,
                        nota = serverDto.nota,
                        notaCualitativa = serverDto.notaCualitativa ?: cualitativa,
                        observacion = serverDto.observacion,
                        registradoPor = serverDto.registradoPor,
                        fechaRegistro = serverDto.fechaRegistro,
                        isPendingSync = false
                    )
                    if (idCalificacion == null) {
                        calificacionDao.updateCalificacionRemoteId(tempId, serverDto.idCalificacion)
                    } else {
                        calificacionDao.insertCalificacion(serverEntity)
                    }
                    return@withContext Resource.Success(
                        CalificacionEstudiante(
                            idCalificacion = serverDto.idCalificacion,
                            idActividad = serverDto.idActividad,
                            idMatricula = serverDto.idMatricula,
                            nota = serverDto.nota,
                            notaCualitativa = serverDto.notaCualitativa ?: cualitativa,
                            observacion = serverDto.observacion,
                            isPendingSync = false
                        )
                    )
                }
            } catch (e: Exception) {
                // Fallo red -> encolar
            }
        }

        val action = if (idCalificacion != null && idCalificacion > 0) "UPDATE" else "CREATE"
        syncManager.enqueueOperation(
            entityType = "CALIFICACION",
            actionType = action,
            localId = tempId,
            remoteId = idCalificacion,
            payload = calificacion
        )

        Resource.Success(
            CalificacionEstudiante(
                idCalificacion = tempId,
                idActividad = calificacion.idActividad,
                idMatricula = calificacion.idMatricula,
                nota = calificacion.nota,
                notaCualitativa = cualitativa,
                observacion = calificacion.observacion,
                isPendingSync = true
            ),
            isOffline = true
        )
    }

    override suspend fun getPromedioFormativo(
        idMatricula: Long,
        idAsignacion: Long,
        idPeriodo: Long
    ): Resource<PromedioFormativoResponseDTO> = withContext(Dispatchers.IO) {
        try {
            val resp = docenteApi.getPromedioFormativo(idMatricula, idAsignacion, idPeriodo)
            if (resp.isSuccessful && resp.body() != null) {
                Resource.Success(resp.body()!!)
            } else {
                Resource.Error("Error al obtener promedio formativo: ${resp.code()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error de conexión")
        }
    }
}
