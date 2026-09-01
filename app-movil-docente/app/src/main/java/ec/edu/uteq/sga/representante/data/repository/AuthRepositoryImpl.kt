package ec.edu.uteq.sga.representante.data.repository

import ec.edu.uteq.sga.representante.core.Constants
import ec.edu.uteq.sga.representante.core.Resource
import ec.edu.uteq.sga.representante.core.SessionManager
import ec.edu.uteq.sga.representante.data.remote.RetrofitClient
import ec.edu.uteq.sga.representante.data.remote.dto.LoginRequest
import ec.edu.uteq.sga.representante.domain.model.UserSession
import ec.edu.uteq.sga.representante.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val retrofitClient: RetrofitClient,
    private val sessionManager: SessionManager
) : AuthRepository {

    override suspend fun login(username: String, password: String): Resource<UserSession> =
        withContext(Dispatchers.IO) {
            try {
                val api = retrofitClient.getAuthApi()
                val response = api.login(LoginRequest(username = username, password = password))

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val isRepresentante = RepresentanteAccessPolicy.isAllowed(body.roles)

                    if (!isRepresentante) {
                        return@withContext Resource.Error("Acceso denegado. Esta aplicación requiere el rol REPRESENTANTE.")
                    }

                    sessionManager.saveSession(
                        token = body.token,
                        idUsuario = body.idUsuario,
                        username = body.username,
                        correo = body.correo,
                        roles = body.roles,
                        primerIngreso = body.primerIngreso
                    )

                    Resource.Success(
                        UserSession(
                            token = body.token,
                            idUsuario = body.idUsuario,
                            username = body.username,
                            correo = body.correo,
                            roles = body.roles,
                            primerIngreso = body.primerIngreso
                        )
                    )
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    val msg = when {
                        errorBody.contains("Bad credentials", ignoreCase = true) ->
                            "Usuario o contraseña incorrectos. Verifica tus credenciales."
                        response.code() == 401 || response.code() == 403 ->
                            "Credenciales incorrectas o usuario no autorizado."
                        else ->
                            "Error al iniciar sesión (${response.code()}): $errorBody"
                    }
                    Resource.Error(msg)
                }
            } catch (e: Exception) {
                Resource.Error("No se pudo conectar con el servidor: ${e.localizedMessage ?: e.message}", e)
            }
        }

    override fun logout() {
        sessionManager.clearSession()
    }

    override fun isUserLoggedIn(): Boolean = sessionManager.isLoggedIn.value

    override fun isRepresentante(): Boolean = sessionManager.isRepresentante()

    override fun getUserId(): Long = sessionManager.getUserId()
}

object RepresentanteAccessPolicy {
    fun isAllowed(roles: List<String>): Boolean =
        roles.any { it == Constants.ROL_REPRESENTANTE_LOGIN }
}
