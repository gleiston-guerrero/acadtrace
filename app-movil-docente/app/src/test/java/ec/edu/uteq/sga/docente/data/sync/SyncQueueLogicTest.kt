package ec.edu.uteq.sga.docente.data.sync

import com.google.gson.Gson
import ec.edu.uteq.sga.docente.data.local.entity.PendingSyncEntity
import ec.edu.uteq.sga.docente.data.remote.dto.ActividadCreateDTO
import ec.edu.uteq.sga.docente.data.remote.dto.AsistenciaCreateDTO
import ec.edu.uteq.sga.docente.data.remote.dto.CalificacionCreateDTO
import org.junit.Assert.*
import org.junit.Test

class SyncQueueLogicTest {

    private val gson = Gson()

    @Test
    fun `serializacion y deserializacion de payload para crear Actividad`() {
        val actividadDTO = ActividadCreateDTO(
            idAsignacion = 13,
            idPeriodo = 1,
            tipo = "TAREA",
            nombre = "Investigación Offline",
            descripcion = "Tema 1",
            fechaEntrega = "2026-09-01",
            ponderacion = 20.0,
            notaMaxima = 10.0,
            esSumativa = false
        )

        val json = gson.toJson(actividadDTO)
        val entity = PendingSyncEntity(
            id = 1,
            entityType = "ACTIVIDAD",
            actionType = "CREATE",
            localId = -100L,
            remoteId = null,
            payloadJson = json,
            attempts = 0
        )

        assertEquals("ACTIVIDAD", entity.entityType)
        assertEquals("CREATE", entity.actionType)
        assertTrue(entity.localId < 0) // ID temporal negativo

        val deserialized = gson.fromJson(entity.payloadJson, ActividadCreateDTO::class.java)
        assertEquals("Investigación Offline", deserialized.nombre)
        assertEquals(20.0, deserialized.ponderacion, 0.001)
        assertEquals(13L, deserialized.idAsignacion)
    }

    @Test
    fun `serializacion y deserializacion de payload para guardar Asistencia`() {
        val asistenciaDTO = AsistenciaCreateDTO(
            idMatricula = 621,
            idAsignacion = 13,
            idPeriodo = 1,
            fecha = "2026-08-25",
            estado = "PRESENTE",
            justificacion = null
        )

        val json = gson.toJson(asistenciaDTO)
        val entity = PendingSyncEntity(
            id = 2,
            entityType = "ASISTENCIA",
            actionType = "CREATE",
            localId = -200L,
            payloadJson = json
        )

        val deserialized = gson.fromJson(entity.payloadJson, AsistenciaCreateDTO::class.java)
        assertEquals(621L, deserialized.idMatricula)
        assertEquals("PRESENTE", deserialized.estado)
        assertEquals("2026-08-25", deserialized.fecha)
    }

    @Test
    fun `serializacion y deserializacion de payload para guardar Calificacion`() {
        val calificacionDTO = CalificacionCreateDTO(
            idActividad = 50,
            idMatricula = 621,
            nota = 9.50,
            observacion = "Excelente trabajo individual"
        )

        val json = gson.toJson(calificacionDTO)
        val entity = PendingSyncEntity(
            id = 3,
            entityType = "CALIFICACION",
            actionType = "CREATE",
            localId = -300L,
            payloadJson = json
        )

        val deserialized = gson.fromJson(entity.payloadJson, CalificacionCreateDTO::class.java)
        assertEquals(50L, deserialized.idActividad)
        assertEquals(9.50, deserialized.nota, 0.001)
        assertEquals("Excelente trabajo individual", deserialized.observacion)
    }
}
