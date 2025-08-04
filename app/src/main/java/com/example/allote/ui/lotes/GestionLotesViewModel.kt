package com.example.allote.ui.lotes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allote.data.GestionLotesRepository
import com.example.allote.data.Lote
import com.example.allote.data.SurplusSummary
import com.example.allote.ui.AppDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GestionLotesUiState(
    val lotes: List<Lote> = emptyList(),
    val isLoading: Boolean = true,
    val jobHectareasTotales: Double? = null,
    val surplusSummary: SurplusSummary? = null
)

@HiltViewModel
class GestionLotesViewModel @Inject constructor(
    private val repository: GestionLotesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val jobId: Int = savedStateHandle[AppDestinations.JOB_ID_ARG] ?: 0

    private val _uiState = MutableStateFlow(GestionLotesUiState(isLoading = true))
    val uiState: StateFlow<GestionLotesUiState> = _uiState.asStateFlow()

    init {
        if (jobId != 0) {
            viewModelScope.launch {
                repository.getJobStream(jobId).combine(repository.getLotesStream(jobId)) { job, lotes ->
                    // Leemos el valor actual del uiState para no perder el surplusSummary si se actualiza la lista de lotes
                    val currentSummary = _uiState.value.surplusSummary
                    GestionLotesUiState(
                        lotes = lotes,
                        isLoading = false,
                        jobHectareasTotales = job?.surface,
                        surplusSummary = currentSummary
                    )
                }.collect { newState ->
                    _uiState.value = newState
                }
            }
        } else {
            _uiState.value = GestionLotesUiState(isLoading = false)
        }
    }

    private val _recipeSummary = MutableStateFlow<String?>(null)
    val recipeSummary: StateFlow<String?> = _recipeSummary.asStateFlow()

    private val _isLoadingRecipe = MutableStateFlow(false)
    val isLoadingRecipe: StateFlow<Boolean> = _isLoadingRecipe.asStateFlow()

    fun addLote(nombre: String, hectareas: Double) {
        if (jobId == 0) return
        viewModelScope.launch {
            repository.saveLote(Lote(jobId = jobId, nombre = nombre, hectareas = hectareas))
        }
    }

    fun updateLote(lote: Lote) = viewModelScope.launch {
        repository.updateLote(lote)
    }

    fun deleteLote(lote: Lote) = viewModelScope.launch {
        repository.deleteLote(lote)
    }

    fun updateLoteLocation(lote: Lote, lat: Double, lng: Double) = viewModelScope.launch {
        val updatedLote = lote.copy(latitude = lat, longitude = lng)
        repository.updateLote(updatedLote)
        _uiState.update { currentState ->
            val updatedLotes = currentState.lotes.map {
                if (it.id == updatedLote.id) updatedLote else it
            }
            currentState.copy(lotes = updatedLotes)
        }
    }

    fun registrarTrabajoRealizado(lote: Lote, hectareasReales: Double) {
        viewModelScope.launch {
            repository.updateLote(lote.copy(hectareasReales = hectareasReales))
        }
    }

    fun loadRecipeForLote(lote: Lote) {
        if (jobId == 0) return
        viewModelScope.launch {
            _isLoadingRecipe.value = true
            _recipeSummary.value = repository.calculateRecipeSummaryForLote(jobId, lote)
            _isLoadingRecipe.value = false
        }
    }

    fun clearRecipeSummary() {
        _recipeSummary.value = null
    }

    fun generateSurplusSummary() {
        if (jobId == 0) return
        viewModelScope.launch {
            val summary = repository.generateSurplusSummary(jobId)
            _uiState.value = _uiState.value.copy(surplusSummary = summary)
        }
    }

    fun clearSurplusSummary() {
        _uiState.value = _uiState.value.copy(surplusSummary = null)
    }
}
