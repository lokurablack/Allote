package com.example.allote.data

import kotlinx.coroutines.flow.Flow

class JobDetailRepository(
    private val jobDao: JobDao,
    private val jobParametrosDao: JobParametrosDao,
    private val loteDao: LoteDao
) {
    // Usamos Flow para que si el trabajo cambia en otra parte, la UI se actualice sola.
    fun getJobStream(jobId: Int): Flow<Job?> {
        return jobDao.getByIdStream(jobId) // Necesitarás crear esta función en JobDao
    }

    fun getParametrosStream(jobId: Int): Flow<JobParametros?> {
        return jobParametrosDao.getByJobIdStream(jobId) // Y esta en JobParametrosDao
    }

    fun getLotesStream(jobId: Int): Flow<List<Lote>> {
        return loteDao.getByJobIdStream(jobId)
    }

    suspend fun updateJob(job: Job) {
        jobDao.update(job)
    }
}