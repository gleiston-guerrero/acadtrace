package ec.edu.uteq.sga.docente.ui.screens.cursos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.sga.docente.core.Resource
import ec.edu.uteq.sga.docente.domain.model.Asignacion
import ec.edu.uteq.sga.docente.domain.model.Estudiante
import ec.edu.uteq.sga.docente.domain.repository.DocenteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetalleCursoUiState(
    val asignacion: Asignacion? = null,
    val estudiantes: List<Estudiante> = emptyList(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)

class CursosViewModel(
    private val docenteRepository: DocenteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetalleCursoUiState())
    val uiState: StateFlow<DetalleCursoUiState> = _uiState.asStateFlow()

    fun loadCurso(idAsignacion: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // Cargar asignación
            docenteRepository.getAsignaciones().collect { resource ->
                if (resource is Resource.Success) {
                    val curso = resource.data.find { it.idAsignacion == idAsignacion }
                    _uiState.value = _uiState.value.copy(
                        asignacion = curso,
                        isOffline = resource.isOffline
                    )
                }
            }
        }

        viewModelScope.launch {
            // Cargar estudiantes
            docenteRepository.getEstudiantesPorAsignacion(idAsignacion).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            estudiantes = resource.data,
                            isLoading = false,
                            isOffline = resource.isOffline
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = resource.message
                        )
                    }
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                }
            }
        }
    }
}
