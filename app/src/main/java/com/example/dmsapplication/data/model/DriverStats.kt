package com.example.dmsapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "driver_stats")
data class DriverStats(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val timestamp: Long,
    val drowsyCount: Int,
    val headDistractedCount: Int = 0,  // Thêm đếm quay đầu
    val speed: Float = 0f,             // Thêm tốc độ lúc xảy ra
    val localImagePath: String = "",   // Đường dẫn ảnh local
    val cloudImageUrl: String = "",    // URL Cloudinary sau khi sync
    val isSynced: Boolean = false
)