package com.example.allote.ui.addjob

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allote.data.Client
import com.example.allote.data.Job
import com.example.allote.data.MainDashboardRepository // Reusamos este repo que ya obtiene clientes y trabajos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddJobViewModel @Inject constructor(
    private val repository: MainDashboardRepository,
    private val jobDao: com.example.allote.data.JobDao // Inyectamos el DAO directamente para guardar
) : ViewModel() {

    // Exponemos la lista de todos los clientes para el desplegable del diálogo
    val clients: StateFlow<List<Client>> = repository.getAllClientsStream()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Función para guardar el nuevo trabajo
    fun saveJob(job: Job) = viewModelScope.launch {
        jobDao.insert(job)
    }
}