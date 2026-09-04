package ec.edu.uteq.sga.representante.data.sync

import android.content.Context
import androidx.work.*
import ec.edu.uteq.sga.representante.SgaRepresentanteApp
import ec.edu.uteq.sga.representante.core.Resource
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as SgaRepresentanteApp
        val representados = app.representanteRepository.getRepresentados().first { it !is Resource.Loading }
        if (representados !is Resource.Success) return Result.retry()
        for (representado in representados.data) {
            val notas = app.representanteRepository.getCalificaciones(representado.idEstudiante).first { it !is Resource.Loading }
            val asistencia = app.representanteRepository.getAsistencia(representado.idEstudiante).first { it !is Resource.Loading }
            if (notas !is Resource.Success || asistencia !is Resource.Success) return Result.retry()
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "sga_representante_cache_refresh"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }

        fun triggerImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(oneTimeRequest)
        }
    }
}
