package il.co.or.abicook.presentation.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import il.co.or.abicook.domain.repository.AuthRepository
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(LoginUiState())
    val uiState: LiveData<LoginUiState> = _uiState

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState(
                isLoading = false,
                error = "Email and password are required"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState(
                isLoading = true,
                error = null,
                success = false
            )

            try {
                authRepository
                    .login(email, password)
                    .getOrThrow()

                _uiState.value = LoginUiState(
                    isLoading = false,
                    error = null,
                    success = true
                )
            } catch (e: Exception) {
                _uiState.value = LoginUiState(
                    isLoading = false,
                    error = e.message ?: "Login failed"
                )
            }
        }
    }
}
