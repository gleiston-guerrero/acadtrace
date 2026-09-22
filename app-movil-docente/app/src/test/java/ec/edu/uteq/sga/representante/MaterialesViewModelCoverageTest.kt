package ec.edu.uteq.sga.representante

import ec.edu.uteq.sga.representante.core.Resource
import ec.edu.uteq.sga.representante.data.remote.dto.MaterialCreateDTO
import ec.edu.uteq.sga.representante.domain.model.MaterialCurso
import ec.edu.uteq.sga.representante.domain.repository.MaterialesRepository
import ec.edu.uteq.sga.representante.ui.screens.materiales.MaterialesViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MaterialesViewModelCoverageTest {
    @get:Rule val main = CoverageMainDispatcherRule()
    private val docente = CoverageDocenteRepository()
    private val repository = FakeMateriales()

    @Test fun cargaErrorYRecuperacionOfflineConCambioDeCurso() = runTest {
        val vm = MaterialesViewModel(repository, docente)
        vm.init(10)
        assertEquals(docente.curso, vm.uiState.value.selectedAsignacion)
        assertTrue(vm.uiState.value.isLoading)
        repository.flow.value = Resource.Error("sin red")
        assertEquals("sin red", vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isLoading)
        repository.flow.value = Resource.Success(listOf(repository.material), isOffline = true)
        assertEquals(listOf(repository.material), vm.uiState.value.materiales)
        assertTrue(vm.uiState.value.isOffline)
        assertNull(vm.uiState.value.errorMessage)
        vm.selectAsignacion(docente.curso.copy(idAsignacion = 11))
        assertEquals(11L, repository.requests.last())
        assertEquals(11L, vm.uiState.value.idAsignacion)
        val count = repository.requests.size
        vm.loadMateriales(0)
        assertEquals(count, repository.requests.size)
    }

    @Test fun cursoInicialSinIdUsaPrimeroYSubidaYBorradoRecargan() = runTest {
        val vm = MaterialesViewModel(repository, docente)
        vm.init(0)
        assertEquals(docente.curso, vm.uiState.value.selectedAsignacion)
        assertEquals(10L, vm.uiState.value.idAsignacion)
        val before = repository.requests.size
        vm.subirMaterial("Lectura", "Capítulo 1", "PDF", "https://example.test/lectura.pdf")
        val dto = requireNotNull(repository.created)
        assertEquals(10L, dto.idAsignacion)
        assertEquals("Lectura", dto.titulo)
        assertEquals("Capítulo 1", dto.descripcion)
        assertEquals("PDF", dto.tipo)
        assertEquals("https://example.test/lectura.pdf", dto.url)
        assertEquals(before + 1, repository.requests.size)
        vm.eliminarMaterial(7)
        assertEquals(7L, repository.deleted)
        assertEquals(before + 2, repository.requests.size)
    }

    private class FakeMateriales : MaterialesRepository {
        val material = MaterialCurso(7, 10, "PDF", "Lectura", null, "https://example.test/lectura.pdf", null, null)
        val flow = MutableStateFlow<Resource<List<MaterialCurso>>>(Resource.Loading)
        val requests = mutableListOf<Long>()
        var created: MaterialCreateDTO? = null
        var deleted: Long? = null
        override fun getMateriales(idAsignacion: Long) = flow.also { requests += idAsignacion }
        override suspend fun createMaterial(material: MaterialCreateDTO) = Resource.Success(this.material).also { created = material }
        override suspend fun deleteMaterial(id: Long) = Resource.Success(Unit).also { deleted = id }
    }
}
