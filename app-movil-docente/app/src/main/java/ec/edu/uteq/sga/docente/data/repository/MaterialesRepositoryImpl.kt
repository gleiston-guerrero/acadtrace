package ec.edu.uteq.sga.docente.data.repository

import ec.edu.uteq.sga.docente.core.NetworkConnectivityObserver
import ec.edu.uteq.sga.docente.core.Resource
import ec.edu.uteq.sga.docente.data.local.AppDatabase
import ec.edu.uteq.sga.docente.data.local.entity.MaterialEntity
import ec.edu.uteq.sga.docente.data.remote.RetrofitClient
import ec.edu.uteq.sga.docente.data.remote.dto.MaterialCreateDTO
import ec.edu.uteq.sga.docente.data.sync.SyncManager
import ec.edu.uteq.sga.docente.domain.model.MaterialCurso
import ec.edu.uteq.sga.docente.domain.repository.MaterialesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MaterialesRepositoryImpl(
    private val database: AppDatabase,
    private val retrofitClient: RetrofitClient,
    private val syncManager: SyncManager,
    private val connectivityObserver: NetworkConnectivityObserver
) : MaterialesRepository {

    private val materialDao = database.materialDao()
    private val docenteApi get() = retrofitClient.getDocenteApi()

    override fun getMateriales(idAsignacion: Long): Flow<Resource<List<MaterialCurso>>> = flow {
        emit(Resource.Loading)

        val cached = materialDao.getMaterialesByAsignacion(idAsignacion).firstOrNull() ?: emptyList()
        if (cached.isNotEmpty()) {
            val domainList = cached.map { e ->
                MaterialCurso(
                    idMaterial = e.idMaterial,
                    idAsignacion = e.idAsignacion,
                    tipo = e.tipo,
                    titulo = e.titulo,
                    descripcion = e.descripcion,
                    url = e.url,
                    tamanoBytes = e.tamanoBytes,
                    fecha = e.fecha,
                    isPendingSync = e.isPendingSync
                )
            }
            emit(Resource.Success(domainList, isOffline = !connectivityObserver.isCurrentlyConnected()))
        }

        if (connectivityObserver.isCurrentlyConnected()) {
            try {
                val resp = docenteApi.getMateriales(idAsignacion = idAsignacion)
                if (resp.isSuccessful && resp.body() != null) {
                    val entities = resp.body()!!.map { dto ->
                        MaterialEntity(
                            idMaterial = dto.idMaterial,
                            idAsignacion = dto.idAsignacion,
                            tipo = dto.tipo,
                            titulo = dto.titulo,
                            descripcion = dto.descripcion,
                            url = dto.url,
                            tamanoBytes = dto.tamanoBytes,
                            fecha = dto.fecha,
                            isPendingSync = false
                        )
                    }
                    materialDao.insertMateriales(entities)

                    val domainList = entities.map { e ->
                        MaterialCurso(
                            idMaterial = e.idMaterial,
                            idAsignacion = e.idAsignacion,
                            tipo = e.tipo,
                            titulo = e.titulo,
                            descripcion = e.descripcion,
                            url = e.url,
                            tamanoBytes = e.tamanoBytes,
                            fecha = e.fecha,
                            isPendingSync = false
                        )
                    }
                    emit(Resource.Success(domainList, isOffline = false))
                }
            } catch (e: Exception) {
                if (cached.isEmpty()) {
                    emit(Resource.Error("No se pudieron cargar los materiales."))
                }
            }
        } else if (cached.isEmpty()) {
            emit(Resource.Error("Sin conexión a Internet."))
        }
    }

    override suspend fun createMaterial(material: MaterialCreateDTO): Resource<MaterialCurso> =
        withContext(Dispatchers.IO) {
            val isOnline = connectivityObserver.isCurrentlyConnected()
            val tempId = -System.currentTimeMillis()

            val localEntity = MaterialEntity(
                idMaterial = tempId,
                idAsignacion = material.idAsignacion,
                tipo = material.tipo,
                titulo = material.titulo,
                descripcion = material.descripcion,
                url = material.url,
                tamanoBytes = null,
                fecha = "Hoy",
                isPendingSync = !isOnline
            )
            materialDao.insertMaterial(localEntity)

            if (isOnline) {
                try {
                    val resp = docenteApi.createMaterial(material)
                    if (resp.isSuccessful && resp.body() != null) {
                        val serverDto = resp.body()!!
                        val serverEntity = MaterialEntity(
                            idMaterial = serverDto.idMaterial,
                            idAsignacion = serverDto.idAsignacion,
                            tipo = serverDto.tipo,
                            titulo = serverDto.titulo,
                            descripcion = serverDto.descripcion,
                            url = serverDto.url,
                            tamanoBytes = serverDto.tamanoBytes,
                            fecha = serverDto.fecha,
                            isPendingSync = false
                        )
                        materialDao.deleteMaterialById(tempId)
                        materialDao.insertMaterial(serverEntity)
                        return@withContext Resource.Success(
                            MaterialCurso(
                                idMaterial = serverDto.idMaterial,
                                idAsignacion = serverDto.idAsignacion,
                                tipo = serverDto.tipo,
                                titulo = serverDto.titulo,
                                descripcion = serverDto.descripcion,
                                url = serverDto.url,
                                tamanoBytes = serverDto.tamanoBytes,
                                fecha = serverDto.fecha,
                                isPendingSync = false
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Fallo red -> encolar
                }
            }

            syncManager.enqueueOperation(
                entityType = "MATERIAL",
                actionType = "CREATE",
                localId = tempId,
                payload = material
            )

            Resource.Success(
                MaterialCurso(
                    idMaterial = tempId,
                    idAsignacion = material.idAsignacion,
                    tipo = material.tipo,
                    titulo = material.titulo,
                    descripcion = material.descripcion,
                    url = material.url,
                    tamanoBytes = null,
                    fecha = "Pendiente de sincronizar",
                    isPendingSync = true
                ),
                isOffline = true
            )
        }

    override suspend fun deleteMaterial(id: Long): Resource<Unit> = withContext(Dispatchers.IO) {
        val isOnline = connectivityObserver.isCurrentlyConnected()
        materialDao.deleteMaterialById(id)

        if (isOnline) {
            try {
                val resp = docenteApi.deleteMaterial(id)
                if (resp.isSuccessful) {
                    return@withContext Resource.Success(Unit)
                }
            } catch (e: Exception) {
                // Continuar a encolar
            }
        }

        syncManager.enqueueOperation(
            entityType = "MATERIAL",
            actionType = "DELETE",
            localId = id,
            remoteId = id,
            payload = mapOf("id" to id)
        )

        Resource.Success(Unit, isOffline = true)
    }
}
