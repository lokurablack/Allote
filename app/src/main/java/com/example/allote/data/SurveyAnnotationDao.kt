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
interface SurveyAnnotationDao {

    @Transaction
    @Query("SELECT * FROM survey_annotations WHERE surveyId = :surveyId ORDER BY sortOrder ASC, updatedAt DESC")
    fun observeAnnotationsForSurvey(surveyId: Int): Flow<List<AnnotationWithMedia>>

    @Query("SELECT * FROM survey_annotations WHERE id = :annotationId")
    suspend fun getAnnotationById(annotationId: Int): SurveyAnnotation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(annotation: SurveyAnnotation): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(annotations: List<SurveyAnnotation>)

    @Update
    suspend fun update(annotation: SurveyAnnotation)

    @Delete
    suspend fun delete(annotation: SurveyAnnotation)

    @Query("DELETE FROM survey_annotations WHERE surveyId = :surveyId")
    suspend fun deleteBySurveyId(surveyId: Int)
}
