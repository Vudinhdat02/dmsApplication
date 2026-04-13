package com.example.dmsapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dmsapplication.data.model.UserAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: UserAccount)

    @Query("SELECT * FROM user_account WHERE uid = :uid LIMIT 1")
    fun getUserByUidFlow(uid: String): Flow<UserAccount?>

    @Query("SELECT * FROM user_account WHERE uid = :uid LIMIT 1")
    suspend fun getUserByUid(uid: String): UserAccount?

    @Query("DELETE FROM user_account WHERE uid = :uid")
    suspend fun deleteUser(uid: String)
}
