package ec.edu.uteq.sga.docente.ui.screens.seguimiento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.sga.docente.core.Resource
import ec.edu.uteq.sga.docente.core.SessionManager
import ec.edu.uteq.sga.docente.data.remote.dto.SeguimientoCreateDTO
import ec.edu.uteq.sga.docente.domain.model.Asignacion
import ec.edu.uteq.sga.docente.domain.model.Estudiante
import ec.edu.uteq.sga.docente.domain.model.PeriodoEvaluacion
import ec.edu.uteq.sga.docente.domain.model.ResumenAsistencia
import ec.edu.uteq.sga.docente.domain.model.SeguimientoItem
import ec.edu.uteq.sga.docente.domain.repository.AsistenciasRepository
import ec.edu.uteq.sga.docente.domain.repository.DocenteRepository
import ec.edu.uteq.sga.docente.domain.repository.SeguimientoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EstudianteAlertaItem(
    val estudiante: Estudiante,
    val resumenAsistencia: ResumenAsistencia? = null,
    val esRiesgoAsistencia: Boolean = false,
    val totalObservaciones: Int = 0
)

data class SeguimientoUiState(
    val items: List<SeguimientoItem> = emptyList(),
    val asignaciones: List<Asignacion> = emptyList(),
    val selectedAsignacion: Asignacion? = null,
    val estudiantesAlerta: List<EstudianteAlertaItem> = emptyList(),
    val periodos: List<PeriodoEvaluacion> = emptyList(),
    val selectedPeriodo: PeriodoEvaluacion? = null,
    val selectedTab: Int = 0, // 0 = Observaciones, 1 = Alertas y Rendimiento
    val selectedCategoriaFiltro: String? = null,
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)

class SeguimientoViewModel(
    private val seguimientoRepository: SeguimientoRepository,
    private val docenteRepository: DocenteRepository,
    private val asistenciasRepository: AsistenciasRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeguimientoUiState())
    val uiState: StateFlow<SeguimientoUiState> = _uiState.asStateFlow()

    private var currentEstudiantes: List<Estudiante> = emptyList()
    private var currentResumenes: List<ResumenAsistencia> = emptyList()

    private var loadAsignacionesJob: kotlinx.coroutines.Job? = null
    private var loadEstudiantesJob: kotlinx.coroutines.Job? = null
    private var loadResumenJob: kotlinx.coroutines.Job? = null
    private var loadSeguimientosJob: kotlinx.coroutines.Job? = null

    fun init(idMatricula: Long?) {
        loadPeriodos()
        loadAsignaciones(idMatricula)
        loadSeguimientos(idMatricula)
    }

    fun setTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun setCategoriaFiltro(cat: String?) {
        _uiState.value = _uiState.value.copy(selectedCategoriaFiltro = cat)
    }

    fun selectAsignacion(asignacion: Asignacion) {
        _uiState.value = _uiState.value.copy(selectedAsignacion = asignacion)
        loadEstudiantesYRendimiento(asignacion.idAsignacion)
    }

    private fun loadAsignaciones(idMatricula: Long?) {
        loadAsignacionesJob?.cancel()
        loadAsignacionesJob = viewModelScope.launch {
            docenteRepository.getAsignaciones().collect { res ->
                if (res is Resource.Success && res.data.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        asignaciones = res.data,
                        selectedAsignacion = res.data.first()
                    )
                    loadEstudiantesYRendimiento(res.data.first().idAsignacion)
                }
            }
        }
    }

    private fun loadEstudiantesYRendimiento(idAsignacion: Long) {
        if (idAsignacion <= 0) return

        loadEstudiantesJob?.cancel()
        loadEstudiantesJob = viewModelScope.launch {
            docenteRepository.getEstudiantesPorAsignacion(idAsignacion).collect { resEst ->
                if (resEst is Resource.Success) {
                    currentEstudiantes = resEst.data
                    combineAlertas()
                }
            }
        }

        loadResumenJob?.cancel()
        loadResumenJob = viewModelScope.launch {
            asistenciasRepository.getResumenAsistencia(idAsignacion, null).collect { resAsis ->
                if (resAsis is Resource.Success) {
                    currentResumenes = resAsis.data
                    combineAlertas()
                }
            }
        }
    }

    private fun combineAlertas() {
        val resMap = currentResumenes.associateBy { it.idMatricula }
        val obsCountMap = _uiState.value.items.groupBy { it.idMatricula }.mapValues { it.value.size }

        val alertas = currentEstudiantes.map { est ->
            val res = resMap[est.idMatricula]
            val pct = res?.porcentajeAsistencia ?: 100.0
            EstudianteAlertaItem(
                estudiante = est,
                resumenAsistencia = res,
                esRiesgoAsistencia = pct < 80.0,
                totalObservaciones = obsCountMap[est.idMatricula] ?: 0
            )
        }
        _uiState.value = _uiState.value.copy(estudiantesAlerta = alertas)
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
        loadSeguimientosJob?.cancel()
        loadSeguimientosJob = viewModelScope.launch {
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
                        combineAlertas()
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
                    loadSeguimientos(null)
                }
                is Resource.Error -> onError(res.message)
                is Resource.Loading -> {}
            }
        }
    }
}
