package com.example.dmsapplication.ui.auth.loginView

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dmsapplication.data.repository.AuthRepository
class LoginViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> = _loginSuccess
    private val _isResetSent = MutableLiveData<Boolean>()
    val isResetSent: LiveData<Boolean> = _isResetSent
    fun loginWithEmail(email: String, pass: String) {
        repository.signInWithEmail(email, pass) { success, error ->
            if (success) _loginSuccess.value = true
            else _errorMessage.value = error
        }
    }
    fun forgotPassword(email: String) {
        repository.resetPassword(email) { success, error ->
            if (success) _isResetSent.value = true
            else _errorMessage.value = error
        }
    }
}