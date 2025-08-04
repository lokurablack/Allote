package com.example.allote.ui.jobdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allote.data.*
import com.example.allote.ui.AppDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JobDetailUiState(
    val job: Job? = null,
    val parametros: JobParametros? = null,
    val isLoading: Boolean = true,
    val forecast: List<DailyWeather>? = null,
    val selectedDayForecast: DailyWeather? = null // Nuevo estado para el diálogo
)

@HiltViewModel
class JobDetailViewModel @Inject constructor(
    private val jobDetailRepository: JobDetailRepository,
    private val meteoRepository: MeteoRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(JobDetailUiState(isLoading = true))
    val uiState: StateFlow<JobDetailUiState> = _uiState.asStateFlow()

    private val jobId: Int = savedStateHandle[AppDestinations.JOB_ID_ARG] ?: 0

    init {
        if (jobId != 0) {
            viewModelScope.launch {
                jobDetailRepository.getJobStream(jobId).collect { job ->
                    _uiState.update { it.copy(job = job) }
                    // Si el trabajo tiene ubicación, buscamos el pronóstico
                    job?.latitude?.let { lat ->
                        job.longitude?.let { lon ->
                            val weatherReport = meteoRepository.getWeatherReport(lat, lon)
                            _uiState.update { it.copy(forecast = weatherReport?.daily, isLoading = false) }
                        }
                    } ?: _uiState.update { it.copy(isLoading = false) }
                }
            }
            viewModelScope.launch {
                jobDetailRepository.getParametrosStream(jobId).collect { parametros ->
                    _uiState.update { it.copy(parametros = parametros) }
                }
            }
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun updateJobLocation(lat: Double, lng: Double) {
        if (jobId == 0) return
        viewModelScope.launch {
            _uiState.value.job?.let { currentJob ->
                val updatedJob = currentJob.copy(latitude = lat, longitude = lng)
                jobDetailRepository.updateJob(updatedJob)
            }
        }
    }

    // --- NUEVAS FUNCIONES PARA EL DIÁLOGO ---
    fun onDaySelected(day: DailyWeather) {
        _uiState.update { it.copy(selectedDayForecast = day) }
    }

    fun onDismissHourlyDialog() {
        _uiState.update { it.copy(selectedDayForecast = null) }
    }
}