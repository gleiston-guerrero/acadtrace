package ec.edu.uteq.sga.representante

import ec.edu.uteq.sga.representante.core.Resource
import ec.edu.uteq.sga.representante.data.remote.dto.ActividadCreateDTO
import ec.edu.uteq.sga.representante.domain.model.ActividadAcademica
import ec.edu.uteq.sga.representante.domain.repository.ActividadesRepository
import ec.edu.uteq.sga.representante.ui.screens.actividades.ActividadesViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ActividadesViewModelCoverageTest {
    @get:Rule val main = CoverageMainDispatcherRule()
    private val docente = CoverageDocenteRepository()
    private val repository = FakeActividades()

    @Test fun cargaEstadosSumaPonderacionesYSeleccionaPeriodo() = runTest {
        val vm = ActividadesViewModel(repository, docente)
        vm.init(10)
        assertTrue(vm.uiState.value.isLoading)
        repository.flow.value = Resource.Error("sin red")
        assertEquals("sin red", vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isLoading)
        val actividades = listOf(actividad(1, 40.0), actividad(2, 30.0), actividad(3, 20.0, true))
        repository.flow.value = Resource.Success(actividades, isOffline = true)
        assertEquals(actividades, vm.uiState.value.actividades)
        assertEquals(70.0, vm.uiState.value.totalFormativa, 0.001)
        assertEquals(20.0, vm.uiState.value.totalSumativa, 0.001)
        assertTrue(vm.uiState.value.isOffline)
        assertNull(vm.uiState.value.errorMessage)
        vm.selectPeriodo(docente.periodo)
        assertEquals(10L to 20L, repository.requests.last())
        vm.selectAsignacion(docente.curso.copy(idAsignacion = 11))
        assertEquals(11L to 20L, repository.requests.last())
        val count = repository.requests.size
        vm.loadActividades(0, null)
        assertEquals(count, repository.requests.size)
    }

    @Test fun asignacionDesconocidaUsaPrimeraYPeriodosVaciosNoInventanSeleccion() = runTest {
        docente.periodos = kotlinx.coroutines.flow.flowOf(Resource.Success(emptyList()))
        val vm = ActividadesViewModel(repository, docente)
        vm.init(999)
        assertEquals(docente.curso, vm.uiState.value.selectedAsignacion)
        assertNull(vm.uiState.value.selectedPeriodo)
        assertEquals(10L to null, repository.requests.single())
    }

    @Test fun rechazaExcesoSinGuardarYEditarDescuentaPonderacionExistente() = runTest {
        repository.flow.value = Resource.Success(listOf(actividad(1, 70.0)))
        val vm = ActividadesViewModel(repository, docente)
        vm.init(10)
        var error: String? = null
        save(vm, null, 1.0, onError = { error = it })
        assertNotNull(error)
        assertTrue(repository.saved.isEmpty())
        var success = false
        save(vm, 1, 60.0, onSuccess = { success = true })
        assertTrue(success)
        assertEquals(1L, repository.saved.single().first)
        assertEquals(60.0, repository.saved.single().second.ponderacion, 0.001)
        assertEquals(10L to 20L, repository.requests.last())
    }

    @Test fun creaActividadPropagaErrorDeEdicionYEliminaConRecarga() = runTest {
        repository.flow.value = Resource.Success(emptyList())
        val vm = ActividadesViewModel(repository, docente)
        vm.init(10)
        var success = false
        save(vm, null, 20.0, sumativa = true, onSuccess = { success = true })
        assertTrue(success)
        val (id, dto) = repository.saved.single()
        assertNull(id)
        assertEquals(10L, dto.idAsignacion)
        assertEquals(20L, dto.idPeriodo)
        assertEquals("Examen", dto.nombre)
        assertTrue(dto.esSumativa)
        repository.result = Resource.Error("rechazado")
        var error: String? = null
        save(vm, 55, 10.0, onError = { error = it })
        assertEquals("rechazado", error)
        val requests = repository.requests.size
        vm.deleteActividad(55)
        assertEquals(55L, repository.deleted)
        assertEquals(requests + 1, repository.requests.size)
    }

    private fun save(vm: ActividadesViewModel, id: Long?, ponderacion: Double,
                     sumativa: Boolean = false, onSuccess: () -> Unit = {},
                     onError: (String) -> Unit = { fail(it) }) =
        vm.saveActividad(id, 10, 20, "EXAMEN", "Examen", null, "2026-09-22",
            ponderacion, 10.0, sumativa, onSuccess, onError)

    private class FakeActividades : ActividadesRepository {
        val flow = MutableStateFlow<Resource<List<ActividadAcademica>>>(Resource.Loading)
        val requests = mutableListOf<Pair<Long, Long?>>()
        val saved = mutableListOf<Pair<Long?, ActividadCreateDTO>>()
        var result: Resource<ActividadAcademica> = Resource.Success(actividad(99, 20.0))
        var deleted: Long? = null
        override fun getActividades(idAsignacion: Long, idPeriodo: Long?) = flow.also {
            requests += idAsignacion to idPeriodo
        }
        override suspend fun createActividad(actividad: ActividadCreateDTO) = result.also { saved += null to actividad }
        override suspend fun updateActividad(id: Long, actividad: ActividadCreateDTO) = result.also { saved += id to actividad }
        override suspend fun deleteActividad(id: Long): Resource<Unit> = Resource.Success(Unit).also { deleted = id }
    }

    companion object {
        private fun actividad(id: Long, ponderacion: Double, sumativa: Boolean = false) =
            ActividadAcademica(id, 10, 20, "EXAMEN", "Examen", fechaEntrega = "2026-09-22",
                ponderacion = ponderacion, esSumativa = sumativa)
    }
}
