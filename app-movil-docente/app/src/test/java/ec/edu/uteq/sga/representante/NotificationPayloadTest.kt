package ec.edu.uteq.sga.representante

import ec.edu.uteq.sga.representante.notifications.*
import ec.edu.uteq.sga.representante.ui.navigation.notificationRouteAfterStart
import ec.edu.uteq.sga.representante.ui.navigation.Screen
import org.junit.Assert.*
import org.junit.Test

class NotificationPayloadTest {
    private val attendanceData = mapOf(
        "type" to "ATRASO", "studentId" to "12", "studentName" to "Daniel Solórzano",
        "date" to "2026-09-11", "attendanceId" to "91"
    )
    @Test fun comunicadoNavegaAComunicados() = assertEquals(Screen.Comunicados.route, NotificationDestination.route(mapOf("type" to "COMUNICADO", "announcementId" to "9")))
    @Test fun ausenciaNavegaAAsistenciaDelIdEstudiante() = assertEquals(
        Screen.Asistencia.create(12),
        NotificationDestination.route(mapOf("type" to "AUSENTE", "idEstudiante" to "12"))
    )
    @Test fun atrasoNavegaAAsistenciaDelIdEstudiante() = assertEquals(
        Screen.Asistencia.create(12),
        NotificationDestination.route(mapOf("type" to "ATRASO", "idEstudiante" to "12"))
    )
    @Test fun atrasoTransportaContextoEstructuradoYFormateaFecha() {
        val payload = AcademicNotificationPayload.from(attendanceData)
        val context = payload.attendanceContext()!!
        assertEquals(12L, context.studentId)
        assertEquals(91L, context.attendanceId)
        assertEquals("Atraso registrado", context.title)
        assertEquals("Daniel Solórzano llegó atrasado el 11/09/2026", context.message)
    }
    @Test fun ausenciaConstruyeMensajeContextual() {
        val context = AcademicNotificationPayload.from(attendanceData + ("type" to "AUSENTE")).attendanceContext()!!
        assertEquals("Ausencia registrada", context.title)
        assertEquals("Daniel Solórzano registró una ausencia el 11/09/2026", context.message)
    }
    @Test fun fechaIsoSeFormateaSinInventarFecha() {
        assertEquals("11/09/2026", formatAttendanceNotificationDate("2026-09-11"))
        assertNull(formatAttendanceNotificationDate(null))
        assertNull(formatAttendanceNotificationDate("11/09/2026"))
    }
    @Test fun entradaNormalOPayloadIncompletoNoMuestraContexto() {
        assertNull(AcademicNotificationPayload.from(emptyMap()).attendanceContext())
        assertNull(AcademicNotificationPayload.from(mapOf("type" to "ATRASO", "studentId" to "12")).attendanceContext())
    }
    @Test fun cierreNavegaACalificaciones() = assertEquals(Screen.Calificaciones.create(12), NotificationDestination.route(mapOf("type" to "CIERRE_CALIFICACIONES", "studentId" to "12", "periodId" to "3")))
    @Test fun notificacionDeAsistenciaNoSeUsaComoStartDestination() {
        val data = mapOf("type" to "AUSENTE", "studentId" to "12")
        assertEquals(Screen.Home.route, NotificationDestination.startRoute(true, Screen.Home.route, data))
        val notificationRoute = NotificationDestination.route(data)
        assertEquals(Screen.Asistencia.create(12), notificationRouteAfterStart(Screen.Home.route, notificationRoute))
    }
    @Test fun incompletoODesconocidoNoNavega() {
        assertNull(NotificationDestination.route(mapOf("type" to "AUSENTE")))
        assertNull(NotificationDestination.route(mapOf("type" to "AUSENCIA", "studentId" to "12")))
        assertNull(NotificationDestination.route(mapOf("type" to "OTRO")))
        assertEquals(AcademicNotificationType.UNKNOWN, AcademicNotificationPayload.from(emptyMap()).type)
    }
    @Test fun sesionVencidaSiempreAbreLogin() = assertEquals(
        Screen.Login.route,
        NotificationDestination.startRoute(false, Screen.Home.route, mapOf("type" to "AUSENTE", "studentId" to "12"))
    )
    @Test fun refreshSoloSeRegistraConSesionValidaDeRepresentante() {
        assertTrue(DeviceTokenPolicy.shouldRegister("nuevo-token", true, true))
        assertFalse(DeviceTokenPolicy.shouldRegister("nuevo-token", true, false))
        assertFalse(DeviceTokenPolicy.shouldRegister("nuevo-token", false, true))
    }
}
