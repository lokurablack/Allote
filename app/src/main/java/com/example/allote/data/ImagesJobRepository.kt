package com.example.allote.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImagesJobRepository @Inject constructor(
    private val imageDao: ImageDao,
    @ApplicationContext private val context: Context
) {
    fun getImagesForJobStream(jobId: Int): Flow<List<ImageEntity>> {
        return imageDao.getImagesByJobIdStream(jobId)
    }

    suspend fun addImageForJob(jobId: Int, imageUri: Uri) {
        val savedUri = saveCompressedImageToInternalStorage(imageUri)
        savedUri?.let {
            val imageEntity = ImageEntity(jobId = jobId, imageUri = it.toString())
            imageDao.insert(imageEntity)
        }
    }

    suspend fun deleteImage(image: ImageEntity) {
        withContext(Dispatchers.IO) {
            try {
                // Borra el archivo del almacenamiento interno.
                val file = File(Uri.parse(image.imageUri).path!!)
                if (file.exists()) {
                    file.delete()
                }
                // Borra la entrada de la base de datos.
                imageDao.delete(image)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun saveCompressedImageToInternalStorage(uri: Uri): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                val compressedFile = File(context.filesDir, "${UUID.randomUUID()}.jpg")
                val outputStream = FileOutputStream(compressedFile)

                originalBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                outputStream.flush()
                outputStream.close()

                // --- CORRECCIÓN CLAVE ---
                // En lugar de Uri.fromFile, usamos el FileProvider para obtener una URI segura.
                val authority = "${context.packageName}.fileprovider"
                FileProvider.getUriForFile(context, authority, compressedFile)

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}