package com.example.allote.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JobParametrosDao {
    @Query("SELECT * FROM job_parametros WHERE jobId = :jobId LIMIT 1")
    fun getByJobIdStream(jobId: Int): Flow<JobParametros?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(parametros: JobParametros)

    @Update
    suspend fun update(parametros: JobParametros)
}
