package ec.edu.uteq.sga.representante.data.remote

import ec.edu.uteq.sga.representante.core.AuthInterceptor
import ec.edu.uteq.sga.representante.core.SessionManager
import ec.edu.uteq.sga.representante.data.remote.api.AuthApi
import ec.edu.uteq.sga.representante.data.remote.api.DocenteApi
import ec.edu.uteq.sga.representante.data.remote.api.RepresentanteApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitClient(private val sessionManager: SessionManager) {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
        redactHeader("Authorization")
    }

    fun getRepresentantePrincipalApi(): RepresentanteApi = Retrofit.Builder()
        .baseUrl(sanitizeBaseUrl(sessionManager.getGatewayUrl())).client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create()).build().create(RepresentanteApi::class.java)

    fun getRepresentanteDocenteApi(): RepresentanteApi = Retrofit.Builder()
        .baseUrl(sanitizeBaseUrl(sessionManager.getDocenteUrl())).client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create()).build().create(RepresentanteApi::class.java)

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(sessionManager))
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun getAuthApi(): AuthApi {
        val baseUrl = sanitizeBaseUrl(sessionManager.getGatewayUrl())
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }

    fun getDocenteApi(): DocenteApi {
        // Usa la URL configurada para el microservicio de docente o gateway
        val baseUrl = sanitizeBaseUrl(sessionManager.getDocenteUrl())
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DocenteApi::class.java)
    }

    fun getPrincipalDocenteApi(): DocenteApi {
        // Usa la URL del SGA Principal (para asignaciones, estudiantes y horarios)
        val baseUrl = sanitizeBaseUrl(sessionManager.getGatewayUrl())
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DocenteApi::class.java)
    }

    private fun sanitizeBaseUrl(url: String): String {
        var clean = url.trim()
        if (!clean.endsWith("/")) {
            clean += "/"
        }
        return clean
    }
}
