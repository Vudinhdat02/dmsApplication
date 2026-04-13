package com.example.dmsapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_account")
data class UserAccount(
    @PrimaryKey val uid: String,
    val name: String,
    val email: String,
    val dob: String,
    val lastUpdated: Long = System.currentTimeMillis()
)
