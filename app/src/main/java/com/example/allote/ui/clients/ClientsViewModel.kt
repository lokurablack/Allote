package com.example.allote.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allote.data.ClientsRepository
import com.example.allote.data.Client
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class ClientsUiState(
    val clients: List<Client> = listOf(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
) {
    val filteredClients: List<Client>
        get() = if (searchQuery.isBlank()) {
            clients
        } else {
            clients.filter { client ->
                client.name.contains(searchQuery, ignoreCase = true) ||
                        client.lastname.contains(searchQuery, ignoreCase = true)
            }
        }
}

@HiltViewModel
class ClientsViewModel @Inject constructor(private val repository: ClientsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientsUiState())
    val uiState: StateFlow<ClientsUiState> = _uiState.asStateFlow()

    init {
        // En cuanto el ViewModel se crea, empieza a observar la base de datos.
        viewModelScope.launch {
            repository.getAllClientsStream().collect { clientList ->
                _uiState.value = _uiState.value.copy(
                    clients = clientList,
                    isLoading = false
                )
            }
        }
    }

    // EVENTOS: La UI llamará a estas funciones
    fun onSearchQueryChanged(newQuery: String) {
        _uiState.value = _uiState.value.copy(searchQuery = newQuery)
    }

    fun onClearSearch() {
        _uiState.value = _uiState.value.copy(searchQuery = "")
    }

    fun onDeleteClient(client: Client) {
        viewModelScope.launch {
            try {
                repository.deleteClientAndJobs(client)
                // No necesitas mostrar un Toast aquí, la UI lo hará.
            } catch (_: Exception) {
                // Manejar el error, quizás exponerlo en el UiState
            }
        }
    }

    fun onSaveClient(client: Client) {
        viewModelScope.launch {
            if (client.id == 0) {
                repository.insertClient(client)
            } else {
                repository.updateClient(client)
            }
        }
    }
}

