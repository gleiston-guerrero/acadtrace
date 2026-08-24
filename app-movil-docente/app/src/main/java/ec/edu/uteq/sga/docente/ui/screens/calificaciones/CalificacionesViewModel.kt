package ec.edu.uteq.sga.docente.ui.screens.calificaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.sga.docente.core.Resource
import ec.edu.uteq.sga.docente.data.remote.dto.CalificacionCreateDTO
import ec.edu.uteq.sga.docente.domain.model.CalificacionEstudiante
import ec.edu.uteq.sga.docente.domain.model.Estudiante
import ec.edu.uteq.sga.docente.domain.repository.CalificacionesRepository
import ec.edu.uteq.sga.docente.domain.repository.DocenteRepository
import ec.edu.uteq.sga.docente.domain.rules.AcademicRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EstudianteCalificacionItem(
    val estudiante: Estudiante,
    val calificacion: CalificacionEstudiante? = null
)

data class CalificacionesUiState(
    val idActividad: Long = 0,
    val idAsignacion: Long = 0,
    val actividadNombre: String = "",
    val notaMaxima: Double = 10.0,
    val items: List<EstudianteCalificacionItem> = emptyList(),
    val promedioActividad: Double = 0.0,
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)

class CalificacionesViewModel(
    private val calificacionesRepository: CalificacionesRepository,
    private val docenteRepository: DocenteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalificacionesUiState())
    val uiState: StateFlow<CalificacionesUiState> = _uiState.asStateFlow()

    private var currentEstudiantes: List<Estudiante> = emptyList()
    private var currentCalificaciones: List<CalificacionEstudiante> = emptyList()

    fun init(idActividad: Long, idAsignacion: Long, actividadNombre: String, notaMaxima: Double) {
        _uiState.value = _uiState.value.copy(
            idActividad = idActividad,
            idAsignacion = idAsignacion,
            actividadNombre = actividadNombre,
            notaMaxima = notaMaxima,
            isLoading = true
        )
        loadData(idActividad, idAsignacion)
    }

    private fun loadData(idActividad: Long, idAsignacion: Long) {
        // Cargar Estudiantes
        viewModelScope.launch {
            docenteRepository.getEstudiantesPorAsignacion(idAsignacion).collect { res ->
                if (res is Resource.Success) {
                    currentEstudiantes = res.data
                    combineData()
                }
            }
        }

        // Cargar Calificaciones
        viewModelScope.launch {
            calificacionesRepository.getCalificaciones(idActividad).collect { res ->
                when (res) {
                    is Resource.Success -> {
                        currentCalificaciones = res.data
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
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                }
            }
        }
    }

    private fun combineData() {
        val califMap = currentCalificaciones.associateBy { it.idMatricula }
        val combined = currentEstudiantes.map { est ->
            EstudianteCalificacionItem(
                estudiante = est,
                calificacion = califMap[est.idMatricula]
            )
        }

        val notas = currentCalificaciones.map { it.nota }
        val promedio = if (notas.isNotEmpty()) notas.average() else 0.0

        _uiState.value = _uiState.value.copy(
            items = combined,
            promedioActividad = promedio
        )
    }

    fun guardarNota(
        idMatricula: Long,
        nota: Double,
        observacion: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (nota < 0.0 || nota > _uiState.value.notaMaxima) {
            onError("La nota debe estar entre 0 y ${_uiState.value.notaMaxima}")
            return
        }

        val existingCalif = currentCalificaciones.find { it.idMatricula == idMatricula }
        val dto = CalificacionCreateDTO(
            idActividad = _uiState.value.idActividad,
            idMatricula = idMatricula,
            nota = nota,
            observacion = observacion,
            nivel = "EGB"
        )

        viewModelScope.launch {
            val res = calificacionesRepository.saveCalificacion(dto, existingCalif?.idCalificacion)
            when (res) {
                is Resource.Success -> {
                    onSuccess()
                    loadData(_uiState.value.idActividad, _uiState.value.idAsignacion)
                }
                is Resource.Error -> onError(res.message)
                is Resource.Loading -> {}
            }
        }
    }
}
