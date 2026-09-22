package ec.edu.uteq.sga.representante

import ec.edu.uteq.sga.representante.core.Resource
import ec.edu.uteq.sga.representante.data.remote.dto.AsistenciaCreateDTO
import ec.edu.uteq.sga.representante.domain.model.*
import ec.edu.uteq.sga.representante.domain.repository.AsistenciasRepository
import ec.edu.uteq.sga.representante.domain.repository.DocenteRepository
import ec.edu.uteq.sga.representante.ui.screens.asistencia.AsistenciaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AsistenciaViewModelCoverageTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadDataCombinaEstudiantesConAsistencias() = runTest {
        val estudiante1 = estudiante(1, "Ana")
        val estudiante2 = estudiante(2, "Luis")
        val registro = asistencia(50, 1, "AUSENTE")

        val vm = AsistenciaViewModel(
            FakeAsistenciasRepository(
                asistenciasFlow = flowOf(
                    Resource.Success(listOf(registro), isOffline = true)
                )
            ),
            FakeDocenteRepository(
                estudiantesFlow = flowOf(
                    Resource.Success(listOf(estudiante1, estudiante2))
                )
            )
        )

        vm.loadData(10, "2026-09-21")
        advanceUntilIdle()

        val state = vm.uiState.value

        assertEquals(2, state.items.size)
        assertEquals("AUSENTE", state.items[0].asistencia?.estado)
        assertNull(state.items[1].asistencia)
        assertTrue(state.isOffline)
        assertFalse(state.isLoading)
    }

    @Test
    fun errorDeAsistenciaSePublicaEnEstado() = runTest {
        val vm = AsistenciaViewModel(
            FakeAsistenciasRepository(
                asistenciasFlow = flowOf(
                    Resource.Loading,
                    Resource.Error("servicio no disponible")
                )
            ),
            FakeDocenteRepository()
        )

        vm.loadData(10, "2026-09-21")
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(
            "servicio no disponible",
            vm.uiState.value.errorMessage
        )
    }

    @Test
    fun idAsignacionInvalidoNoConsultaRepositorios() = runTest {
        val asistencias = FakeAsistenciasRepository()
        val docente = FakeDocenteRepository()

        val vm = AsistenciaViewModel(asistencias, docente)

        vm.loadData(0, "2026-09-21")
        advanceUntilIdle()

        assertEquals(0, asistencias.asistenciaRequests)
        assertEquals(0, docente.estudianteRequests)
    }

    @Test
    fun initCargaAsignacionYPeriodo() = runTest {
        val asignacion = asignacion(10)
        val periodo = periodo(20)

        val vm = AsistenciaViewModel(
            FakeAsistenciasRepository(),
            FakeDocenteRepository(
                asignacionesFlow = flowOf(
                    Resource.Success(listOf(asignacion))
                ),
                periodosFlow = flowOf(
                    Resource.Success(listOf(periodo))
                )
            )
        )

        vm.init(10)
        advanceUntilIdle()

        assertEquals(10L, vm.uiState.value.idAsignacion)
        assertEquals(asignacion, vm.uiState.value.selectedAsignacion)
        assertEquals(periodo, vm.uiState.value.selectedPeriodo)
        assertTrue(vm.uiState.value.selectedFecha.isNotBlank())
    }

    @Test
    fun cambiarFechaRecargaDatos() = runTest {
        val asistencias = FakeAsistenciasRepository()

        val vm = AsistenciaViewModel(
            asistencias,
            FakeDocenteRepository()
        )

        vm.changeFecha("2026-09-22")
        advanceUntilIdle()

        assertEquals("2026-09-22", vm.uiState.value.selectedFecha)
    }

    @Test
    fun resumenSeCargaDesdeRepositorio() = runTest {
        val resumen = ResumenAsistencia(
            idResumen = 1,
            idMatricula = 1,
            idAsignacion = 10,
            idPeriodo = 20,
            totalPresentes = 8,
            totalAusentes = 1,
            totalJustificados = 0,
            totalAtrasos = 1
        )

        val vm = AsistenciaViewModel(
            FakeAsistenciasRepository(
                resumenFlow = flowOf(
                    Resource.Success(listOf(resumen))
                )
            ),
            FakeDocenteRepository()
        )

        vm.loadResumenAsistencias(10)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.resumenes.size)
        assertEquals(90.0, vm.uiState.value.resumenes.single().porcentajeAsistencia, 0.001)
    }

    private class FakeAsistenciasRepository(
        private val asistenciasFlow: Flow<Resource<List<AsistenciaRegistro>>> =
            flowOf(Resource.Success(emptyList())),
        private val resumenFlow: Flow<Resource<List<ResumenAsistencia>>> =
            flowOf(Resource.Success(emptyList()))
    ) : AsistenciasRepository {

        var asistenciaRequests = 0

        override fun getAsistenciasPorFecha(
            idAsignacion: Long,
            fecha: String
        ): Flow<Resource<List<AsistenciaRegistro>>> {
            asistenciaRequests++
            return asistenciasFlow
        }

        override suspend fun saveAsistencia(
            asistencia: AsistenciaCreateDTO,
            idAsistencia: Long?
        ): Resource<AsistenciaRegistro> =
            Resource.Success(
                AsistenciaRegistro(
                    idAsistencia = idAsistencia ?: 99,
                    idMatricula = asistencia.idMatricula,
                    idAsignacion = asistencia.idAsignacion,
                    idPeriodo = asistencia.idPeriodo,
                    fecha = asistencia.fecha,
                    estado = asistencia.estado,
                    justificacion = asistencia.justificacion
                )
            )

        override fun getResumenAsistencia(
            idAsignacion: Long,
            idPeriodo: Long?
        ): Flow<Resource<List<ResumenAsistencia>>> =
            resumenFlow

        override suspend fun calcularResumen(
            idMatricula: Long,
            idAsignacion: Long,
            idPeriodo: Long
        ): Resource<ResumenAsistencia> =
            Resource.Error("unused")
    }

    private class FakeDocenteRepository(
        private val asignacionesFlow: Flow<Resource<List<Asignacion>>> =
            flowOf(Resource.Success(emptyList())),
        private val estudiantesFlow: Flow<Resource<List<Estudiante>>> =
            flowOf(Resource.Success(emptyList())),
        private val periodosFlow: Flow<Resource<List<PeriodoEvaluacion>>> =
            flowOf(Resource.Success(emptyList()))
    ) : DocenteRepository {

        var estudianteRequests = 0

        override fun getAsignaciones(): Flow<Resource<List<Asignacion>>> =
            asignacionesFlow

        override suspend fun getAsignacion(
            idAsignacion: Long
        ): Resource<Asignacion> =
            Resource.Error("unused")

        override fun getEstudiantesPorAsignacion(
            idAsignacion: Long
        ): Flow<Resource<List<Estudiante>>> {
            estudianteRequests++
            return estudiantesFlow
        }

        override fun getPeriodosEvaluacion():
            Flow<Resource<List<PeriodoEvaluacion>>> =
            periodosFlow

        override suspend fun refreshDocenteContext():
            Resource<Unit> =
            Resource.Success(Unit)
    }

    companion object {
        fun estudiante(id: Long, nombre: String) =
            Estudiante(
                idMatricula = id,
                idAsignacion = 10,
                estudianteId = id + 100,
                nombres = nombre,
                apellidos = "Prueba"
            )

        fun asistencia(id: Long, matricula: Long, estado: String) =
            AsistenciaRegistro(
                idAsistencia = id,
                idMatricula = matricula,
                idAsignacion = 10,
                idPeriodo = 20,
                fecha = "2026-09-21",
                estado = estado
            )

        fun asignacion(id: Long) =
            Asignacion(
                idAsignacion = id,
                asignaturaNombre = "Matemática",
                gradoNombre = "Séptimo",
                paraleloLetra = "A",
                anoLectivoNombre = "2026"
            )

        fun periodo(id: Long) =
            PeriodoEvaluacion(
                idPeriodo = id,
                idAnoLectivo = 1,
                tipo = "TRIMESTRE",
                nombre = "Primer trimestre",
                fechaInicio = "2026-05-01",
                fechaFin = "2026-08-01"
            )
    }
}
