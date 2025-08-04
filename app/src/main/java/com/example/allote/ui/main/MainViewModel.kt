package com.example.allote.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allote.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- DATA CLASSES (Sin cambios) ---
data class DolarInfo(
    val isLoading: Boolean = true,
    val dolarBlue: DolarValue? = null,
    val dolarOficial: DolarValue? = null,
    val lastUpdate: String? = null,
    val error: String? = null
)

data class NewsState(
    val articles: List<Article> = emptyList(),
    val nextPage: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class MainUiState(
    val trabajosPendientes: Int = 0,
    val hectareasPendientes: Double = 0.0,
    val totalClientes: Int = 0,
    val saldoGeneral: Double = 0.0,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val dolarInfo: DolarInfo = DolarInfo(),
    val currencySettings: CurrencySettings = CurrencySettings("USD", 1.0),
    val weatherReport: WeatherReport? = null,
    val locationName: String? = null,
    val newsState: NewsState = NewsState()
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dashboardRepository: MainDashboardRepository,
    private val settingsRepository: SettingsRepository,
    private val locationProvider: LocationProvider,
    private val meteoRepository: MeteoRepository,
    private val newsRepository: NewsRepository
) : ViewModel() {

    private val _dolarInfoState = MutableStateFlow(DolarInfo())
    private val _weatherReportState = MutableStateFlow<WeatherReport?>(null)
    private val _locationNameState = MutableStateFlow<String?>(null)
    private val _newsState = MutableStateFlow(NewsState())
    private val _isRefreshing = MutableStateFlow(false)

    init {
        fetchDolarRates()
        fetchInitialNews()
        // No llamamos a fetchWeather aquí para esperar el permiso del usuario.
    }

    val uiState: StateFlow<MainUiState> = combine(
        dashboardRepository.getAllJobsStream(),
        dashboardRepository.getAllClientsStream(),
        dashboardRepository.getAllMovimientosStream(),
        _dolarInfoState,
        settingsRepository.getCurrencySettingsFlow(),
        _weatherReportState,
        _locationNameState,
        _newsState,
        _isRefreshing
    ) { results ->
        // ... (Tu lógica de combine se mantiene igual)
        val jobs = results[0] as List<Job>
        val clients = results[1] as List<Client>
        val movimientos = results[2] as List<MovimientoContable>
        val dolarInfo = results[3] as DolarInfo
        val settings = results[4] as CurrencySettings
        val weatherReport = results[5] as WeatherReport?
        val locationName = results[6] as String?
        val newsState = results[7] as NewsState
        val isRefreshing = results[8] as Boolean

        val pendingJobs = jobs.filter { it.status.equals("Pendiente", ignoreCase = true) }
        val trabajosPendientes = pendingJobs.size
        val hectareasPendientes = pendingJobs.sumOf { it.surface.toString().toDoubleOrNull() ?: 0.0 }
        val saldoGeneral = movimientos.sumOf { it.haber - it.debe }

        MainUiState(
            trabajosPendientes = trabajosPendientes,
            hectareasPendientes = hectareasPendientes, // <-- Descomentado para que funcione
            totalClientes = clients.size,
            saldoGeneral = saldoGeneral,
            isLoading = false,
            isRefreshing = isRefreshing,
            dolarInfo = dolarInfo,
            currencySettings = settings,
            weatherReport = weatherReport,
            locationName = locationName,
            newsState = newsState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    // --- FUNCIONES PÚBLICAS PARA SER LLAMADAS DESDE LA UI ---
    // ESTA ES LA SECCIÓN QUE SE AÑADE PARA SOLUCIONAR TUS ERRORES

    fun onLocationPermissionGranted() {
        // Llama a la lógica que ya tenías para obtener el clima.
        fetchWeatherForCurrentLocation()
    }

    fun onRefresh() {
        // Llama a la lógica de refresco que ya tenías.
        refresh()
    }

    fun onFetchNextPage() {
        // Llama a la lógica de paginación que ya tenías.
        fetchNextPage()
    }


    // --- LÓGICA INTERNA DEL VIEWMODEL (Sin cambios) ---

    private fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchDolarRates()
            fetchWeatherForCurrentLocation()
            fetchInitialNews() // Recarga la primera página de noticias
            _isRefreshing.value = false
        }
    }

    private fun fetchDolarRates() {
        viewModelScope.launch {
            _dolarInfoState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = dashboardRepository.getDolarRates()
                if (response != null) {
                    _dolarInfoState.update {
                        it.copy(
                            isLoading = false,
                            dolarBlue = response.blue,
                            dolarOficial = response.oficial,
                            lastUpdate = response.lastUpdate
                        )
                    }
                } else {
                    _dolarInfoState.update {
                        it.copy(isLoading = false, error = "No se pudo obtener la cotización.")
                    }
                }
            } catch (e: Exception) {
                _dolarInfoState.update {
                    it.copy(isLoading = false, error = "Error de red.")
                }
            }
        }
    }

    private fun fetchWeatherForCurrentLocation() {
        viewModelScope.launch {
            locationProvider.getCurrentLocationDetails()?.let { locationDetails ->
                _locationNameState.value = locationDetails.cityName
                val report = meteoRepository.getWeatherReport(
                    locationDetails.coordinates.latitude,
                    locationDetails.coordinates.longitude
                )
                _weatherReportState.value = report
            }
        }
    }

    private fun fetchInitialNews() {
        viewModelScope.launch {
            _newsState.update { it.copy(isLoading = true) }
            newsRepository.getNews()
                .onSuccess { response ->
                    _newsState.update {
                        it.copy(
                            articles = response.results,
                            nextPage = response.nextPage,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _newsState.update { it.copy(error = error.message, isLoading = false) }
                }
        }
    }

    private fun fetchNextPage() {
        val nextPage = _newsState.value.nextPage ?: return
        if (_newsState.value.isLoading) return

        viewModelScope.launch {
            _newsState.update { it.copy(isLoading = true) }
            newsRepository.getNews(page = nextPage)
                .onSuccess { response ->
                    _newsState.update {
                        it.copy(
                            articles = it.articles + response.results,
                            nextPage = response.nextPage,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _newsState.update { it.copy(error = error.message, isLoading = false) }
                }
        }
    }
}