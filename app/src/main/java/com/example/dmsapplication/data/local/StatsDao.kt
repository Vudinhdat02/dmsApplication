package com.example.dmsapplication.data.local

import androidx.room.*
import com.example.dmsapplication.data.model.DriverStats
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {
    @Insert
    suspend fun insert(stats: DriverStats): Long
    @Update
    suspend fun update(stats: DriverStats)
    @Delete
    suspend fun delete(stats: DriverStats)
    @Query("SELECT * FROM driver_stats WHERE userId = :userId AND timestamp >= :since ORDER BY timestamp DESC")
    fun getRecentByUser(userId: String, since: Long): Flow<List<DriverStats>>
    // Lấy các bản ghi cũ hơn 2 ngày đã sync (để xóa)
    @Query("SELECT * FROM driver_stats WHERE userId = :userId AND timestamp < :before AND isSynced = 1")
    suspend fun getOldSyncedByUser(userId: String, before: Long): List<DriverStats>
    // Lấy các bản ghi chưa sync
    @Query("SELECT * FROM driver_stats WHERE isSynced = 0")
    suspend fun getUnsynced(): List<DriverStats>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoreConflict(stats: DriverStats): Long
}