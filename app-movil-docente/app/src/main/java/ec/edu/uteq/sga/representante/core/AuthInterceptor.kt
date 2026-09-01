package ec.edu.uteq.sga.representante.core

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = sessionManager.getToken()

        val requestBuilder = originalRequest.newBuilder()
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")

        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val response = chain.proceed(requestBuilder.build())

        // Si el token es inválido o expiró (401), se puede limpiar la sesión si no es la ruta de login
        if (response.code == 401 && !originalRequest.url.encodedPath.contains("/auth/login")) {
            sessionManager.clearSession()
        }

        return response
    }
}
