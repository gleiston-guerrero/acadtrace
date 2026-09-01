package ec.edu.uteq.sga.representante.data.repository

import ec.edu.uteq.sga.representante.core.NetworkConnectivityObserver
import ec.edu.uteq.sga.representante.core.Resource
import ec.edu.uteq.sga.representante.data.local.AppDatabase
import ec.edu.uteq.sga.representante.data.local.entity.SeguimientoEntity
import ec.edu.uteq.sga.representante.data.remote.RetrofitClient
import ec.edu.uteq.sga.representante.data.remote.dto.SeguimientoCreateDTO
import ec.edu.uteq.sga.representante.data.sync.SyncManager
import ec.edu.uteq.sga.representante.domain.model.SeguimientoItem
import ec.edu.uteq.sga.representante.domain.repository.SeguimientoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SeguimientoRepositoryImpl(
    private val database: AppDatabase,
    private val retrofitClient: RetrofitClient,
    private val syncManager: SyncManager,
    private val connectivityObserver: NetworkConnectivityObserver
) : SeguimientoRepository {

    private val seguimientoDao = database.seguimientoDao()
    private val docenteApi get() = retrofitClient.getDocenteApi()

    override fun getSeguimientos(idMatricula: Long?): Flow<Resource<List<SeguimientoItem>>> = flow {
        emit(Resource.Loading)

        val flowQuery = if (idMatricula != null) {
            seguimientoDao.getSeguimientosByMatricula(idMatricula)
        } else {
            seguimientoDao.getAllSeguimientos()
        }

        val cached = flowQuery.firstOrNull() ?: emptyList()
        if (cached.isNotEmpty()) {
            val domainList = cached.map { e ->
                SeguimientoItem(
                    idSeguimiento = e.idSeguimiento,
                    idMatricula = e.idMatricula,
                    idPeriodo = e.idPeriodo,
                    categoria = e.categoria,
                    descripcion = e.descripcion,
                    accionesTomadas = e.accionesTomadas,
                    requiereFollowup = e.requiereFollowup,
                    fechaEvento = e.fechaEvento,
                    isPendingSync = e.isPendingSync
                )
            }
            emit(Resource.Success(domainList, isOffline = !connectivityObserver.isCurrentlyConnected()))
        }

        if (connectivityObserver.isCurrentlyConnected()) {
            try {
                val resp = docenteApi.getSeguimientoAcademico(idMatricula = idMatricula)
                if (resp.isSuccessful && resp.body() != null) {
                    val entities = resp.body()!!.map { dto ->
                        SeguimientoEntity(
                            idSeguimiento = dto.idSeguimiento,
                            idMatricula = dto.idMatricula,
                            idPeriodo = dto.idPeriodo,
                            categoria = dto.categoria,
                            descripcion = dto.descripcion,
                            accionesTomadas = dto.accionesTomadas,
                            requiereFollowup = dto.requiereFollowup,
                            fechaEvento = dto.fechaEvento,
                            registradoPor = dto.registradoPor,
                            fechaRegistro = dto.fechaRegistro,
                            isPendingSync = false
                        )
                    }
                    seguimientoDao.insertSeguimientos(entities)

                    val domainList = entities.map { e ->
                        SeguimientoItem(
                            idSeguimiento = e.idSeguimiento,
                            idMatricula = e.idMatricula,
                            idPeriodo = e.idPeriodo,
                            categoria = e.categoria,
                            descripcion = e.descripcion,
                            accionesTomadas = e.accionesTomadas,
                            requiereFollowup = e.requiereFollowup,
                            fechaEvento = e.fechaEvento,
                            isPendingSync = false
                        )
                    }
                    emit(Resource.Success(domainList, isOffline = false))
                }
            } catch (e: Exception) {
                if (cached.isEmpty()) {
                    emit(Resource.Error("No se pudieron cargar los registros de seguimiento."))
                }
            }
        } else if (cached.isEmpty()) {
            emit(Resource.Error("Sin conexión a Internet."))
        }
    }

    override suspend fun createSeguimiento(seguimiento: SeguimientoCreateDTO): Resource<SeguimientoItem> =
        withContext(Dispatchers.IO) {
            val isOnline = connectivityObserver.isCurrentlyConnected()
            val tempId = -System.currentTimeMillis()

            val localEntity = SeguimientoEntity(
                idSeguimiento = tempId,
                idMatricula = seguimiento.idMatricula,
                idPeriodo = seguimiento.idPeriodo,
                categoria = seguimiento.categoria,
                descripcion = seguimiento.descripcion,
                accionesTomadas = seguimiento.accionesTomadas,
                requiereFollowup = seguimiento.requiereFollowup,
                fechaEvento = seguimiento.fechaEvento,
                registradoPor = seguimiento.registradoPor,
                isPendingSync = !isOnline
            )

            seguimientoDao.insertSeguimiento(localEntity)

            if (isOnline) {
                try {
                    val resp = docenteApi.createSeguimientoAcademico(seguimiento)
                    if (resp.isSuccessful && resp.body() != null) {
                        val serverDto = resp.body()!!
                        val serverEntity = SeguimientoEntity(
                            idSeguimiento = serverDto.idSeguimiento,
                            idMatricula = serverDto.idMatricula,
                            idPeriodo = serverDto.idPeriodo,
                            categoria = serverDto.categoria,
                            descripcion = serverDto.descripcion,
                            accionesTomadas = serverDto.accionesTomadas,
                            requiereFollowup = serverDto.requiereFollowup,
                            fechaEvento = serverDto.fechaEvento,
                            registradoPor = serverDto.registradoPor,
                            fechaRegistro = serverDto.fechaRegistro,
                            isPendingSync = false
                        )
                        seguimientoDao.updateSeguimientoRemoteId(tempId, serverDto.idSeguimiento)
                        return@withContext Resource.Success(
                            SeguimientoItem(
                                idSeguimiento = serverDto.idSeguimiento,
                                idMatricula = serverDto.idMatricula,
                                idPeriodo = serverDto.idPeriodo,
                                categoria = serverDto.categoria,
                                descripcion = serverDto.descripcion,
                                accionesTomadas = serverDto.accionesTomadas,
                                requiereFollowup = serverDto.requiereFollowup,
                                fechaEvento = serverDto.fechaEvento,
                                isPendingSync = false
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Fallo red -> encolar
                }
            }

            syncManager.enqueueOperation(
                entityType = "SEGUIMIENTO",
                actionType = "CREATE",
                localId = tempId,
                payload = seguimiento
            )

            Resource.Success(
                SeguimientoItem(
                    idSeguimiento = tempId,
                    idMatricula = seguimiento.idMatricula,
                    idPeriodo = seguimiento.idPeriodo,
                    categoria = seguimiento.categoria,
                    descripcion = seguimiento.descripcion,
                    accionesTomadas = seguimiento.accionesTomadas,
                    requiereFollowup = seguimiento.requiereFollowup,
                    fechaEvento = seguimiento.fechaEvento,
                    isPendingSync = true
                ),
                isOffline = true
            )
        }
}
