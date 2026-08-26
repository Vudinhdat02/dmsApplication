// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

package com.example.dmsapplication.data.repository

import com.example.dmsapplication.data.remote.CrashAlertRequest
import com.example.dmsapplication.data.remote.LocalServerService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class AlertRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getEmergencyContacts(): List<String> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            val emails = doc.get("emergencyEmails") as? List<*>
            emails?.mapNotNull { it as? String } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveEmergencyContacts(emails: List<String>) {
        val userId = auth.currentUser?.uid ?: return
        val data = hashMapOf("emergencyEmails" to emails)
        firestore.collection("users").document(userId)
            .set(data, SetOptions.merge()).await()
    }

    suspend fun sendCrashAlert(latitude: Double, longitude: Double): Boolean {
        if (getEmergencyContacts().isEmpty()) return false
        return try {
            val response = LocalServerService.api.sendCrashAlert(
                CrashAlertRequest(latitude = latitude, longitude = longitude)
            )
            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            false
        }
    }
}
