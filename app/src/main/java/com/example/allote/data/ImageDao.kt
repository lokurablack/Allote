package com.example.allote.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {
    @Query("SELECT * FROM images WHERE jobId = :jobId")
    suspend fun getImagesByJobId(jobId: Int): List<ImageEntity>

    @Insert
    suspend fun insert(image: ImageEntity)

    @Delete
    suspend fun delete(image: ImageEntity)

    @Query("DELETE FROM images WHERE jobId = :jobId")
    suspend fun deleteImagesByJobId(jobId: Int)

    @Query("SELECT * FROM images WHERE jobId = :jobId")
    fun getImagesByJobIdStream(jobId: Int): Flow<List<ImageEntity>>
}
