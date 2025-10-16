package com.example.allote.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnotationMediaDao {

    @Query("SELECT * FROM annotation_media WHERE annotationId = :annotationId")
    fun observeMediaForAnnotation(annotationId: Int): Flow<List<AnnotationMedia>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(media: AnnotationMedia): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(media: List<AnnotationMedia>)

    @Update
    suspend fun update(media: AnnotationMedia)

    @Delete
    suspend fun delete(media: AnnotationMedia)

    @Query("SELECT * FROM annotation_media WHERE id = :mediaId")
    suspend fun getById(mediaId: Int): AnnotationMedia?

    @Query("DELETE FROM annotation_media WHERE annotationId = :annotationId")
    suspend fun deleteByAnnotationId(annotationId: Int)
}
