package com.example.allote.ui.clientadmin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allote.data.Client
import com.example.allote.data.ClientAdministracionRepository
import com.example.allote.ui.AppDestinations // <-- IMPORTANTE: Añade esta importación
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ClientAdministracionViewModel @Inject constructor(
    repository: ClientAdministracionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // === CORRECCIÓN CLAVE ===
    // 1. Obtenemos el clientId usando la constante de AppDestinations para evitar errores de tipeo.
    // Antes usaba la clave incorrecta "CLIENT_ID". Ahora usa AppDestinations.CLIENT_ID_ARG ("clientId").
    private val clientId: StateFlow<Int> = savedStateHandle.getStateFlow(AppDestinations.CLIENT_ID_ARG, 0)

    // 2. Usamos un enfoque más robusto con flatMapLatest.
    // Esto reacciona si el clientId cambiara y obtiene el stream del cliente correspondiente.
    // filterNotNull() asegura que solo continuamos si el cliente existe.
    @OptIn(ExperimentalCoroutinesApi::class)
    val clientState: StateFlow<Client?> = clientId
        .flatMapLatest { id ->
            repository.getClientStream(id)
        }
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null // El valor inicial sigue siendo null, lo que mostrará la carga brevemente.
        )
}