package com.example.allote.data

import androidx.core.net.toUri
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientJobsRepository @Inject constructor(
    private val jobDao: JobDao,
    private val clientDao: ClientDao,
    private val imageDao: ImageDao
) {
    fun getClientStream(clientId: Int): Flow<Client?> = clientDao.getByIdStream(clientId)
    fun getJobsForClientStream(clientId: Int): Flow<List<Job>> = jobDao.getJobsByClientIdStream(clientId)

    suspend fun updateJob(job: Job) {
        jobDao.update(job)
    }

    suspend fun saveJob(job: Job) {
        jobDao.insert(job)
    }

    // Encapsulamos la lógica de borrado complejo aquí
    suspend fun deleteJobAndImages(job: Job) {
        val images = imageDao.getImagesByJobId(job.id)
        images.forEach { imageEntity ->
            try {
                val file = File(imageEntity.imageUri.toUri().path ?: "")
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                // Es bueno loguear el error en un sistema real
                e.printStackTrace()
            }
        }
        imageDao.deleteImagesByJobId(job.id)
        jobDao.delete(job)
    }
}