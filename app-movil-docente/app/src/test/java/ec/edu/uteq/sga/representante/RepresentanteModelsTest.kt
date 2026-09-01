package ec.edu.uteq.sga.representante

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ec.edu.uteq.sga.representante.data.remote.dto.AsistenciaRepresentadoDTO
import ec.edu.uteq.sga.representante.data.remote.dto.CalificacionesRepresentadoDTO
import ec.edu.uteq.sga.representante.data.remote.dto.RepresentadoDTO
import ec.edu.uteq.sga.representante.domain.model.*
import ec.edu.uteq.sga.representante.ui.screens.representante.ConsultaUiState
import ec.edu.uteq.sga.representante.ui.screens.representante.ConsultaStateReducer
import ec.edu.uteq.sga.representante.core.Resource
import org.junit.Assert.*
import org.junit.Test

class RepresentanteModelsTest {
    private val gson = Gson()

    @Test fun representadoConservaIdentidadYMatriculasAutorizadas() {
        val item = Representado(11, "Ana", "Paz", "Séptimo", "A", listOf(21, 22))
        assertEquals("Ana Paz", item.nombreCompleto)
        assertEquals(11, item.idEstudiante)
        assertEquals(listOf(21L, 22L), item.matriculas)
    }
    @Test fun estadosEmptyErrorYSuccessSonDistinguibles() {
        val empty = ConsultaUiState(data = emptyList<Representado>())
        val error = ConsultaUiState<List<Representado>>(error = "No autorizado")
        val loading = ConsultaUiState<List<Representado>>(loading = true)
        assertTrue(empty.data!!.isEmpty())
        assertEquals("No autorizado", error.error)
        assertTrue(loading.loading)
    }

    @Test fun consultaExitosaAbandonaLoadingInclusoConListaVacia() {
        val state = ConsultaStateReducer.reduce(Resource.Success(emptyList<Representado>()))
        assertFalse(state.loading)
        assertTrue(state.data!!.isEmpty())
        assertNull(state.error)
    }

    @Test fun consultaHttpErrorAbandonaLoading() {
        val state = ConsultaStateReducer.reduce<CalificacionesRepresentado>(Resource.Error("Error HTTP del servicio (500)"))
        assertFalse(state.loading)
        assertNull(state.data)
        assertEquals("Error HTTP del servicio (500)", state.error)
    }

    @Test fun consultaDeRedAbandonaLoading() {
        val state = ConsultaStateReducer.reduce<AsistenciaRepresentado>(Resource.Error("Sin conexión y sin datos almacenados"))
        assertFalse(state.loading)
        assertNull(state.data)
        assertEquals("Sin conexión y sin datos almacenados", state.error)
    }
    @Test fun resumenAsistenciaMantieneEstadosReales() {
        val resumen = ResumenAsistenciaHijo(10, 7, 1, 1, 1, 70.0)
        assertEquals(7, resumen.presentes)
        assertEquals(1, resumen.justificados)
        assertEquals(70.0, resumen.porcentajeAsistencia, 0.0)
    }

    @Test fun deserializaArrayRealDeMisRepresentados() {
        val json = """[{"idEstudiante":681,"nombres":"Julieta","apellidos":"Paz","curso":"Décimo año EGB","paralelo":"C","matriculas":[680]}]"""
        val type = object : TypeToken<List<RepresentadoDTO>>() {}.type
        val result: List<RepresentadoDTO> = gson.fromJson(json, type)

        assertEquals(1, result.size)
        assertEquals(681L, result.single().idEstudiante)
        assertEquals("Julieta", result.single().nombres)
        assertEquals("Décimo año EGB", result.single().curso)
        assertEquals("C", result.single().paralelo)
        assertEquals(listOf(680L), result.single().matriculas)
    }

    @Test fun deserializaContratoRealDeCalificaciones() {
        val json = """{"calificaciones":[{"id_calificacion":1,"id_matricula":680,"id_actividad":2,"actividad":"Tarea","id_asignacion":3,"id_periodo":4,"periodo":"Primer trimestre","nota":9.5,"nota_cualitativa":"A"}],"promedios":[]}"""
        val result = gson.fromJson(json, CalificacionesRepresentadoDTO::class.java)

        assertEquals(680L, result.calificaciones.single().idMatricula)
        assertEquals(9.5, result.calificaciones.single().nota, 0.0)
    }

    @Test fun deserializaContratoRealDeAsistencia() {
        val json = """{"asistencias":[{"id_asistencia":1,"id_matricula":680,"id_asignacion":3,"id_periodo":4,"periodo":"Primer trimestre","fecha":"2026-09-01","estado":"PRESENTE"}],"resumen":{"total":1,"presentes":1,"ausentes":0,"justificados":0,"atrasos":0,"porcentaje_asistencia":100.0}}"""
        val result = gson.fromJson(json, AsistenciaRepresentadoDTO::class.java)

        assertEquals("PRESENTE", result.asistencias.single().estado)
        assertEquals(100.0, result.resumen.porcentajeAsistencia, 0.0)
    }
}
