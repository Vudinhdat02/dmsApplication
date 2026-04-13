package com.example.dmsapplication.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.dmsapplication.data.local.AppDatabase
import com.example.dmsapplication.data.model.DriverStats
import com.example.dmsapplication.data.remote.CloudinaryService
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class StatsRepository(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).statsDao()

    // Kiểm tra có mạng không
    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Lưu ảnh bitmap vào local
    fun saveImageLocally(bitmap: Bitmap, fileName: String): String {
        val dir = File(context.filesDir, "alerts")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "$fileName.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }
        return file.absolutePath
    }

    // Lưu stats vào Room
    suspend fun saveStats(stats: DriverStats): Long {
        return dao.insert(stats)
    }

    // Lấy danh sách 2 ngày gần nhất theo user
    fun getRecentStatsByUser(userId: String): Flow<List<DriverStats>> {
        val twoDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)
        return dao.getRecentByUser(userId, twoDaysAgo)
    }

    // Upload lên Cloudinary và xóa local
    suspend fun syncToCloud(stats: DriverStats): Boolean {
        if (stats.localImagePath.isEmpty()) return false
        val file = File(stats.localImagePath)
        if (!file.exists()) return false

        return try {
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val preset = CloudinaryService.UPLOAD_PRESET
                .toRequestBody("text/plain".toMediaTypeOrNull())

            // Thêm folder theo userId để phân biệt tài khoản
            val folder = "dms/${stats.userId}"
                .toRequestBody("text/plain".toMediaTypeOrNull())

            val response = CloudinaryService.api.uploadImage(
                cloudName    = CloudinaryService.CLOUD_NAME,
                file         = body,
                uploadPreset = preset,
                folder       = folder
            )

            if (response.isSuccessful) {
                val url = response.body()?.secure_url ?: return false
                // Cập nhật DB: đánh dấu đã sync + lưu URL cloud
                dao.update(stats.copy(
                    cloudImageUrl = url,
                    isSynced      = true,
                    localImagePath = "" // Không cần local nữa
                ))
                // Xóa file local để tiết kiệm bộ nhớ
                file.delete()
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    // Xóa ảnh cũ hơn 2 ngày trên Cloudinary
    suspend fun deleteOldCloudImages(userId: String) {
        val twoDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)
        val oldStats = dao.getOldSyncedByUser(userId, twoDaysAgo)
        oldStats.forEach { stats ->
            if (stats.cloudImageUrl.isNotEmpty()) {
                // Xóa trên Cloudinary qua public_id
                val publicId = extractPublicId(stats.cloudImageUrl)
                if (publicId.isNotEmpty()) {
                    CloudinaryService.api.deleteImage(
                        cloudName  = CloudinaryService.CLOUD_NAME,
                        publicId   = publicId,
                        apiKey     = CloudinaryService.API_KEY,
                        signature  = CloudinaryService.generateSignature(publicId)
                    )
                }
            }
            dao.delete(stats)
        }
    }

    suspend fun getUnsynced(): List<DriverStats> = dao.getUnsynced()

    // Lấy public_id từ URL Cloudinary
    private fun extractPublicId(url: String): String {
        return try {
            // URL dạng: https://res.cloudinary.com/cloud/image/upload/v123/dms/userId/filename.jpg
            val parts = url.split("/upload/")
            if (parts.size < 2) return ""
            val afterUpload = parts[1] // v123/dms/userId/filename.jpg
            val withoutVersion = afterUpload.substringAfter("/") // dms/userId/filename.jpg
            withoutVersion.substringBeforeLast(".") // dms/userId/filename
        } catch (e: Exception) { "" }
    }
}
