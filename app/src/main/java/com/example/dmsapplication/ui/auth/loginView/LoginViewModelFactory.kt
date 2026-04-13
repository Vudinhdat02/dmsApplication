package com.example.dmsapplication.ui.auth.loginView

import androidx.lifecycle.ViewModelProvider
import com.example.dmsapplication.data.repository.AuthRepository

class LoginViewModelFactory(private val repository: AuthRepository) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return LoginViewModel(repository) as T
    }
}