package ec.edu.uteq.sga.docente.data.repository

import ec.edu.uteq.sga.docente.core.Constants
import ec.edu.uteq.sga.docente.core.Resource
import ec.edu.uteq.sga.docente.core.SessionManager
import ec.edu.uteq.sga.docente.data.remote.RetrofitClient
import ec.edu.uteq.sga.docente.data.remote.dto.LoginRequest
import ec.edu.uteq.sga.docente.domain.model.UserSession
import ec.edu.uteq.sga.docente.domain.repository.AuthRepository
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
                    val isDocente = body.roles.any { it.equals(Constants.ROLE_DOCENTE, ignoreCase = true) }

                    if (!isDocente) {
                        return@withContext Resource.Error("Acceso denegado. La cuenta no posee el rol de DOCENTE.")
                    }

                    sessionManager.saveSession(
                        token = body.token,
                        idUsuario = body.idUsuario,
                        username = body.username,
                        correo = body.correo,
                        roles = body.roles
                    )

                    Resource.Success(
                        UserSession(
                            token = body.token,
                            idUsuario = body.idUsuario,
                            username = body.username,
                            correo = body.correo,
                            roles = body.roles
                        )
                    )
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    val msg = if (response.code() == 401 || response.code() == 403) {
                        "Credenciales incorrectas o usuario no autorizado."
                    } else {
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

    override fun isDocente(): Boolean = sessionManager.isDocente()

    override fun getTeacherUserId(): Long = sessionManager.getUserId()
}
