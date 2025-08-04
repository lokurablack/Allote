package com.example.allote.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientContabilidadRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val movimientoContableDao: MovimientoContableDao,
    private val clientDao: ClientDao,
    private val documentoMovimientoDao: DocumentoMovimientoDao,
    private val settingsRepository: SettingsRepository
) {

    fun getMovimientosForClientStream(clientId: Int): Flow<List<MovimientoContable>> =
        movimientoContableDao.getMovimientosByClientIdStream(clientId)

    fun getClientStream(clientId: Int): Flow<Client?> = clientDao.getByIdStream(clientId)

    fun getDocumentCountsStream(clientId: Int): Flow<List<DocumentCount>> =
        movimientoContableDao.getDocumentCountsForClientStream(clientId)

    suspend fun saveMovimientoWithDocuments(
        movimiento: MovimientoContable,
        documentos: List<DocumentoMovimiento>
    ) {
        appDatabase.withTransaction {
            val settings = settingsRepository.getCurrencySettingsFlow().first()

            val debeEnUsd = if (settings.displayCurrency == "ARS" && settings.exchangeRate > 0) {
                movimiento.debe / settings.exchangeRate
            } else {
                movimiento.debe
            }

            val haberEnUsd = if (settings.displayCurrency == "ARS" && settings.exchangeRate > 0) {
                movimiento.haber / settings.exchangeRate
            } else {
                movimiento.haber
            }

            val movimientoConMonedaCorrecta = movimiento.copy(debe = debeEnUsd, haber = haberEnUsd)

            val movimientoFinalAGuardar = if (movimiento.id == 0) {
                movimientoConMonedaCorrecta.copy(estadoAprobacion = AprobacionStatus.PENDIENTE)
            } else {
                movimientoConMonedaCorrecta
            }

            val movimientoId = movimientoContableDao.upsert(movimientoFinalAGuardar)

            // Borramos los documentos viejos para reemplazarlos con la nueva lista
            documentoMovimientoDao.deleteByMovimientoId(movimientoId)

            val documentosAGuardar = documentos.map { doc ->
                doc.copy(movimientoId = movimientoId)
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
}