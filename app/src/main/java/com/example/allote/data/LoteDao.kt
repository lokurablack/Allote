package com.example.allote.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LoteDao {
    @Insert
    suspend fun insert(lote: Lote)

    @Query("SELECT * FROM lotes WHERE jobId = :jobId ORDER BY id DESC")
    fun getByJobIdStream(jobId: Int): Flow<List<Lote>>

    @Update
    suspend fun update(lote: Lote)

    @Delete
    suspend fun delete(lote: Lote)

    @Query("SELECT * FROM lotes WHERE jobId = :jobId")
    suspend fun getByJobId(jobId: Int): List<Lote>

    @Query("SELECT * FROM lotes WHERE id = :loteId")
    fun getByIdStream(loteId: Int): Flow<Lote?>

    @Query("SELECT * FROM lotes WHERE id = :loteId")
    suspend fun getById(loteId: Int): Lote?

    @Query("SELECT * FROM lotes WHERE id = :loteId")
    suspend fun getLoteByIdSync(loteId: Int): Lote?
}