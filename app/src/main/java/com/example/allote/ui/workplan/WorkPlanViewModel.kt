package com.example.allote.ui.workplan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allote.data.FlightSegment
import com.example.allote.data.Job
import com.example.allote.data.Lote
import com.example.allote.data.WorkPlan
import com.example.allote.data.WorkPlanRepository
import com.example.allote.service.FlightPlanningService
import com.example.allote.ui.AppDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import org.json.JSONArray
import org.json.JSONObject

private const val DEFAULT_INTERLINEADO = "7"
private const val DEFAULT_VELOCIDAD = "18"
private const val DEFAULT_AUTONOMIA = "9"
private const val DEFAULT_CAPACIDAD = "40"
private const val DEFAULT_TIEMPO_REAB = "3"
private const val DEFAULT_DRONES = "1"
private const val TIEMPO_GIRO_SEG = 12.0
private const val METERS_PER_DEGREE_LAT = 111320.0

@HiltViewModel
class WorkPlanViewModel @Inject constructor(
    private val workPlanRepository: WorkPlanRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val jobId: Int = savedStateHandle[AppDestinations.JOB_ID_ARG] ?: 0
    private val loteId: Int? = savedStateHandle.get<Int?>(AppDestinations.LOTE_ID_ARG)?.takeIf { it != 0 }

    private val _uiState = MutableStateFlow(WorkPlanUiState())
    val uiState: StateFlow<WorkPlanUiState> = _uiState.asStateFlow()

    init {
        loadJobAndLoteInfo()
        observeExistingPlan()
    }

    private fun loadJobAndLoteInfo() {
        viewModelScope.launch {
            try {
                val job = workPlanRepository.getJobInfo(jobId)
                val lote = loteId?.let { workPlanRepository.getLoteInfo(it) }
                val parametros = workPlanRepository.getJobParameters(jobId)

                _uiState.update { state ->
                    state.copy(
                        job = job ?: state.job,
                        lote = lote ?: state.lote,
                        interlineado = state.interlineado.ifBlank {
                            parametros?.interlineado?.formatEditableOrEmpty() ?: DEFAULT_INTERLINEADO
                        },
                        velocidadTrabajo = state.velocidadTrabajo.ifBlank {
                            parametros?.velocidad?.formatEditableOrEmpty() ?: DEFAULT_VELOCIDAD
                        },
                        autonomiaBateria = state.autonomiaBateria.ifBlank { DEFAULT_AUTONOMIA },
                        capacidadTanque = state.capacidadTanque.ifBlank { DEFAULT_CAPACIDAD },
                        tiempoReabastecimiento = state.tiempoReabastecimiento.ifBlank { DEFAULT_TIEMPO_REAB },
                        droneCount = state.droneCount.ifBlank { DEFAULT_DRONES },
                        latReabastecedor = state.latReabastecedor.ifBlank {
                            (lote?.latitude ?: job?.latitude)?.formatEditableOrEmpty() ?: ""
                        },
                        lngReabastecedor = state.lngReabastecedor.ifBlank {
                            (lote?.longitude ?: job?.longitude)?.formatEditableOrEmpty() ?: ""
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al cargar informacion inicial: ${e.message}") }
            }
        }
    }

    private fun observeExistingPlan() {
        viewModelScope.launch {
            try {
                val planFlow = if (loteId != null) {
                    workPlanRepository.getLatestWorkPlanForLote(jobId, loteId)
                } else {
                    workPlanRepository.getLatestWorkPlanForJob(jobId)
                }

                planFlow.collectLatest { plan ->
                    if (plan != null) {
                        val boundaryPoints = plan.boundaryGeoJson?.let { parseBoundaryJson(it) } ?: emptyList()
                        val boundaryMetrics = calculateBoundaryMetrics(boundaryPoints)
                        _uiState.update { state ->
                            state.copy(
                                currentPlan = plan,
                                extensionEsteOeste = plan.extensionEsteOeste.formatEditable(),
                                extensionNorteSur = plan.extensionNorteSur.formatEditable(),
                                caudal = plan.caudalAplicacion.formatEditable(),
                                interlineado = plan.interlineado.formatEditable(),
                                velocidadTrabajo = plan.velocidadTrabajo.formatEditable(),
                                autonomiaBateria = plan.autonomiaBateria.formatEditable(),
                                capacidadTanque = plan.capacidadTanque.formatEditable(),
                                tiempoReabastecimiento = plan.tiempoReabastecimiento.formatEditable(),
                                latReabastecedor = plan.latReabastecedor.formatEditable(),
                                lngReabastecedor = plan.lngReabastecedor.formatEditable(),
                                direccionViento = plan.direccionViento.toFloat(),
                                velocidadViento = plan.velocidadViento.formatEditable(),
                                droneCount = plan.numeroDrones.toString(),
                                lotBoundary = boundaryPoints,
                                boundaryAreaHa = boundaryMetrics?.areaHa,
                                showSuccess = false
                            )
                        }
                        loadFlightSegments(plan.id)
                    } else {
                        _uiState.update { it.copy(currentPlan = null, flightSegments = emptyList()) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al cargar plan existente: ${e.message}") }
            }
        }
    }

    private fun loadFlightSegments(planId: Int) {
        viewModelScope.launch {
            try {
                workPlanRepository.getFlightSegmentsForPlan(planId).collectLatest { segments ->
                    _uiState.update { it.copy(flightSegments = segments) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al cargar segmentos: ${e.message}") }
            }
        }
    }

    fun onExtensionEOChanged(value: String) {
        _uiState.update { it.copy(extensionEsteOeste = value) }
    }

    fun onExtensionNSChanged(value: String) {
        _uiState.update { it.copy(extensionNorteSur = value) }
    }

    fun onCaudalChanged(value: String) {
        _uiState.update { it.copy(caudal = value) }
    }

    fun onInterlineadoChanged(value: String) {
        _uiState.update { it.copy(interlineado = value) }
    }

    fun onVelocidadTrabajoChanged(value: String) {
        _uiState.update { it.copy(velocidadTrabajo = value) }
    }

    fun onAutonomiaChanged(value: String) {
        _uiState.update { it.copy(autonomiaBateria = value) }
    }

    fun onCapacidadTanqueChanged(value: String) {
        _uiState.update { it.copy(capacidadTanque = value) }
    }

    fun onTiempoReabastecimientoChanged(value: String) {
        _uiState.update { it.copy(tiempoReabastecimiento = value) }
    }

    fun onDroneCountChanged(value: String) {
        _uiState.update { it.copy(droneCount = value) }
    }

    fun onLatReabastecedorChanged(value: String) {
        _uiState.update { it.copy(latReabastecedor = value) }
    }

    fun onLngReabastecedorChanged(value: String) {
        _uiState.update { it.copy(lngReabastecedor = value) }
    }

    fun onDireccionVientoChanged(value: Float) {
        _uiState.update { it.copy(direccionViento = value) }
    }

    fun onVelocidadVientoChanged(value: String) {
        _uiState.update { it.copy(velocidadViento = value) }
    }

    fun onUseJobLocationForRefuel() {
        val job = _uiState.value.job
        if (job?.latitude != null && job.longitude != null) {
            _uiState.update {
                it.copy(
                    latReabastecedor = job.latitude.formatEditableOrEmpty(),
                    lngReabastecedor = job.longitude.formatEditableOrEmpty()
                )
            }
        }
    }

    fun onUseLoteLocationForRefuel() {
        val lote = _uiState.value.lote
        if (lote?.latitude != null && lote.longitude != null) {
            _uiState.update {
                it.copy(
                    latReabastecedor = lote.latitude.formatEditableOrEmpty(),
                    lngReabastecedor = lote.longitude.formatEditableOrEmpty()
                )
            }
        }
    }

    fun onBoundaryDefined(points: List<BoundaryPoint>) {
        val metrics = calculateBoundaryMetrics(points)
        _uiState.update { state ->
            state.copy(
                lotBoundary = points,
                boundaryAreaHa = metrics?.areaHa,
                extensionEsteOeste = metrics?.extensionEsteOeste?.formatEditable() ?: state.extensionEsteOeste,
                extensionNorteSur = metrics?.extensionNorteSur?.formatEditable() ?: state.extensionNorteSur
            )
        }
    }

    fun onClearBoundary() {
        _uiState.update {
            it.copy(
                lotBoundary = emptyList(),
                boundaryAreaHa = null
            )
        }
    }

    private fun parseBoundaryJson(json: String): List<BoundaryPoint> {
        return try {
            val jsonArray = JSONArray(json)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(index) ?: continue
                    val lat = obj.optDouble("lat")
                    val lng = obj.optDouble("lng")
                    add(BoundaryPoint(lat, lng))
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun calculateBoundaryMetrics(points: List<BoundaryPoint>): BoundaryMetrics? {
        if (points.size < 3) return null
        val first = points.first()
        val averageLat = points.map { it.latitude }.average()
        val metersPerDegreeLng = METERS_PER_DEGREE_LAT * cos(averageLat * PI / 180.0).coerceAtLeast(0.01)
        val coordinates = points.map {
            val x = (it.longitude - first.longitude) * metersPerDegreeLng
            val y = (it.latitude - first.latitude) * METERS_PER_DEGREE_LAT
            x to y
        }

        var areaAccumulator = 0.0
        for (i in coordinates.indices) {
            val (x0, y0) = coordinates[i]
            val (x1, y1) = coordinates[(i + 1) % coordinates.size]
            areaAccumulator += (x0 * y1) - (x1 * y0)
        }
        val area = abs(areaAccumulator) / 2.0
        val extentX = coordinates.maxOf { it.first } - coordinates.minOf { it.first }
        val extentY = coordinates.maxOf { it.second } - coordinates.minOf { it.second }

        return BoundaryMetrics(
            areaHa = area / 10000.0,
            extensionEsteOeste = extentX,
            extensionNorteSur = extentY
        )
    }

    fun calculatePlan() {
        val state = _uiState.value

        val extensionEO = state.extensionEsteOeste.toDoubleOrNull()
        val extensionNS = state.extensionNorteSur.toDoubleOrNull()
        val caudal = state.caudal.toDoubleOrNull()
        val interlineado = state.interlineado.toDoubleOrNull()
        val velocidadTrabajo = state.velocidadTrabajo.toDoubleOrNull()
        val autonomia = state.autonomiaBateria.toDoubleOrNull()
        val capacidadTanque = state.capacidadTanque.toDoubleOrNull()
        val tiempoReab = state.tiempoReabastecimiento.toDoubleOrNull()
        val latReab = state.latReabastecedor.toDoubleOrNull()
        val lngReab = state.lngReabastecedor.toDoubleOrNull()
        val velocidadViento = state.velocidadViento.toDoubleOrNull() ?: 0.0
        val droneCount = state.droneCount.toIntOrNull()

        when {
            extensionEO == null || extensionEO <= 0 -> {
                _uiState.update { it.copy(error = "Extension Este-Oeste invalida") }
                return
            }
            extensionNS == null || extensionNS <= 0 -> {
                _uiState.update { it.copy(error = "Extension Norte-Sur invalida") }
                return
            }
            caudal == null || caudal <= 0 -> {
                _uiState.update { it.copy(error = "Caudal de aplicacion invalido") }
                return
            }
            interlineado == null || interlineado <= 0 -> {
                _uiState.update { it.copy(error = "Interlineado invalido") }
                return
            }
            velocidadTrabajo == null || velocidadTrabajo <= 0 -> {
                _uiState.update { it.copy(error = "Velocidad de trabajo invalida") }
                return
            }
            autonomia == null || autonomia <= 0 -> {
                _uiState.update { it.copy(error = "Autonomia de bateria invalida") }
                return
            }
            capacidadTanque == null || capacidadTanque <= 0 -> {
                _uiState.update { it.copy(error = "Capacidad de tanque invalida") }
                return
            }
            tiempoReab == null || tiempoReab < 0 -> {
                _uiState.update { it.copy(error = "Tiempo de reabastecimiento invalido") }
                return
            }
            latReab == null || lngReab == null -> {
                _uiState.update { it.copy(error = "Ubicacion del reabastecedor invalida") }
                return
            }
            velocidadViento < 0 -> {
                _uiState.update { it.copy(error = "Velocidad del viento invalida") }
                return
            }
            droneCount == null || droneCount <= 0 -> {
                _uiState.update { it.copy(error = "Cantidad de drones invalida") }
                return
            }
        }

        val hectareasBase = when {
            state.boundaryAreaHa != null && state.boundaryAreaHa > 0 -> state.boundaryAreaHa
            state.lote != null -> state.lote.hectareas
            state.job?.surface != null && state.job.surface!! > 0 -> state.job.surface!!
            state.currentPlan != null -> state.currentPlan.hectareasTotales
            else -> 0.0
        }

        if (hectareasBase <= 0) {
            _uiState.update { it.copy(error = "Superficie del trabajo o lote invalida") }
            return
        }

        _uiState.update { it.copy(isCalculating = true, error = null) }

        viewModelScope.launch {
            try {
                val input = FlightPlanningService.PlanningInput(
                    jobId = jobId,
                    loteId = loteId,
                    hectareas = hectareasBase,
                    extensionEsteOeste = extensionEO,
                    extensionNorteSur = extensionNS,
                    caudalLitrosHa = caudal,
                    latReabastecedor = latReab,
                    lngReabastecedor = lngReab,
                    direccionViento = state.direccionViento.toDouble(),
                    velocidadViento = velocidadViento,
                    interlineado = interlineado,
                    velocidadTrabajoKmh = velocidadTrabajo,
                    autonomiaBateriaMin = autonomia,
                    capacidadTanqueL = capacidadTanque,
                    tiempoReabastecimientoMin = tiempoReab,
                    tiempoGiroSeg = TIEMPO_GIRO_SEG,
                    centroLat = state.lote?.latitude ?: state.job?.latitude,
                    centroLng = state.lote?.longitude ?: state.job?.longitude,
                    numeroDrones = droneCount,
                    boundaryPoints = state.lotBoundary.takeIf { it.size >= 3 }?.map { it.latitude to it.longitude }
                )

                val currentPlan = state.currentPlan
                if (currentPlan != null) {
                    workPlanRepository.recalculateWorkPlan(currentPlan, input)
                } else {
                    workPlanRepository.createWorkPlan(input)
                }

                _uiState.update { it.copy(isCalculating = false, showSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCalculating = false,
                        error = "Error al calcular el plan: ${e.message}"
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun dismissSuccess() {
        _uiState.update { it.copy(showSuccess = false) }
    }

    fun deletePlan() {
        val plan = _uiState.value.currentPlan ?: return
        viewModelScope.launch {
            try {
                workPlanRepository.deleteWorkPlan(plan)
                _uiState.update { it.copy(currentPlan = null, flightSegments = emptyList()) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al eliminar plan: ${e.message}") }
            }
        }
    }
}

data class WorkPlanUiState(
    val job: Job? = null,
    val lote: Lote? = null,
    val currentPlan: WorkPlan? = null,
    val flightSegments: List<FlightSegment> = emptyList(),
    val extensionEsteOeste: String = "",
    val extensionNorteSur: String = "",
    val caudal: String = "",
    val interlineado: String = DEFAULT_INTERLINEADO,
    val velocidadTrabajo: String = DEFAULT_VELOCIDAD,
    val autonomiaBateria: String = DEFAULT_AUTONOMIA,
    val capacidadTanque: String = DEFAULT_CAPACIDAD,
    val tiempoReabastecimiento: String = DEFAULT_TIEMPO_REAB,
    val droneCount: String = DEFAULT_DRONES,
    val latReabastecedor: String = "",
    val lngReabastecedor: String = "",
    val direccionViento: Float = 0f,
    val velocidadViento: String = "",
    val lotBoundary: List<BoundaryPoint> = emptyList(),
    val boundaryAreaHa: Double? = null,
    val isCalculating: Boolean = false,
    val showSuccess: Boolean = false,
    val error: String? = null
)

private fun Double.formatEditable(decimals: Int = 2): String {
    if (this.isNaN() || this.isInfinite()) return ""
    val difference = abs(this - this.toInt().toDouble())
    return if (difference < 1e-6) {
        this.toInt().toString()
    } else {
        String.format(Locale.US, "%.${decimals}f", this).trimEnd('0').trimEnd('.')
    }
}

private fun Double?.formatEditableOrEmpty(decimals: Int = 2): String {
    return this?.formatEditable(decimals) ?: ""
}

private fun Int.formatEditable(): String = this.toString()

data class BoundaryPoint(val latitude: Double, val longitude: Double)

private data class BoundaryMetrics(
    val areaHa: Double,
    val extensionEsteOeste: Double,
    val extensionNorteSur: Double
)
