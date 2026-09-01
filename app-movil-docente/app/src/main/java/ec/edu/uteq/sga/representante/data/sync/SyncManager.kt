package ec.edu.uteq.sga.representante.data.sync

import android.util.Log
import com.google.gson.Gson
import ec.edu.uteq.sga.representante.core.NetworkConnectivityObserver
import ec.edu.uteq.sga.representante.data.local.AppDatabase
import ec.edu.uteq.sga.representante.data.local.entity.PendingSyncEntity
import ec.edu.uteq.sga.representante.data.remote.RetrofitClient
import ec.edu.uteq.sga.representante.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SyncManager(
    private val database: AppDatabase,
    private val retrofitClient: RetrofitClient,
    private val connectivityObserver: NetworkConnectivityObserver
) {
    private val gson = Gson()
    private val pendingSyncDao = database.pendingSyncDao()
    private val docenteApi get() = retrofitClient.getDocenteApi()

    val pendingCountFlow: Flow<Int> = pendingSyncDao.getPendingCountFlow()

    suspend fun enqueueOperation(
        entityType: String,
        actionType: String,
        localId: Long,
        remoteId: Long? = null,
        payload: Any
    ): Long = withContext(Dispatchers.IO) {
        val payloadJson = gson.toJson(payload)
        val entity = PendingSyncEntity(
            entityType = entityType,
            actionType = actionType,
            localId = localId,
            remoteId = remoteId,
            payloadJson = payloadJson
        )
        val insertedId = pendingSyncDao.insertPendingOperation(entity)
        Log.d("SyncManager", "Enqueued $actionType for $entityType with localId=$localId, queueId=$insertedId")
        
        // Si hay conexión inmediata, intentar sincronizar de inmediato
        if (connectivityObserver.isCurrentlyConnected()) {
            syncPendingOperations()
        }
        
        insertedId
    }

    suspend fun syncPendingOperations(): Result<Int> = withContext(Dispatchers.IO) {
        if (!connectivityObserver.isCurrentlyConnected()) {
            return@withContext Result.failure(Exception("Sin conexión a Internet"))
        }

        val pendingList = pendingSyncDao.getAllPendingOperations()
        if (pendingList.isEmpty()) {
            return@withContext Result.success(0)
        }

        var syncedCount = 0

        for (item in pendingList) {
            try {
                val success = processSingleOperation(item)
                if (success) {
                    pendingSyncDao.deletePendingOperation(item)
                    syncedCount++
                    Log.d("SyncManager", "Successfully synced item id=${item.id} type=${item.entityType}")
                }
            } catch (e: Exception) {
                Log.e("SyncManager", "Error syncing item id=${item.id}: ${e.message}", e)
            }
        }

        Result.success(syncedCount)
    }

    private suspend fun processSingleOperation(item: PendingSyncEntity): Boolean {
        return when (item.entityType) {
            "ACTIVIDAD" -> syncActividad(item)
            "CALIFICACION" -> syncCalificacion(item)
            "ASISTENCIA" -> syncAsistencia(item)
            "SEGUIMIENTO" -> syncSeguimiento(item)
            "ANUNCIO" -> syncAnuncio(item)
            "MATERIAL" -> syncMaterial(item)
            else -> false
        }
    }

    private suspend fun syncActividad(item: PendingSyncEntity): Boolean {
        val dto = gson.fromJson(item.payloadJson, ActividadCreateDTO::class.java)
        return when (item.actionType) {
            "CREATE" -> {
                val resp = docenteApi.createActividad(dto)
                if (resp.isSuccessful && resp.body() != null) {
                    val serverId = resp.body()!!.idActividad
                    database.actividadDao().updateActividadRemoteId(item.localId, serverId)
                    true
                } else false
            }
            "UPDATE" -> {
                val targetId = item.remoteId ?: item.localId
                val resp = docenteApi.updateActividad(targetId, dto)
                resp.isSuccessful
            }
            "DELETE" -> {
                val targetId = item.remoteId ?: item.localId
                val resp = docenteApi.deleteActividad(targetId)
                resp.isSuccessful || resp.code() == 404
            }
            else -> false
        }
    }

    private suspend fun syncCalificacion(item: PendingSyncEntity): Boolean {
        val dto = gson.fromJson(item.payloadJson, CalificacionCreateDTO::class.java)
        return when (item.actionType) {
            "CREATE" -> {
                val resp = docenteApi.createCalificacion(dto)
                if (resp.isSuccessful && resp.body() != null) {
                    val serverId = resp.body()!!.idCalificacion
                    database.calificacionDao().updateCalificacionRemoteId(item.localId, serverId)
                    true
                } else false
            }
            "UPDATE" -> {
                val targetId = item.remoteId ?: item.localId
                val resp = docenteApi.updateCalificacion(targetId, dto)
                resp.isSuccessful
            }
            else -> false
        }
    }

    private suspend fun syncAsistencia(item: PendingSyncEntity): Boolean {
        val dto = gson.fromJson(item.payloadJson, AsistenciaCreateDTO::class.java)
        return when (item.actionType) {
            "CREATE" -> {
                val resp = docenteApi.createAsistencia(dto)
                if (resp.isSuccessful && resp.body() != null) {
                    val serverId = resp.body()!!.idAsistencia
                    database.asistenciaDao().updateAsistenciaRemoteId(item.localId, serverId)
                    true
                } else false
            }
            "UPDATE" -> {
                val targetId = item.remoteId ?: item.localId
                val resp = docenteApi.updateAsistencia(targetId, dto)
                resp.isSuccessful
            }
            else -> false
        }
    }

    private suspend fun syncSeguimiento(item: PendingSyncEntity): Boolean {
        val dto = gson.fromJson(item.payloadJson, SeguimientoCreateDTO::class.java)
        return when (item.actionType) {
            "CREATE" -> {
                val resp = docenteApi.createSeguimientoAcademico(dto)
                if (resp.isSuccessful && resp.body() != null) {
                    val serverId = resp.body()!!.idSeguimiento
                    database.seguimientoDao().updateSeguimientoRemoteId(item.localId, serverId)
                    true
                } else false
            }
            else -> false
        }
    }

    private suspend fun syncAnuncio(item: PendingSyncEntity): Boolean {
        val dto = gson.fromJson(item.payloadJson, AnuncioCreateDTO::class.java)
        return when (item.actionType) {
            "CREATE" -> {
                val resp = docenteApi.createAnuncio(dto)
                resp.isSuccessful
            }
            "DELETE" -> {
                val targetId = item.remoteId ?: item.localId
                val resp = docenteApi.deleteAnuncio(targetId)
                resp.isSuccessful || resp.code() == 404
            }
            else -> false
        }
    }

    private suspend fun syncMaterial(item: PendingSyncEntity): Boolean {
        val dto = gson.fromJson(item.payloadJson, MaterialCreateDTO::class.java)
        return when (item.actionType) {
            "CREATE" -> {
                val resp = docenteApi.createMaterial(dto)
                resp.isSuccessful
            }
            "DELETE" -> {
                val targetId = item.remoteId ?: item.localId
                val resp = docenteApi.deleteMaterial(targetId)
                resp.isSuccessful || resp.code() == 404
            }
            else -> false
        }
    }
}
