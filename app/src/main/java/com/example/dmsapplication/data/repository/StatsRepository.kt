package com.example.dmsapplication.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.dmsapplication.data.local.AppDatabase
import com.example.dmsapplication.data.model.DriverStats
import com.example.dmsapplication.data.remote.LocalServerService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

class StatsRepository(private val context: Context) {
    private val dao = AppDatabase.getInstance(context).statsDao()

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun saveImageLocally(bitmap: Bitmap, fileName: String): String {
        val dir = File(context.filesDir, "alerts")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "$fileName.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }
        return file.absolutePath
    }

    suspend fun saveStats(stats: DriverStats): Long = dao.insert(stats)

    fun getRecentStatsByUser(userId: String): Flow<List<DriverStats>> {
        val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        return dao.getRecentByUser(userId, sevenDaysAgo)
    }

    suspend fun syncToServer(stats: DriverStats): Boolean {
        if (stats.localImagePath.isEmpty()) return false
        val file = File(stats.localImagePath)
        if (!file.exists()) return false
        return try {
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val response = LocalServerService.api.uploadImage(body)
            if (!response.isSuccessful) return false
            val remotePath = response.body()?.path ?: return false
            val syncedStats = stats.copy(
                cloudImageUrl = LocalServerService.absoluteUrl(remotePath),
                isSynced = true,
                localImagePath = ""
            )
            if (!saveToFirestore(syncedStats)) return false
            dao.update(syncedStats)
            file.delete()
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun saveToFirestore(stats: DriverStats): Boolean {
        return try {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) return false
            FirebaseFirestore.getInstance()
                .collection("driver_stats")
                .document(stats.firestoreDocId())
                .set(stats)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteOldServerImages(userId: String) {
        val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        val localOldStats = dao.getOldSyncedByUser(userId, sevenDaysAgo)
        val statsByDocId = linkedMapOf<String, DriverStats>()
        localOldStats.forEach { stats -> statsByDocId[stats.firestoreDocId()] = stats }
        if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
            localOldStats.forEach { stats -> dao.delete(stats) }
            return
        }
        val db = FirebaseFirestore.getInstance()
        try {
            val remoteSnapshot = db.collection("driver_stats")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            remoteSnapshot.toObjects(DriverStats::class.java)
                .filter { it.timestamp < sevenDaysAgo }
                .forEach { stats -> statsByDocId[stats.firestoreDocId()] = stats }
        } catch (_: Exception) {
        }
        statsByDocId.forEach { (docId, stats) ->
            deleteServerImage(stats.cloudImageUrl)
            try {
                db.collection("driver_stats").document(docId).delete().await()
            } catch (_: Exception) {
            }
            localOldStats.firstOrNull { it.firestoreDocId() == docId }?.let { dao.delete(it) }
        }
    }

    suspend fun getUnsynced(): List<DriverStats> = dao.getUnsynced()

    private suspend fun deleteServerImage(imageUrl: String) {
        val imageId = imageUrl.substringBefore('?').substringAfterLast('/')
        if (runCatching { UUID.fromString(imageId) }.isFailure) return
        try {
            LocalServerService.api.deleteImage(imageId)
        } catch (_: Exception) {
        }
    }

    private fun DriverStats.firestoreDocId(): String = "${userId}_${timestamp}"

    suspend fun fetchFromCloud(userId: String) {
        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) return
            val snapshot = FirebaseFirestore.getInstance()
                .collection("driver_stats")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            snapshot.toObjects(DriverStats::class.java).forEach { dao.insertIgnoreConflict(it) }
        } catch (_: Exception) {
        }
    }

    suspend fun refreshStatsFromCloud(userId: String) = fetchFromCloud(userId)
}
