package ec.edu.uteq.sga.representante

import ec.edu.uteq.sga.representante.core.Resource
import ec.edu.uteq.sga.representante.data.remote.dto.CalificacionCreateDTO
import ec.edu.uteq.sga.representante.data.remote.dto.PromedioFormativoResponseDTO
import ec.edu.uteq.sga.representante.domain.model.*
import ec.edu.uteq.sga.representante.domain.repository.CalificacionesRepository
import ec.edu.uteq.sga.representante.domain.repository.DocenteRepository
import ec.edu.uteq.sga.representante.ui.screens.calificaciones.CalificacionesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalificacionesViewModelCoverageTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initCombinaEstudiantesCalificacionesYCalculaPromedio() = runTest {
        val estudiante1 = estudiante(1, "Ana")
        val estudiante2 = estudiante(2, "Luis")

        val nota1 = calificacion(11, 1, 9.0)
        val nota2 = calificacion(12, 2, 7.0)

        val calificaciones = FakeCalificacionesRepository(
            gradesFlow = flowOf(
                Resource.Success(
                    listOf(nota1, nota2),
                    isOffline = true
                )
            )
        )

        val docente = FakeDocenteRepository(
            estudiantesFlow = flowOf(
                Resource.Success(listOf(estudiante1, estudiante2))
            )
        )

        val vm = CalificacionesViewModel(calificaciones, docente)

        vm.init(
            idActividad = 50,
            idAsignacion = 10,
            actividadNombre = "Examen",
            notaMaxima = 10.0
        )

        advanceUntilIdle()

        val state = vm.uiState.value

        assertEquals(50L, state.idActividad)
        assertEquals(10L, state.idAsignacion)
        assertEquals("Examen", state.actividadNombre)
        assertEquals(2, state.items.size)
        assertEquals(9.0, state.items[0].calificacion!!.nota, 0.001)
        assertEquals(7.0, state.items[1].calificacion!!.nota, 0.001)
        assertEquals(8.0, state.promedioActividad, 0.001)
        assertTrue(state.isOffline)
        assertFalse(state.isLoading)
    }

    @Test
    fun loadingYErrorSeReflejanEnEstado() = runTest {
        val calificaciones = FakeCalificacionesRepository(
            gradesFlow = flowOf(
                Resource.Loading,
                Resource.Error("fallo al cargar")
            )
        )

        val vm = CalificacionesViewModel(
            calificaciones,
            FakeDocenteRepository()
        )

        vm.init(50, 10, "Actividad", 10.0)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals("fallo al cargar", vm.uiState.value.errorMessage)
    }

    @Test
    fun notaFueraDeRangoNoInvocaRepositorio() = runTest {
        val repository = FakeCalificacionesRepository()
        val vm = CalificacionesViewModel(
            repository,
            FakeDocenteRepository()
        )

        var error: String? = null
        var success = false

        vm.guardarNota(
            idMatricula = 1,
            nota = 11.0,
            observacion = null,
            onSuccess = { success = true },
            onError = { error = it }
        )

        advanceUntilIdle()

        assertFalse(success)
        assertEquals(0, repository.saveCalls)
        assertNotNull(error)
        assertTrue(error!!.contains("entre 0"))
    }

    @Test
    fun guardarNotaNuevaInvocaRepositorioYRecarga() = runTest {
        val repository = FakeCalificacionesRepository(
            gradesFlow = flowOf(Resource.Success(emptyList())),
            saveResult = Resource.Success(
                calificacion(
                    idCalificacion = 99,
                    idMatricula = 1,
                    nota = 9.5
                )
            )
        )

        val vm = CalificacionesViewModel(
            repository,
            FakeDocenteRepository(
                estudiantesFlow = flowOf(
                    Resource.Success(listOf(estudiante(1, "Ana")))
                )
            )
        )

        vm.init(50, 10, "Actividad", 10.0)
        advanceUntilIdle()

        var success = false
        var error: String? = null

        vm.guardarNota(
            idMatricula = 1,
            nota = 9.5,
            observacion = "Muy bien",
            onSuccess = { success = true },
            onError = { error = it }
        )

        advanceUntilIdle()

        assertTrue(success)
        assertNull(error)
        assertEquals(1, repository.saveCalls)
        assertNull(repository.lastExistingId)
        assertEquals(50L, repository.lastSaved!!.idActividad)
        assertEquals(1L, repository.lastSaved!!.idMatricula)
        assertEquals(9.5, repository.lastSaved!!.nota, 0.001)
        assertEquals("Muy bien", repository.lastSaved!!.observacion)
        assertEquals("EGB", repository.lastSaved!!.nivel)

        // La carga inicial y la recarga posterior al guardado.
        assertTrue(repository.gradeRequests >= 2)
    }

    @Test
    fun guardarNotaExistenteUsaIdYPropagaError() = runTest {
        val existente = calificacion(
            idCalificacion = 77,
            idMatricula = 1,
            nota = 8.0
        )

        val repository = FakeCalificacionesRepository(
            gradesFlow = flowOf(Resource.Success(listOf(existente))),
            saveResult = Resource.Error("no se pudo guardar")
        )

        val vm = CalificacionesViewModel(
            repository,
            FakeDocenteRepository(
                estudiantesFlow = flowOf(
                    Resource.Success(listOf(estudiante(1, "Ana")))
                )
            )
        )

        vm.init(50, 10, "Actividad", 10.0)
        advanceUntilIdle()

        var error: String? = null
        var success = false

        vm.guardarNota(
            idMatricula = 1,
            nota = 8.5,
            observacion = null,
            onSuccess = { success = true },
            onError = { error = it }
        )

        advanceUntilIdle()

        assertFalse(success)
        assertEquals("no se pudo guardar", error)
        assertEquals(77L, repository.lastExistingId)
        assertEquals(1, repository.saveCalls)
    }

    private class FakeCalificacionesRepository(
        private val gradesFlow: Flow<Resource<List<CalificacionEstudiante>>> =
            flowOf(Resource.Success(emptyList())),
        var saveResult: Resource<CalificacionEstudiante> =
            Resource.Success(calificacion(999, 1, 10.0))
    ) : CalificacionesRepository {

        var gradeRequests = 0
        var saveCalls = 0
        var lastSaved: CalificacionCreateDTO? = null
        var lastExistingId: Long? = null

        override fun getCalificaciones(
            idActividad: Long
        ): Flow<Resource<List<CalificacionEstudiante>>> {
            gradeRequests++
            return gradesFlow
        }

        override suspend fun saveCalificacion(
            calificacion: CalificacionCreateDTO,
            idCalificacion: Long?
        ): Resource<CalificacionEstudiante> {
            saveCalls++
            lastSaved = calificacion
            lastExistingId = idCalificacion
            return saveResult
        }

        override suspend fun getPromedioFormativo(
            idMatricula: Long,
            idAsignacion: Long,
            idPeriodo: Long
        ): Resource<PromedioFormativoResponseDTO> {
            return Resource.Error("unused")
        }
    }

    private class FakeDocenteRepository(
        private val estudiantesFlow: Flow<Resource<List<Estudiante>>> =
            flowOf(Resource.Success(emptyList()))
    ) : DocenteRepository {

        override fun getAsignaciones(): Flow<Resource<List<Asignacion>>> =
            flowOf(Resource.Success(emptyList()))

        override suspend fun getAsignacion(
            idAsignacion: Long
        ): Resource<Asignacion> =
            Resource.Error("unused")

        override fun getEstudiantesPorAsignacion(
            idAsignacion: Long
        ): Flow<Resource<List<Estudiante>>> =
            estudiantesFlow

        override fun getPeriodosEvaluacion(): Flow<Resource<List<PeriodoEvaluacion>>> =
            flowOf(Resource.Success(emptyList()))

        override suspend fun refreshDocenteContext(): Resource<Unit> =
            Resource.Success(Unit)
    }

    companion object {

        private fun estudiante(
            idMatricula: Long,
            nombre: String
        ) = Estudiante(
            idMatricula = idMatricula,
            idAsignacion = 10,
            estudianteId = idMatricula + 100,
            nombres = nombre,
            apellidos = "Prueba"
        )

        private fun calificacion(
            idCalificacion: Long,
            idMatricula: Long,
            nota: Double
        ) = CalificacionEstudiante(
            idCalificacion = idCalificacion,
            idActividad = 50,
            idMatricula = idMatricula,
            nota = nota
        )
    }
}
