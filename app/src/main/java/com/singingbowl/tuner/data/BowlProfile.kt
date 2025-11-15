package com.singingbowl.tuner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bowl_profiles")
data class BowlProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val frequency: Float,
    val noteName: String,
    val cents: Int,
    val timestamp: Long = System.currentTimeMillis()
)
