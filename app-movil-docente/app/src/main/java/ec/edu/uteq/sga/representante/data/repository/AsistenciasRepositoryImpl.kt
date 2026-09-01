package ec.edu.uteq.sga.representante.data.repository

import ec.edu.uteq.sga.representante.core.NetworkConnectivityObserver
import ec.edu.uteq.sga.representante.core.Resource
import ec.edu.uteq.sga.representante.data.local.AppDatabase
import ec.edu.uteq.sga.representante.data.local.entity.AsistenciaEntity
import ec.edu.uteq.sga.representante.data.local.entity.ResumenAsistenciaEntity
import ec.edu.uteq.sga.representante.data.remote.RetrofitClient
import ec.edu.uteq.sga.representante.data.remote.dto.AsistenciaCreateDTO
import ec.edu.uteq.sga.representante.data.sync.SyncManager
import ec.edu.uteq.sga.representante.domain.model.AsistenciaRegistro
import ec.edu.uteq.sga.representante.domain.model.ResumenAsistencia
import ec.edu.uteq.sga.representante.domain.repository.AsistenciasRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AsistenciasRepositoryImpl(
    private val database: AppDatabase,
    private val retrofitClient: RetrofitClient,
    private val syncManager: SyncManager,
    private val connectivityObserver: NetworkConnectivityObserver
) : AsistenciasRepository {

    private val asistenciaDao = database.asistenciaDao()
    private val resumenDao = database.resumenAsistenciaDao()
    private val docenteApi get() = retrofitClient.getDocenteApi()

    override fun getAsistenciasPorFecha(
        idAsignacion: Long,
        fecha: String
    ): Flow<Resource<List<AsistenciaRegistro>>> = flow {
        emit(Resource.Loading)

        // 1. Emitir caché local de inmediato
        val cached = asistenciaDao.getAsistenciasPorFecha(idAsignacion, fecha).firstOrNull() ?: emptyList()
        if (cached.isNotEmpty()) {
            val domainCached = cached.map { e ->
                AsistenciaRegistro(
                    idAsistencia = e.idAsistencia,
                    idMatricula = e.idMatricula,
                    idAsignacion = e.idAsignacion,
                    idPeriodo = e.idPeriodo,
                    fecha = e.fecha,
                    estado = e.estado,
                    justificacion = e.justificacion,
                    isPendingSync = e.isPendingSync
                )
            }
            emit(Resource.Success(domainCached, isOffline = !connectivityObserver.isCurrentlyConnected()))
        }

        // 2. Consultar red si hay conexión
        if (connectivityObserver.isCurrentlyConnected()) {
            try {
                val resp = docenteApi.getAsistencias(idAsignacion = idAsignacion, fecha = fecha)
                if (resp.isSuccessful && resp.body() != null) {
                    val entities = resp.body()!!.map { dto ->
                        AsistenciaEntity(
                            idAsistencia = dto.idAsistencia,
                            idMatricula = dto.idMatricula,
                            idAsignacion = dto.idAsignacion,
                            idPeriodo = dto.idPeriodo,
                            fecha = dto.fecha,
                            estado = dto.estado,
                            justificacion = dto.justificacion,
                            registradoPor = dto.registradoPor,
                            isPendingSync = false
                        )
                    }
                    asistenciaDao.insertAsistencias(entities)

                    val domainList = entities.map { e ->
                        AsistenciaRegistro(
                            idAsistencia = e.idAsistencia,
                            idMatricula = e.idMatricula,
                            idAsignacion = e.idAsignacion,
                            idPeriodo = e.idPeriodo,
                            fecha = e.fecha,
                            estado = e.estado,
                            justificacion = e.justificacion,
                            isPendingSync = false
                        )
                    }
                    emit(Resource.Success(domainList, isOffline = false))
                }
            } catch (e: Exception) {
                if (cached.isEmpty()) {
                    emit(Resource.Error("No se pudo cargar la asistencia para la fecha seleccionada."))
                }
            }
        } else if (cached.isEmpty()) {
            emit(Resource.Error("Sin conexión a Internet."))
        }
    }

    override suspend fun saveAsistencia(
        asistencia: AsistenciaCreateDTO,
        idAsistencia: Long?
    ): Resource<AsistenciaRegistro> = withContext(Dispatchers.IO) {
        val isOnline = connectivityObserver.isCurrentlyConnected()
        val tempId = idAsistencia ?: -System.currentTimeMillis()

        val localEntity = AsistenciaEntity(
            idAsistencia = tempId,
            idMatricula = asistencia.idMatricula,
            idAsignacion = asistencia.idAsignacion,
            idPeriodo = asistencia.idPeriodo,
            fecha = asistencia.fecha,
            estado = asistencia.estado,
            justificacion = asistencia.justificacion,
            registradoPor = asistencia.registradoPor,
            isPendingSync = !isOnline
        )

        asistenciaDao.insertAsistencia(localEntity)

        if (isOnline) {
            try {
                val resp = if (idAsistencia != null && idAsistencia > 0) {
                    docenteApi.updateAsistencia(idAsistencia, asistencia)
                } else {
                    docenteApi.createAsistencia(asistencia)
                }

                if (resp.isSuccessful && resp.body() != null) {
                    val serverDto = resp.body()!!
                    val serverEntity = AsistenciaEntity(
                        idAsistencia = serverDto.idAsistencia,
                        idMatricula = serverDto.idMatricula,
                        idAsignacion = serverDto.idAsignacion,
                        idPeriodo = serverDto.idPeriodo,
                        fecha = serverDto.fecha,
                        estado = serverDto.estado,
                        justificacion = serverDto.justificacion,
                        registradoPor = serverDto.registradoPor,
                        isPendingSync = false
                    )
                    if (idAsistencia == null) {
                        asistenciaDao.updateAsistenciaRemoteId(tempId, serverDto.idAsistencia)
                    } else {
                        asistenciaDao.insertAsistencia(serverEntity)
                    }
                    return@withContext Resource.Success(
                        AsistenciaRegistro(
                            idAsistencia = serverDto.idAsistencia,
                            idMatricula = serverDto.idMatricula,
                            idAsignacion = serverDto.idAsignacion,
                            idPeriodo = serverDto.idPeriodo,
                            fecha = serverDto.fecha,
                            estado = serverDto.estado,
                            justificacion = serverDto.justificacion,
                            isPendingSync = false
                        )
                    )
                }
            } catch (e: Exception) {
                // Fallo red -> encolar
            }
        }

        val action = if (idAsistencia != null && idAsistencia > 0) "UPDATE" else "CREATE"
        syncManager.enqueueOperation(
            entityType = "ASISTENCIA",
            actionType = action,
            localId = tempId,
            remoteId = idAsistencia,
            payload = asistencia
        )

        Resource.Success(
            AsistenciaRegistro(
                idAsistencia = tempId,
                idMatricula = asistencia.idMatricula,
                idAsignacion = asistencia.idAsignacion,
                idPeriodo = asistencia.idPeriodo,
                fecha = asistencia.fecha,
                estado = asistencia.estado,
                justificacion = asistencia.justificacion,
                isPendingSync = true
            ),
            isOffline = true
        )
    }

    override fun getResumenAsistencia(
        idAsignacion: Long,
        idPeriodo: Long?
    ): Flow<Resource<List<ResumenAsistencia>>> = flow {
        emit(Resource.Loading)

        val cached = resumenDao.getResumenesAsistencia(idAsignacion, idPeriodo).firstOrNull() ?: emptyList()
        if (cached.isNotEmpty()) {
            val domainList = cached.map { e ->
                ResumenAsistencia(
                    idResumen = e.idResumen,
                    idMatricula = e.idMatricula,
                    idAsignacion = e.idAsignacion,
                    idPeriodo = e.idPeriodo,
                    totalPresentes = e.totalPresentes,
                    totalAusentes = e.totalAusentes,
                    totalJustificados = e.totalJustificados,
                    totalAtrasos = e.totalAtrasos
                )
            }
            emit(Resource.Success(domainList, isOffline = !connectivityObserver.isCurrentlyConnected()))
        }

        if (connectivityObserver.isCurrentlyConnected()) {
            try {
                val resp = docenteApi.getResumenAsistencia(idAsignacion = idAsignacion, idPeriodo = idPeriodo)
                if (resp.isSuccessful && resp.body() != null) {
                    val entities = resp.body()!!.map { dto ->
                        ResumenAsistenciaEntity(
                            idResumen = dto.idResumen,
                            idMatricula = dto.idMatricula,
                            idAsignacion = dto.idAsignacion,
                            idPeriodo = dto.idPeriodo,
                            totalPresentes = dto.totalPresentes,
                            totalAusentes = dto.totalAusentes,
                            totalJustificados = dto.totalJustificados,
                            totalAtrasos = dto.totalAtrasos,
                            calculadoEn = dto.calculadoEn
                        )
                    }
                    resumenDao.insertResumenes(entities)

                    val domainList = entities.map { e ->
                        ResumenAsistencia(
                            idResumen = e.idResumen,
                            idMatricula = e.idMatricula,
                            idAsignacion = e.idAsignacion,
                            idPeriodo = e.idPeriodo,
                            totalPresentes = e.totalPresentes,
                            totalAusentes = e.totalAusentes,
                            totalJustificados = e.totalJustificados,
                            totalAtrasos = e.totalAtrasos
                        )
                    }
                    emit(Resource.Success(domainList, isOffline = false))
                }
            } catch (e: Exception) {
                if (cached.isEmpty()) {
                    emit(Resource.Error("No se pudo cargar el resumen de asistencias."))
                }
            }
        } else if (cached.isEmpty()) {
            emit(Resource.Error("Sin conexión a Internet."))
        }
    }

    override suspend fun calcularResumen(
        idMatricula: Long,
        idAsignacion: Long,
        idPeriodo: Long
    ): Resource<ResumenAsistencia> = withContext(Dispatchers.IO) {
        try {
            val resp = docenteApi.calcularResumenAsistencia(
                mapOf(
                    "id_matricula" to idMatricula,
                    "id_asignacion" to idAsignacion,
                    "id_periodo" to idPeriodo
                )
            )
            if (resp.isSuccessful && resp.body() != null) {
                val dto = resp.body()!!
                Resource.Success(
                    ResumenAsistencia(
                        idResumen = dto.idResumen,
                        idMatricula = dto.idMatricula,
                        idAsignacion = dto.idAsignacion,
                        idPeriodo = dto.idPeriodo,
                        totalPresentes = dto.totalPresentes,
                        totalAusentes = dto.totalAusentes,
                        totalJustificados = dto.totalJustificados,
                        totalAtrasos = dto.totalAtrasos
                    )
                )
            } else {
                Resource.Error("Error al calcular resumen: ${resp.code()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error de red")
        }
    }
}
