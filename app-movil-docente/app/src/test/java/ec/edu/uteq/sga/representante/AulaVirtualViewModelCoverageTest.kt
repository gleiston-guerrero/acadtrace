package ec.edu.uteq.sga.representante

import ec.edu.uteq.sga.representante.core.Resource
import ec.edu.uteq.sga.representante.data.remote.dto.*
import ec.edu.uteq.sga.representante.domain.repository.AulaVirtualRepository
import ec.edu.uteq.sga.representante.ui.screens.aulavirtual.AulaVirtualViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AulaVirtualViewModelCoverageTest {
    @get:Rule val main = CoverageMainDispatcherRule()

    @Test fun cargaAgendaYPropagaErrorAlSeleccionarOtroCurso() = runTest {
        val docente = CoverageDocenteRepository()
        val repository = FakeAula()
        val agenda = AulaVirtualSemanasResponseDTO()
        repository.result = Resource.Success(agenda)
        val vm = AulaVirtualViewModel(repository, docente)
        vm.init(10)
        assertEquals(docente.curso, vm.uiState.value.selectedAsignacion)
        assertSame(agenda, vm.uiState.value.agenda)
        assertFalse(vm.uiState.value.isLoading)
        repository.result = Resource.Error("agenda no disponible")
        vm.selectAsignacion(docente.curso.copy(idAsignacion = 11))
        assertEquals(11L, repository.requests.last())
        assertEquals("agenda no disponible", vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isLoading)
        repository.result = Resource.Loading
        vm.loadAgenda(11)
        assertTrue(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.errorMessage)
    }

    private class FakeAula : AulaVirtualRepository {
        var result: Resource<AulaVirtualSemanasResponseDTO> = Resource.Loading
        val requests = mutableListOf<Long>()
        override suspend fun getAgendaSemanas(idAsignacion: Long) = result.also { requests += idAsignacion }
        override suspend fun getResumenCursos(idsAsignacion: List<Long>): Resource<AulaVirtualResumenResponseDTO> = error("No esperado")
    }
}
