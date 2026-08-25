package ec.edu.uteq.sga.docente.data.repository

import ec.edu.uteq.sga.docente.core.NetworkConnectivityObserver
import ec.edu.uteq.sga.docente.core.Resource
import ec.edu.uteq.sga.docente.data.local.AppDatabase
import ec.edu.uteq.sga.docente.data.local.entity.AsignacionEntity
import ec.edu.uteq.sga.docente.data.local.entity.EstudianteEntity
import ec.edu.uteq.sga.docente.data.local.entity.PeriodoEntity
import ec.edu.uteq.sga.docente.data.remote.RetrofitClient
import ec.edu.uteq.sga.docente.domain.model.Asignacion
import ec.edu.uteq.sga.docente.domain.model.Estudiante
import ec.edu.uteq.sga.docente.domain.model.PeriodoEvaluacion
import ec.edu.uteq.sga.docente.domain.repository.DocenteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class DocenteRepositoryImpl(
    private val database: AppDatabase,
    private val retrofitClient: RetrofitClient,
    private val connectivityObserver: NetworkConnectivityObserver
) : DocenteRepository {

    private val asignacionDao = database.asignacionDao()
    private val estudianteDao = database.estudianteDao()
    private val periodoDao = database.periodoDao()

    override fun getAsignaciones(): Flow<Resource<List<Asignacion>>> = flow {
        emit(Resource.Loading)

        var emittedFromNetwork = false

        if (connectivityObserver.isCurrentlyConnected()) {
            try {
                val api = retrofitClient.getPrincipalDocenteApi()
                val response = api.getMisAsignaciones()
                if (response.isSuccessful && response.body() != null) {
                    val dtos = response.body()!!
                    val domainList = dtos.map { dto ->
                        Asignacion(
                            idAsignacion = dto.idAsignacion,
                            asignaturaNombre = dto.asignatura.nombre,
                            gradoNombre = dto.grado.nombre,
                            paraleloLetra = dto.paralelo.letra,
                            anoLectivoNombre = dto.anoLectivo.nombre,
                            cantidadEstudiantes = dto.cantidadEstudiantes,
                            porcentajeAsistencia = dto.porcentajeAsistencia,
                            promedioCalificaciones = dto.promedioCalificaciones
                        )
                    }

                    val entities = dtos.map { dto ->
                        AsignacionEntity(
                            idAsignacion = dto.idAsignacion,
                            asignaturaId = dto.asignatura.id,
                            asignaturaNombre = dto.asignatura.nombre,
                            gradoId = dto.grado.id,
                            gradoNombre = dto.grado.nombre,
                            paraleloId = dto.paralelo.id,
                            paraleloLetra = dto.paralelo.letra,
                            anoLectivoId = dto.anoLectivo.id,
                            anoLectivoNombre = dto.anoLectivo.nombre,
                            cantidadEstudiantes = dto.cantidadEstudiantes,
                            porcentajeAsistencia = dto.porcentajeAsistencia,
                            promedioCalificaciones = dto.promedioCalificaciones
                        )
                    }

                    try {
                        asignacionDao.clearAsignaciones()
                        asignacionDao.insertAsignaciones(entities)
                    } catch (dbEx: Exception) {
                        // DB cache warning
                    }

                    emit(Resource.Success(domainList, isOffline = false))
                    emittedFromNetwork = true
                }
            } catch (e: Exception) {
                // Continuar a caché
            }
        }

        if (!emittedFromNetwork) {
            try {
                val entities = asignacionDao.getAllAsignaciones().first()
                val domainList = entities.map { e ->
                    Asignacion(
                        idAsignacion = e.idAsignacion,
                        asignaturaNombre = e.asignaturaNombre,
                        gradoNombre = e.gradoNombre,
                        paraleloLetra = e.paraleloLetra,
                        anoLectivoNombre = e.anoLectivoNombre,
                        cantidadEstudiantes = e.cantidadEstudiantes,
                        porcentajeAsistencia = e.porcentajeAsistencia,
                        promedioCalificaciones = e.promedioCalificaciones
                    )
                }
                emit(Resource.Success(domainList, isOffline = !connectivityObserver.isCurrentlyConnected()))
            } catch (dbEx: Exception) {
                emit(Resource.Error("No se pudieron obtener las asignaciones"))
            }
        }
    }

    override fun getEstudiantesPorAsignacion(idAsignacion: Long): Flow<Resource<List<Estudiante>>> = flow {
        emit(Resource.Loading)

        var emittedFromNetwork = false

        if (connectivityObserver.isCurrentlyConnected()) {
            try {
                val api = retrofitClient.getPrincipalDocenteApi()
                val response = api.getEstudiantesPorAsignacion(idAsignacion)
                if (response.isSuccessful && response.body() != null) {
                    val dtos = response.body()!!
                    val domainList = dtos.map { dto ->
                        Estudiante(
                            idMatricula = dto.idMatricula,
                            idAsignacion = idAsignacion,
                            estudianteId = dto.estudiante.id,
                            nombres = dto.estudiante.nombres,
                            apellidos = dto.estudiante.apellidos,
                            cedula = dto.estudiante.cedula ?: "",
                            estadoMatricula = dto.estado ?: "ACTIVA"
                        )
                    }

                    val entities = dtos.map { dto ->
                        EstudianteEntity(
                            idMatricula = dto.idMatricula,
                            idAsignacion = idAsignacion,
                            estudianteId = dto.estudiante.id,
                            nombres = dto.estudiante.nombres,
                            apellidos = dto.estudiante.apellidos,
                            cedula = dto.estudiante.cedula ?: "",
                            estadoMatricula = dto.estado ?: "ACTIVA"
                        )
                    }

                    try {
                        estudianteDao.clearEstudiantesByAsignacion(idAsignacion)
                        estudianteDao.insertEstudiantes(entities)
                    } catch (dbEx: Exception) {
                        // DB cache warning
                    }

                    emit(Resource.Success(domainList, isOffline = false))
                    emittedFromNetwork = true
                }
            } catch (e: Exception) {
                // Continuar con caché
            }
        }

        if (!emittedFromNetwork) {
            try {
                val entities = estudianteDao.getEstudiantesByAsignacion(idAsignacion).first()
                val domainList = entities.map { e ->
                    Estudiante(
                        idMatricula = e.idMatricula,
                        idAsignacion = e.idAsignacion,
                        estudianteId = e.estudianteId,
                        nombres = e.nombres,
                        apellidos = e.apellidos,
                        cedula = e.cedula,
                        estadoMatricula = e.estadoMatricula
                    )
                }
                emit(Resource.Success(domainList, isOffline = !connectivityObserver.isCurrentlyConnected()))
            } catch (dbEx: Exception) {
                emit(Resource.Error("No se pudieron obtener los estudiantes"))
            }
        }
    }

    override fun getPeriodosEvaluacion(): Flow<Resource<List<PeriodoEvaluacion>>> = flow {
        emit(Resource.Loading)

        if (connectivityObserver.isCurrentlyConnected()) {
            try {
                val api = retrofitClient.getDocenteApi()
                val response = api.getPeriodosEvaluacion()
                if (response.isSuccessful && response.body() != null) {
                    val entities = response.body()!!.map { dto ->
                        PeriodoEntity(
                            idPeriodo = dto.idPeriodo,
                            idAnoLectivo = dto.idAnoLectivo,
                            tipo = dto.tipo,
                            nombre = dto.nombre,
                            fechaInicio = dto.fechaInicio,
                            fechaFin = dto.fechaFin,
                            activo = dto.activo
                        )
                    }
                    periodoDao.insertPeriodos(entities)
                }
            } catch (e: Exception) {
                // Continuar con caché
            }
        }

        try {
            val entities = periodoDao.getPeriodosActivos().first()
            val domainList = entities.map { e ->
                PeriodoEvaluacion(
                    idPeriodo = e.idPeriodo,
                    idAnoLectivo = e.idAnoLectivo,
                    tipo = e.tipo,
                    nombre = e.nombre,
                    fechaInicio = e.fechaInicio,
                    fechaFin = e.fechaFin,
                    activo = e.activo
                )
            }
            emit(Resource.Success(domainList, isOffline = !connectivityObserver.isCurrentlyConnected()))
        } catch (e: Exception) {
            emit(Resource.Error("No se pudieron cargar los períodos de evaluación"))
        }
    }

    override suspend fun refreshDocenteContext(): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!connectivityObserver.isCurrentlyConnected()) {
                return@withContext Resource.Error("Sin conexión a Internet")
            }

            val apiPrincipal = retrofitClient.getPrincipalDocenteApi()
            val apiDocente = retrofitClient.getDocenteApi()

            val asigResp = apiPrincipal.getMisAsignaciones()
            if (asigResp.isSuccessful && asigResp.body() != null) {
                val entities = asigResp.body()!!.map { dto ->
                    AsignacionEntity(
                        idAsignacion = dto.idAsignacion,
                        asignaturaId = dto.asignatura.id,
                        asignaturaNombre = dto.asignatura.nombre,
                        gradoId = dto.grado.id,
                        gradoNombre = dto.grado.nombre,
                        paraleloId = dto.paralelo.id,
                        paraleloLetra = dto.paralelo.letra,
                        anoLectivoId = dto.anoLectivo.id,
                        anoLectivoNombre = dto.anoLectivo.nombre,
                        cantidadEstudiantes = dto.cantidadEstudiantes,
                        porcentajeAsistencia = dto.porcentajeAsistencia,
                        promedioCalificaciones = dto.promedioCalificaciones
                    )
                }
                asignacionDao.clearAsignaciones()
                asignacionDao.insertAsignaciones(entities)

                for (asig in asigResp.body()!!) {
                    val estResp = apiPrincipal.getEstudiantesPorAsignacion(asig.idAsignacion)
                    if (estResp.isSuccessful && estResp.body() != null) {
                        val estEntities = estResp.body()!!.map { dto ->
                            EstudianteEntity(
                                idMatricula = dto.idMatricula,
                                idAsignacion = asig.idAsignacion,
                                estudianteId = dto.estudiante.id,
                                nombres = dto.estudiante.nombres,
                                apellidos = dto.estudiante.apellidos,
                                cedula = dto.estudiante.cedula ?: "",
                                estadoMatricula = dto.estado ?: "ACTIVA"
                            )
                        }
                        estudianteDao.clearEstudiantesByAsignacion(asig.idAsignacion)
                        estudianteDao.insertEstudiantes(estEntities)
                    }
                }
            }

            val perResp = apiDocente.getPeriodosEvaluacion()
            if (perResp.isSuccessful && perResp.body() != null) {
                val perEntities = perResp.body()!!.map { dto ->
                    PeriodoEntity(
                        idPeriodo = dto.idPeriodo,
                        idAnoLectivo = dto.idAnoLectivo,
                        tipo = dto.tipo,
                        nombre = dto.nombre,
                        fechaInicio = dto.fechaInicio,
                        fechaFin = dto.fechaFin,
                        activo = dto.activo
                    )
                }
                periodoDao.insertPeriodos(perEntities)
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al actualizar datos")
        }
    }
}
