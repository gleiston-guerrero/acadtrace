package ec.edu.uteq.sga.representante.data.repository

import ec.edu.uteq.sga.representante.core.NetworkConnectivityObserver
import ec.edu.uteq.sga.representante.core.Resource
import ec.edu.uteq.sga.representante.data.local.AppDatabase
import ec.edu.uteq.sga.representante.data.local.entity.ActividadEntity
import ec.edu.uteq.sga.representante.data.remote.RetrofitClient
import ec.edu.uteq.sga.representante.data.remote.dto.ActividadCreateDTO
import ec.edu.uteq.sga.representante.data.sync.SyncManager
import ec.edu.uteq.sga.representante.domain.model.ActividadAcademica
import ec.edu.uteq.sga.representante.domain.repository.ActividadesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ActividadesRepositoryImpl(
    private val database: AppDatabase,
    private val retrofitClient: RetrofitClient,
    private val syncManager: SyncManager,
    private val connectivityObserver: NetworkConnectivityObserver
) : ActividadesRepository {

    private val actividadDao = database.actividadDao()
    private val docenteApi get() = retrofitClient.getDocenteApi()

    override fun getActividades(
        idAsignacion: Long,
        idPeriodo: Long?
    ): Flow<Resource<List<ActividadAcademica>>> = flow {
        emit(Resource.Loading)

        // 1. Emitir caché local de inmediato si existe
        val cached = actividadDao.getActividades(idAsignacion, idPeriodo).firstOrNull() ?: emptyList()
        if (cached.isNotEmpty()) {
            val domainList = cached.map { e ->
                ActividadAcademica(
                    idActividad = e.idActividad,
                    idAsignacion = e.idAsignacion,
                    idPeriodo = e.idPeriodo,
                    tipo = e.tipo,
                    nombre = e.nombre,
                    descripcion = e.descripcion,
                    fechaEntrega = e.fechaEntrega,
                    ponderacion = e.ponderacion,
                    notaMaxima = e.notaMaxima,
                    esSumativa = e.esSumativa,
                    isPendingSync = e.isPendingSync
                )
            }
            emit(Resource.Success(domainList, isOffline = !connectivityObserver.isCurrentlyConnected()))
        }

        // 2. Consultar red si hay conexión
        if (connectivityObserver.isCurrentlyConnected()) {
            try {
                val resp = docenteApi.getActividades(idAsignacion = idAsignacion, idPeriodo = idPeriodo)
                if (resp.isSuccessful && resp.body() != null) {
                    val entities = resp.body()!!.map { dto ->
                        ActividadEntity(
                            idActividad = dto.idActividad,
                            idAsignacion = dto.idAsignacion,
                            idPeriodo = dto.idPeriodo,
                            tipo = dto.tipo,
                            nombre = dto.nombre,
                            descripcion = dto.descripcion,
                            fechaEntrega = dto.fechaEntrega,
                            ponderacion = dto.ponderacion,
                            notaMaxima = dto.notaMaxima,
                            esSumativa = dto.esSumativa,
                            isPendingSync = false
                        )
                    }
                    actividadDao.insertActividades(entities)

                    val domainList = entities.map { e ->
                        ActividadAcademica(
                            idActividad = e.idActividad,
                            idAsignacion = e.idAsignacion,
                            idPeriodo = e.idPeriodo,
                            tipo = e.tipo,
                            nombre = e.nombre,
                            descripcion = e.descripcion,
                            fechaEntrega = e.fechaEntrega,
                            ponderacion = e.ponderacion,
                            notaMaxima = e.notaMaxima,
                            esSumativa = e.esSumativa,
                            isPendingSync = false
                        )
                    }
                    emit(Resource.Success(domainList, isOffline = false))
                }
            } catch (e: Exception) {
                if (cached.isEmpty()) {
                    emit(Resource.Error("No se pudieron cargar las actividades."))
                }
            }
        } else if (cached.isEmpty()) {
            emit(Resource.Error("Sin conexión a Internet."))
        }
    }

    override suspend fun createActividad(actividad: ActividadCreateDTO): Resource<ActividadAcademica> =
        withContext(Dispatchers.IO) {
            val isOnline = connectivityObserver.isCurrentlyConnected()
            val tempId = -System.currentTimeMillis() // ID temporal negativo para identificarlo offline

            val localEntity = ActividadEntity(
                idActividad = tempId,
                idAsignacion = actividad.idAsignacion,
                idPeriodo = actividad.idPeriodo,
                tipo = actividad.tipo,
                nombre = actividad.nombre,
                descripcion = actividad.descripcion,
                fechaEntrega = actividad.fechaEntrega,
                ponderacion = actividad.ponderacion,
                notaMaxima = actividad.notaMaxima,
                esSumativa = actividad.esSumativa,
                isPendingSync = !isOnline
            )

            // Guardar localmente
            actividadDao.insertActividad(localEntity)

            if (isOnline) {
                try {
                    val resp = docenteApi.createActividad(actividad)
                    if (resp.isSuccessful && resp.body() != null) {
                        val serverDto = resp.body()!!
                        val serverEntity = ActividadEntity(
                            idActividad = serverDto.idActividad,
                            idAsignacion = serverDto.idAsignacion,
                            idPeriodo = serverDto.idPeriodo,
                            tipo = serverDto.tipo,
                            nombre = serverDto.nombre,
                            descripcion = serverDto.descripcion,
                            fechaEntrega = serverDto.fechaEntrega,
                            ponderacion = serverDto.ponderacion,
                            notaMaxima = serverDto.notaMaxima,
                            esSumativa = serverDto.esSumativa,
                            isPendingSync = false
                        )
                        actividadDao.deleteActividadById(tempId)
                        actividadDao.insertActividad(serverEntity)
                        return@withContext Resource.Success(
                            ActividadAcademica(
                                idActividad = serverDto.idActividad,
                                idAsignacion = serverDto.idAsignacion,
                                idPeriodo = serverDto.idPeriodo,
                                tipo = serverDto.tipo,
                                nombre = serverDto.nombre,
                                descripcion = serverDto.descripcion,
                                fechaEntrega = serverDto.fechaEntrega,
                                ponderacion = serverDto.ponderacion,
                                notaMaxima = serverDto.notaMaxima,
                                esSumativa = serverDto.esSumativa,
                                isPendingSync = false
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Falló la llamada a la API, encolar para sincronización
                }
            }

            // Encolar en la tabla de sincronización offline
            syncManager.enqueueOperation(
                entityType = "ACTIVIDAD",
                actionType = "CREATE",
                localId = tempId,
                payload = actividad
            )

            Resource.Success(
                ActividadAcademica(
                    idActividad = tempId,
                    idAsignacion = actividad.idAsignacion,
                    idPeriodo = actividad.idPeriodo,
                    tipo = actividad.tipo,
                    nombre = actividad.nombre,
                    descripcion = actividad.descripcion,
                    fechaEntrega = actividad.fechaEntrega,
                    ponderacion = actividad.ponderacion,
                    notaMaxima = actividad.notaMaxima,
                    esSumativa = actividad.esSumativa,
                    isPendingSync = true
                ),
                isOffline = true
            )
        }

    override suspend fun updateActividad(id: Long, actividad: ActividadCreateDTO): Resource<ActividadAcademica> =
        withContext(Dispatchers.IO) {
            val isOnline = connectivityObserver.isCurrentlyConnected()

            val updatedEntity = ActividadEntity(
                idActividad = id,
                idAsignacion = actividad.idAsignacion,
                idPeriodo = actividad.idPeriodo,
                tipo = actividad.tipo,
                nombre = actividad.nombre,
                descripcion = actividad.descripcion,
                fechaEntrega = actividad.fechaEntrega,
                ponderacion = actividad.ponderacion,
                notaMaxima = actividad.notaMaxima,
                esSumativa = actividad.esSumativa,
                isPendingSync = !isOnline
            )
            actividadDao.insertActividad(updatedEntity)

            if (isOnline) {
                try {
                    val resp = docenteApi.updateActividad(id, actividad)
                    if (resp.isSuccessful && resp.body() != null) {
                        return@withContext Resource.Success(
                            ActividadAcademica(
                                idActividad = id,
                                idAsignacion = actividad.idAsignacion,
                                idPeriodo = actividad.idPeriodo,
                                tipo = actividad.tipo,
                                nombre = actividad.nombre,
                                descripcion = actividad.descripcion,
                                fechaEntrega = actividad.fechaEntrega,
                                ponderacion = actividad.ponderacion,
                                notaMaxima = actividad.notaMaxima,
                                esSumativa = actividad.esSumativa,
                                isPendingSync = false
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Falló red
                }
            }

            syncManager.enqueueOperation(
                entityType = "ACTIVIDAD",
                actionType = "UPDATE",
                localId = id,
                remoteId = id,
                payload = actividad
            )

            Resource.Success(
                ActividadAcademica(
                    idActividad = id,
                    idAsignacion = actividad.idAsignacion,
                    idPeriodo = actividad.idPeriodo,
                    tipo = actividad.tipo,
                    nombre = actividad.nombre,
                    descripcion = actividad.descripcion,
                    fechaEntrega = actividad.fechaEntrega,
                    ponderacion = actividad.ponderacion,
                    notaMaxima = actividad.notaMaxima,
                    esSumativa = actividad.esSumativa,
                    isPendingSync = true
                ),
                isOffline = true
            )
        }

    override suspend fun deleteActividad(id: Long): Resource<Unit> = withContext(Dispatchers.IO) {
        val isOnline = connectivityObserver.isCurrentlyConnected()
        actividadDao.deleteActividadById(id)

        if (isOnline) {
            try {
                val resp = docenteApi.deleteActividad(id)
                if (resp.isSuccessful) {
                    return@withContext Resource.Success(Unit)
                }
            } catch (e: Exception) {
                // Falló red
            }
        }

        syncManager.enqueueOperation(
            entityType = "ACTIVIDAD",
            actionType = "DELETE",
            localId = id,
            remoteId = id,
            payload = mapOf("id" to id)
        )

        Resource.Success(Unit, isOffline = true)
    }
}
