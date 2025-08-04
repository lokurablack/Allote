package com.example.allote.ui.pdfviewer

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allote.data.DocumentoTrabajo
import com.example.allote.data.DocumentosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class DocumentoItem(
    val id: Int,
    val uri: Uri,
    val name: String,
    val isPdf: Boolean,
    val dbEntity: DocumentoTrabajo
)

data class PdfViewerUiState(
    val documents: List<DocumentoItem> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class PdfViewerViewModel @Inject constructor(
    private val repository: DocumentosRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // === CORRECCIÓN 1: Obtenemos el jobId como nulable ===
    private val jobId: Int? = savedStateHandle["JOB_ID"]

    // === CORRECCIÓN 2: El StateFlow ahora es condicional ===
    val uiState: StateFlow<PdfViewerUiState> =
        if (jobId != null) {
            repository.getDocumentsForJobStream(jobId)
                .map { docEntities ->
                    val docItems = docEntities.map { entity ->
                        val uri = Uri.parse(entity.documentUri)
                        DocumentoItem(
                            id = entity.id,
                            uri = uri,
                            name = File(uri.path ?: "documento").name,
                            isPdf = entity.documentUri.endsWith(".pdf", true),
                            dbEntity = entity
                        )
                    }
                    PdfViewerUiState(documents = docItems, isLoading = false)
                }
        } else {
            flowOf(PdfViewerUiState(isLoading = false))
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PdfViewerUiState())

    fun addDocuments(contentUris: List<Uri>) {
        val currentJobId = jobId ?: return
        viewModelScope.launch {
            contentUris.forEach { uri ->
                repository.addDocumentForJob(currentJobId, uri)
            }
        }
    }

    fun deleteDocument(documentoItem: DocumentoItem) {
        viewModelScope.launch {
            repository.deleteDocument(documentoItem.dbEntity)
        }
    }

    suspend fun loadThumbnailFor(documentoItem: DocumentoItem): Bitmap? {
        return repository.loadThumbnail(documentoItem.uri)
    }
}