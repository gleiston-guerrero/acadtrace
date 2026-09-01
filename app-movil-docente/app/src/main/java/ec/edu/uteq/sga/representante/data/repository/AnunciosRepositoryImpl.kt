package ec.edu.uteq.sga.representante.data.repository

import ec.edu.uteq.sga.representante.core.NetworkConnectivityObserver
import ec.edu.uteq.sga.representante.core.Resource
import ec.edu.uteq.sga.representante.data.local.AppDatabase
import ec.edu.uteq.sga.representante.data.local.entity.AnuncioEntity
import ec.edu.uteq.sga.representante.data.remote.RetrofitClient
import ec.edu.uteq.sga.representante.data.remote.dto.AnuncioCreateDTO
import ec.edu.uteq.sga.representante.data.sync.SyncManager
import ec.edu.uteq.sga.representante.domain.model.AnuncioCurso
import ec.edu.uteq.sga.representante.domain.repository.AnunciosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AnunciosRepositoryImpl(
    private val database: AppDatabase,
    private val retrofitClient: RetrofitClient,
    private val syncManager: SyncManager,
    private val connectivityObserver: NetworkConnectivityObserver
) : AnunciosRepository {

    private val anuncioDao = database.anuncioDao()
    private val docenteApi get() = retrofitClient.getDocenteApi()

    override fun getAnuncios(idAsignacion: Long): Flow<Resource<List<AnuncioCurso>>> = flow {
        emit(Resource.Loading)

        val cached = anuncioDao.getAnunciosByAsignacion(idAsignacion).firstOrNull() ?: emptyList()
        if (cached.isNotEmpty()) {
            val domainList = cached.map { e ->
                AnuncioCurso(
                    idAnuncio = e.idAnuncio,
                    idAsignacion = e.idAsignacion,
                    titulo = e.titulo,
                    contenido = e.contenido,
                    fecha = e.fecha,
                    fijado = e.fijado,
                    isPendingSync = e.isPendingSync
                )
            }
            emit(Resource.Success(domainList, isOffline = !connectivityObserver.isCurrentlyConnected()))
        }

        if (connectivityObserver.isCurrentlyConnected()) {
            try {
                val resp = docenteApi.getAnuncios(idAsignacion = idAsignacion)
                if (resp.isSuccessful && resp.body() != null) {
                    val entities = resp.body()!!.map { dto ->
                        AnuncioEntity(
                            idAnuncio = dto.idAnuncio,
                            idAsignacion = dto.idAsignacion,
                            titulo = dto.titulo,
                            contenido = dto.contenido,
                            autorId = dto.autorId,
                            fecha = dto.fecha,
                            fijado = dto.fijado,
                            isPendingSync = false
                        )
                    }
                    anuncioDao.insertAnuncios(entities)

                    val domainList = entities.map { e ->
                        AnuncioCurso(
                            idAnuncio = e.idAnuncio,
                            idAsignacion = e.idAsignacion,
                            titulo = e.titulo,
                            contenido = e.contenido,
                            fecha = e.fecha,
                            fijado = e.fijado,
                            isPendingSync = false
                        )
                    }
                    emit(Resource.Success(domainList, isOffline = false))
                }
            } catch (e: Exception) {
                if (cached.isEmpty()) {
                    emit(Resource.Error("No se pudieron cargar los anuncios."))
                }
            }
        } else if (cached.isEmpty()) {
            emit(Resource.Error("Sin conexión a Internet."))
        }
    }

    override suspend fun createAnuncio(anuncio: AnuncioCreateDTO): Resource<AnuncioCurso> =
        withContext(Dispatchers.IO) {
            val isOnline = connectivityObserver.isCurrentlyConnected()
            val tempId = -System.currentTimeMillis()

            val localEntity = AnuncioEntity(
                idAnuncio = tempId,
                idAsignacion = anuncio.idAsignacion,
                titulo = anuncio.titulo,
                contenido = anuncio.contenido,
                autorId = anuncio.autorId,
                fecha = "Hoy",
                fijado = anuncio.fijado,
                isPendingSync = !isOnline
            )
            anuncioDao.insertAnuncio(localEntity)

            if (isOnline) {
                try {
                    val resp = docenteApi.createAnuncio(anuncio)
                    if (resp.isSuccessful && resp.body() != null) {
                        val serverDto = resp.body()!!
                        val serverEntity = AnuncioEntity(
                            idAnuncio = serverDto.idAnuncio,
                            idAsignacion = serverDto.idAsignacion,
                            titulo = serverDto.titulo,
                            contenido = serverDto.contenido,
                            autorId = serverDto.autorId,
                            fecha = serverDto.fecha,
                            fijado = serverDto.fijado,
                            isPendingSync = false
                        )
                        anuncioDao.deleteAnuncioById(tempId)
                        anuncioDao.insertAnuncio(serverEntity)
                        return@withContext Resource.Success(
                            AnuncioCurso(
                                idAnuncio = serverDto.idAnuncio,
                                idAsignacion = serverDto.idAsignacion,
                                titulo = serverDto.titulo,
                                contenido = serverDto.contenido,
                                fecha = serverDto.fecha,
                                fijado = serverDto.fijado,
                                isPendingSync = false
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Fallo red -> encolar
                }
            }

            syncManager.enqueueOperation(
                entityType = "ANUNCIO",
                actionType = "CREATE",
                localId = tempId,
                payload = anuncio
            )

            Resource.Success(
                AnuncioCurso(
                    idAnuncio = tempId,
                    idAsignacion = anuncio.idAsignacion,
                    titulo = anuncio.titulo,
                    contenido = anuncio.contenido,
                    fecha = "Pendiente de sincronizar",
                    fijado = anuncio.fijado,
                    isPendingSync = true
                ),
                isOffline = true
            )
        }

    override suspend fun deleteAnuncio(id: Long): Resource<Unit> = withContext(Dispatchers.IO) {
        val isOnline = connectivityObserver.isCurrentlyConnected()
        anuncioDao.deleteAnuncioById(id)

        if (isOnline) {
            try {
                val resp = docenteApi.deleteAnuncio(id)
                if (resp.isSuccessful) {
                    return@withContext Resource.Success(Unit)
                }
            } catch (e: Exception) {
                // Continuar a encolar
            }
        }

        syncManager.enqueueOperation(
            entityType = "ANUNCIO",
            actionType = "DELETE",
            localId = id,
            remoteId = id,
            payload = mapOf("id" to id)
        )

        Resource.Success(Unit, isOffline = true)
    }
}
