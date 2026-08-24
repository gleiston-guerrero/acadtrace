package ec.edu.uteq.sga.docente.ui.screens.horario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.sga.docente.core.Resource
import ec.edu.uteq.sga.docente.core.SessionManager
import ec.edu.uteq.sga.docente.domain.model.HorarioItem
import ec.edu.uteq.sga.docente.domain.repository.HorarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HorarioUiState(
    val slots: List<HorarioItem> = emptyList(),
    val selectedDia: Int = 1, // 1=Lunes .. 5=Viernes
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)

class HorarioViewModel(
    private val horarioRepository: HorarioRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HorarioUiState())
    val uiState: StateFlow<HorarioUiState> = _uiState.asStateFlow()

    init {
        loadHorario()
    }

    fun setDia(dia: Int) {
        _uiState.value = _uiState.value.copy(selectedDia = dia)
    }

    fun loadHorario() {
        val idPersona = sessionManager.getIdPersona()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            horarioRepository.getHorarios(idPersona = idPersona).collect { res ->
                when (res) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            slots = res.data,
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
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                }
            }
        }
    }
}
