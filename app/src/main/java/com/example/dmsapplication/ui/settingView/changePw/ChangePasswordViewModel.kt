package com.example.dmsapplication.ui.settingView.password

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val _changeSuccess = MutableLiveData<Boolean>()
    val changeSuccess: LiveData<Boolean> = _changeSuccess
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    fun changePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return
        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential).addOnCompleteListener { reAuthTask ->
            if (reAuthTask.isSuccessful) {
                user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        _changeSuccess.postValue(true)
                    } else {
                        _errorMessage.postValue(updateTask.exception?.message)
                    }
                }
            } else {
                _errorMessage.postValue("Mật khẩu hiện tại không đúng")
            }
        }
    }
}