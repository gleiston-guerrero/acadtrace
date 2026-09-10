package ec.edu.uteq.sga.representante

import ec.edu.uteq.sga.representante.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import ec.edu.uteq.sga.representante.core.Resource
import ec.edu.uteq.sga.representante.domain.repository.RepresentanteRepository
import ec.edu.uteq.sga.representante.ui.screens.representante.RepresentanteViewModel
import org.junit.After
import org.junit.Before

class CalificacionesPorPeriodoTest {
    @OptIn(ExperimentalCoroutinesApi::class) private val dispatcher = UnconfinedTestDispatcher()
    @OptIn(ExperimentalCoroutinesApi::class) @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @OptIn(ExperimentalCoroutinesApi::class) @After fun tearDown() = Dispatchers.resetMain()
    private val periods = listOf(
        PeriodoCalificaciones(10, "Primer Trimestre", false, "2026-05-01"),
        PeriodoCalificaciones(20, "Segundo Trimestre", true, "2026-08-01"),
        PeriodoCalificaciones(30, "Tercer Trimestre", false, "2026-11-01")
    )
    private val data = CalificacionesRepresentado(
        calificaciones = periods.mapIndexed { i, p -> NotaRepresentado((i + 1).toLong(), (i + 101).toLong(), p.idPeriodo, "Asignatura $i", "Actividad T${i + 1}", p.nombre, if (i == 2) null else 9.0, "A_MAS") },
        promedios = periods.mapIndexed { i, p -> PromedioRepresentado((i + 101).toLong(), p.idPeriodo, "Asignatura $i", p.nombre, 9.1, 9.4, 9.19, "A_MAS") },
        periodos = periods
    )

    @Test fun contieneTresPeriodosYSeleccionaElActivo() { assertEquals(3, data.periodos.size); assertEquals(20L, data.periodoInicial()) }
    @Test fun sinActivoSeleccionaElMasReciente() { assertEquals(30L, data.copy(periodos = periods.map { it.copy(activo = false) }).periodoInicial()) }
    @Test fun cadaSeleccionContieneSoloSuPeriodo() {
        periods.forEach { p -> assertTrue(data.asignaturas(p.idPeriodo).flatMap { it.actividades }.all { it.idPeriodo == p.idPeriodo }) }
        assertEquals("Actividad T1", data.asignaturas(10).single().actividades.single().actividad)
        assertEquals("Actividad T2", data.asignaturas(20).single().actividades.single().actividad)
        assertEquals("Actividad T3", data.asignaturas(30).single().actividades.single().actividad)
    }
    @Test fun identificaAsignaturaYNoMezclaActividad() { assertEquals("Asignatura 1", data.asignaturas(20).single().asignatura) }
    @Test fun periodoSinDatosEsVacio() { assertTrue(data.asignaturas(999).isEmpty()) }
    @Test fun cualitativasSePresentanLegibles() {
        assertEquals(listOf("A+", "A-", "B+", "B-", "C+", "C-", "D"), listOf("A_MAS","A_MENOS","B_MAS","B_MENOS","C_MAS","C_MENOS","D").map { it.etiquetaCualitativa() })
    }
    @Test fun notaNulaPermanecePendiente() { assertNull(data.asignaturas(30).single().actividades.single().nota) }
    @Test fun anualSeOcultaDuranteCursoYSeMuestraAlCerrar() {
        val annual = listOf(PromedioAnualRepresentado(101, "Lengua", 9.3, "A_MAS"))
        assertFalse(data.copy(promediosAnuales = annual, mostrarPromediosAnuales = false).mostrarPromediosAnuales)
        assertTrue(data.copy(promediosAnuales = annual, mostrarPromediosAnuales = true).mostrarPromediosAnuales)
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun cambiarPeriodoNoConsultaNuevamenteElBackend() = runTest {
        val repository = CountingRepository(data)
        val vm = RepresentanteViewModel(repository)
        vm.cargarCalificaciones(7); advanceUntilIdle()
        vm.seleccionarPeriodo(10); vm.seleccionarPeriodo(30); advanceUntilIdle()
        assertEquals(1, repository.gradeRequests)
        assertEquals(30L, vm.periodoSeleccionado.value)
    }

    private class CountingRepository(private val data: CalificacionesRepresentado) : RepresentanteRepository {
        var gradeRequests = 0
        override fun getRepresentados(): Flow<Resource<List<Representado>>> = flowOf(Resource.Success(emptyList()))
        override fun getCalificaciones(idEstudiante: Long): Flow<Resource<CalificacionesRepresentado>> { gradeRequests++; return flowOf(Resource.Success(data)) }
        override fun getAsistencia(idEstudiante: Long): Flow<Resource<AsistenciaRepresentado>> = flowOf(Resource.Error("unused"))
        override fun getComunicados(): Flow<Resource<List<Comunicado>>> = flowOf(Resource.Error("unused"))
    }
}
