package com.example.dmsapplication.ui.register.ui.auth.registerView

import androidx.lifecycle.ViewModelProvider
import com.example.dmsapplication.data.repository.AuthRepository

class RegisterViewModelFactory(private val repository: AuthRepository) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return RegisterViewModel(repository) as T
    }
}