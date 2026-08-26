// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

package com.example.dmsapplication.ui.auth.registerView

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dmsapplication.data.repository.AuthRepository
class RegisterViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _registerSuccess = MutableLiveData<Boolean>()
    val registerSuccess: LiveData<Boolean> = _registerSuccess
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    fun register(email: String, pass: String, name: String, dob: String) {
        android.util.Log.d("DMS_DEBUG", "ViewModel.register() được gọi")
        repository.registerUser(email, pass, name, dob) { success, error ->
            android.util.Log.d("DMS_DEBUG", "Callback nhận được: success=$success, error=$error")
            if (success) {
                android.util.Log.d("DMS_DEBUG", "postValue(true) đang được gọi")
                _registerSuccess.postValue(true)
            } else {
                android.util.Log.e("DMS_DEBUG", "postValue error: $error")
                _errorMessage.postValue(error ?: "Đăng ký thất bại, vui lòng thử lại.")
            }
        }
    }
}
