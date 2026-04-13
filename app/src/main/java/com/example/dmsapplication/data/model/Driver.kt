package com.example.dmsapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drivers")
data class Driver(
    @PrimaryKey(autoGenerate = true) val driverId: Int = 0,
    val uid: String, // Firebase User ID thay cho PhoneNumber
    val fullName: String,
    val phoneNumber: String,
    val email: String,
    val dateOfBirth: String,
    val isSynced: Boolean = false
)