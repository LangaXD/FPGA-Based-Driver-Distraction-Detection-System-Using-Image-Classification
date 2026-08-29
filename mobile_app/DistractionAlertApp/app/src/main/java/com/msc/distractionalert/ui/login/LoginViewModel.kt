package com.msc.distractionalert.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.msc.distractionalert.data.repository.AuthRepository
import com.msc.distractionalert.data.repository.LoginResult
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Failed(val message: String) : LoginUiState()
}

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableLiveData<LoginUiState>(LoginUiState.Idle)
    val uiState: LiveData<LoginUiState> = _uiState

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Failed("Enter both username and password")
            return
        }

        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val result = authRepository.login(username, password)) {
                is LoginResult.Success -> LoginUiState.Success
                is LoginResult.InvalidCredentials -> LoginUiState.Failed("Incorrect username or password")
                is LoginResult.Error -> LoginUiState.Failed(result.message)
            }
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LoginViewModel(authRepository) as T
    }
}
