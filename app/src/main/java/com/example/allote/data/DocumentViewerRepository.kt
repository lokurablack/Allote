package com.example.allote.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentViewerRepository @Inject constructor(
    private val movimientoContableDao: MovimientoContableDao,
    private val documentoMovimientoDao: DocumentoMovimientoDao
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getMovimientoStream(movimientoId: StateFlow<Int>): Flow<MovimientoContable?> {
        return movimientoId.flatMapLatest { id ->
            movimientoContableDao.getMovimientoByIdStream(id)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getDocumentosStream(movimientoId: StateFlow<Int>): Flow<List<DocumentoMovimiento>> {
        return movimientoId.flatMapLatest { id ->
            documentoMovimientoDao.getDocumentosByMovimientoIdStream(id)
        }
    }
}