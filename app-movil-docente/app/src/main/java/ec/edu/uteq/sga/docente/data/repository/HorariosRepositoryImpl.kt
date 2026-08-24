package ec.edu.uteq.sga.docente.data.repository

import ec.edu.uteq.sga.docente.core.NetworkConnectivityObserver
import ec.edu.uteq.sga.docente.core.Resource
import ec.edu.uteq.sga.docente.data.local.AppDatabase
import ec.edu.uteq.sga.docente.data.local.entity.HorarioEntity
import ec.edu.uteq.sga.docente.data.remote.RetrofitClient
import ec.edu.uteq.sga.docente.domain.model.HorarioItem
import ec.edu.uteq.sga.docente.domain.repository.HorarioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class HorariosRepositoryImpl(
    private val database: AppDatabase,
    private val retrofitClient: RetrofitClient,
    private val connectivityObserver: NetworkConnectivityObserver
) : HorarioRepository {

    private val horarioDao = database.horarioDao()
    private val docenteApi get() = retrofitClient.getPrincipalDocenteApi()

    override fun getHorarios(idPersona: Long?, idAsignacion: Long?): Flow<Resource<List<HorarioItem>>> = flow {
        emit(Resource.Loading)

        if (connectivityObserver.isCurrentlyConnected() && idPersona != null) {
            try {
                val resp = docenteApi.getHorarioDocente(idPersona)
                if (resp.isSuccessful && resp.body() != null) {
                    val entities = resp.body()!!.slots.map { slot ->
                        HorarioEntity(
                            idHorario = slot.idHorario,
                            idAsignacion = slot.idAsignacion,
                            diaSemana = slot.diaSemana,
                            idPeriodo = slot.idPeriodo,
                            horaInicio = slot.horaInicio,
                            horaFin = slot.horaFin,
                            aula = slot.aula,
                            asignatura = slot.asignatura,
                            docente = slot.docente,
                            grado = slot.grado,
                            paralelo = slot.paralelo
                        )
                    }
                    horarioDao.clearHorarios()
                    horarioDao.insertHorarios(entities)
                }
            } catch (e: Exception) {
                // Room fallback
            }
        }

        val flowQuery = if (idAsignacion != null) {
            horarioDao.getHorarioByAsignacion(idAsignacion)
        } else {
            horarioDao.getHorarioCompleto()
        }

        flowQuery.map { list ->
            val domainList = list.map { e ->
                HorarioItem(
                    idHorario = e.idHorario,
                    idAsignacion = e.idAsignacion,
                    diaSemana = e.diaSemana,
                    idPeriodo = e.idPeriodo,
                    horaInicio = e.horaInicio,
                    horaFin = e.horaFin,
                    aula = e.aula,
                    asignatura = e.asignatura,
                    docente = e.docente,
                    grado = e.grado,
                    paralelo = e.paralelo
                )
            }
            Resource.Success(domainList, isOffline = !connectivityObserver.isCurrentlyConnected())
        }.collect { emit(it) }
    }

    override suspend fun refreshHorarios(idPersona: Long): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!connectivityObserver.isCurrentlyConnected()) {
                return@withContext Resource.Error("Sin conexión a Internet")
            }
            val resp = docenteApi.getHorarioDocente(idPersona)
            if (resp.isSuccessful && resp.body() != null) {
                val entities = resp.body()!!.slots.map { slot ->
                    HorarioEntity(
                        idHorario = slot.idHorario,
                        idAsignacion = slot.idAsignacion,
                        diaSemana = slot.diaSemana,
                        idPeriodo = slot.idPeriodo,
                        horaInicio = slot.horaInicio,
                        horaFin = slot.horaFin,
                        aula = slot.aula,
                        asignatura = slot.asignatura,
                        docente = slot.docente,
                        grado = slot.grado,
                        paralelo = slot.paralelo
                    )
                }
                horarioDao.clearHorarios()
                horarioDao.insertHorarios(entities)
                Resource.Success(Unit)
            } else {
                Resource.Error("Error al cargar horarios: ${resp.code()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error de red")
        }
    }
}
