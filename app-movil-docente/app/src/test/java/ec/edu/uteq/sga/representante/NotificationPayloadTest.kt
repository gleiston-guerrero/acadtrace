package ec.edu.uteq.sga.representante

import ec.edu.uteq.sga.representante.notifications.*
import ec.edu.uteq.sga.representante.ui.navigation.Screen
import org.junit.Assert.*
import org.junit.Test

class NotificationPayloadTest {
    @Test fun comunicadoNavegaAComunicados() = assertEquals(Screen.Comunicados.route, NotificationDestination.route(mapOf("type" to "COMUNICADO", "announcementId" to "9")))
    @Test fun ausenciaNavegaAAsistencia() = assertEquals(Screen.Asistencia.create(12), NotificationDestination.route(mapOf("type" to "AUSENTE", "studentId" to "12")))
    @Test fun atrasoNavegaAAsistencia() = assertEquals(Screen.Asistencia.create(12), NotificationDestination.route(mapOf("type" to "ATRASO", "studentId" to "12")))
    @Test fun cierreNavegaACalificaciones() = assertEquals(Screen.Calificaciones.create(12), NotificationDestination.route(mapOf("type" to "CIERRE_CALIFICACIONES", "studentId" to "12", "periodId" to "3")))
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
