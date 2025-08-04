package com.example.allote.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs ORDER BY startDate DESC")
    fun getAllStream(): Flow<List<Job>>

    @Insert
    suspend fun insert(job: Job)

    @Update
    suspend fun update(job: Job)

    @Delete
    suspend fun delete(job: Job)

    @Query("SELECT * FROM jobs WHERE status = :status")
    suspend fun getJobsByStatus(status: String): List<Job>

    @Query("SELECT * FROM jobs WHERE clientId = :clientId")
    suspend fun getJobsByClientId(clientId: Int): List<Job>

    @Query("SELECT * FROM jobs WHERE id = :jobId LIMIT 1")
    suspend fun getById(jobId: Int): Job?

    @Query("DELETE FROM jobs")
    suspend fun deleteAllJobs()

    @Query("DELETE FROM jobs WHERE clientId = :clientId")
    suspend fun deleteJobsByClientId(clientId: Int)

    @Query("SELECT * FROM jobs WHERE id = :jobId")
    fun getByIdStream(jobId: Int): Flow<Job?>

    @Query("SELECT * FROM jobs WHERE clientId = :clientId ORDER BY startDate DESC")
    fun getJobsByClientIdStream(clientId: Int): Flow<List<Job>>
}