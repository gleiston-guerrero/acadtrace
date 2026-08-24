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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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

        // 1. Emitir datos locales de Room de inmediato
        val localEntities = asignacionDao.getAllAsignaciones()
        
        // 2. Si hay conexión a internet, sincronizar con el servidor
        if (connectivityObserver.isCurrentlyConnected()) {
            try {
                val api = retrofitClient.getPrincipalDocenteApi()
                val response = api.getMisAsignaciones()
                if (response.isSuccessful && response.body() != null) {
                    val entities = response.body()!!.map { dto ->
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
                }
            } catch (e: Exception) {
                // Si falla la red, continuamos con los datos cacheados
            }
        }

        // 3. Emitir el flujo local actualizado
        localEntities.map { entities ->
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
            Resource.Success(domainList, isOffline = !connectivityObserver.isCurrentlyConnected())
        }.collect { emit(it) }
    }

    override fun getEstudiantesPorAsignacion(idAsignacion: Long): Flow<Resource<List<Estudiante>>> = flow {
        emit(Resource.Loading)

        if (connectivityObserver.isCurrentlyConnected()) {
            try {
                val api = retrofitClient.getPrincipalDocenteApi()
                val response = api.getEstudiantesPorAsignacion(idAsignacion)
                if (response.isSuccessful && response.body() != null) {
                    val entities = response.body()!!.map { dto ->
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
                    estudianteDao.clearEstudiantesByAsignacion(idAsignacion)
                    estudianteDao.insertEstudiantes(entities)
                }
            } catch (e: Exception) {
                // Continuar con caché
            }
        }

        estudianteDao.getEstudiantesByAsignacion(idAsignacion).map { entities ->
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
            Resource.Success(domainList, isOffline = !connectivityObserver.isCurrentlyConnected())
        }.collect { emit(it) }
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

        periodoDao.getPeriodosActivos().map { entities ->
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
            Resource.Success(domainList, isOffline = !connectivityObserver.isCurrentlyConnected())
        }.collect { emit(it) }
    }

    override suspend fun refreshDocenteContext(): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!connectivityObserver.isCurrentlyConnected()) {
                return@withContext Resource.Error("Sin conexión para actualizar datos.")
            }
            val api = retrofitClient.getPrincipalDocenteApi()
            val asignacionesResp = api.getMisAsignaciones()
            if (asignacionesResp.isSuccessful && asignacionesResp.body() != null) {
                val entities = asignacionesResp.body()!!.map { dto ->
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
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al actualizar contexto docente.")
        }
    }
}
