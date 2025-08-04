package com.example.allote.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentosRepository @Inject constructor(
    private val administracionDao: AdministracionDao,
    @ApplicationContext private val context: Context
) {
    fun getDocumentsForJobStream(jobId: Int): Flow<List<DocumentoTrabajo>> {
        return administracionDao.getAllDocumentsForJobStream(jobId) // Necesitarás crear esta función
    }

    suspend fun addDocumentForJob(jobId: Int, contentUri: Uri) {
        // La lógica de copia y guardado ahora vive aquí
        val internalFileUri = copyFileToInternalStorage(contentUri)
        internalFileUri?.let {
            val docEntity = DocumentoTrabajo(jobId = jobId, documentUri = it.toString())
            administracionDao.insertDocumento(docEntity)
        }
    }

    suspend fun deleteDocument(documento: DocumentoTrabajo) {
        // Borramos el archivo físico
        val file = File(Uri.parse(documento.documentUri).path ?: "")
        if (file.exists()) file.delete()
        // Borramos el registro de la DB
        administracionDao.deleteDocumento(documento)
    }

    private suspend fun copyFileToInternalStorage(uri: Uri): Uri? = withContext(Dispatchers.IO) {
        val fileName = getFileName(context, uri) ?: "document_${UUID.randomUUID()}"
        val file = File(context.filesDir, fileName)
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun loadThumbnail(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        val isPdf = uri.toString().endsWith(".pdf", ignoreCase = true)
        try {
            if (isPdf) {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        if (renderer.pageCount > 0) {
                            renderer.openPage(0).use { page ->
                                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                bitmap
                            }
                        } else null
                    }
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if(nameIndex != -1) return cursor.getString(nameIndex)
            }
        }
        return null
    }
}