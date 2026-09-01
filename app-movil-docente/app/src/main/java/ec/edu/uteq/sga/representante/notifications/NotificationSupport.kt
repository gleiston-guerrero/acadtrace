package ec.edu.uteq.sga.representante.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import ec.edu.uteq.sga.representante.MainActivity
import ec.edu.uteq.sga.representante.core.SessionManager
import java.util.concurrent.TimeUnit

object NotificationSupport {
    const val CHANNEL_ID = "session_security"
    private const val WORK_NAME = "representante_session_expiry_notifications"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Seguridad de la sesión",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Avisos locales cuando la sesión de SGA Representante haya expirado"
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<SessionExpiryNotificationWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

class SessionExpiryNotificationWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {
    override fun doWork(): Result {
        val session = SessionManager(applicationContext)
        if (!session.areNotificationsEnabled() || session.getToken().isNullOrBlank() || !session.isTokenExpired()) {
            return Result.success()
        }
        if (!NotificationSupport.hasPermission(applicationContext)) return Result.success()

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, NotificationSupport.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("Sesión de SGA Representante expirada")
            .setContentText("Inicia sesión nuevamente para consultar la información académica.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(applicationContext).notify(SESSION_EXPIRED_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // El permiso puede revocarse entre la comprobación y la publicación.
            return Result.success()
        }
        return Result.success()
    }

    private companion object {
        const val SESSION_EXPIRED_NOTIFICATION_ID = 6001
    }
}
