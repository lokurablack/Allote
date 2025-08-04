package com.example.allote.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentoMovimientoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(documento: DocumentoMovimiento)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(documentos: List<DocumentoMovimiento>)

    @Query("SELECT * FROM documentos_movimiento WHERE movimientoId = :movimientoId")
    fun getDocumentosByMovimientoIdStream(movimientoId: Int): Flow<List<DocumentoMovimiento>>

    @Query("DELETE FROM documentos_movimiento WHERE id = :documentoId")
    suspend fun deleteById(documentoId: Int)

    @Query("DELETE FROM documentos_movimiento WHERE movimientoId = :movimientoId")
    suspend fun deleteByMovimientoId(movimientoId: Int)

    @Query("SELECT * FROM documentos_movimiento WHERE movimientoId = :movimientoId")
    suspend fun getDocumentosByMovimientoId(movimientoId: Int): List<DocumentoMovimiento>
}