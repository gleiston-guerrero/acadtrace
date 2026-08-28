package ec.edu.uteq.sga.docente.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.sga.docente.core.Resource
import ec.edu.uteq.sga.docente.core.SessionManager
import ec.edu.uteq.sga.docente.data.sync.SyncManager
import ec.edu.uteq.sga.docente.domain.model.Asignacion
import ec.edu.uteq.sga.docente.domain.repository.AuthRepository
import ec.edu.uteq.sga.docente.domain.repository.DocenteRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val teacherName: String = "",
    val teacherUsername: String = "",
    val asignaciones: List<Asignacion> = emptyList(),
    val totalEstudiantes: Int = 0,
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val pendingSyncCount: Int = 0,
    val errorMessage: String? = null
)

class DashboardViewModel(
    private val authRepository: AuthRepository,
    private val docenteRepository: DocenteRepository,
    private val syncManager: SyncManager,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            teacherUsername = sessionManager.getUsername() ?: "Docente",
            teacherName = sessionManager.getUsername() ?: "Docente"
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observePendingSync()
    }

    private fun observePendingSync() {
        viewModelScope.launch {
            syncManager.pendingCountFlow.collect { count ->
                _uiState.value = _uiState.value.copy(pendingSyncCount = count)
            }
        }
    }

    private var loadDataJob: kotlinx.coroutines.Job? = null

    fun loadData() {
        loadDataJob?.cancel()
        loadDataJob = viewModelScope.launch {
            docenteRepository.getAsignaciones().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val totalEst = resource.data.sumOf { it.cantidadEstudiantes }
                        _uiState.value = _uiState.value.copy(
                            asignaciones = resource.data,
                            totalEstudiantes = totalEst,
                            isLoading = false,
                            isOffline = resource.isOffline,
                            errorMessage = null
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

    fun syncNow() {
        viewModelScope.launch {
            syncManager.syncPendingOperations()
            loadData()
        }
    }

    fun logout() {
        authRepository.logout()
    }
}
