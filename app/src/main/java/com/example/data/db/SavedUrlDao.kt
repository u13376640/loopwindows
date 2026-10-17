package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedUrlDao {
    @Query("SELECT * FROM saved_urls ORDER BY timestamp DESC")
    fun getAllSavedUrls(): Flow<List<SavedUrlEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUrl(url: SavedUrlEntity): Long

    @Delete
    suspend fun deleteUrl(url: SavedUrlEntity)

    @Query("DELETE FROM saved_urls WHERE isPreset = 0")
    suspend fun clearCustomHistory()
}
