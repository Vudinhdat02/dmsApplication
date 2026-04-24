package com.example.dmsapplication.ui.dashboardView

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GroqApiService {
    @POST("v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") auth: String,
        @Body request: GroqRequest
    ): GroqResponse
}