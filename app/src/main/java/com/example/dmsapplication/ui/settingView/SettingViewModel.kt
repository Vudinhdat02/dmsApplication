package com.example.dmsapplication.ui.settingView

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class SettingViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> get() = _userName
    private val _isVectorEnabled = MutableLiveData<Boolean>(false)
    val isVectorEnabled: LiveData<Boolean> get() = _isVectorEnabled

    init {
        loadUserData()
    }

    private fun loadUserData() {
        val user = auth.currentUser
        _userName.value = user?.displayName ?: "Đạt"
    }

    fun toggleVectorTracking(enabled: Boolean) {
        _isVectorEnabled.value = enabled
    }

    fun logout() {
        auth.signOut()
    }
}