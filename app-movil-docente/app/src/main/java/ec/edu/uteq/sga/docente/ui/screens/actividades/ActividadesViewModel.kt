package ec.edu.uteq.sga.docente.ui.screens.actividades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.sga.docente.core.Resource
import ec.edu.uteq.sga.docente.data.remote.dto.ActividadCreateDTO
import ec.edu.uteq.sga.docente.domain.model.ActividadAcademica
import ec.edu.uteq.sga.docente.domain.model.Asignacion
import ec.edu.uteq.sga.docente.domain.model.PeriodoEvaluacion
import ec.edu.uteq.sga.docente.domain.repository.ActividadesRepository
import ec.edu.uteq.sga.docente.domain.repository.DocenteRepository
import ec.edu.uteq.sga.docente.domain.rules.AcademicRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActividadesUiState(
    val idAsignacion: Long = 0,
    val asignaciones: List<Asignacion> = emptyList(),
    val selectedAsignacion: Asignacion? = null,
    val periodos: List<PeriodoEvaluacion> = emptyList(),
    val selectedPeriodo: PeriodoEvaluacion? = null,
    val actividades: List<ActividadAcademica> = emptyList(),
    val totalFormativa: Double = 0.0,
    val totalSumativa: Double = 0.0,
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
    val operationSuccess: Boolean = false
)

class ActividadesViewModel(
    private val actividadesRepository: ActividadesRepository,
    private val docenteRepository: DocenteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActividadesUiState())
    val uiState: StateFlow<ActividadesUiState> = _uiState.asStateFlow()

    private var loadActividadesJob: kotlinx.coroutines.Job? = null

    fun init(idAsignacion: Long) {
        _uiState.value = _uiState.value.copy(idAsignacion = idAsignacion)
        loadAsignaciones(idAsignacion)
        loadPeriodos()
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
                    loadActividades(current.idAsignacion, _uiState.value.selectedPeriodo?.idPeriodo)
                }
            }
        }
    }

    fun selectAsignacion(asignacion: Asignacion) {
        _uiState.value = _uiState.value.copy(
            selectedAsignacion = asignacion,
            idAsignacion = asignacion.idAsignacion
        )
        loadActividades(asignacion.idAsignacion, _uiState.value.selectedPeriodo?.idPeriodo)
    }

    private fun loadPeriodos() {
        viewModelScope.launch {
            docenteRepository.getPeriodosEvaluacion().collect { res ->
                if (res is Resource.Success) {
                    val periodos = res.data
                    val current = _uiState.value.selectedPeriodo ?: periodos.firstOrNull()
                    _uiState.value = _uiState.value.copy(
                        periodos = periodos,
                        selectedPeriodo = current
                    )
                }
            }
        }
    }

    fun selectPeriodo(periodo: PeriodoEvaluacion) {
        _uiState.value = _uiState.value.copy(selectedPeriodo = periodo)
        loadActividades(_uiState.value.idAsignacion, periodo.idPeriodo)
    }

    fun loadActividades(idAsignacion: Long, idPeriodo: Long?) {
        if (idAsignacion <= 0) return
        _uiState.value = _uiState.value.copy(isLoading = true)

        loadActividadesJob?.cancel()
        loadActividadesJob = viewModelScope.launch {
            actividadesRepository.getActividades(idAsignacion, idPeriodo).collect { res ->
                when (res) {
                    is Resource.Success -> {
                        val formativas = res.data.filter { !it.esSumativa }.sumOf { it.ponderacion }
                        val sumativas = res.data.filter { it.esSumativa }.sumOf { it.ponderacion }
                        _uiState.value = _uiState.value.copy(
                            actividades = res.data,
                            totalFormativa = formativas,
                            totalSumativa = sumativas,
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

    fun saveActividad(
        idActividad: Long?,
        idAsignacion: Long,
        idPeriodo: Long,
        tipo: String,
        nombre: String,
        descripcion: String?,
        fechaEntrega: String,
        ponderacion: Double,
        notaMaxima: Double,
        esSumativa: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val dto = ActividadCreateDTO(
            idAsignacion = idAsignacion,
            idPeriodo = idPeriodo,
            tipo = tipo,
            nombre = nombre,
            descripcion = descripcion,
            fechaEntrega = fechaEntrega,
            ponderacion = ponderacion,
            notaMaxima = notaMaxima,
            esSumativa = esSumativa
        )

        // Validación de ponderaciones
        val currentPond = if (esSumativa) _uiState.value.totalSumativa else _uiState.value.totalFormativa
        val existingPond = if (idActividad != null) {
            _uiState.value.actividades.find { it.idActividad == idActividad }?.ponderacion ?: 0.0
        } else 0.0
        val (valido, msg) = AcademicRules.validarPonderacion(currentPond - existingPond, ponderacion, esSumativa)
        if (!valido) {
            onError(msg ?: "Ponderación inválida")
            return
        }

        viewModelScope.launch {
            val result = if (idActividad != null && idActividad > 0) {
                actividadesRepository.updateActividad(idActividad, dto)
            } else {
                actividadesRepository.createActividad(dto)
            }

            when (result) {
                is Resource.Success -> {
                    onSuccess()
                    loadActividades(idAsignacion, idPeriodo)
                }
                is Resource.Error -> onError(result.message)
                is Resource.Loading -> {}
            }
        }
    }

    fun deleteActividad(idActividad: Long) {
        viewModelScope.launch {
            actividadesRepository.deleteActividad(idActividad)
            loadActividades(_uiState.value.idAsignacion, _uiState.value.selectedPeriodo?.idPeriodo)
        }
    }
}
