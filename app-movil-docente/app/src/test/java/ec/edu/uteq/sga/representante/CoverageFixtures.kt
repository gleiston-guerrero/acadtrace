package ec.edu.uteq.sga.representante

import ec.edu.uteq.sga.representante.core.Resource
import ec.edu.uteq.sga.representante.domain.model.*
import ec.edu.uteq.sga.representante.domain.repository.DocenteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class CoverageMainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(UnconfinedTestDispatcher())
    override fun finished(description: Description) = Dispatchers.resetMain()
}

internal class CoverageDocenteRepository : DocenteRepository {
    val curso = AsistenciaViewModelCoverageTest.asignacion(10)
    val periodo = AsistenciaViewModelCoverageTest.periodo(20)
    var asignacionesFlow: Flow<Resource<List<Asignacion>>> = flowOf(Resource.Success(listOf(curso)))
    var periodos: Flow<Resource<List<PeriodoEvaluacion>>> = flowOf(Resource.Success(listOf(periodo)))
    var estudiantes: Flow<Resource<List<Estudiante>>> = flowOf(Resource.Success(emptyList()))
    val solicitudesEstudiantes = mutableListOf<Long>()
    override fun getAsignaciones() = asignacionesFlow
    override suspend fun getAsignacion(idAsignacion: Long): Resource<Asignacion> = error("No esperado")
    override fun getEstudiantesPorAsignacion(idAsignacion: Long): Flow<Resource<List<Estudiante>>> {
        solicitudesEstudiantes += idAsignacion
        return estudiantes
    }
    override fun getPeriodosEvaluacion() = periodos
    override suspend fun refreshDocenteContext(): Resource<Unit> = error("No esperado")
}
