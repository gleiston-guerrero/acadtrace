package ec.edu.uteq.sga.representante.ui.screens.reportes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.sga.representante.core.Resource
import ec.edu.uteq.sga.representante.domain.model.Asignacion
import ec.edu.uteq.sga.representante.domain.model.Estudiante
import ec.edu.uteq.sga.representante.domain.model.PeriodoEvaluacion
import ec.edu.uteq.sga.representante.domain.model.PromedioAnual
import ec.edu.uteq.sga.representante.domain.model.PromedioTrimestral
import ec.edu.uteq.sga.representante.domain.repository.DocenteRepository
import ec.edu.uteq.sga.representante.domain.repository.PromediosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReportePromedioItem(
    val estudiante: Estudiante,
    val promedioTrimestral: PromedioTrimestral? = null,
    val promedioAnual: PromedioAnual? = null
)

data class ReportesUiState(
    val idAsignacion: Long = 0,
    val asignaciones: List<Asignacion> = emptyList(),
    val selectedAsignacion: Asignacion? = null,
    val periodos: List<PeriodoEvaluacion> = emptyList(),
    val selectedPeriodo: PeriodoEvaluacion? = null,
    val vistaAnual: Boolean = false,
    val items: List<ReportePromedioItem> = emptyList(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)

class ReportesViewModel(
    private val promediosRepository: PromediosRepository,
    private val docenteRepository: DocenteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportesUiState())
    val uiState: StateFlow<ReportesUiState> = _uiState.asStateFlow()

    private var currentEstudiantes: List<Estudiante> = emptyList()
    private var currentTrimestrales: List<PromedioTrimestral> = emptyList()
    private var currentAnuales: List<PromedioAnual> = emptyList()

    private var loadEstudiantesJob: kotlinx.coroutines.Job? = null
    private var loadTrimestralesJob: kotlinx.coroutines.Job? = null
    private var loadAnualesJob: kotlinx.coroutines.Job? = null

    fun init(idAsignacion: Long) {
        _uiState.value = _uiState.value.copy(idAsignacion = idAsignacion, isLoading = true)
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
                    loadData(current.idAsignacion)
                }
            }
        }
    }

    fun selectAsignacion(asignacion: Asignacion) {
        _uiState.value = _uiState.value.copy(
            selectedAsignacion = asignacion,
            idAsignacion = asignacion.idAsignacion
        )
        loadData(asignacion.idAsignacion)
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

    fun setVistaAnual(anual: Boolean) {
        _uiState.value = _uiState.value.copy(vistaAnual = anual)
        combineData()
    }

    fun selectPeriodo(periodo: PeriodoEvaluacion) {
        _uiState.value = _uiState.value.copy(selectedPeriodo = periodo, vistaAnual = false)
        loadData(_uiState.value.idAsignacion)
    }

    fun loadData(idAsignacion: Long) {
        if (idAsignacion <= 0) return
        _uiState.value = _uiState.value.copy(isLoading = true)

        // Cargar Estudiantes
        loadEstudiantesJob?.cancel()
        loadEstudiantesJob = viewModelScope.launch {
            docenteRepository.getEstudiantesPorAsignacion(idAsignacion).collect { res ->
                if (res is Resource.Success) {
                    currentEstudiantes = res.data
                    combineData()
                }
            }
        }

        // Cargar Promedios Trimestrales
        loadTrimestralesJob?.cancel()
        loadTrimestralesJob = viewModelScope.launch {
            promediosRepository.getPromediosTrimestrales(
                idAsignacion,
                _uiState.value.selectedPeriodo?.idPeriodo
            ).collect { res ->
                if (res is Resource.Success) {
                    currentTrimestrales = res.data
                    _uiState.value = _uiState.value.copy(isOffline = res.isOffline, isLoading = false)
                    combineData()
                }
            }
        }

        // Cargar Promedios Anuales
        loadAnualesJob?.cancel()
        loadAnualesJob = viewModelScope.launch {
            promediosRepository.getPromediosAnuales(idAsignacion).collect { res ->
                if (res is Resource.Success) {
                    currentAnuales = res.data
                    combineData()
                }
            }
        }
    }

    private fun combineData() {
        val trimMap = currentTrimestrales.associateBy { it.idMatricula }
        val anualMap = currentAnuales.associateBy { it.idMatricula }

        val list = currentEstudiantes.map { est ->
            ReportePromedioItem(
                estudiante = est,
                promedioTrimestral = trimMap[est.idMatricula],
                promedioAnual = anualMap[est.idMatricula]
            )
        }
        _uiState.value = _uiState.value.copy(items = list, isLoading = false)
    }

    fun recalcularPromedioTrimestral(idMatricula: Long) {
        val periodoId = _uiState.value.selectedPeriodo?.idPeriodo ?: 1L
        viewModelScope.launch {
            promediosRepository.calcularPromedioTrimestral(
                idMatricula,
                _uiState.value.idAsignacion,
                periodoId
            )
            loadData(_uiState.value.idAsignacion)
        }
    }
}
