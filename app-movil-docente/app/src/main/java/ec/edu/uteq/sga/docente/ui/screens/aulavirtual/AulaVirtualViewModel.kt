package ec.edu.uteq.sga.docente.ui.screens.aulavirtual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.sga.docente.core.Resource
import ec.edu.uteq.sga.docente.data.remote.dto.AulaVirtualSemanasResponseDTO
import ec.edu.uteq.sga.docente.domain.model.Asignacion
import ec.edu.uteq.sga.docente.domain.repository.AulaVirtualRepository
import ec.edu.uteq.sga.docente.domain.repository.DocenteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AulaVirtualUiState(
    val idAsignacion: Long = 0,
    val asignaciones: List<Asignacion> = emptyList(),
    val selectedAsignacion: Asignacion? = null,
    val agenda: AulaVirtualSemanasResponseDTO? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AulaVirtualViewModel(
    private val aulaVirtualRepository: AulaVirtualRepository,
    private val docenteRepository: DocenteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AulaVirtualUiState())
    val uiState: StateFlow<AulaVirtualUiState> = _uiState.asStateFlow()

    fun init(idAsignacion: Long) {
        _uiState.value = _uiState.value.copy(idAsignacion = idAsignacion, isLoading = true, errorMessage = null)
        loadAsignaciones(idAsignacion)
        loadAgenda(idAsignacion)
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
                    loadAgenda(current.idAsignacion)
                }
            }
        }
    }

    fun selectAsignacion(asignacion: Asignacion) {
        _uiState.value = _uiState.value.copy(
            selectedAsignacion = asignacion,
            idAsignacion = asignacion.idAsignacion
        )
        loadAgenda(asignacion.idAsignacion)
    }

    fun loadAgenda(idAsignacion: Long) {
        _uiState.value = _uiState.value.copy(idAsignacion = idAsignacion, isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val res = aulaVirtualRepository.getAgendaSemanas(idAsignacion)
            when (res) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        agenda = res.data,
                        isLoading = false,
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
