package ec.edu.uteq.sga.docente.core

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            Constants.PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val gson = Gson()

    private val _isLoggedIn = MutableStateFlow(!getToken().isNullOrBlank())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun saveSession(
        token: String,
        idUsuario: Long,
        username: String,
        correo: String?,
        roles: List<String>,
        idPersona: Long? = null
    ) {
        prefs.edit()
            .putString(Constants.KEY_JWT_TOKEN, token)
            .putLong(Constants.KEY_USER_ID, idUsuario)
            .putString(Constants.KEY_USERNAME, username)
            .putString(Constants.KEY_CORREO, correo ?: "")
            .putString(Constants.KEY_ROLES, gson.toJson(roles))
            .apply()

        if (idPersona != null) {
            prefs.edit().putLong(Constants.KEY_ID_PERSONA, idPersona).apply()
        }

        _isLoggedIn.value = true
    }

    fun getToken(): String? = prefs.getString(Constants.KEY_JWT_TOKEN, null)

    fun getUserId(): Long = prefs.getLong(Constants.KEY_USER_ID, 0L)

    fun getUsername(): String? = prefs.getString(Constants.KEY_USERNAME, null)

    fun getCorreo(): String? = prefs.getString(Constants.KEY_CORREO, null)

    fun getRoles(): List<String> {
        val rolesJson = prefs.getString(Constants.KEY_ROLES, null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(rolesJson, type) ?: emptyList()
    }

    fun isDocente(): Boolean {
        return getRoles().any { it.equals(Constants.ROLE_DOCENTE, ignoreCase = true) }
    }

    fun getIdPersona(): Long = prefs.getLong(Constants.KEY_ID_PERSONA, getUserId())

    fun setIdPersona(id: Long) {
        prefs.edit().putLong(Constants.KEY_ID_PERSONA, id).apply()
    }

    fun getGatewayUrl(): String = prefs.getString(Constants.KEY_GATEWAY_URL, Constants.DEFAULT_BASE_GATEWAY_URL)
        ?: Constants.DEFAULT_BASE_GATEWAY_URL

    fun setGatewayUrl(url: String) {
        prefs.edit().putString(Constants.KEY_GATEWAY_URL, url).apply()
    }

    fun getDocenteUrl(): String = prefs.getString(Constants.KEY_DOCENTE_URL, Constants.DEFAULT_BASE_DOCENTE_URL)
        ?: Constants.DEFAULT_BASE_DOCENTE_URL

    fun setDocenteUrl(url: String) {
        prefs.edit().putString(Constants.KEY_DOCENTE_URL, url).apply()
    }

    fun clearSession() {
        prefs.edit()
            .remove(Constants.KEY_JWT_TOKEN)
            .remove(Constants.KEY_USER_ID)
            .remove(Constants.KEY_USERNAME)
            .remove(Constants.KEY_CORREO)
            .remove(Constants.KEY_ROLES)
            .remove(Constants.KEY_ID_PERSONA)
            .apply()
        _isLoggedIn.value = false
    }
}
