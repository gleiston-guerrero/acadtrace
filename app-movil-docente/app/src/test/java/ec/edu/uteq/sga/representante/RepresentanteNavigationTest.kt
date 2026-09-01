package ec.edu.uteq.sga.representante

import ec.edu.uteq.sga.representante.ui.navigation.Screen
import org.junit.Assert.*
import org.junit.Test

class RepresentanteNavigationTest {
    @Test fun grafoContieneSoloRutasDeConsultaRepresentante() {
        val routes = listOf(Screen.Login.route, Screen.Home.route, Screen.BiometricUnlock.route,
            Screen.BiometricFallback.route, Screen.Security.route, Screen.MisRepresentados.route,
            Screen.ResumenRepresentado.route, Screen.Calificaciones.route, Screen.Asistencia.route)
        assertEquals(9, routes.size)
        assertTrue(routes.contains("home_representante"))
        assertFalse(routes.any { it.contains("crear") || it.contains("registrar") || it.contains("docente") })
    }
    @Test fun seleccionConstruyeRutaDelEstudiante() {
        val route = Screen.ResumenRepresentado.create(11, "Ana Paz")
        assertTrue(route.startsWith("representado/11/"))
        assertTrue(route.contains("Ana+Paz"))
        assertFalse(route.contains("idMatricula="))
    }
    @Test fun detalleConservaElIdAutorizado() {
        assertEquals("representado/11/calificaciones", Screen.Calificaciones.create(11))
        assertEquals("representado/11/asistencia", Screen.Asistencia.create(11))
        assertNotEquals(Screen.Calificaciones.create(11), Screen.Calificaciones.create(12))
    }
}
