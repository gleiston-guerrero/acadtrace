package ec.edu.uteq.sga.docente.ui.screens.anuncios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.sga.docente.core.Resource
import ec.edu.uteq.sga.docente.core.SessionManager
import ec.edu.uteq.sga.docente.data.remote.dto.AnuncioCreateDTO
import ec.edu.uteq.sga.docente.domain.model.AnuncioCurso
import ec.edu.uteq.sga.docente.domain.model.Asignacion
import ec.edu.uteq.sga.docente.domain.repository.AnunciosRepository
import ec.edu.uteq.sga.docente.domain.repository.DocenteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AnunciosUiState(
    val idAsignacion: Long = 0,
    val asignaciones: List<Asignacion> = emptyList(),
    val selectedAsignacion: Asignacion? = null,
    val anuncios: List<AnuncioCurso> = emptyList(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)

class AnunciosViewModel(
    private val anunciosRepository: AnunciosRepository,
    private val docenteRepository: DocenteRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnunciosUiState())
    val uiState: StateFlow<AnunciosUiState> = _uiState.asStateFlow()

    fun init(idAsignacion: Long) {
        _uiState.value = _uiState.value.copy(idAsignacion = idAsignacion)
        loadAsignaciones(idAsignacion)
        loadAnuncios(idAsignacion)
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
                    loadAnuncios(current.idAsignacion)
                }
            }
        }
    }

    fun selectAsignacion(asignacion: Asignacion) {
        _uiState.value = _uiState.value.copy(
            selectedAsignacion = asignacion,
            idAsignacion = asignacion.idAsignacion
        )
        loadAnuncios(asignacion.idAsignacion)
    }

    private var loadAnunciosJob: kotlinx.coroutines.Job? = null

    fun loadAnuncios(idAsignacion: Long) {
        if (idAsignacion <= 0) return
        _uiState.value = _uiState.value.copy(idAsignacion = idAsignacion, isLoading = true)
        loadAnunciosJob?.cancel()
        loadAnunciosJob = viewModelScope.launch {
            anunciosRepository.getAnuncios(idAsignacion).collect { res ->
                when (res) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            anuncios = res.data,
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

    fun publicarAnuncio(titulo: String, contenido: String, fijado: Boolean) {
        val dto = AnuncioCreateDTO(
            idAsignacion = _uiState.value.idAsignacion,
            titulo = titulo,
            contenido = contenido,
            autorId = sessionManager.getUserId(),
            fijado = fijado
        )

        viewModelScope.launch {
            anunciosRepository.createAnuncio(dto)
            loadAnuncios(_uiState.value.idAsignacion)
        }
    }

    fun eliminarAnuncio(idAnuncio: Long) {
        viewModelScope.launch {
            anunciosRepository.deleteAnuncio(idAnuncio)
            loadAnuncios(_uiState.value.idAsignacion)
        }
    }
}
