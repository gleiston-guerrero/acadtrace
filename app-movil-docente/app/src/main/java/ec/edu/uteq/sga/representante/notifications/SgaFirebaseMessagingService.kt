package ec.edu.uteq.sga.representante.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import ec.edu.uteq.sga.representante.MainActivity
import ec.edu.uteq.sga.representante.SgaRepresentanteApp
import ec.edu.uteq.sga.representante.data.remote.api.DispositivoRequest
import ec.edu.uteq.sga.representante.ui.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class AcademicNotificationType { COMUNICADO, AUSENTE, ATRASO, CIERRE_CALIFICACIONES, UNKNOWN }

data class AttendanceNotificationContext(val studentId: Long, val type: AcademicNotificationType, val studentName: String, val date: String, val attendanceId: Long?) {
    val title: String get() = if (type == AcademicNotificationType.ATRASO) "Atraso registrado" else "Ausencia registrada"
    val message: String get() = if (type == AcademicNotificationType.ATRASO) "$studentName llegó atrasado el $date" else "$studentName registró una ausencia el $date"
}

internal fun formatAttendanceNotificationDate(value: String?): String? {
    val match = value?.let { Regex("^(\\d{4})-(\\d{2})-(\\d{2})$").matchEntire(it) } ?: return null
    val (year, month, day) = match.destructured
    return "$day/$month/$year"
}

data class AcademicNotificationPayload(val type: AcademicNotificationType, val studentId: Long?, val periodId: Long?, val announcementId: Long?, val studentName: String?, val date: String?, val attendanceId: Long?) {
    companion object {
        fun from(data: Map<String, String>) = AcademicNotificationPayload(
            runCatching { AcademicNotificationType.valueOf(data["type"].orEmpty()) }.getOrDefault(AcademicNotificationType.UNKNOWN),
            (data["idEstudiante"] ?: data["studentId"])?.toLongOrNull(),
            data["periodId"]?.toLongOrNull(), data["announcementId"]?.toLongOrNull(),
            data["studentName"], data["date"], data["attendanceId"]?.toLongOrNull()
        )
    }

    fun attendanceContext(): AttendanceNotificationContext? {
        if (type != AcademicNotificationType.AUSENTE && type != AcademicNotificationType.ATRASO) return null
        val id = studentId?.takeIf { it > 0 } ?: return null
        val name = studentName?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val formattedDate = formatAttendanceNotificationDate(date) ?: return null
        return AttendanceNotificationContext(id, type, name, formattedDate, attendanceId)
    }
}

object NotificationDestination {
    fun route(data: Map<String, String>): String? {
        val payload = AcademicNotificationPayload.from(data)
        return when (payload.type) {
            AcademicNotificationType.COMUNICADO -> Screen.Comunicados.route
            AcademicNotificationType.AUSENTE, AcademicNotificationType.ATRASO -> payload.studentId?.let(Screen.Asistencia::create)
            AcademicNotificationType.CIERRE_CALIFICACIONES -> payload.studentId?.let(Screen.Calificaciones::create)
            AcademicNotificationType.UNKNOWN -> null
        }
    }

    fun startRoute(sessionValid: Boolean, accessStart: String, data: Map<String, String>): String {
        if (!sessionValid) return Screen.Login.route
        return accessStart
    }
}

object DeviceTokenRegistrar {
    fun refreshAndRegister(app: SgaRepresentanteApp) {
        if (!app.sessionManager.isRepresentante() || app.sessionManager.isTokenExpired()) return
        runCatching { FirebaseMessaging.getInstance().token.addOnSuccessListener { register(app, it) } }
    }

    fun register(app: SgaRepresentanteApp, token: String) {
        if (!DeviceTokenPolicy.shouldRegister(token, app.sessionManager.isRepresentante(), !app.sessionManager.isTokenExpired())) return
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { ec.edu.uteq.sga.representante.data.remote.RetrofitClient(app.sessionManager)
                .getRepresentantePrincipalApi().registrarDispositivo(DispositivoRequest(token)) }
        }
    }
}

object DeviceTokenPolicy {
    fun shouldRegister(token: String, isRepresentante: Boolean, sessionValid: Boolean) =
        token.isNotBlank() && isRepresentante && sessionValid
}

class SgaFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) = DeviceTokenRegistrar.register(application as SgaRepresentanteApp, token)

    override fun onMessageReceived(message: RemoteMessage) {
        val payload = AcademicNotificationPayload.from(message.data)
        if (payload.type == AcademicNotificationType.UNKNOWN || NotificationDestination.route(message.data) == null) return
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"] ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val intent = Intent(this, MainActivity::class.java).apply {
            message.data.forEach { (key, value) -> putExtra(key, value) }
        }
        val channel = when (payload.type) {
            AcademicNotificationType.COMUNICADO -> NotificationSupport.CHANNEL_COMUNICADOS
            AcademicNotificationType.AUSENTE, AcademicNotificationType.ATRASO -> NotificationSupport.CHANNEL_ASISTENCIA
            AcademicNotificationType.CIERRE_CALIFICACIONES -> NotificationSupport.CHANNEL_CALIFICACIONES
            else -> return
        }
        val notification = NotificationCompat.Builder(this, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(this, message.messageId?.hashCode() ?: body.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)).build()
        try {
            NotificationManagerCompat.from(this)
                .notify(message.messageId?.hashCode() ?: body.hashCode(), notification)
        } catch (_: SecurityException) {
            // El permiso puede revocarse entre la comprobación y la publicación.
        }
    }
}
