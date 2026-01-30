package il.co.or.abicook.presentation.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import il.co.or.abicook.domain.repository.AuthRepository
import kotlinx.coroutines.launch

data class SignUpUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class SignUpViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(SignUpUiState())
    val uiState: LiveData<SignUpUiState> = _uiState

    fun signUp(email: String, password: String, confirmPassword: String, username: String) {
        if (email.isBlank() || password.isBlank() || username.isBlank()) {
            _uiState.value = SignUpUiState(
                isLoading = false,
                error = "All fields are required"
            )
            return
        }

        if (password != confirmPassword) {
            _uiState.value = SignUpUiState(
                isLoading = false,
                error = "Passwords do not match"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = SignUpUiState(
                isLoading = true,
                error = null,
                success = false
            )

            try {
                authRepository
                    .signUp(email, password, username)
                    .getOrThrow()

                _uiState.value = SignUpUiState(
                    isLoading = false,
                    error = null,
                    success = true
                )
            } catch (e: Exception) {
                _uiState.value = SignUpUiState(
                    isLoading = false,
                    error = e.message ?: "Sign up failed"
                )
            }
        }
    }
}
