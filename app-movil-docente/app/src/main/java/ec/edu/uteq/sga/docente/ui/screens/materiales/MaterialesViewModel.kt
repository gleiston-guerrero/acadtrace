package ec.edu.uteq.sga.docente.ui.screens.materiales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.sga.docente.core.Resource
import ec.edu.uteq.sga.docente.data.remote.dto.MaterialCreateDTO
import ec.edu.uteq.sga.docente.domain.model.Asignacion
import ec.edu.uteq.sga.docente.domain.model.MaterialCurso
import ec.edu.uteq.sga.docente.domain.repository.DocenteRepository
import ec.edu.uteq.sga.docente.domain.repository.MaterialesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MaterialesUiState(
    val idAsignacion: Long = 0,
    val asignaciones: List<Asignacion> = emptyList(),
    val selectedAsignacion: Asignacion? = null,
    val materiales: List<MaterialCurso> = emptyList(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)

class MaterialesViewModel(
    private val materialesRepository: MaterialesRepository,
    private val docenteRepository: DocenteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MaterialesUiState())
    val uiState: StateFlow<MaterialesUiState> = _uiState.asStateFlow()

    fun init(idAsignacion: Long) {
        _uiState.value = _uiState.value.copy(idAsignacion = idAsignacion)
        loadAsignaciones(idAsignacion)
        loadMateriales(idAsignacion)
    }

    private fun loadAsignaciones(idAsignacion: Long) {
        viewModelScope.launch {
            docenteRepository.getAsignaciones().collect { res ->
                if (res is Resource.Success && res.data.isNotEmpty()) {
                    val current = if (idAsignacion > 0) {
                        res.data.find { it.idAsignacion == idAsignacion } ?: res.data.first()
                    } else {
                        res.data.first()
                    }
                    _uiState.value = _uiState.value.copy(
                        asignaciones = res.data,
                        selectedAsignacion = current,
                        idAsignacion = current.idAsignacion
                    )
                    loadMateriales(current.idAsignacion)
                }
            }
        }
    }

    fun selectAsignacion(asignacion: Asignacion) {
        _uiState.value = _uiState.value.copy(
            selectedAsignacion = asignacion,
            idAsignacion = asignacion.idAsignacion
        )
        loadMateriales(asignacion.idAsignacion)
    }

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
