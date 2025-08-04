package com.example.allote.ui.docviewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allote.data.CurrencySettings
import com.example.allote.data.DocumentViewerRepository
import com.example.allote.data.DocumentoMovimiento
import com.example.allote.data.MovimientoContable
import com.example.allote.data.SettingsRepository
import com.example.allote.ui.AppDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DocumentViewerUiState(
    val movimiento: MovimientoContable? = null,
    val documentos: List<DocumentoMovimiento> = emptyList(),
    val isLoading: Boolean = true,
    val currencySettings: CurrencySettings = CurrencySettings("USD", 1.0)
)

@HiltViewModel
class DocumentViewerViewModel @Inject constructor(
    private val repository: DocumentViewerRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val movimientoId: StateFlow<Int> = savedStateHandle.getStateFlow(AppDestinations.MOVIMIENTO_ID_ARG, 0)

    val uiState: StateFlow<DocumentViewerUiState> =
        combine(
            // --- ¡ESTA ES LA CORRECCIÓN CLAVE! ---
            // Pasamos el StateFlow<Int> directamente al repositorio.
            // El repositorio se encargará de usar flatMapLatest internamente.
            repository.getMovimientoStream(movimientoId),
            repository.getDocumentosStream(movimientoId),
            settingsRepository.getCurrencySettingsFlow()
        ) { movimiento, documentos, settings ->
            DocumentViewerUiState(
                movimiento = movimiento,
                documentos = documentos,
                isLoading = false,
                currencySettings = settings
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5000),
            initialValue = DocumentViewerUiState()
        )
}