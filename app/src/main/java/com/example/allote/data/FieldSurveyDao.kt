package com.example.allote.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FieldSurveyDao {

    @Transaction
    @Query("SELECT * FROM field_surveys WHERE id = :surveyId")
    fun observeSurveyWithAnnotations(surveyId: Int): Flow<FieldSurveyWithAnnotations?>

    @Transaction
    @Query("SELECT * FROM field_surveys WHERE id = :surveyId")
    suspend fun getSurveyWithAnnotations(surveyId: Int): FieldSurveyWithAnnotations?

    @Query("SELECT * FROM field_surveys WHERE id = :surveyId")
    suspend fun getSurveyById(surveyId: Int): FieldSurvey?

    @Transaction
    @Query("SELECT * FROM field_surveys WHERE jobId = :jobId AND loteId = :loteId ORDER BY updatedAt DESC LIMIT 1")
    fun observeLatestSurveyForLote(jobId: Int, loteId: Int): Flow<FieldSurveyWithAnnotations?>

    @Transaction
    @Query("SELECT * FROM field_surveys WHERE jobId = :jobId AND loteId IS NULL ORDER BY updatedAt DESC LIMIT 1")
    fun observeLatestSurveyForJob(jobId: Int): Flow<FieldSurveyWithAnnotations?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(survey: FieldSurvey): Long

    @Update
    suspend fun update(survey: FieldSurvey)

    @Delete
    suspend fun delete(survey: FieldSurvey)
}
