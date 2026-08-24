package ec.edu.uteq.sga.docente.data.repository

import ec.edu.uteq.sga.docente.core.NetworkConnectivityObserver
import ec.edu.uteq.sga.docente.core.Resource
import ec.edu.uteq.sga.docente.data.local.AppDatabase
import ec.edu.uteq.sga.docente.data.local.entity.PromedioAnualEntity
import ec.edu.uteq.sga.docente.data.local.entity.PromedioTrimestralEntity
import ec.edu.uteq.sga.docente.data.remote.RetrofitClient
import ec.edu.uteq.sga.docente.domain.model.PromedioAnual
import ec.edu.uteq.sga.docente.domain.model.PromedioTrimestral
import ec.edu.uteq.sga.docente.domain.repository.PromediosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PromediosRepositoryImpl(
    private val database: AppDatabase,
    private val retrofitClient: RetrofitClient,
    private val connectivityObserver: NetworkConnectivityObserver
) : PromediosRepository {

    private val promediosDao = database.promediosDao()
    private val docenteApi get() = retrofitClient.getDocenteApi()

    override fun getPromediosTrimestrales(
        idAsignacion: Long,
        idPeriodo: Long?
    ): Flow<Resource<List<PromedioTrimestral>>> = flow {
        emit(Resource.Loading)

        if (connectivityObserver.isCurrentlyConnected()) {
            try {
                val resp = docenteApi.getPromediosTrimestrales(idAsignacion = idAsignacion, idPeriodo = idPeriodo)
                if (resp.isSuccessful && resp.body() != null) {
                    val entities = resp.body()!!.map { dto ->
                        PromedioTrimestralEntity(
                            idPromedio = dto.idPromedio,
                            idMatricula = dto.idMatricula,
                            idAsignacion = dto.idAsignacion,
                            idPeriodo = dto.idPeriodo,
                            promedioFormativo = dto.promedioFormativo,
                            notaSumativa = dto.notaSumativa,
                            promedioTrimestral = dto.promedioTrimestral,
                            notaCualitativa = dto.notaCualitativa,
                            calculadoEn = dto.calculadoEn
                        )
                    }
                    promediosDao.insertPromediosTrimestrales(entities)
                }
            } catch (e: Exception) {
                // Continuar con Room
            }
        }

        promediosDao.getPromediosTrimestrales(idAsignacion, idPeriodo).map { list ->
            val domainList = list.map { e ->
                PromedioTrimestral(
                    idPromedio = e.idPromedio,
                    idMatricula = e.idMatricula,
                    idAsignacion = e.idAsignacion,
                    idPeriodo = e.idPeriodo,
                    promedioFormativo = e.promedioFormativo,
                    notaSumativa = e.notaSumativa,
                    promedioTrimestral = e.promedioTrimestral,
                    notaCualitativa = e.notaCualitativa
                )
            }
            Resource.Success(domainList, isOffline = !connectivityObserver.isCurrentlyConnected())
        }.collect { emit(it) }
    }

    override suspend fun calcularPromedioTrimestral(
        idMatricula: Long,
        idAsignacion: Long,
        idPeriodo: Long
    ): Resource<PromedioTrimestral> = withContext(Dispatchers.IO) {
        try {
            val resp = docenteApi.calcularPromedioTrimestral(
                mapOf(
                    "id_matricula" to idMatricula,
                    "id_asignacion" to idAsignacion,
                    "id_periodo" to idPeriodo,
                    "nivel" to "EGB"
                )
            )
            if (resp.isSuccessful && resp.body() != null) {
                val dto = resp.body()!!
                Resource.Success(
                    PromedioTrimestral(
                        idPromedio = dto.idPromedio,
                        idMatricula = dto.idMatricula,
                        idAsignacion = dto.idAsignacion,
                        idPeriodo = dto.idPeriodo,
                        promedioFormativo = dto.promedioFormativo,
                        notaSumativa = dto.notaSumativa,
                        promedioTrimestral = dto.promedioTrimestral,
                        notaCualitativa = dto.notaCualitativa
                    )
                )
            } else {
                Resource.Error("Error al calcular promedio trimestral: ${resp.code()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error de red")
        }
    }

    override fun getPromediosAnuales(idAsignacion: Long): Flow<Resource<List<PromedioAnual>>> = flow {
        emit(Resource.Loading)

        if (connectivityObserver.isCurrentlyConnected()) {
            try {
                val resp = docenteApi.getPromediosAnuales(idAsignacion = idAsignacion)
                if (resp.isSuccessful && resp.body() != null) {
                    val entities = resp.body()!!.map { dto ->
                        PromedioAnualEntity(
                            idPromedioAnual = dto.idPromedioAnual,
                            idMatricula = dto.idMatricula,
                            idAsignacion = dto.idAsignacion,
                            idAnoLectivo = dto.idAnoLectivo,
                            promedioAnual = dto.promedioAnual,
                            notaCualitativa = dto.notaCualitativa,
                            registradoPor = dto.registradoPor,
                            calculadoEn = dto.calculadoEn
                        )
                    }
                    promediosDao.insertPromediosAnuales(entities)
                }
            } catch (e: Exception) {
                // Room fallback
            }
        }

        promediosDao.getPromediosAnuales(idAsignacion).map { list ->
            val domainList = list.map { e ->
                PromedioAnual(
                    idPromedioAnual = e.idPromedioAnual,
                    idMatricula = e.idMatricula,
                    idAsignacion = e.idAsignacion,
                    idAnoLectivo = e.idAnoLectivo,
                    promedioAnual = e.promedioAnual,
                    notaCualitativa = e.notaCualitativa
                )
            }
            Resource.Success(domainList, isOffline = !connectivityObserver.isCurrentlyConnected())
        }.collect { emit(it) }
    }

    override suspend fun calcularPromedioAnual(
        idMatricula: Long,
        idAsignacion: Long,
        idAnoLectivo: Long
    ): Resource<PromedioAnual> = withContext(Dispatchers.IO) {
        try {
            val resp = docenteApi.calcularPromedioAnual(
                mapOf(
                    "id_matricula" to idMatricula,
                    "id_asignacion" to idAsignacion,
                    "id_ano_lectivo" to idAnoLectivo,
                    "nivel" to "EGB"
                )
            )
            if (resp.isSuccessful && resp.body() != null) {
                val dto = resp.body()!!
                Resource.Success(
                    PromedioAnual(
                        idPromedioAnual = dto.idPromedioAnual,
                        idMatricula = dto.idMatricula,
                        idAsignacion = dto.idAsignacion,
                        idAnoLectivo = dto.idAnoLectivo,
                        promedioAnual = dto.promedioAnual,
                        notaCualitativa = dto.notaCualitativa
                    )
                )
            } else {
                Resource.Error("Error al calcular promedio anual: ${resp.code()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error de red")
        }
    }
}
