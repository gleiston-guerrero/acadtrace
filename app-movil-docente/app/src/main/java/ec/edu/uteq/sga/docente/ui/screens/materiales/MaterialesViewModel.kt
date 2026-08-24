package ec.edu.uteq.sga.docente.ui.screens.materiales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.sga.docente.core.Resource
import ec.edu.uteq.sga.docente.data.remote.dto.MaterialCreateDTO
import ec.edu.uteq.sga.docente.domain.model.MaterialCurso
import ec.edu.uteq.sga.docente.domain.repository.MaterialesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MaterialesUiState(
    val idAsignacion: Long = 0,
    val materiales: List<MaterialCurso> = emptyList(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)

class MaterialesViewModel(
    private val materialesRepository: MaterialesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MaterialesUiState())
    val uiState: StateFlow<MaterialesUiState> = _uiState.asStateFlow()

    fun loadMateriales(idAsignacion: Long) {
        _uiState.value = _uiState.value.copy(idAsignacion = idAsignacion, isLoading = true)
        viewModelScope.launch {
            materialesRepository.getMateriales(idAsignacion).collect { res ->
                when (res) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            materiales = res.data,
                            isLoading = false,
                            isOffline = res.isOffline,
                            errorMessage = null
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = res.message
                        )
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    fun subirMaterial(titulo: String, descripcion: String?, tipo: String, url: String) {
        val dto = MaterialCreateDTO(
            idAsignacion = _uiState.value.idAsignacion,
            titulo = titulo,
            descripcion = descripcion,
            tipo = tipo,
            url = url
        )

        viewModelScope.launch {
            materialesRepository.createMaterial(dto)
            loadMateriales(_uiState.value.idAsignacion)
        }
    }

    fun eliminarMaterial(idMaterial: Long) {
        viewModelScope.launch {
            materialesRepository.deleteMaterial(idMaterial)
            loadMateriales(_uiState.value.idAsignacion)
        }
    }
}
