package com.singingbowl.tuner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BowlProfileDao {
    @Insert
    suspend fun insert(profile: BowlProfile)

    @Query("SELECT * FROM bowl_profiles ORDER BY timestamp DESC")
    fun getAllProfiles(): Flow<List<BowlProfile>>
}
