package com.example.allote.ui.clientjobs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allote.data.Client
import com.example.allote.data.ClientJobsRepository
import com.example.allote.data.Job
import com.example.allote.ui.AppDestinations // <-- IMPORTANTE: Añadir esta importación
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClientJobsUiState(
    val isLoading: Boolean = true,
    val client: Client? = null,
    val allJobs: List<Job> = emptyList(),
    val selectedStatus: String = "",
    val selectedType: String = "",
    val selectedBilling: String = "",
    val fromDate: Long? = null,
    val toDate: Long? = null
) {
    val clientFullName: String get() = if (client != null) "${client.name} ${client.lastname}" else "Cliente"

    val filteredJobs: List<Job> get() = allJobs.filter { job ->
        (selectedStatus.isBlank() || job.status.equals(selectedStatus, ignoreCase = true)) &&
                (selectedType.isBlank() || job.tipoAplicacion.equals(selectedType, ignoreCase = true)) &&
                (selectedBilling.isBlank() || job.billingStatus.equals(selectedBilling, ignoreCase = true)) &&
                (fromDate == null || job.startDate >= fromDate) &&
                // Corrección aquí para manejar trabajos sin fecha de fin
                (toDate == null || (job.endDate != null && job.endDate!! <= toDate))
    }
}

private data class Filters(
    val status: String, val type: String, val billing: String, val fromDate: Long?, val toDate: Long?
)

@HiltViewModel
class ClientJobsViewModel @Inject constructor(
    private val repository: ClientJobsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // === CORRECCIÓN CLAVE ===
    // Antes: private val clientId: Int? = savedStateHandle["CLIENT_ID"]
    // Ahora: Usamos la constante correcta de AppDestinations.
    private val clientId: StateFlow<Int> = savedStateHandle.getStateFlow(AppDestinations.CLIENT_ID_ARG, 0)

    private val _statusFilter = MutableStateFlow("")
    private val _typeFilter = MutableStateFlow("")
    private val _billingFilter = MutableStateFlow("")
    private val _dateFilter = MutableStateFlow<Pair<Long?, Long?>>(Pair(null, null))

    private val filtersFlow: Flow<Filters> = combine(
        _statusFilter, _typeFilter, _billingFilter, _dateFilter
    ) { status, type, billing, dates ->
        Filters(status, type, billing, dates.first, dates.second)
    }

    // Usamos flatMapLatest para reaccionar a cambios en clientId y obtener los flujos correctos.
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ClientJobsUiState> = clientId.flatMapLatest { id ->
        if (id == 0) {
            // Si el ID es 0 (inválido), emitimos un estado de carga/vacío.
            flowOf(ClientJobsUiState(isLoading = false, client = null))
        } else {
            combine(
                repository.getClientStream(id).filterNotNull(),
                repository.getJobsForClientStream(id),
                filtersFlow
            ) { client, jobs, filters ->
                ClientJobsUiState(
                    isLoading = false,
                    client = client,
                    allJobs = jobs,
                    selectedStatus = filters.status,
                    selectedType = filters.type,
                    selectedBilling = filters.billing,
                    fromDate = filters.fromDate,
                    toDate = filters.toDate
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ClientJobsUiState()
    )


    fun onStatusChange(status: String) { _statusFilter.value = status }
    fun onTypeChange(type: String) { _typeFilter.value = type }
    fun onBillingChange(billing: String) { _billingFilter.value = billing }
    fun onDateChange(from: Long?, to: Long?) { _dateFilter.value = Pair(from, to) }

    fun updateJob(job: Job) = viewModelScope.launch { repository.updateJob(job) }

    fun saveJob(job: Job) {
        // Obtenemos el ID del StateFlow, nos aseguramos de que no sea 0.
        val currentClientId = clientId.value
        if (currentClientId == 0) return

        viewModelScope.launch {
            val jobToSave = if (job.description.isNullOrBlank()) {
                job.copy(
                    description = "Trabajo sin descripción",
                    clientId = currentClientId,
                    clientName = uiState.value.clientFullName
                )
            } else {
                job.copy(
                    clientId = currentClientId,
                    clientName = uiState.value.clientFullName
                )
            }
            // Usamos el nombre del cliente del estado actual, que ahora será correcto.
            repository.saveJob(jobToSave)
        }
    }

    fun deleteJob(job: Job) = viewModelScope.launch { repository.deleteJobAndImages(job) }
}