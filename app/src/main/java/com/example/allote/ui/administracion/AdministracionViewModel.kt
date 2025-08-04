package com.example.allote.ui.administracion

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allote.data.AdministracionRepository
import com.example.allote.data.AdministracionTrabajo
import com.example.allote.data.CurrencySettings
import com.example.allote.data.Job
import com.example.allote.data.MovimientoContable
import com.example.allote.data.SettingsRepository
import com.example.allote.ui.AppDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdministracionUiState(
    val isLoading: Boolean = true,
    val job: Job? = null,
    val administracion: AdministracionTrabajo? = null,
    val costoPorHectareaText: String = "",
    val hectareasText: String = "",
    val aplicarIVA: Boolean = false,
    val showResults: Boolean = false,
    val totalSinIVA: Double = 0.0,
    val totalConIVA: Double = 0.0,
    val currencySettings: CurrencySettings = CurrencySettings("USD", 1.0)
)

@HiltViewModel
class AdministracionViewModel @Inject constructor(
    private val repository: AdministracionRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val jobId: Int = savedStateHandle[AppDestinations.JOB_ID_ARG] ?: 0
    private val IVA_PORCENTAJE = 0.105

    private val _uiState = MutableStateFlow(AdministracionUiState())
    val uiState: StateFlow<AdministracionUiState> = _uiState.asStateFlow()

    init {
        if (jobId != 0) {
            combine(
                repository.getJobStream(jobId),
                repository.getAdministracionStream(jobId),
                settingsRepository.getCurrencySettingsFlow()
            ) { job, adm, settings ->
                Triple(job, adm, settings)
            }.onEach { (job, adm, settings) ->
                val costoGuardado = adm?.costoPorHectarea?.takeIf { it > 0 }?.toString()
                val costoPorDefecto = job?.tipoAplicacion?.let { settingsRepository.getDefaultPrice(it) }?.takeIf { it > 0 }?.toString()
                val hectareasInitial = job?.surface?.toString() ?: ""

                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        job = job,
                        administracion = adm,
                        costoPorHectareaText = costoGuardado ?: costoPorDefecto ?: currentState.costoPorHectareaText,
                        hectareasText = if (currentState.hectareasText.isEmpty()) hectareasInitial else currentState.hectareasText,
                        aplicarIVA = adm?.aplicaIVA ?: false,
                        showResults = adm != null && adm.totalConIVA > 0,
                        totalSinIVA = adm?.totalSinIVA ?: 0.0,
                        totalConIVA = adm?.totalConIVA ?: 0.0,
                        currencySettings = settings
                    )
                }
            }.launchIn(viewModelScope)
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onHectareasChange(newValue: String) {
        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*\$"))) {
            _uiState.update { it.copy(hectareasText = newValue) }
        }
    }

    fun onCostoChange(newValue: String) {
        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*\$"))) {
            _uiState.update { it.copy(costoPorHectareaText = newValue) }
        }
    }

    fun onIvaChange(isChecked: Boolean) {
        _uiState.update { it.copy(aplicarIVA = isChecked) }
    }

    fun calcularTotales() {
        val costo = _uiState.value.costoPorHectareaText.toDoubleOrNull() ?: return
        val hectareas = _uiState.value.hectareasText.toDoubleOrNull() ?: return
        if (hectareas <= 0) return
        val sinIVA = hectareas * costo
        val conIVA = if (_uiState.value.aplicarIVA) sinIVA * (1 + IVA_PORCENTAJE) else sinIVA
        _uiState.update { it.copy(totalSinIVA = sinIVA, totalConIVA = conIVA, showResults = true) }
    }

    fun saveChanges() {
        if (jobId == 0) return
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.showResults) return@launch

            val adm = state.administracion?.copy(
                costoPorHectarea = state.costoPorHectareaText.toDoubleOrNull() ?: 0.0,
                aplicaIVA = state.aplicarIVA,
                totalSinIVA = state.totalSinIVA,
                totalConIVA = state.totalConIVA
            ) ?: AdministracionTrabajo(
                jobId = jobId,
                costoPorHectarea = state.costoPorHectareaText.toDoubleOrNull() ?: 0.0,
                aplicaIVA = state.aplicarIVA,
                totalSinIVA = state.totalSinIVA,
                totalConIVA = state.totalConIVA
            )
            repository.saveAdministracionData(adm)

            val movimientoExistente = repository.getMovimientoContable(jobId)

            val movimientoAGuardar = if (movimientoExistente != null) {
                movimientoExistente.copy(
                    debe = state.totalConIVA
                )
            } else {
                state.job?.let {
                    MovimientoContable(
                        clientId = it.clientId,
                        jobId = jobId,
                        fecha = System.currentTimeMillis(),
                        descripcion = "Costo del trabajo: ${it.description ?: "N/A"}",
                        debe = state.totalConIVA,
                        haber = 0.0,
                        tipoMovimiento = "TRABAJO"
                    )
                }
            }

            if (movimientoAGuardar != null) {
                repository.saveMovimientoContable(movimientoAGuardar)
            }
        }
    }
}
