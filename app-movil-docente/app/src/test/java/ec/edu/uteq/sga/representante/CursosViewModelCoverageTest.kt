package ec.edu.uteq.sga.representante

import ec.edu.uteq.sga.representante.core.Resource
import ec.edu.uteq.sga.representante.domain.model.Estudiante
import ec.edu.uteq.sga.representante.ui.screens.cursos.CursosViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CursosViewModelCoverageTest {
    @get:Rule val main = CoverageMainDispatcherRule()

    @Test fun validaIdYCargaNominaConErrorAntesYDespuesDeCache() = runTest {
        val docente = CoverageDocenteRepository()
        val estudiantes = MutableStateFlow<Resource<List<Estudiante>>>(Resource.Loading)
        docente.estudiantes = estudiantes
        val vm = CursosViewModel(docente)
        vm.loadCurso(0)
        assertTrue(docente.solicitudesEstudiantes.isEmpty())
        vm.loadCurso(10)
        assertTrue(vm.uiState.value.isLoading)
        assertEquals(docente.curso, vm.uiState.value.asignacion)
        estudiantes.value = Resource.Error("sin conexión")
        assertEquals("sin conexión", vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isLoading)
        val ana = AsistenciaViewModelCoverageTest.estudiante(1, "Ana")
        estudiantes.value = Resource.Success(listOf(ana), isOffline = true)
        assertEquals(listOf(ana), vm.uiState.value.estudiantes)
        assertTrue(vm.uiState.value.isOffline)
        assertNull(vm.uiState.value.errorMessage)
        estudiantes.value = Resource.Error("red perdida")
        assertEquals(listOf(ana), vm.uiState.value.estudiantes)
        assertNull(vm.uiState.value.errorMessage)
        vm.loadCurso(10)
        assertEquals(listOf(10L, 10L), docente.solicitudesEstudiantes)
    }

    @Test fun cursoDesconocidoNoInventaAsignacion() = runTest {
        val docente = CoverageDocenteRepository()
        val vm = CursosViewModel(docente)
        vm.loadCurso(999)
        assertNull(vm.uiState.value.asignacion)
        assertEquals(999L, vm.uiState.value.idAsignacion)
        assertTrue(vm.uiState.value.estudiantes.isEmpty())
        assertFalse(vm.uiState.value.isLoading)
    }
}
