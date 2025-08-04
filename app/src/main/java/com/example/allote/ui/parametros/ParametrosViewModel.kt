package com.example.allote.ui.parametros

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allote.data.Job
import com.example.allote.data.JobParametros
import com.example.allote.data.ParametrosRepository
import com.example.allote.ui.AppDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ParametrosUiState(
    val job: Job? = null,
    val parametros: JobParametros? = null,
    val isLoading: Boolean = true
) {
    val tipoAplicacion: String get() = job?.tipoAplicacion ?: ""
    val isSolidApplication: Boolean get() = tipoAplicacion.equals("Aplicacion solida", ignoreCase = true)
}

@HiltViewModel
class ParametrosViewModel @Inject constructor(
    private val repository: ParametrosRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val jobId: StateFlow<Int> = savedStateHandle.getStateFlow(AppDestinations.JOB_ID_ARG, 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ParametrosUiState> = jobId.flatMapLatest { id ->
        if (id == 0) {
            flowOf(ParametrosUiState(isLoading = false))
        } else {
            combine(repository.getJobStream(id), repository.getParametrosStream(id)) { job, parametros ->
                ParametrosUiState(job = job, parametros = parametros, isLoading = false)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ParametrosUiState(isLoading = true)
    )

    fun saveParametros(
        dosis: Double?,
        tamanoGota: Double?,
        interlineado: Double?,
        velocidad: Double?,
        altura: Double?,
        discoUtilizado: String?,
        revoluciones: Double?
    ) {
        val currentJobId = jobId.value
        if (currentJobId == 0) return

        viewModelScope.launch {
            val currentParametros = uiState.value.parametros
            val newParametros = JobParametros(
                id = currentParametros?.id ?: 0,
                jobId = currentJobId,
                dosis = dosis,
                tamanoGota = tamanoGota,
                interlineado = interlineado,
                velocidad = velocidad,
                altura = altura,
                discoUtilizado = discoUtilizado,
                revoluciones = revoluciones
            )
            repository.saveParametros(newParametros)
        }
    }
}