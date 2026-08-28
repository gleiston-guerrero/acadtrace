package ec.edu.uteq.sga.docente.core

sealed class Resource<out T> {
    data class Success<out T>(val data: T, val isOffline: Boolean = false) : Resource<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}
