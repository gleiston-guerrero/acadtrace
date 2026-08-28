package ec.edu.uteq.sga.docente.domain.model

import org.junit.Assert.*
import org.junit.Test

class DomainModelsTest {

    @Test
    fun `Estudiante nombreCompleto concatena apellidos y nombres correctamente`() {
        val estudiante = Estudiante(
            idMatricula = 621,
            idAsignacion = 13,
            estudianteId = 622,
            nombres = "Mateo",
            apellidos = "García Parrales",
            cedula = "0618248116",
            estadoMatricula = "ACTIVA"
        )

        assertEquals("García Parrales Mateo", estudiante.nombreCompleto)
        assertEquals("ACTIVA", estudiante.estadoMatricula)
        assertEquals("0618248116", estudiante.cedula)
    }

    @Test
    fun `UserSession isDocente verifica el rol correctamente`() {
        val sessionDocente = UserSession(
            idUsuario = 100,
            username = "jsjimenezt",
            correo = "docente@uteq.edu.ec",
            roles = listOf("DOCENTE")
        )
        assertTrue(sessionDocente.isDocente())

        val sessionEstudiante = UserSession(
            idUsuario = 200,
            username = "alumno1",
            correo = "alumno@uteq.edu.ec",
            roles = listOf("ESTUDIANTE")
        )
        assertFalse(sessionEstudiante.isDocente())
    }

    @Test
    fun `ActividadAcademica valida propiedades formativa y sumativa`() {
        val actividadFormativa = ActividadAcademica(
            idActividad = 1,
            idAsignacion = 13,
            idPeriodo = 1,
            tipo = "TAREA",
            nombre = "Taller en Clase 1",
            descripcion = "Ejercicios de historia",
            fechaEntrega = "2026-08-30",
            ponderacion = 25.0,
            notaMaxima = 10.0,
            esSumativa = false
        )
        assertFalse(actividadFormativa.esSumativa)
        assertEquals(25.0, actividadFormativa.ponderacion, 0.001)

        val actividadSumativa = ActividadAcademica(
            idActividad = 2,
            idAsignacion = 13,
            idPeriodo = 1,
            tipo = "EXAMEN",
            nombre = "Examen Trimestral",
            fechaEntrega = "2026-09-15",
            ponderacion = 30.0,
            notaMaxima = 10.0,
            esSumativa = true
        )
        assertTrue(actividadSumativa.esSumativa)
        assertEquals(30.0, actividadSumativa.ponderacion, 0.001)
    }
}
