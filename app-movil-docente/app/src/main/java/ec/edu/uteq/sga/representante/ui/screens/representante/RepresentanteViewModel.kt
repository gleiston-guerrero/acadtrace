package ec.edu.uteq.sga.representante.ui.screens.representante

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import ec.edu.uteq.sga.representante.core.Resource
import ec.edu.uteq.sga.representante.domain.model.*
import ec.edu.uteq.sga.representante.domain.repository.RepresentanteRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ConsultaUiState<T>(val loading: Boolean = false, val data: T? = null, val error: String? = null)

class RepresentanteViewModel(private val repository: RepresentanteRepository) : ViewModel() {
    private val _representados = MutableStateFlow(ConsultaUiState<List<Representado>>())
    val representados = _representados.asStateFlow()
    private val _calificaciones = MutableStateFlow(ConsultaUiState<CalificacionesRepresentado>())
    val calificaciones = _calificaciones.asStateFlow()
    private val _asistencia = MutableStateFlow(ConsultaUiState<AsistenciaRepresentado>())
    val asistencia = _asistencia.asStateFlow()

    init { cargarRepresentados() }
    fun cargarRepresentados() = collect(repository.getRepresentados(), _representados)
    fun cargarCalificaciones(id: Long) = collect(repository.getCalificaciones(id), _calificaciones)
    fun cargarAsistencia(id: Long) = collect(repository.getAsistencia(id), _asistencia)

    private fun <T> collect(source: Flow<Resource<T>>, state: MutableStateFlow<ConsultaUiState<T>>) = viewModelScope.launch {
        var terminalStateReceived = false
        source
            .catch { error ->
                Log.e(TAG, "Consulta finalizada por excepción local: ${error::class.java.simpleName}: ${error.message}")
                terminalStateReceived = true
                emit(Resource.Error("Error local inesperado (${error::class.java.simpleName})", error))
            }
            .collect { result ->
                state.value = ConsultaStateReducer.reduce(result)
                terminalStateReceived = result !is Resource.Loading
            }
        if (!terminalStateReceived && state.value.loading) {
            state.value = ConsultaUiState(error = "La consulta terminó sin respuesta del servicio")
        }
    }

    private companion object { const val TAG = "RepresentanteViewModel" }
}

object ConsultaStateReducer {
    fun <T> reduce(result: Resource<T>): ConsultaUiState<T> = when (result) {
        Resource.Loading -> ConsultaUiState(loading = true)
        is Resource.Success -> ConsultaUiState(data = result.data)
        is Resource.Error -> ConsultaUiState(error = result.message)
    }
}
