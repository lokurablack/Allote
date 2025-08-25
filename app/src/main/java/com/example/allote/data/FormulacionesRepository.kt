package com.example.allote.data

import kotlinx.coroutines.flow.Flow

class FormulacionesRepository(private val formulacionDao: FormulacionDao) {

    fun getFormulacionesStream(): Flow<List<Formulacion>> {
        return formulacionDao.getAllFormulacionesStream()
    }

    suspend fun saveAll(formulaciones: List<Formulacion>) {
        formulacionDao.insertAll(formulaciones)
    }

    suspend fun deleteAll(formulaciones: List<Formulacion>) {
        formulacionDao.delete(formulaciones)
    }

    suspend fun getProductCountForFormulacion(formulacionId: Int): Int {
        return formulacionDao.getProductCountForFormulacion(formulacionId)
    }
}