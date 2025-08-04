package com.example.allote.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allote.data.CurrencySettings
import com.example.allote.data.ExchangeRateRepository
import com.example.allote.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val precioLiquida: String = "",
    val precioSolida: String = "",
    val precioMixta: String = "",
    val precioVarias: String = "",
    val tankCapacity: String = "",
    val currencySettings: CurrencySettings = CurrencySettings("USD", 1.0),
    val isUpdatingRate: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val exchangeRateRepository: ExchangeRateRepository
) : ViewModel() {

    private val _isUpdatingRate = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.getCurrencySettingsFlow(),
        settingsRepository.getPricesFlow(),
        _isUpdatingRate
    ) { settings, prices, isUpdating ->
        SettingsUiState(
            precioLiquida = prices.liquida,
            precioSolida = prices.solida,
            precioMixta = prices.mixta,
            precioVarias = prices.varias,
            tankCapacity = settingsRepository.getTankCapacity(),
            currencySettings = settings,
            isUpdatingRate = isUpdating
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun savePrice(key: String, value: String) {
        when (key) {
            "liquida" -> settingsRepository.savePrecioLiquida(value)
            "solida" -> settingsRepository.savePrecioSolida(value)
            "mixta" -> settingsRepository.savePrecioMixta(value)
            "varias" -> settingsRepository.savePrecioVarias(value)
        }
    }

    fun actualizarTasaDeCambioDesdeApi() {
        viewModelScope.launch {
            _isUpdatingRate.value = true
            val response = exchangeRateRepository.getDolarApiResponse()
            if (response?.blue?.valueSell != null) {
                val newRate = response.blue.valueSell
                settingsRepository.saveExchangeRate(newRate.toString())
            } else {
            }
            _isUpdatingRate.value = false
        }
    }

    fun saveDisplayCurrency(currency: String) {
        settingsRepository.saveDisplayCurrency(currency)
    }

    fun saveExchangeRate(rate: String) {
        settingsRepository.saveExchangeRate(rate)
    }
}