package com.example.allote.data

import kotlinx.coroutines.flow.Flow

class JobDetailRepository(
    private val jobDao: JobDao,
    private val jobParametrosDao: JobParametrosDao
) {
    // Usamos Flow para que si el trabajo cambia en otra parte, la UI se actualice sola.
    fun getJobStream(jobId: Int): Flow<Job?> {
        return jobDao.getByIdStream(jobId) // Necesitarás crear esta función en JobDao
    }

    fun getParametrosStream(jobId: Int): Flow<JobParametros?> {
        return jobParametrosDao.getByJobIdStream(jobId) // Y esta en JobParametrosDao
    }

    suspend fun updateJob(job: Job) {
        jobDao.update(job)
    }
}