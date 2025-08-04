package com.example.allote.ui.clientcontabilidad

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allote.data.*
import com.example.allote.ui.AppDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClientContabilidadUiState(
    val client: Client? = null,
    val todosLosMovimientos: List<MovimientoContable> = emptyList(),
    val saldo: Double = 0.0,
    val totalDebe: Double = 0.0, // <-- AÑADIDO
    val totalHaber: Double = 0.0, // <-- AÑADIDO
    val isLoading: Boolean = true,
    val fromDate: Long? = null,
    val toDate: Long? = null,
    val currencySettings: CurrencySettings = CurrencySettings("USD", 1.0),
    val documentosPorMovimiento: Map<Int, Int> = emptyMap(),
    val movimientoAEliminar: MovimientoContable? = null
) {
    val clientFullName: String get() = if (client != null) "${client.name} ${client.lastname}" else "Cliente"
    val movimientos: List<MovimientoContable>
        get() = todosLosMovimientos.filter {
            val fromCondition = fromDate?.let { from -> it.fecha >= from } ?: true
            val toCondition = toDate?.let { to -> it.fecha <= to } ?: true
            fromCondition && toCondition
        }
}

@HiltViewModel
class ClientContabilidadViewModel @Inject constructor(
    private val repository: ClientContabilidadRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val clientId: StateFlow<Int> = savedStateHandle.getStateFlow(AppDestinations.CLIENT_ID_ARG, 0)
    private val _dateFilter = MutableStateFlow<Pair<Long?, Long?>>(Pair(null, null))
    private val _movimientoAEliminar = MutableStateFlow<MovimientoContable?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ClientContabilidadUiState> =
        clientId.flatMapLatest { id ->
            if (id == 0) {
                flowOf(ClientContabilidadUiState(isLoading = false))
            } else {
                combine(
                    repository.getMovimientosForClientStream(id),
                    repository.getDocumentCountsStream(id),
                    repository.getClientStream(id),
                    settingsRepository.getCurrencySettingsFlow(),
                    _dateFilter,
                    _movimientoAEliminar
                ) { results ->
                    val movimientos = results[0] as List<MovimientoContable>
                    val docCounts = results[1] as List<DocumentCount>
                    val client = results[2] as Client?
                    val settings = results[3] as CurrencySettings
                    val dates = results[4] as Pair<Long?, Long?>
                    val movAEliminar = results[5] as MovimientoContable?

                    // Filtra los movimientos ANTES de calcular los totales
                    val movimientosFiltrados = movimientos.filter {
                        val fromCondition = dates.first?.let { from -> it.fecha >= from } ?: true
                        val toCondition = dates.second?.let { to -> it.fecha <= to } ?: true
                        fromCondition && toCondition
                    }

                    val totalDebe = movimientosFiltrados.sumOf { it.debe }
                    val totalHaber = movimientosFiltrados.sumOf { it.haber }
                    val saldoCalculado = totalHaber - totalDebe // <-- Corregido: Haber - Debe
                    val docCountMap = docCounts.associateBy({ it.id }, { it.doc_count })

                    ClientContabilidadUiState(
                        client = client,
                        todosLosMovimientos = movimientos, // Mantiene todos los movimientos para el filtro
                        documentosPorMovimiento = docCountMap,
                        saldo = saldoCalculado,
                        totalDebe = totalDebe, // <-- AÑADIDO
                        totalHaber = totalHaber, // <-- AÑADIDO
                        isLoading = false,
                        currencySettings = settings,
                        fromDate = dates.first,
                        toDate = dates.second,
                        movimientoAEliminar = movAEliminar
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ClientContabilidadUiState()
        )

    fun onDateChange(from: Long?, to: Long?) { _dateFilter.value = Pair(from, to) }

    fun addMovimiento(movimiento: MovimientoContable, documentos: List<DocumentoMovimiento>) {
        val currentClientId = clientId.value
        if (currentClientId == 0) return
        viewModelScope.launch {
            repository.saveMovimientoWithDocuments(movimiento.copy(clientId = currentClientId), documentos)
        }
    }

    fun updateMovimiento(movimiento: MovimientoContable, documentos: List<DocumentoMovimiento>) = viewModelScope.launch {
        repository.saveMovimientoWithDocuments(movimiento, documentos)
    }

    fun deleteMovimiento(movimiento: MovimientoContable) {
        if (movimiento.esAprobadoGeneral) {
            _movimientoAEliminar.value = movimiento
        } else {
            viewModelScope.launch {
                repository.deleteMovimiento(movimiento)
            }
        }
    }

    fun confirmarEliminacionDeContabilidadGeneral(movimiento: MovimientoContable) {
        viewModelScope.launch {
            repository.deleteMovimiento(movimiento)
            _movimientoAEliminar.value = null
        }
    }

    fun cancelarEliminacion() {
        _movimientoAEliminar.value = null
    }
}