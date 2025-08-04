package com.example.allote.ui.adminresumen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allote.data.AdministracionResumenRepository
import com.example.allote.data.CurrencySettings
import com.example.allote.data.ResumenData
import com.example.allote.data.SettingsRepository
import com.example.allote.ui.AppDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ResumenUiState(
    val resumenData: ResumenData? = null,
    val currencySettings: CurrencySettings = CurrencySettings("USD", 1.0),
    val isLoading: Boolean = true
)

@HiltViewModel
class AdministracionResumenViewModel @Inject constructor(
    private val repository: AdministracionResumenRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val jobId: StateFlow<Int> = savedStateHandle.getStateFlow(AppDestinations.JOB_ID_ARG, 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ResumenUiState> = jobId.flatMapLatest { id ->
        if (id == 0) {
            flowOf(ResumenUiState(isLoading = false))
        } else {
            combine(
                repository.getResumenDataStream(id),
                settingsRepository.getCurrencySettingsFlow()
            ) { resumen, settings ->
                ResumenUiState(
                    resumenData = resumen,
                    currencySettings = settings,
                    isLoading = false
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ResumenUiState()
    )
}