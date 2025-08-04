package com.example.allote.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allote.data.Client
import com.example.allote.data.Job
import com.example.allote.data.JobsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// 1. CLASE DE ESTADO: Contiene TODA la información que la UI necesita
data class JobsUiState(
    val isLoading: Boolean = true,
    val allJobs: List<Job> = emptyList(),
    val allClients: List<Client> = emptyList(),
    // Filtros
    val selectedClient: String = "",
    val selectedStatus: String = "",
    val selectedType: String = "",
    val selectedBilling: String = "",
    val fromDate: Long? = null,
    val toDate: Long? = null
) {
    // 2. LÓGICA DE FILTRADO Y CÁLCULO: Ahora vive junto al estado, no en el Composable
    val filteredJobs: List<Job>
        get() = allJobs.filter { job ->
            (selectedClient.isBlank() || job.clientName.contains(selectedClient, ignoreCase = true)) &&
                    (selectedStatus.isBlank() || job.status.equals(selectedStatus, ignoreCase = true)) &&
                    (selectedType.isBlank() || job.tipoAplicacion.equals(selectedType, ignoreCase = true)) &&
                    (selectedBilling.isBlank() || job.billingStatus.equals(selectedBilling, ignoreCase = true)) &&
                    (fromDate == null || job.startDate >= fromDate) &&
                    (toDate == null || job.endDate?.let { it <= toDate } ?: true)
        }

    val pendingJobs: List<Job>
        get() = filteredJobs.filter { it.status.equals("Pendiente", true) }

    val finishedJobs: List<Job>
        get() = filteredJobs.filter { it.status.equals("Finalizado", true) }

    val pendingHectares: Double
        get() = pendingJobs.sumOf { it.surface ?: 0.0 }

    val finishedHectares: Double
        get() = finishedJobs.sumOf { it.surface ?: 0.0 }
}

// 3. VIEWMODEL
@HiltViewModel
class JobsViewModel @Inject constructor(private val repository: JobsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(JobsUiState())
    val uiState: StateFlow<JobsUiState> = _uiState.asStateFlow()

    init {
        // Combina los flujos de trabajos y clientes.
        combine(repository.getJobsStream(), repository.getClientsStream()) { jobs, clients ->
            // Esta lambda de transformación simplemente devuelve el nuevo estado.
            JobsUiState(
                isLoading = false,
                allJobs = jobs,
                allClients = clients,
                // Mantenemos los filtros que el usuario ya había seleccionado
                selectedClient = _uiState.value.selectedClient,
                selectedStatus = _uiState.value.selectedStatus,
                selectedType = _uiState.value.selectedType,
                selectedBilling = _uiState.value.selectedBilling,
                fromDate = _uiState.value.fromDate,
                toDate = _uiState.value.toDate
            )
        }
            .onEach { newState ->
                // onEach se ejecuta con cada nuevo estado que el 'combine' emite.
                // Aquí es donde actualizamos el StateFlow.
                _uiState.value = newState
            }
            .launchIn(viewModelScope) // Inicia y mantiene el flujo activo mientras el ViewModel viva.
    }

    // 4. EVENTOS: La UI llamará a estas funciones
    fun onFilterChange(
        client: String? = null,
        status: String? = null,
        type: String? = null,
        billing: String? = null
    ) {
        _uiState.update {
            it.copy(
                selectedClient = client ?: it.selectedClient,
                selectedStatus = status ?: it.selectedStatus,
                selectedType = type ?: it.selectedType,
                selectedBilling = billing ?: it.selectedBilling
            )
        }
    }

    // === NUEVA FUNCIÓN AÑADIDA ===
    // Dedicada exclusivamente a manejar los cambios de fecha
    fun onDateChange(from: Long?, to: Long?) {
        _uiState.update {
            it.copy(
                fromDate = from,
                toDate = to
            )
        }
    }

    fun saveJob(job: Job) = viewModelScope.launch {
        val jobToSave = if (job.description.isNullOrBlank()) {
            job.copy(description = "Trabajo sin descripción")
        } else {
            job
        }
        repository.saveJob(jobToSave)
    }

    fun deleteJob(job: Job) = viewModelScope.launch {
        repository.deleteJobAndImages(job)
    }

    fun updateJob(job: Job) = viewModelScope.launch {
        repository.updateJob(job)
    }
}