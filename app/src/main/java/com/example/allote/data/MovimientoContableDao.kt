package com.example.allote.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MovimientoContableDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(movimiento: MovimientoContable): Long // Devuelve el nuevo ID

    @Update
    suspend fun update(movimiento: MovimientoContable)

    // Función de ayuda para la transacción
    suspend fun upsert(movimiento: MovimientoContable): Int {
        val id = insert(movimiento)
        if (id == -1L) { // -1 significa que hubo un conflicto y no se insertó
            update(movimiento)
            return movimiento.id
        }
        return id.toInt()
    }

    @Delete
    suspend fun delete(movimiento: MovimientoContable)

    @Query("SELECT * FROM movimientos_contables WHERE clientId = :clientId ORDER BY fecha ASC, id ASC")
    fun getMovimientosByClientIdStream(clientId: Int): Flow<List<MovimientoContable>>

    @Query("SELECT * FROM movimientos_contables WHERE jobId = :jobId LIMIT 1")
    suspend fun getMovimientoByJobId(jobId: Int): MovimientoContable?

    @Query("SELECT * FROM movimientos_contables")
    fun getAllMovimientosStream(): Flow<List<MovimientoContable>>

    @Query("""
        SELECT m.id, COUNT(d.id) as doc_count
        FROM movimientos_contables m
        LEFT JOIN documentos_movimiento d ON m.id = d.movimientoId
        WHERE m.clientId = :clientId
        GROUP BY m.id
    """)
    fun getDocumentCountsForClientStream(clientId: Int): Flow<List<DocumentCount>>

    @Query("""
        SELECT m.id, COUNT(d.id) as doc_count
        FROM movimientos_contables m
        LEFT JOIN documentos_movimiento d ON m.id = d.movimientoId
        WHERE m.esAprobadoGeneral = 1
        GROUP BY m.id
    """)
    fun getAprobadoDocumentCountsStream(): Flow<List<DocumentCount>>

    @Query("SELECT * FROM movimientos_contables WHERE id = :movimientoId")
    fun getMovimientoByIdStream(movimientoId: Int): Flow<MovimientoContable?>

    @Query("SELECT * FROM movimientos_contables WHERE estadoAprobacion = 'APROBADO' ORDER BY fecha ASC, id ASC")
    fun getMovimientosGeneralesAprobadosStream(): Flow<List<MovimientoContable>>

    @Query("SELECT * FROM movimientos_contables WHERE estadoAprobacion = 'PENDIENTE' ORDER BY fecha ASC, id ASC")
    fun getMovimientosGeneralesPendientesStream(): Flow<List<MovimientoContable>>

    @Query("SELECT COUNT(id) FROM movimientos_contables WHERE estadoAprobacion = 'PENDIENTE'")
    fun getMovimientosPendientesCountStream(): Flow<Int>
}

data class DocumentCount(
    val id: Int,
    val doc_count: Int
)