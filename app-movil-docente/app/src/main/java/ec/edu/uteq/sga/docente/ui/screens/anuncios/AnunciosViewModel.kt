package ec.edu.uteq.sga.docente.ui.screens.anuncios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.sga.docente.core.Resource
import ec.edu.uteq.sga.docente.core.SessionManager
import ec.edu.uteq.sga.docente.data.remote.dto.AnuncioCreateDTO
import ec.edu.uteq.sga.docente.domain.model.AnuncioCurso
import ec.edu.uteq.sga.docente.domain.repository.AnunciosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AnunciosUiState(
    val idAsignacion: Long = 0,
    val anuncios: List<AnuncioCurso> = emptyList(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)

class AnunciosViewModel(
    private val anunciosRepository: AnunciosRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnunciosUiState())
    val uiState: StateFlow<AnunciosUiState> = _uiState.asStateFlow()

    fun loadAnuncios(idAsignacion: Long) {
        _uiState.value = _uiState.value.copy(idAsignacion = idAsignacion, isLoading = true)
        viewModelScope.launch {
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
