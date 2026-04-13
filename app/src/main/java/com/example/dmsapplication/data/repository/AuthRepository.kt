package com.example.dmsapplication.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val dbUrl = "https://dmsdatabase-aceb4-default-rtdb.asia-southeast1.firebasedatabase.app/"

    fun signInWithEmail(email: String, pass: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                callback(task.isSuccessful, task.exception?.message)
            }
    }

    fun resetPassword(email: String, callback: (Boolean, String?) -> Unit) {
        auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                callback(true, null)
            } else {
                callback(false, task.exception?.message ?: "Lỗi không xác định")
            }
        }
    }

    fun registerUser(email: String, pass: String, name: String, dob: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                val data = mapOf("name" to name, "dob" to dob, "email" to email)

                FirebaseDatabase.getInstance(dbUrl).getReference("Users").child(userId).setValue(data)
                    .addOnSuccessListener { callback(true, null) }
                    .addOnFailureListener { callback(false, it.message) }
            } else {
                callback(false, task.exception?.message)
            }
        }
    }
}
