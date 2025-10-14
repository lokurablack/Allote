package com.example.allote.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkPlanDao {
    @Query("SELECT * FROM work_plans WHERE jobId = :jobId ORDER BY fechaCreacion DESC")
    fun getWorkPlansForJob(jobId: Int): Flow<List<WorkPlan>>

    @Query("SELECT * FROM work_plans WHERE jobId = :jobId AND loteId = :loteId ORDER BY fechaCreacion DESC LIMIT 1")
    fun getLatestWorkPlanForLote(jobId: Int, loteId: Int): Flow<WorkPlan?>

    @Query("SELECT * FROM work_plans WHERE jobId = :jobId AND loteId IS NULL ORDER BY fechaCreacion DESC LIMIT 1")
    fun getLatestWorkPlanForJob(jobId: Int): Flow<WorkPlan?>

    @Query("SELECT * FROM work_plans WHERE id = :planId")
    suspend fun getWorkPlanById(planId: Int): WorkPlan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workPlan: WorkPlan): Long

    @Update
    suspend fun update(workPlan: WorkPlan)

    @Delete
    suspend fun delete(workPlan: WorkPlan)

    @Query("DELETE FROM work_plans WHERE jobId = :jobId")
    suspend fun deleteAllForJob(jobId: Int)

    @Query("DELETE FROM work_plans WHERE jobId = :jobId AND loteId = :loteId")
    suspend fun deleteAllForLote(jobId: Int, loteId: Int)
}
