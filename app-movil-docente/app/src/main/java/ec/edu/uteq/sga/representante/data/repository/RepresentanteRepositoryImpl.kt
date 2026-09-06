package ec.edu.uteq.sga.representante.data.repository

import android.util.Log
import com.google.gson.JsonParseException
import com.google.gson.Gson
import ec.edu.uteq.sga.representante.core.Resource
import ec.edu.uteq.sga.representante.data.local.AppDatabase
import ec.edu.uteq.sga.representante.data.local.entity.*
import ec.edu.uteq.sga.representante.data.remote.RetrofitClient
import ec.edu.uteq.sga.representante.data.remote.dto.*
import ec.edu.uteq.sga.representante.domain.model.*
import ec.edu.uteq.sga.representante.domain.repository.RepresentanteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException

class RepresentanteRepositoryImpl(private val db: AppDatabase, private val client: RetrofitClient) : RepresentanteRepository {
    private val gson = Gson()
    private val cache get() = db.representanteCacheDao()

    override fun getRepresentados(): Flow<Resource<List<Representado>>> = flow {
        emit(Resource.Loading)
        val result = try {
            val response = client.getRepresentantePrincipalApi().getRepresentados()
            if (!response.isSuccessful) throw HttpException(response)
            val body = response.body().orEmpty()
            cache.putRepresentados(body.map { RepresentadoCacheEntity(it.idEstudiante, gson.toJson(it)) })
            Resource.Success(body.map { it.domain() })
        } catch (error: Exception) {
            logFailure("getRepresentados", error)
            val cached = runCatching {
                cache.getRepresentados().map { gson.fromJson(it.json, RepresentadoDTO::class.java).domain() }
            }.getOrDefault(emptyList())
            if (cached.isNotEmpty()) Resource.Success(cached, isOffline = true) else Resource.Error(message(error), error)
        }
        emit(result)
    }

    override fun getCalificaciones(idEstudiante: Long): Flow<Resource<CalificacionesRepresentado>> = flow {
        emit(Resource.Loading)
        val result = try {
            val response = client.getRepresentantePrincipalApi().getCalificaciones(idEstudiante)
            if (!response.isSuccessful) throw HttpException(response)
            val body = requireNotNull(response.body())
            cache.putCalificaciones(CalificacionesRepresentadoCacheEntity(idEstudiante, gson.toJson(body)))
            Resource.Success(body.domain())
        } catch (error: Exception) {
            logFailure("getCalificaciones", error)
            val cached = runCatching {
                cache.getCalificaciones(idEstudiante)?.let { gson.fromJson(it.json, CalificacionesRepresentadoDTO::class.java).domain() }
            }.getOrNull()
            if (cached != null) Resource.Success(cached, isOffline = true) else Resource.Error(message(error), error)
        }
        emit(result)
    }

    override fun getAsistencia(idEstudiante: Long): Flow<Resource<AsistenciaRepresentado>> = flow {
        emit(Resource.Loading)
        val result = try {
            val response = client.getRepresentantePrincipalApi().getAsistencia(idEstudiante)
            if (!response.isSuccessful) throw HttpException(response)
            val body = requireNotNull(response.body())
            cache.putAsistencia(AsistenciaHijoCacheEntity(idEstudiante, gson.toJson(body)))
            Resource.Success(body.domain())
        } catch (error: Exception) {
            logFailure("getAsistencia", error)
            val cached = runCatching {
                cache.getAsistencia(idEstudiante)?.let { gson.fromJson(it.json, AsistenciaRepresentadoDTO::class.java).domain() }
            }.getOrNull()
            if (cached != null) Resource.Success(cached, isOffline = true) else Resource.Error(message(error), error)
        }
        emit(result)
    }

    override fun getComunicados(): Flow<Resource<List<Comunicado>>> = flow {
        emit(Resource.Loading)
        val result = try {
            val response = client.getRepresentantePrincipalApi().getComunicados()
            if (!response.isSuccessful) throw HttpException(response)
            val body = response.body().orEmpty()
            cache.putComunicados(ComunicadosRepresentanteCacheEntity(json = gson.toJson(body)))
            Resource.Success(body.map { it.domain() })
        } catch (error: Exception) {
            logFailure("getComunicados", error)
            val cached = runCatching {
                cache.getComunicados()?.let { entity ->
                    val type = com.google.gson.reflect.TypeToken.getParameterized(List::class.java, ComunicadoDTO::class.java).type
                    gson.fromJson<List<ComunicadoDTO>>(entity.json, type).map { it.domain() }
                }
            }.getOrNull()
            if (cached != null) Resource.Success(cached, isOffline = true) else Resource.Error(message(error), error)
        }
        emit(result)
    }

    private fun RepresentadoDTO.domain() = Representado(idEstudiante, nombres, apellidos, curso, paralelo, matriculas)
    private fun CalificacionesRepresentadoDTO.domain() = CalificacionesRepresentado(
        calificaciones.map { NotaRepresentado(it.actividad, it.periodo, it.nota, it.notaCualitativa) },
        promedios.map { PromedioRepresentado(it.periodo, it.promedioFormativo, it.notaSumativa, it.promedioTrimestral, it.notaCualitativa) })
    private fun AsistenciaRepresentadoDTO.domain() = AsistenciaRepresentado(
        asistencias.map { AsistenciaHijo(it.fecha, it.periodo, it.estado) },
        ResumenAsistenciaHijo(resumen.total, resumen.presentes, resumen.ausentes, resumen.justificados, resumen.atrasos, resumen.porcentajeAsistencia))
    private fun ComunicadoDTO.domain() = Comunicado(id, titulo, contenido, fecha, fijado)
    private fun message(error: Exception): String = when (error) {
        is HttpException -> when (error.code()) {
            401 -> "Sesión expirada"
            403 -> "No autorizado para consultar este estudiante"
            404 -> "Estudiante no encontrado"
            503 -> "Servicio no disponible"
            else -> "Error HTTP del servicio (${error.code()})"
        }
        is IOException -> "Sin conexión y sin datos almacenados"
        is JsonParseException -> "La respuesta del servicio no tiene el formato esperado"
        is IllegalStateException -> "La respuesta del servicio está incompleta"
        else -> "Error local inesperado (${error::class.java.simpleName})"
    }

    private fun logFailure(operation: String, error: Exception) {
        val status = (error as? HttpException)?.code()?.toString() ?: "n/a"
        Log.e(TAG, "$operation failed; http=$status; type=${error::class.java.simpleName}; message=${error.message}")
    }

    private companion object {
        const val TAG = "RepresentanteRepository"
    }
}
