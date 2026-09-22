package ec.edu.uteq.sga.representante

import ec.edu.uteq.sga.representante.core.Resource
import ec.edu.uteq.sga.representante.domain.model.*
import ec.edu.uteq.sga.representante.domain.repository.PromediosRepository
import ec.edu.uteq.sga.representante.ui.screens.reportes.ReportesViewModel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ReportesViewModelCoverageTest {
    @get:Rule val main = CoverageMainDispatcherRule()
    private val docente = CoverageDocenteRepository()
    private val repository = FakePromedios()

    @Test fun combinaPromediosPorMatriculaConservandoEstudianteSinNotas() = runTest {
        val ana = AsistenciaViewModelCoverageTest.estudiante(1, "Ana")
        val luis = AsistenciaViewModelCoverageTest.estudiante(2, "Luis")
        docente.estudiantes = flowOf(Resource.Loading, Resource.Success(listOf(ana, luis)))
        val vm = ReportesViewModel(repository, docente)
        vm.init(10)
        assertEquals(listOf(ana, luis), vm.uiState.value.items.map { it.estudiante })
        assertEquals(repository.trim, vm.uiState.value.items[0].promedioTrimestral)
        assertEquals(repository.anual, vm.uiState.value.items[0].promedioAnual)
        assertNull(vm.uiState.value.items[1].promedioTrimestral)
        assertNull(vm.uiState.value.items[1].promedioAnual)
        assertTrue(vm.uiState.value.isOffline)
        assertFalse(vm.uiState.value.isLoading)
        vm.setVistaAnual(true)
        assertTrue(vm.uiState.value.vistaAnual)
        vm.selectPeriodo(docente.periodo.copy(idPeriodo = 21))
        assertFalse(vm.uiState.value.vistaAnual)
        assertEquals(10L to 21L, repository.requests.last())
        vm.selectAsignacion(docente.curso.copy(idAsignacion = 11))
        assertEquals(11L to 21L, repository.requests.last())
        vm.recalcularPromedioTrimestral(1)
        assertEquals(Triple(1L, 11L, 21L), repository.calculated)
    }

    @Test fun seleccionAusenteUsaPrimerCursoYRecalculoSinPeriodoUsaPredeterminado() = runTest {
        docente.periodos = flowOf(Resource.Success(emptyList()))
        val vm = ReportesViewModel(repository, docente)
        vm.init(999)
        assertEquals(docente.curso, vm.uiState.value.selectedAsignacion)
        assertNull(vm.uiState.value.selectedPeriodo)
        vm.recalcularPromedioTrimestral(2)
        assertEquals(Triple(2L, 10L, 1L), repository.calculated)
        val calls = repository.requests.size
        vm.loadData(0)
        assertEquals(calls, repository.requests.size)
    }

    private class FakePromedios : PromediosRepository {
        val trim = PromedioTrimestral(1, 1, 10, 20, 8.0, 9.0, 8.3, "B_MAS")
        val anual = PromedioAnual(1, 1, 10, 1, 8.3, "B_MAS")
        val requests = mutableListOf<Pair<Long, Long?>>()
        var calculated: Triple<Long, Long, Long>? = null
        override fun getPromediosTrimestrales(idAsignacion: Long, idPeriodo: Long?) =
            flowOf(Resource.Loading, Resource.Success(listOf(trim), isOffline = true)).also {
                requests += idAsignacion to idPeriodo
            }
        override fun getPromediosAnuales(idAsignacion: Long) = flowOf(Resource.Loading, Resource.Success(listOf(anual)))
        override suspend fun calcularPromedioTrimestral(idMatricula: Long, idAsignacion: Long, idPeriodo: Long) =
            Resource.Success(trim).also { calculated = Triple(idMatricula, idAsignacion, idPeriodo) }
        override suspend fun calcularPromedioAnual(idMatricula: Long, idAsignacion: Long, idAnoLectivo: Long): Resource<PromedioAnual> = error("No esperado")
    }
}
