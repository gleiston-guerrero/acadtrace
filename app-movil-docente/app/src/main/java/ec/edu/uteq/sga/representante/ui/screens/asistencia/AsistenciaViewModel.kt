package ec.edu.uteq.sga.representante.ui.screens.asistencia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.sga.representante.core.Resource
import ec.edu.uteq.sga.representante.data.remote.dto.AsistenciaCreateDTO
import ec.edu.uteq.sga.representante.domain.model.Asignacion
import ec.edu.uteq.sga.representante.domain.model.AsistenciaRegistro
import ec.edu.uteq.sga.representante.domain.model.Estudiante
import ec.edu.uteq.sga.representante.domain.model.PeriodoEvaluacion
import ec.edu.uteq.sga.representante.domain.model.ResumenAsistencia
import ec.edu.uteq.sga.representante.domain.repository.AsistenciasRepository
import ec.edu.uteq.sga.representante.domain.repository.DocenteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class EstudianteAsistenciaItem(
    val estudiante: Estudiante,
    val asistencia: AsistenciaRegistro? = null
)

data class AsistenciaUiState(
    val idAsignacion: Long = 0,
    val asignaciones: List<Asignacion> = emptyList(),
    val selectedAsignacion: Asignacion? = null,
    val selectedFecha: String = "",
    val periodos: List<PeriodoEvaluacion> = emptyList(),
    val selectedPeriodo: PeriodoEvaluacion? = null,
    val items: List<EstudianteAsistenciaItem> = emptyList(),
    val resumenes: List<ResumenAsistencia> = emptyList(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)

class AsistenciaViewModel(
    private val asistenciasRepository: AsistenciasRepository,
    private val docenteRepository: DocenteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AsistenciaUiState())
    val uiState: StateFlow<AsistenciaUiState> = _uiState.asStateFlow()

    private var currentEstudiantes: List<Estudiante> = emptyList()
    private var currentAsistencias: List<AsistenciaRegistro> = emptyList()

    private var loadDataJob: kotlinx.coroutines.Job? = null
    private var loadEstudiantesJob: kotlinx.coroutines.Job? = null

    fun init(idAsignacion: Long) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())
        _uiState.value = _uiState.value.copy(idAsignacion = idAsignacion, selectedFecha = today)
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
                    loadData(current.idAsignacion, _uiState.value.selectedFecha)
                }
            }
        }
    }

    fun selectAsignacion(asignacion: Asignacion) {
        _uiState.value = _uiState.value.copy(
            selectedAsignacion = asignacion,
            idAsignacion = asignacion.idAsignacion
        )
        loadData(asignacion.idAsignacion, _uiState.value.selectedFecha)
    }

    private fun loadPeriodos() {
        viewModelScope.launch {
            docenteRepository.getPeriodosEvaluacion().collect { res ->
                if (res is Resource.Success) {
                    val periodos = res.data
                    val current = periodos.firstOrNull()
                    _uiState.value = _uiState.value.copy(
                        periodos = periodos,
                        selectedPeriodo = current
                    )
                }
            }
        }
    }

    fun changeFecha(fecha: String) {
        _uiState.value = _uiState.value.copy(selectedFecha = fecha)
        loadData(_uiState.value.idAsignacion, fecha)
    }

    fun loadData(idAsignacion: Long, fecha: String) {
        if (idAsignacion <= 0) return
        _uiState.value = _uiState.value.copy(isLoading = true)

        loadEstudiantesJob?.cancel()
        loadEstudiantesJob = viewModelScope.launch {
            docenteRepository.getEstudiantesPorAsignacion(idAsignacion).collect { res ->
                if (res is Resource.Success) {
                    currentEstudiantes = res.data
                    combineData()
                }
            }
        }

        loadDataJob?.cancel()
        loadDataJob = viewModelScope.launch {
            asistenciasRepository.getAsistenciasPorFecha(idAsignacion, fecha).collect { res ->
                when (res) {
                    is Resource.Success -> {
                        currentAsistencias = res.data
                        _uiState.value = _uiState.value.copy(
                            isOffline = res.isOffline,
                            isLoading = false
                        )
                        combineData()
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

    private fun combineData() {
        val map = currentAsistencias.associateBy { it.idMatricula }
        val list = currentEstudiantes.map { est ->
            EstudianteAsistenciaItem(
                estudiante = est,
                asistencia = map[est.idMatricula]
            )
        }
        _uiState.value = _uiState.value.copy(items = list)
    }

    fun setEstadoAsistencia(idMatricula: Long, nuevoEstado: String, justificacion: String? = null) {
        val periodoId = _uiState.value.selectedPeriodo?.idPeriodo ?: 1L
        val existing = currentAsistencias.find { it.idMatricula == idMatricula }

        val dto = AsistenciaCreateDTO(
            idMatricula = idMatricula,
            idAsignacion = _uiState.value.idAsignacion,
            idPeriodo = periodoId,
            fecha = _uiState.value.selectedFecha,
            estado = nuevoEstado,
            justificacion = justificacion
        )

        viewModelScope.launch {
            asistenciasRepository.saveAsistencia(dto, existing?.idAsistencia)
            loadData(_uiState.value.idAsignacion, _uiState.value.selectedFecha)
        }
    }

    fun marcarTodosPresentes() {
        currentEstudiantes.forEach { est ->
            setEstadoAsistencia(est.idMatricula, "PRESENTE")
        }
    }

    fun loadResumenAsistencias(idAsignacion: Long) {
        viewModelScope.launch {
            asistenciasRepository.getResumenAsistencia(
                idAsignacion,
                _uiState.value.selectedPeriodo?.idPeriodo
            ).collect { res ->
                if (res is Resource.Success) {
                    _uiState.value = _uiState.value.copy(resumenes = res.data)
                }
            }
        }
    }
}
