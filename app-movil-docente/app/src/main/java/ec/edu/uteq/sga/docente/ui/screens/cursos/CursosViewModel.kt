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
    val idAsignacion: Long = 0,
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
        _uiState.value = _uiState.value.copy(idAsignacion = idAsignacion, isLoading = true, errorMessage = null)

        // 1. Cargar asignación (caché y red reactiva)
        viewModelScope.launch {
            docenteRepository.getAsignaciones().collect { res ->
                if (res is Resource.Success) {
                    val curso = res.data.find { it.idAsignacion == idAsignacion }
                    if (curso != null) {
                        _uiState.value = _uiState.value.copy(
                            asignacion = curso,
                            isOffline = res.isOffline
                        )
                    }
                }
            }
        }

        // 2. Cargar nómina de estudiantes
        viewModelScope.launch {
            docenteRepository.getEstudiantesPorAsignacion(idAsignacion).collect { res ->
                when (res) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            estudiantes = res.data,
                            isLoading = false,
                            isOffline = res.isOffline,
                            errorMessage = null
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = if (_uiState.value.estudiantes.isEmpty()) res.message else null
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
