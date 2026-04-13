package com.example.dmsapplication.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

data class BrevoRequest(
    val sender: BrevoContact,
    val to: List<BrevoContact>,
    val subject: String,
    val htmlContent: String
)

data class BrevoContact(
    val email: String,
    val name: String? = null
)

interface BrevoService {
    @Headers("Content-Type: application/json", "accept: application/json")
    @POST("v3/smtp/email")
    suspend fun sendEmail(
        @Header("api-key") apiKey: String,
        @Body request: BrevoRequest
    ): Response<Unit>
}