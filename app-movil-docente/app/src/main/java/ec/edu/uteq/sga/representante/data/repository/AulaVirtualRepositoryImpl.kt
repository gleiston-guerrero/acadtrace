package ec.edu.uteq.sga.representante.data.repository

import ec.edu.uteq.sga.representante.core.Resource
import ec.edu.uteq.sga.representante.data.remote.RetrofitClient
import ec.edu.uteq.sga.representante.data.remote.dto.AulaVirtualResumenResponseDTO
import ec.edu.uteq.sga.representante.data.remote.dto.AulaVirtualSemanasResponseDTO
import ec.edu.uteq.sga.representante.domain.repository.AulaVirtualRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AulaVirtualRepositoryImpl(
    private val retrofitClient: RetrofitClient
) : AulaVirtualRepository {

    private val docenteApi get() = retrofitClient.getDocenteApi()

    override suspend fun getResumenCursos(idsAsignacion: List<Long>): Resource<AulaVirtualResumenResponseDTO> =
        withContext(Dispatchers.IO) {
            try {
                if (idsAsignacion.isEmpty()) {
                    return@withContext Resource.Success(AulaVirtualResumenResponseDTO())
                }
                val resp = docenteApi.getAulaVirtualResumen(idsAsignacion)
                if (resp.isSuccessful && resp.body() != null) {
                    Resource.Success(resp.body()!!)
                } else {
                    Resource.Error("Error al obtener resumen de aula virtual: ${resp.code()}")
                }
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Error de conexión con aula virtual")
            }
        }

    override suspend fun getAgendaSemanas(idAsignacion: Long): Resource<AulaVirtualSemanasResponseDTO> =
        withContext(Dispatchers.IO) {
            try {
                val resp = docenteApi.getAulaVirtualSemanas(idAsignacion)
                if (resp.isSuccessful && resp.body() != null) {
                    Resource.Success(resp.body()!!)
                } else {
                    Resource.Error("Error al obtener agenda de semanas: ${resp.code()}")
                }
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Error de conexión con aula virtual")
            }
        }
}
