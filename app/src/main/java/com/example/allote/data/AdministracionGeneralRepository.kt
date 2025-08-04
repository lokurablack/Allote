package com.example.allote.data

import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdministracionGeneralRepository @Inject constructor(
    private val jobDao: JobDao,
    private val administracionDao: AdministracionDao,
    private val movimientoContableDao: MovimientoContableDao,
    private val clientDao: ClientDao,
    private val documentoMovimientoDao: DocumentoMovimientoDao,
    private val appDatabase: AppDatabase
) {
    fun getMovimientosAprobadosStream(): Flow<List<MovimientoContable>> =
        movimientoContableDao.getMovimientosGeneralesAprobadosStream()

    fun getAllClientsStream(): Flow<List<Client>> = clientDao.getAllStream()

    fun getMovimientosPendientesStream(): Flow<List<MovimientoContable>> =
        movimientoContableDao.getMovimientosGeneralesPendientesStream()

    fun getMovimientosPendientesCountStream(): Flow<Int> =
        movimientoContableDao.getMovimientosPendientesCountStream()

    suspend fun updateMovimiento(movimiento: MovimientoContable) {
        movimientoContableDao.update(movimiento.copy(esAprobadoGeneral = true))
    }

    suspend fun insertMovimiento(movimiento: MovimientoContable) {
        movimientoContableDao.insert(movimiento.copy(esAprobadoGeneral = true))
    }

    fun getAprobadoDocumentCountsStream(): Flow<List<DocumentCount>> =
        movimientoContableDao.getAprobadoDocumentCountsStream()

    suspend fun insertMovimientoConDocumentos(
        movimiento: MovimientoContable,
        documentosUris: List<Uri>,
        context: android.content.Context // Necesitamos el contexto para obtener los nombres de archivo
    ) {
        appDatabase.withTransaction {
            val movimientoId = movimientoContableDao.insert(movimiento.copy(esAprobadoGeneral = true)).toInt()

            val documentosAGuardar = documentosUris.map { uri ->
                // Aquí deberías tener una lógica para obtener el nombre y tipo del archivo desde la URI
                // Esta es una implementación básica:
                DocumentoMovimiento(
                    movimientoId = movimientoId,
                    uri = uri.toString(),
                    mimeType = context.contentResolver.getType(uri),
                    fileName = "Documento" // Idealmente, obtener el nombre real
                )
            }
            documentoMovimientoDao.insertAll(documentosAGuardar)
        }
    }

    suspend fun updateMovimientoConDocumentos(
        movimiento: MovimientoContable,
        documentosUris: List<Uri>,
        context: android.content.Context
    ) {
        appDatabase.withTransaction {
            movimientoContableDao.update(movimiento.copy(esAprobadoGeneral = true))

            // Lógica simple: borra los documentos antiguos y añade los nuevos.
            // Una implementación más avanzada podría comparar y solo añadir/borrar los que cambiaron.
            documentoMovimientoDao.deleteByMovimientoId(movimiento.id)
            val documentosAGuardar = documentosUris.map { uri ->
                DocumentoMovimiento(
                    movimientoId = movimiento.id,
                    uri = uri.toString(),
                    mimeType = context.contentResolver.getType(uri),
                    fileName = "Documento"
                )
            }
            documentoMovimientoDao.insertAll(documentosAGuardar)
        }
    }

    suspend fun deleteMovimiento(movimiento: MovimientoContable) {
        // Al borrar un movimiento, también borramos sus documentos asociados
        appDatabase.withTransaction {
            documentoMovimientoDao.deleteByMovimientoId(movimiento.id)
            movimientoContableDao.delete(movimiento)
        }
    }

    suspend fun getDocumentosParaMovimiento(movimientoId: Int): List<DocumentoMovimiento> {
        return documentoMovimientoDao.getDocumentosByMovimientoId(movimientoId)
    }
}