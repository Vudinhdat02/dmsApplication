package com.example.dmsapplication.data.repository

import android.content.Context
import com.example.dmsapplication.data.local.AppDatabase
import com.example.dmsapplication.data.model.UserAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AuthRepository(context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance("https://dmsdatabase-aceb4-default-rtdb.asia-southeast1.firebasedatabase.app/")
    private val userDao = AppDatabase.getInstance(context).userDao()
    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    fun getCurrentUserId(): String? = auth.currentUser?.uid
    fun getUserFlow(uid: String): Flow<UserAccount?> = userDao.getUserByUidFlow(uid)
    fun syncUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val user = UserAccount(
                        uid   = userId,
                        name  = snapshot.getString("name") ?: "",
                        email = snapshot.getString("email") ?: "",
                        dob   = snapshot.getString("dob") ?: ""
                    )
                    saveToLocal(user)
                } else {
                    migrateFromRealtimeDb(userId)
                }
            }
    }
    private fun migrateFromRealtimeDb(userId: String) {
        realtimeDb.getReference("Users").child(userId).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val name = snapshot.child("name").value?.toString() ?: ""
                    val dob  = snapshot.child("dob").value?.toString() ?: ""
                    val email = snapshot.child("email").value?.toString() ?: ""
                    val user = UserAccount(userId, name, email, dob)
                    firestore.collection("users").document(userId).set(user, SetOptions.merge())
                    saveToLocal(user)
                }
            }
    }
    private fun saveToLocal(user: UserAccount) {
        repositoryScope.launch {
            userDao.insertOrUpdate(user)
        }
    }
    fun updateProfile(name: String, dob: String, callback: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val updates = mapOf("name" to name, "dob" to dob, "updatedAt" to System.currentTimeMillis())
        firestore.collection("users").document(userId).update(updates)
            .addOnSuccessListener {
                repositoryScope.launch {
                    val currentUser = userDao.getUserByUid(userId)
                    if (currentUser != null) {
                        userDao.insertOrUpdate(currentUser.copy(name = name, dob = dob))
                    }
                }
                callback(true, null)
            }
            .addOnFailureListener { callback(false, it.message) }
    }
    fun signInWithEmail(email: String, pass: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) syncUserProfile()
            callback(task.isSuccessful, task.exception?.message)
        }
    }
    fun registerUser(email: String, pass: String, name: String, dob: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                val data = UserAccount(userId, name, email, dob)
                firestore.collection("users").document(userId).set(data)
                    .addOnSuccessListener {
                        saveToLocal(data)
                        callback(true, null)
                    }
            } else callback(false, task.exception?.message)
        }
    }
    fun resetPassword(email: String, callback: (Boolean, String?) -> Unit) {
        auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            callback(task.isSuccessful, task.exception?.message)
        }
    }
}
