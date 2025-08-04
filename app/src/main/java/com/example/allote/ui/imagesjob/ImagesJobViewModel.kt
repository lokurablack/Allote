package com.example.allote.ui.imagesjob

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allote.data.ImageEntity // Asegúrate de tener este import
import com.example.allote.data.ImagesJobRepository
import com.example.allote.ui.AppDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImagesJobUiState(
    // --- CAMBIO: Ahora guardamos la entidad completa ---
    // Esto nos da acceso al ID para poder borrarla.
    val images: List<ImageEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ImagesJobViewModel @Inject constructor(
    private val repository: ImagesJobRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val jobId: StateFlow<Int> = savedStateHandle.getStateFlow(AppDestinations.JOB_ID_ARG, 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ImagesJobUiState> = jobId.flatMapLatest { id ->
        if (id != 0) {
            repository.getImagesForJobStream(id)
                .map { imageEntities ->
                    // El UiState ahora contiene la lista de entidades directamente.
                    ImagesJobUiState(images = imageEntities, isLoading = false)
                }
        } else {
            flowOf(ImagesJobUiState(isLoading = false))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ImagesJobUiState()
    )

    fun addImages(uris: List<Uri>) {
        val currentJobId = jobId.value
        if (currentJobId == 0) return

        viewModelScope.launch {
            uris.forEach { uri ->
                repository.addImageForJob(currentJobId, uri)
            }
        }
    }

    // --- FUNCIÓN AÑADIDA PARA BORRAR ---
    fun deleteImage(image: ImageEntity) {
        viewModelScope.launch {
            repository.deleteImage(image)
        }
    }
}