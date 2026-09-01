package ec.edu.uteq.sga.representante.data.repository

import ec.edu.uteq.sga.representante.core.NetworkConnectivityObserver
import ec.edu.uteq.sga.representante.core.Resource
import ec.edu.uteq.sga.representante.data.local.AppDatabase
import ec.edu.uteq.sga.representante.data.local.entity.HorarioEntity
import ec.edu.uteq.sga.representante.data.remote.RetrofitClient
import ec.edu.uteq.sga.representante.domain.model.HorarioItem
import ec.edu.uteq.sga.representante.domain.repository.HorarioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
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

        // 1. Emitir caché local de inmediato si existe
        val flowQuery = if (idAsignacion != null) {
            horarioDao.getHorarioByAsignacion(idAsignacion)
        } else {
            horarioDao.getHorarioCompleto()
        }

        val cached = flowQuery.firstOrNull() ?: emptyList()
        if (cached.isNotEmpty()) {
            val domainList = cached.map { e ->
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
            emit(Resource.Success(domainList, isOffline = !connectivityObserver.isCurrentlyConnected()))
        }

        // 2. Consultar red si hay conexión
        if (connectivityObserver.isCurrentlyConnected()) {
            try {
                val allEntities = mutableListOf<HorarioEntity>()

                if (idPersona != null && idPersona > 0) {
                    val resp = docenteApi.getHorarioDocente(idPersona)
                    if (resp.isSuccessful && resp.body() != null) {
                        resp.body()!!.slots.forEach { slot ->
                            allEntities.add(
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
                            )
                        }
                    }
                }

                // Consultar horarios de los cursos asignados al docente (desde la caché local y/o red)
                val localAsigs = database.asignacionDao().getAllAsignaciones().firstOrNull() ?: emptyList()
                val targetAsigIds = mutableSetOf<Long>()
                localAsigs.forEach { targetAsigIds.add(it.idAsignacion) }

                try {
                    val asigsResp = docenteApi.getMisAsignaciones()
                    if (asigsResp.isSuccessful && asigsResp.body() != null) {
                        asigsResp.body()!!.forEach { targetAsigIds.add(it.idAsignacion) }
                    }
                } catch (e: Exception) {
                    // Continuar con las asignaciones locales
                }

                for (idAsig in targetAsigIds) {
                    try {
                        val cursoResp = docenteApi.getHorarioCurso(idAsig)
                        if (cursoResp.isSuccessful && cursoResp.body() != null) {
                            cursoResp.body()!!.slots.forEach { slot ->
                                allEntities.add(
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
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // Continuar con el siguiente curso
                    }
                }

                if (allEntities.isNotEmpty()) {
                    val distinctEntities = allEntities.distinctBy { it.idHorario }
                    horarioDao.clearHorarios()
                    horarioDao.insertHorarios(distinctEntities)

                    val domainList = distinctEntities.map { e ->
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
                    emit(Resource.Success(domainList, isOffline = false))
                } else if (cached.isEmpty()) {
                    emit(Resource.Error("No se pudieron cargar los horarios del servidor."))
                }
            } catch (e: Exception) {
                if (cached.isEmpty()) {
                    emit(Resource.Error("No se pudieron cargar los horarios."))
                }
            }
        } else if (cached.isEmpty()) {
            emit(Resource.Error("Sin conexión a Internet."))
        }
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
