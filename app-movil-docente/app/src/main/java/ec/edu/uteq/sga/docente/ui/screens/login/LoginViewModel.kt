package ec.edu.uteq.sga.docente.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.sga.docente.core.Resource
import ec.edu.uteq.sga.docente.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(value: String) {
        val clean = value.replace("\n", "").replace("\r", "")
        _uiState.value = _uiState.value.copy(username = clean, errorMessage = null)
    }

    fun onPasswordChange(value: String) {
        val clean = value.replace("\n", "").replace("\r", "")
        _uiState.value = _uiState.value.copy(password = clean, errorMessage = null)
    }

    fun login() {
        val cleanUsername = _uiState.value.username.trim()
        val cleanPassword = _uiState.value.password.trim()

        if (cleanUsername.isBlank() || cleanPassword.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Por favor ingresa usuario y contraseña")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.login(cleanUsername, cleanPassword)
            when (result) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }
}
