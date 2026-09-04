package ec.edu.uteq.sga.representante.data.remote.api

import ec.edu.uteq.sga.representante.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface RepresentanteApi {
    @GET("representante/me/estudiantes")
    suspend fun getRepresentados(): Response<List<RepresentadoDTO>>

    @GET("representante/me/estudiantes/{id}/calificaciones")
    suspend fun getCalificaciones(@Path("id") idEstudiante: Long): Response<CalificacionesRepresentadoDTO>

    @GET("representante/me/estudiantes/{id}/asistencia")
    suspend fun getAsistencia(@Path("id") idEstudiante: Long): Response<AsistenciaRepresentadoDTO>

    @GET("representante/me/comunicados")
    suspend fun getComunicados(): Response<List<ComunicadoDTO>>
}
