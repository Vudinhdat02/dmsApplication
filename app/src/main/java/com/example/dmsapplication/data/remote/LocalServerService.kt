package com.example.dmsapplication.data.remote

import com.example.dmsapplication.BuildConfig
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import java.io.IOException
import java.util.concurrent.TimeUnit

data class DrivingAnalysisRequest(
    val dailyScore: Int,
    val todayErrors: Int,
    val morning: Int,
    val afternoon: Int,
    val evening: Int,
    val night: Int,
    val highSpeed: Int
)

data class TextResponse(val text: String)

data class CrashAlertRequest(
    val latitude: Double,
    val longitude: Double
)

data class OperationResponse(val success: Boolean)

data class ImageUploadResponse(
    val id: String,
    val path: String,
    val createdAtUtc: String,
    val expiresAtUtc: String
)

data class ImageListItem(
    val id: String,
    val path: String,
    val contentType: String,
    val sizeBytes: Long,
    val createdAtUtc: String,
    val expiresAtUtc: String
)

interface LocalServerApi {
    @POST("api/analyze-driving")
    suspend fun analyzeDriving(@Body request: DrivingAnalysisRequest): TextResponse

    @POST("api/send-crash-alert")
    suspend fun sendCrashAlert(@Body request: CrashAlertRequest): Response<OperationResponse>

    @Multipart
    @POST("api/images/upload")
    suspend fun uploadImage(@Part file: MultipartBody.Part): Response<ImageUploadResponse>

    @GET("api/images")
    suspend fun listImages(): Response<List<ImageListItem>>

    @DELETE("api/images/{id}")
    suspend fun deleteImage(@Path("id") imageId: String): Response<OperationResponse>
}

object LocalServerService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val user = FirebaseAuth.getInstance().currentUser
                ?: throw IOException("Người dùng chưa đăng nhập")
            val token = try {
                Tasks.await(user.getIdToken(false)).token
            } catch (exception: Exception) {
                throw IOException("Không thể lấy Firebase ID token", exception)
            } ?: throw IOException("Firebase ID token rỗng")

            val request = chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(request)
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.SERVER_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: LocalServerApi = retrofit.create(LocalServerApi::class.java)

    fun absoluteUrl(path: String): String {
        if (path.startsWith("https://") || path.startsWith("http://")) return path
        return BuildConfig.SERVER_BASE_URL.trimEnd('/') + "/" + path.trimStart('/')
    }
}
