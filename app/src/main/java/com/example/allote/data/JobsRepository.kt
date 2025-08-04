package com.example.allote.data

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import java.io.File

class JobsRepository(
    private val jobDao: JobDao,
    private val clientDao: ClientDao,
    private val imageDao: ImageDao
) {

    // Devuelve FLujos para que la UI reaccione a los cambios
    fun getJobsStream(): Flow<List<Job>> = jobDao.getAllStream() // Necesitarás crear esta función en JobDao
    fun getClientsStream(): Flow<List<Client>> = clientDao.getAllStream()

    suspend fun saveJob(job: Job) {
        if (job.id == 0) {
            jobDao.insert(job)
        } else {
            jobDao.update(job)
        }
    }

    // Lógica de negocio compleja, ahora en un solo lugar
    suspend fun deleteJobAndImages(job: Job) {
        val images = imageDao.getImagesByJobId(job.id)
        images.forEach { imageEntity ->
            try {
                // Borrado seguro del archivo físico
                val file = File(Uri.parse(imageEntity.imageUri).path ?: "")
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                // Loguear el error sería una buena práctica
                e.printStackTrace()
            }
        }
        imageDao.deleteImagesByJobId(job.id)
        jobDao.delete(job)
    }

    suspend fun updateJob(job: Job) {
        jobDao.update(job)
    }
}