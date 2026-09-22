package ec.edu.uteq.sga.representante

import ec.edu.uteq.sga.representante.core.Resource
import ec.edu.uteq.sga.representante.domain.model.UserSession
import ec.edu.uteq.sga.representante.domain.repository.AuthRepository
import ec.edu.uteq.sga.representante.ui.screens.login.LoginViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class LoginViewModelCoverageTest {
    @get:Rule val main = CoverageMainDispatcherRule()

    @Test fun camposVaciosNoAutenticanYSaltosDeLineaSeLimpianAntesDeEnviar() = runTest {
        val repository = FakeAuth()
        val vm = LoginViewModel(repository)
        vm.login()
        assertNotNull(vm.uiState.value.errorMessage)
        assertTrue(repository.requests.isEmpty())
        vm.onUsernameChange(" ana\n\r ")
        vm.login()
        assertTrue(repository.requests.isEmpty())
        vm.onPasswordChange(" clave\r\n ")
        assertNull(vm.uiState.value.errorMessage)
        repository.result = Resource.Success(UserSession("token-prueba", 1, "ana", null, listOf("REPRESENTANTE")))
        vm.login()
        assertEquals("ana" to "clave", repository.requests.single())
        assertTrue(vm.uiState.value.isSuccess)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun errorDeAutenticacionPermiteCorregirUsuarioYReintentar() = runTest {
        val repository = FakeAuth()
        val vm = LoginViewModel(repository)
        vm.onUsernameChange("ana")
        vm.onPasswordChange("clave")
        repository.result = Resource.Error("credenciales inválidas")
        vm.login()
        assertFalse(vm.uiState.value.isSuccess)
        assertFalse(vm.uiState.value.isLoading)
        assertEquals("credenciales inválidas", vm.uiState.value.errorMessage)
        vm.onUsernameChange("ana2")
        assertNull(vm.uiState.value.errorMessage)
        repository.result = Resource.Loading
        vm.login()
        assertTrue(vm.uiState.value.isLoading)
        assertEquals("ana2" to "clave", repository.requests.last())
    }

    private class FakeAuth : AuthRepository {
        var result: Resource<UserSession> = Resource.Loading
        val requests = mutableListOf<Pair<String, String>>()
        override suspend fun login(username: String, password: String) = result.also { requests += username to password }
        override fun logout(): Unit = error("No esperado")
        override fun isUserLoggedIn(): Boolean = error("No esperado")
        override fun isRepresentante(): Boolean = error("No esperado")
        override fun getUserId(): Long = error("No esperado")
    }
}
