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
import kotlinx.coroutines.flow.firstOrNull
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

        // 1. Emitir caché local de inmediato si existe
        val cachedEntities = asignacionDao.getAllAsignaciones().firstOrNull() ?: emptyList()
        if (cachedEntities.isNotEmpty()) {
            val cachedDomain = cachedEntities.map { e ->
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
            emit(Resource.Success(cachedDomain, isOffline = !connectivityObserver.isCurrentlyConnected()))
        }

        // 2. Consultar red si hay conexión
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
                        // Error de BD no bloquea la UI
                    }

                    emit(Resource.Success(domainList, isOffline = false))
                } else if (cachedEntities.isEmpty()) {
                    emit(Resource.Error("El servidor SGA respondió con error (${response.code()})"))
                }
            } catch (e: Exception) {
                if (cachedEntities.isEmpty()) {
                    emit(Resource.Error("No se pudo conectar con el servidor SGA. Verifique su red."))
                }
            }
        } else if (cachedEntities.isEmpty()) {
            emit(Resource.Error("Sin conexión a Internet y sin datos en caché."))
        }
    }

    override suspend fun getAsignacion(idAsignacion: Long): Resource<Asignacion> = withContext(Dispatchers.IO) {
        val cached = asignacionDao.getAsignacionById(idAsignacion)
        if (cached != null) {
            return@withContext Resource.Success(
                Asignacion(
                    idAsignacion = cached.idAsignacion,
                    asignaturaNombre = cached.asignaturaNombre,
                    gradoNombre = cached.gradoNombre,
                    paraleloLetra = cached.paraleloLetra,
                    anoLectivoNombre = cached.anoLectivoNombre,
                    cantidadEstudiantes = cached.cantidadEstudiantes,
                    porcentajeAsistencia = cached.porcentajeAsistencia,
                    promedioCalificaciones = cached.promedioCalificaciones
                )
            )
        }

        try {
            val api = retrofitClient.getPrincipalDocenteApi()
            val response = api.getMisAsignaciones()
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!.find { it.idAsignacion == idAsignacion }
                if (dto != null) {
                    val entity = AsignacionEntity(
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
                    asignacionDao.insertAsignaciones(listOf(entity))

                    return@withContext Resource.Success(
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
                    )
                }
            }
        } catch (e: Exception) {
            // Error de red
        }

        return@withContext Resource.Error("No se encontró la asignación solicitada")
    }

    override fun getEstudiantesPorAsignacion(idAsignacion: Long): Flow<Resource<List<Estudiante>>> = flow {
        emit(Resource.Loading)

        // 1. Emitir caché local de inmediato si existe
        val cachedEntities = estudianteDao.getEstudiantesByAsignacion(idAsignacion).firstOrNull() ?: emptyList()
        if (cachedEntities.isNotEmpty()) {
            val cachedDomain = cachedEntities.map { e ->
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
            emit(Resource.Success(cachedDomain, isOffline = !connectivityObserver.isCurrentlyConnected()))
        }

        // 2. Consultar red
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
                        // Error de BD no bloquea la UI
                    }

                    emit(Resource.Success(domainList, isOffline = false))
                } else if (cachedEntities.isEmpty()) {
                    emit(Resource.Error("El servidor SGA respondió con error (${response.code()})"))
                }
            } catch (e: Exception) {
                if (cachedEntities.isEmpty()) {
                    emit(Resource.Error("No se pudo obtener la nómina de estudiantes. Verifique la conexión."))
                }
            }
        } else if (cachedEntities.isEmpty()) {
            emit(Resource.Error("Sin conexión a Internet y sin estudiantes en caché."))
        }
    }

    override fun getPeriodosEvaluacion(): Flow<Resource<List<PeriodoEvaluacion>>> = flow {
        emit(Resource.Loading)

        val cachedEntities = periodoDao.getPeriodosActivos().firstOrNull() ?: emptyList()
        if (cachedEntities.isNotEmpty()) {
            val cachedDomain = cachedEntities.map { e ->
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
            emit(Resource.Success(cachedDomain, isOffline = !connectivityObserver.isCurrentlyConnected()))
        }

        if (connectivityObserver.isCurrentlyConnected()) {
            try {
                val api = retrofitClient.getDocenteApi()
                val response = api.getPeriodosEvaluacion()
                if (response.isSuccessful && response.body() != null) {
                    val dtos = response.body()!!
                    val entities = dtos.map { dto ->
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
                    val domainList = dtos.map { dto ->
                        PeriodoEvaluacion(
                            idPeriodo = dto.idPeriodo,
                            idAnoLectivo = dto.idAnoLectivo,
                            tipo = dto.tipo,
                            nombre = dto.nombre,
                            fechaInicio = dto.fechaInicio,
                            fechaFin = dto.fechaFin,
                            activo = dto.activo
                        )
                    }
                    try {
                        periodoDao.insertPeriodos(entities)
                    } catch (dbEx: Exception) {
                        // DB warning
                    }
                    emit(Resource.Success(domainList, isOffline = false))
                }
            } catch (e: Exception) {
                if (cachedEntities.isEmpty()) {
                    emit(Resource.Error("No se pudieron cargar los períodos de evaluación"))
                }
            }
        }
    }

    override suspend fun refreshDocenteContext(): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!connectivityObserver.isCurrentlyConnected()) {
                return@withContext Resource.Error("Sin conexión a Internet")
            }

            val apiPrincipal = retrofitClient.getPrincipalDocenteApi()
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
                    try {
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
                    } catch (e: Exception) {
                        // Ignore individual course sync errors
                    }
                }
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Error al sincronizar datos del docente: ${e.message}")
        }
    }
}
