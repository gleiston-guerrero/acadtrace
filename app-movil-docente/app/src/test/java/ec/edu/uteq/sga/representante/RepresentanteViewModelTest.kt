package ec.edu.uteq.sga.representante

import ec.edu.uteq.sga.representante.core.Resource
import ec.edu.uteq.sga.representante.domain.model.*
import ec.edu.uteq.sga.representante.domain.repository.RepresentanteRepository
import ec.edu.uteq.sga.representante.ui.screens.representante.RepresentanteViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class RepresentanteViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadingLuegoSuccess() = runTest {
        val vm = RepresentanteViewModel(
            FakeRepository(
                representados = flowOf(
                    Resource.Loading,
                    Resource.Success(listOf(representado))
                )
            )
        )

        advanceUntilIdle()

        assertEquals(listOf(representado), vm.representados.value.data)
        assertFalse(vm.representados.value.loading)
    }

    @Test
    fun loadingLuegoEmpty() = runTest {
        val vm = RepresentanteViewModel(
            FakeRepository(
                representados = flowOf(
                    Resource.Loading,
                    Resource.Success(emptyList())
                )
            )
        )

        advanceUntilIdle()

        assertTrue(vm.representados.value.data!!.isEmpty())
        assertFalse(vm.representados.value.loading)
    }

    @Test
    fun loadingLuegoError() = runTest {
        val vm = RepresentanteViewModel(
            FakeRepository(
                representados = flowOf(
                    Resource.Loading,
                    Resource.Error("HTTP 403")
                )
            )
        )

        advanceUntilIdle()

        assertEquals("HTTP 403", vm.representados.value.error)
        assertFalse(vm.representados.value.loading)
    }

    @Test
    fun excepcionDelFlowTerminaEnError() = runTest {
        val vm = RepresentanteViewModel(
            FakeRepository(
                representados = flow {
                    emit(Resource.Loading)
                    throw IllegalStateException("fallo")
                }
            )
        )

        advanceUntilIdle()

        assertTrue(
            vm.representados.value.error!!.contains("IllegalStateException")
        )
        assertFalse(vm.representados.value.loading)
    }

    @Test
    fun sinRedConCacheTerminaEnSuccess() = runTest {
        val vm = RepresentanteViewModel(
            FakeRepository(
                representados = flowOf(
                    Resource.Success(
                        listOf(representado),
                        isOffline = true
                    )
                )
            )
        )

        advanceUntilIdle()

        assertEquals(
            7L,
            vm.representados.value.data!!.single().idEstudiante
        )
        assertTrue(vm.representados.value.isOffline)
        assertFalse(vm.representados.value.loading)
    }

    @Test
    fun flowVacioNoDejaLoadingInfinito() = runTest {
        val vm = RepresentanteViewModel(
            FakeRepository(
                representados = flowOf(Resource.Loading)
            )
        )

        advanceUntilIdle()

        assertFalse(vm.representados.value.loading)
        assertNotNull(vm.representados.value.error)
    }

    @Test fun comunicadosLoadingSuccess() = runTest {
        val item = Comunicado(1, "Reunión", "Viernes", "2026-09-04", true)
        val vm = RepresentanteViewModel(FakeRepository(flowOf(Resource.Success(emptyList())),
            flowOf(Resource.Loading, Resource.Success(listOf(item)))))
        vm.cargarComunicados(); advanceUntilIdle()
        assertEquals("Reunión", vm.comunicados.value.data!!.single().titulo)
    }

    @Test fun comunicadosEmpty() = runTest {
        val vm = RepresentanteViewModel(FakeRepository(flowOf(Resource.Success(emptyList())),
            flowOf(Resource.Success(emptyList()))))
        vm.cargarComunicados(); advanceUntilIdle()
        assertTrue(vm.comunicados.value.data!!.isEmpty())
    }

    @Test fun comunicadosError() = runTest {
        val vm = RepresentanteViewModel(FakeRepository(flowOf(Resource.Success(emptyList())),
            flowOf(Resource.Error("Servicio no disponible"))))
        vm.cargarComunicados(); advanceUntilIdle()
        assertEquals("Servicio no disponible", vm.comunicados.value.error)
    }

    @Test fun comunicadosDesdeCacheOffline() = runTest {
        val item = Comunicado(1, "Aviso", "Contenido", "2026-09-04", false)
        val vm = RepresentanteViewModel(FakeRepository(flowOf(Resource.Success(emptyList())),
            flowOf(Resource.Success(listOf(item), isOffline = true))))
        vm.cargarComunicados(); advanceUntilIdle()
        assertTrue(vm.comunicados.value.isOffline)
    }

    private class FakeRepository(
        private val representados: Flow<Resource<List<Representado>>>,
        private val comunicados: Flow<Resource<List<Comunicado>>> = flowOf(Resource.Error("sin configurar"))
    ) : RepresentanteRepository {

        override fun getRepresentados(): Flow<Resource<List<Representado>>> {
            return representados
        }

        override fun getCalificaciones(
            idEstudiante: Long
        ): Flow<Resource<CalificacionesRepresentado>> {
            return flowOf(Resource.Error("sin configurar"))
        }

        override fun getAsistencia(
            idEstudiante: Long
        ): Flow<Resource<AsistenciaRepresentado>> {
            return flowOf(Resource.Error("sin configurar"))
        }

        override fun getComunicados(): Flow<Resource<List<Comunicado>>> {
            return comunicados
        }
    }

    private companion object {
        val representado = Representado(
            7,
            "Ana",
            "Paz",
            "Séptimo",
            "A",
            listOf(10)
        )
    }
}
