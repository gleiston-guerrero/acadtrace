package ec.edu.uteq.sga.docente.ui.screens.seguimiento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.sga.docente.core.Resource
import ec.edu.uteq.sga.docente.core.SessionManager
import ec.edu.uteq.sga.docente.data.remote.dto.SeguimientoCreateDTO
import ec.edu.uteq.sga.docente.domain.model.PeriodoEvaluacion
import ec.edu.uteq.sga.docente.domain.model.SeguimientoItem
import ec.edu.uteq.sga.docente.domain.repository.DocenteRepository
import ec.edu.uteq.sga.docente.domain.repository.SeguimientoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SeguimientoUiState(
    val items: List<SeguimientoItem> = emptyList(),
    val periodos: List<PeriodoEvaluacion> = emptyList(),
    val selectedPeriodo: PeriodoEvaluacion? = null,
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)

class SeguimientoViewModel(
    private val seguimientoRepository: SeguimientoRepository,
    private val docenteRepository: DocenteRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeguimientoUiState())
    val uiState: StateFlow<SeguimientoUiState> = _uiState.asStateFlow()

    fun init(idMatricula: Long?) {
        loadPeriodos()
        loadSeguimientos(idMatricula)
    }

    private fun loadPeriodos() {
        viewModelScope.launch {
            docenteRepository.getPeriodosEvaluacion().collect { res ->
                if (res is Resource.Success) {
                    _uiState.value = _uiState.value.copy(
                        periodos = res.data,
                        selectedPeriodo = res.data.firstOrNull()
                    )
                }
            }
        }
    }

    fun loadSeguimientos(idMatricula: Long?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            seguimientoRepository.getSeguimientos(idMatricula).collect { res ->
                when (res) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            items = res.data,
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

    fun guardarSeguimiento(
        idMatricula: Long,
        idPeriodo: Long,
        categoria: String,
        descripcion: String,
        accionesTomadas: String?,
        requiereFollowup: Boolean,
        fechaEvento: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val dto = SeguimientoCreateDTO(
            idMatricula = idMatricula,
            idPeriodo = idPeriodo,
            categoria = categoria,
            descripcion = descripcion,
            accionesTomadas = accionesTomadas,
            requiereFollowup = requiereFollowup,
            fechaEvento = fechaEvento,
            registradoPor = sessionManager.getUserId()
        )

        viewModelScope.launch {
            val res = seguimientoRepository.createSeguimiento(dto)
            when (res) {
                is Resource.Success -> {
                    onSuccess()
                    loadSeguimientos(idMatricula)
                }
                is Resource.Error -> onError(res.message)
                is Resource.Loading -> {}
            }
        }
    }
}
