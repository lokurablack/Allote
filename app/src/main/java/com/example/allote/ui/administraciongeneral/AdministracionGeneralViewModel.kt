package com.example.allote.ui.administraciongeneral

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allote.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MovimientoPendienteItemModel(
    val movimiento: MovimientoContable,
    val clientName: String
)

data class MovimientoGeneralItemModel(
    val movimiento: MovimientoContable,
    val clientName: String?
)

data class AdministracionGeneralUiState(
    val filtroActual: FiltroFecha = FiltroFecha(),
    val movimientosVisibles: List<MovimientoGeneralItemModel> = emptyList(),
    val saldoDelPeriodo: Double = 0.0,
    val movimientosPendientes: List<MovimientoPendienteItemModel> = emptyList(),
    val conteoPendientes: Int = 0,
    val isLoading: Boolean = true,
    val currencySettings: CurrencySettings = CurrencySettings("USD", 1.0),
    val documentosPorMovimiento: Map<Int, Int> = emptyMap()
)

@HiltViewModel
class AdministracionGeneralViewModel @Inject constructor(
    private val repository: AdministracionGeneralRepository,
    private val settingsRepository: SettingsRepository,
    private val application: Application
) : ViewModel() {

    private val _filtroState = MutableStateFlow(FiltroFecha())

    val uiState: StateFlow<AdministracionGeneralUiState> = combine(
        repository.getMovimientosAprobadosStream(),
        repository.getMovimientosPendientesStream(),
        repository.getMovimientosPendientesCountStream(),
        _filtroState,
        settingsRepository.getCurrencySettingsFlow(),
        repository.getAllClientsStream(),
        repository.getAprobadoDocumentCountsStream()
    ) { results ->
        val allMovsAprobados = results[0] as List<MovimientoContable>
        val pendientes = results[1] as List<MovimientoContable>
        val conteo = results[2] as Int
        val filtro = results[3] as FiltroFecha
        val settings = results[4] as CurrencySettings
        val allClients = results[5] as List<Client>
        val docCounts = results[6] as List<DocumentCount>

        val docCountMap = docCounts.associateBy({ it.id }, { it.doc_count })
        val (inicio, fin) = getRangoDeFechas(filtro)
        val movimientosDelPeriodo = allMovsAprobados.filter { it.fecha in inicio..fin }

        // --- LÓGICA QUE FALTABA, AHORA CORREGIDA ---
        val movimientosVisiblesConNombre = movimientosDelPeriodo.map { movimiento ->
            val client = allClients.find { it.id == movimiento.clientId }
            MovimientoGeneralItemModel(
                movimiento = movimiento,
                clientName = client?.let { "${it.name} ${it.lastname}" }
            )
        }

        val pendientesConNombre = pendientes.mapNotNull { movimiento ->
            val client = allClients.find { it.id == movimiento.clientId }
            client?.let {
                MovimientoPendienteItemModel(
                    movimiento = movimiento,
                    clientName = "${it.name} ${it.lastname}"
                )
            }
        }

        val saldoCalculado = movimientosDelPeriodo.sumOf { it.haber - it.debe }

        AdministracionGeneralUiState(
            filtroActual = filtro,
            movimientosVisibles = movimientosVisiblesConNombre,
            saldoDelPeriodo = saldoCalculado,
            movimientosPendientes = pendientesConNombre,
            conteoPendientes = conteo,
            isLoading = false,
            currencySettings = settings,
            documentosPorMovimiento = docCountMap
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AdministracionGeneralUiState()
    )

    fun saveMovimiento(
        movimientoExistente: MovimientoContable?,
        descripcion: String,
        monto: Double,
        tipo: TipoMovimientoGeneral,
        fecha: Long,
        notas: String?,
        documentos: List<Uri>
    ) {
        viewModelScope.launch {
            val settings = settingsRepository.getCurrencySettingsFlow().first()
            val montoEnUsd = if (settings.displayCurrency == "ARS" && settings.exchangeRate > 0) {
                monto / settings.exchangeRate
            } else {
                monto
            }

            if (movimientoExistente == null) {
                // --- LÓGICA PARA AÑADIR (CORREGIDA) ---
                val nuevoMovimiento = MovimientoContable(
                    clientId = -1,
                    jobId = null,
                    fecha = fecha,
                    descripcion = descripcion,
                    debe = if (tipo == TipoMovimientoGeneral.EGRESO) montoEnUsd else 0.0,
                    haber = if (tipo == TipoMovimientoGeneral.INGRESO) montoEnUsd else 0.0,
                    tipoMovimiento = "GENERAL",
                    detallesPago = notas,
                    documentoUri = null, // Este campo es obsoleto, se gestiona en la otra tabla
                    // ¡LA CLAVE! Se asegura de que se cree como APROBADO.
                    estadoAprobacion = AprobacionStatus.APROBADO
                )
                repository.insertMovimientoConDocumentos(nuevoMovimiento, documentos, application)
            } else {
                // --- LÓGICA PARA ACTUALIZAR (CORREGIDA) ---
                val movimientoActualizado = movimientoExistente.copy(
                    descripcion = descripcion,
                    debe = if (tipo == TipoMovimientoGeneral.EGRESO) montoEnUsd else 0.0,
                    haber = if (tipo == TipoMovimientoGeneral.INGRESO) montoEnUsd else 0.0,
                    fecha = fecha,
                    detallesPago = notas,
                    // ¡LA CLAVE! Se asegura de que se mantenga como APROBADO.
                    estadoAprobacion = AprobacionStatus.APROBADO
                )
                repository.updateMovimientoConDocumentos(movimientoActualizado, documentos, application)
            }
        }
    }

    fun eliminarMovimientoGeneral(movimiento: MovimientoContable) {
        viewModelScope.launch {
            if (movimiento.clientId != -1) {
                // Si tiene un cliente asociado, se "rechaza" para que no aparezca más aquí
                // pero siga visible en la contabilidad del cliente.
                repository.updateMovimiento(movimiento.copy(estadoAprobacion = AprobacionStatus.RECHAZADO))
            } else {
                // Si es un movimiento general (sin cliente), se elimina permanentemente.
                repository.deleteMovimiento(movimiento)
            }
        }
    }

    fun cambiarTipoFiltro(tipo: TipoFiltroFecha) {
        _filtroState.value = FiltroFecha(tipo = tipo)
    }

    fun cambiarRangoPersonalizado(inicio: Long, fin: Long) {
        _filtroState.value = FiltroFecha(
            tipo = TipoFiltroFecha.RANGO_PERSONALIZADO,
            fechaInicio = inicio,
            fechaFin = fin
        )
    }

    fun aprobarMovimiento(movimiento: MovimientoContable) {
        viewModelScope.launch {
            repository.updateMovimiento(movimiento.copy(estadoAprobacion = AprobacionStatus.APROBADO))
        }
    }

    fun rechazarMovimientoPendiente(movimiento: MovimientoContable) {
        viewModelScope.launch {
            // No borra, solo cambia el estado a RECHAZADO
            repository.updateMovimiento(movimiento.copy(estadoAprobacion = AprobacionStatus.RECHAZADO))
        }
    }

    suspend fun getDocumentosParaMovimiento(movimientoId: Int): List<DocumentoMovimiento> {
        return repository.getDocumentosParaMovimiento(movimientoId)
    }
}