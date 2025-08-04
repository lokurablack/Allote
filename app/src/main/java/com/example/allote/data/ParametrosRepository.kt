package com.example.allote.data

import kotlinx.coroutines.flow.Flow

class ParametrosRepository(
    private val jobDao: JobDao,
    private val jobParametrosDao: JobParametrosDao
) {
    // Obtenemos los datos como flujos (Streams)
    fun getJobStream(jobId: Int): Flow<Job?> {
        return jobDao.getByIdStream(jobId)
    }

    fun getParametrosStream(jobId: Int): Flow<JobParametros?> {
        return jobParametrosDao.getByJobIdStream(jobId)
    }

    // Función para insertar o actualizar los parámetros.
    // Room maneja la lógica de 'INSERT OR REPLACE' con @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveParametros(parametros: JobParametros) {
        jobParametrosDao.insertOrUpdate(parametros)
    }
}