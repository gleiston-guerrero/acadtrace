package ec.edu.uteq.sga.docente.ui.screens.aulavirtual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.sga.docente.core.Resource
import ec.edu.uteq.sga.docente.data.remote.dto.AulaVirtualSemanasResponseDTO
import ec.edu.uteq.sga.docente.domain.repository.AulaVirtualRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AulaVirtualUiState(
    val idAsignacion: Long = 0,
    val agenda: AulaVirtualSemanasResponseDTO? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AulaVirtualViewModel(
    private val aulaVirtualRepository: AulaVirtualRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AulaVirtualUiState())
    val uiState: StateFlow<AulaVirtualUiState> = _uiState.asStateFlow()

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
