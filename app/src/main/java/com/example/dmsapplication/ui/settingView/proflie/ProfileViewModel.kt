// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

package com.example.dmsapplication.ui.settingView.profile

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
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
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    _userName.postValue(snapshot.getString("name") ?: "")
                    _userEmail.postValue(snapshot.getString("email") ?: "")
                    _userDob.postValue(snapshot.getString("dob") ?: "")
                }
            }
            .addOnFailureListener { e ->
                _errorMessage.postValue(e.message)
            }
    }
    fun updateProfile(name: String, dob: String, imageUri: Uri?) {
        val userId = auth.currentUser?.uid ?: return
        val updates = mapOf(
            "name" to name,
            "dob"  to dob,
            "updatedAt" to System.currentTimeMillis()
        )
        firestore.collection("users").document(userId).update(updates)
            .addOnSuccessListener { _updateSuccess.postValue(true) }
            .addOnFailureListener { _errorMessage.postValue(it.message) }
    }
}