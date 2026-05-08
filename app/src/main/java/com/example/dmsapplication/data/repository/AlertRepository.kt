package com.example.dmsapplication.data.repository

import com.example.dmsapplication.BuildConfig
import com.example.dmsapplication.data.remote.BrevoContact
import com.example.dmsapplication.data.remote.BrevoRequest
import com.example.dmsapplication.data.remote.BrevoService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AlertRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.brevo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val brevoApi = retrofit.create(BrevoService::class.java)

    suspend fun getEmergencyContacts(): List<String> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            val emails = doc.get("emergencyEmails") as? List<String>
            emails ?: emptyList()
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

    suspend fun sendCrashAlert(locationLink: String): Boolean {
        val contacts = getEmergencyContacts()
        if (contacts.isEmpty()) return false

        val sender = BrevoContact(email = "drivemonitorsystem@gmail.com", name = "DMS - Hệ thống giám sát trạng thái người lái")
        val recipients = contacts.map { BrevoContact(email = it) }

        val request = BrevoRequest(
            sender = sender,
            to = recipients,
            subject = "CẢNH BÁO KHẨN CẤP: PHÁT HIỆN VA CHẠM",
            htmlContent = "<h3>PHÁT HIỆN TAI NẠN!</h3><p>Hệ thống DMS ghi nhận xe vừa xảy ra va chạm mạnh.</p><p>Vị trí hiện tại: <a href='$locationLink'>Bấm vào đây để mở Google Maps</a></p>"
        )

        return try {
            val apiKey = BuildConfig.BREVO_API_KEY
            val response = brevoApi.sendEmail(apiKey, request)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}