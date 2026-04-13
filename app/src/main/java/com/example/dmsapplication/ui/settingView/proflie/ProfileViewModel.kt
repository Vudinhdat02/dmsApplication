package com.example.dmsapplication.ui.settingView.profile

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val dbUrl = "https://dmsdatabase-aceb4-default-rtdb.asia-southeast1.firebasedatabase.app/"
    private val db = FirebaseDatabase.getInstance(dbUrl).getReference("Users")

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _userEmail = MutableLiveData<String>()
    val userEmail: LiveData<String> = _userEmail

    private val _userDob = MutableLiveData<String>()
    val userDob: LiveData<String> = _userDob

    private val _updateSuccess = MutableLiveData<Boolean>()
    val updateSuccess: LiveData<Boolean> = _updateSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        loadUserData()
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        db.child(userId).get().addOnSuccessListener { snapshot ->
            _userName.postValue(snapshot.child("name").value?.toString() ?: "")
            _userEmail.postValue(snapshot.child("email").value?.toString() ?: "")
            _userDob.postValue(snapshot.child("dob").value?.toString() ?: "")
        }
    }

    fun updateProfile(name: String, dob: String, imageUri: Uri?) {
        val userId = auth.currentUser?.uid ?: return
        val updates = mapOf("name" to name, "dob" to dob)
        db.child(userId).updateChildren(updates)
            .addOnSuccessListener { _updateSuccess.postValue(true) }
            .addOnFailureListener { _errorMessage.postValue(it.message) }
    }
}