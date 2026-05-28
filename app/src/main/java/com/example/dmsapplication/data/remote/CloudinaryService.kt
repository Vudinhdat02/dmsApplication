package com.example.dmsapplication.data.remote

import com.example.dmsapplication.BuildConfig
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.security.MessageDigest

data class CloudinaryResponse(
    val secure_url: String,
    val public_id: String
)

data class CloudinaryDeleteResponse(
    val result: String
)

interface CloudinaryApi {
    @Multipart
    @POST("v1_1/{cloudName}/image/upload")
    suspend fun uploadImage(
        @Path("cloudName") cloudName: String,
        @Part file: MultipartBody.Part,
        @Part("upload_preset") uploadPreset: RequestBody,
        @Part("folder") folder: RequestBody
    ): Response<CloudinaryResponse>

    @FormUrlEncoded
    @POST("v1_1/{cloudName}/image/destroy")
    suspend fun deleteImage(
        @Path("cloudName") cloudName: String,
        @Field("public_id") publicId: String,
        @Field("api_key") apiKey: String,
        @Field("timestamp") timestamp: Long,
        @Field("signature") signature: String
    ): Response<CloudinaryDeleteResponse>
}

object CloudinaryService {
    val CLOUD_NAME: String         = BuildConfig.CLOUDINARY_CLOUD_NAME
    val UPLOAD_PRESET: String      = BuildConfig.CLOUDINARY_UPLOAD_PRESET
    val API_KEY: String            = BuildConfig.CLOUDINARY_API_KEY
    private val API_SECRET: String = BuildConfig.CLOUDINARY_API_SECRET

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.cloudinary.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: CloudinaryApi = retrofit.create(CloudinaryApi::class.java)

    fun generateSignature(publicId: String): Pair<Long, String> {
        val timestamp = System.currentTimeMillis() / 1000
        val toSign = "public_id=$publicId&timestamp=$timestamp$API_SECRET"
        return Pair(timestamp, sha1(toSign))
    }

    private fun sha1(input: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val bytes = md.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}