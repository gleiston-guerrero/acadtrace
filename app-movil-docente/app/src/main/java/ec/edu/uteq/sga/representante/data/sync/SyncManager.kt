package ec.edu.uteq.sga.representante.data.sync

import ec.edu.uteq.sga.representante.core.NetworkConnectivityObserver
import ec.edu.uteq.sga.representante.data.local.AppDatabase
import ec.edu.uteq.sga.representante.data.remote.RetrofitClient
import kotlinx.coroutines.flow.Flow

/** Puente para código histórico fuera del grafo activo. No transmite escrituras. */
class SyncManager(
    database: AppDatabase,
    @Suppress("UNUSED_PARAMETER") retrofitClient: RetrofitClient,
    @Suppress("UNUSED_PARAMETER") connectivityObserver: NetworkConnectivityObserver
) {
    private val pendingSyncDao = database.pendingSyncDao()
    val pendingCountFlow: Flow<Int> = pendingSyncDao.getPendingCountFlow()

    suspend fun enqueueOperation(
        @Suppress("UNUSED_PARAMETER") entityType: String,
        @Suppress("UNUSED_PARAMETER") actionType: String,
        @Suppress("UNUSED_PARAMETER") localId: Long,
        @Suppress("UNUSED_PARAMETER") remoteId: Long? = null,
        @Suppress("UNUSED_PARAMETER") payload: Any
    ): Long = throw UnsupportedOperationException(
        "La app REPRESENTANTE es de solo lectura; no admite escrituras académicas"
    )

    suspend fun syncPendingOperations(): Result<Int> = Result.success(0)
}
