package com.example.allote.service

import com.example.allote.data.FlightSegment
import com.example.allote.data.WorkPlan
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

class FlightPlanningService {

    companion object {
        private const val DEFAULT_AUTONOMIA_MIN = 9.0
        private const val DEFAULT_CAPACIDAD_L = 40.0
        private const val DEFAULT_VELOCIDAD_KMH = 18.0
        private const val DEFAULT_INTERLINEADO_M = 7.0
        private const val DEFAULT_REABAST_MIN = 3.0
        private const val DEFAULT_TURNO_GIRO_SEG = 12.0
        private const val METERS_PER_DEGREE_LAT = 111320.0
        private const val EPSILON = 1e-6
    }

    data class PlanningInput(
        val jobId: Int,
        val loteId: Int?,
        val hectareas: Double,
        val extensionEsteOeste: Double,
        val extensionNorteSur: Double,
        val caudalLitrosHa: Double,
        val latReabastecedor: Double,
        val lngReabastecedor: Double,
        val direccionViento: Double,
        val velocidadViento: Double,
        val interlineado: Double = DEFAULT_INTERLINEADO_M,
        val velocidadTrabajoKmh: Double = DEFAULT_VELOCIDAD_KMH,
        val autonomiaBateriaMin: Double = DEFAULT_AUTONOMIA_MIN,
        val capacidadTanqueL: Double = DEFAULT_CAPACIDAD_L,
        val tiempoReabastecimientoMin: Double = DEFAULT_REABAST_MIN,
        val tiempoGiroSeg: Double = DEFAULT_TURNO_GIRO_SEG,
        val centroLat: Double? = null,
        val centroLng: Double? = null,
        val numeroDrones: Int = 1,
        val boundaryPoints: List<Pair<Double, Double>>? = null
    )

    data class PlanningResult(
        val workPlan: WorkPlan,
        val flightSegments: List<FlightSegment>
    )

    enum class FlightDirection {
        NORTE_SUR,
        ESTE_OESTE
    }

    fun calculateOptimalPlan(input: PlanningInput): PlanningResult {
        require(input.hectareas > 0) { "Las hectareas deben ser mayores a cero" }
        require(input.caudalLitrosHa > 0) { "El caudal debe ser mayor a cero" }

        val interlineado = input.interlineado.takeIf { it > EPSILON } ?: DEFAULT_INTERLINEADO_M
        val autonomiaMin = input.autonomiaBateriaMin.takeIf { it > EPSILON } ?: DEFAULT_AUTONOMIA_MIN
        val capacidadTanque = input.capacidadTanqueL.takeIf { it > EPSILON } ?: DEFAULT_CAPACIDAD_L
        val velocidadKmh = input.velocidadTrabajoKmh.takeIf { it > EPSILON } ?: DEFAULT_VELOCIDAD_KMH
        val velocidadMs = velocidadKmh / 3.6
        val tiempoReabastecimiento = input.tiempoReabastecimientoMin.takeIf { it >= 0 } ?: DEFAULT_REABAST_MIN
        val tiempoGiroSeg = input.tiempoGiroSeg.takeIf { it >= 0 } ?: DEFAULT_TURNO_GIRO_SEG

        val direccionPasadas = determineOptimalFlightDirection(
            input.extensionEsteOeste,
            input.extensionNorteSur,
            input.direccionViento
        )

        val productoPorHa = input.caudalLitrosHa
        val areaPorTanque = capacidadTanque / productoPorHa
        val distanciaAutonomia = autonomiaMin * 60.0 * velocidadMs
        val areaPorAutonomia = (distanciaAutonomia * interlineado) / 10000.0
        val areaLimitante = min(areaPorTanque, areaPorAutonomia)
        val areaUtil = if (areaLimitante > EPSILON) areaLimitante else input.hectareas

        val segmentos = generateFlightSegments(
            input = input,
            direccion = direccionPasadas,
            interlineado = interlineado,
            velocidadMs = velocidadMs,
            autonomiaMin = autonomiaMin,
            capacidadTanque = capacidadTanque,
            areaMaximaPorVuelo = areaUtil,
            tiempoGiroSeg = tiempoGiroSeg
        )

        val drones = max(1, input.numeroDrones)
        val tiempoTotalEstimado = calculateTotalTime(segmentos, tiempoReabastecimiento, drones)
        val distanciaTotalRecorrida = segmentos.sumOf { it.distancia }
        val numeroReabastecimientos = segmentos.count { it.requiereReabastecimiento }
        val boundaryJson = input.boundaryPoints?.takeIf { it.size >= 3 }?.let { encodeBoundary(it) }

        val workPlan = WorkPlan(
            id = 0,
            jobId = input.jobId,
            loteId = input.loteId,
            fechaCreacion = System.currentTimeMillis(),
            fechaModificacion = System.currentTimeMillis(),
            autonomiaBateria = autonomiaMin.roundToInt(),
            capacidadTanque = capacidadTanque,
            interlineado = interlineado,
            velocidadTrabajo = velocidadKmh,
            tiempoReabastecimiento = tiempoReabastecimiento,
            caudalAplicacion = input.caudalLitrosHa,
            extensionEsteOeste = input.extensionEsteOeste,
            extensionNorteSur = input.extensionNorteSur,
            hectareasTotales = input.hectareas,
            latReabastecedor = input.latReabastecedor,
            lngReabastecedor = input.lngReabastecedor,
            direccionViento = input.direccionViento,
            velocidadViento = input.velocidadViento,
            totalVuelos = segmentos.size,
            tiempoTotalEstimado = tiempoTotalEstimado,
            distanciaTotalRecorrida = distanciaTotalRecorrida,
            numeroReabastecimientos = numeroReabastecimientos,
            direccionPasadas = direccionPasadas.name,
            numeroDrones = drones,
            ordenPasadas = determineWindStrategy(input.direccionViento, direccionPasadas),
            boundaryGeoJson = boundaryJson
        )

        return PlanningResult(workPlan, segmentos)
    }

    private fun determineOptimalFlightDirection(
        extensionEO: Double,
        extensionNS: Double,
        direccionViento: Double
    ): FlightDirection {
        val vientoNormalizado = direccionViento.mod(360.0)
        val vientoEsNorteSur = vientoNormalizado < 45 ||
                (vientoNormalizado in 135.0..225.0) ||
                vientoNormalizado > 315

        return if (vientoEsNorteSur) {
            if (extensionEO >= extensionNS) FlightDirection.NORTE_SUR else FlightDirection.ESTE_OESTE
        } else {
            if (extensionNS >= extensionEO) FlightDirection.ESTE_OESTE else FlightDirection.NORTE_SUR
        }
    }

    private fun determineWindStrategy(
        direccionViento: Double,
        direccionPasadas: FlightDirection
    ): String {
        val vientoNormalizado = direccionViento.mod(360.0)
        val vientoEsNorteSur = vientoNormalizado < 45 ||
                (vientoNormalizado in 135.0..225.0) ||
                vientoNormalizado > 315

        return when (direccionPasadas) {
            FlightDirection.NORTE_SUR -> if (vientoEsNorteSur) "FAVOR_VIENTO" else "PERPENDICULAR_VIENTO"
            FlightDirection.ESTE_OESTE -> if (!vientoEsNorteSur) "FAVOR_VIENTO" else "PERPENDICULAR_VIENTO"
        }
    }

    private fun generateFlightSegments(
        input: PlanningInput,
        direccion: FlightDirection,
        interlineado: Double,
        velocidadMs: Double,
        autonomiaMin: Double,
        capacidadTanque: Double,
        areaMaximaPorVuelo: Double,
        tiempoGiroSeg: Double
    ): List<FlightSegment> {
        val segments = mutableListOf<FlightSegment>()
        val longitudPasada = when (direccion) {
            FlightDirection.NORTE_SUR -> maxOf(input.extensionNorteSur, interlineado)
            FlightDirection.ESTE_OESTE -> maxOf(input.extensionEsteOeste, interlineado)
        }

        val areaPasadaCompleta = (longitudPasada * interlineado) / 10000.0
        val productoPasadaCompleta = areaPasadaCompleta * input.caudalLitrosHa
        val tiempoPasadaCompleta = if (velocidadMs > 0) (longitudPasada / velocidadMs) / 60.0 else 0.0
        val maxPasadasTanque = if (productoPasadaCompleta <= EPSILON) {
            1
        } else {
            maxOf(1, floor(capacidadTanque / productoPasadaCompleta).toInt())
        }
        val maxPasadasBateria = calculateMaxPasadasPorBateria(autonomiaMin, tiempoPasadaCompleta, tiempoGiroSeg / 60.0)
        val maxPasadasPorVuelo = maxOf(1, min(maxPasadasTanque, maxPasadasBateria))

        val centroidFromBoundary = input.boundaryPoints?.takeIf { it.size >= 3 }?.let { computeBoundaryCentroid(it) }
        val centroLat = centroidFromBoundary?.first ?: input.centroLat ?: input.latReabastecedor
        val centroLng = centroidFromBoundary?.second ?: input.centroLng ?: input.lngReabastecedor
        val metrosPorGradoLng = METERS_PER_DEGREE_LAT * cos(centroLat * PI / 180.0).coerceAtLeast(0.01)

        var hectareasPendientes = input.hectareas
        var vueloIndex = 1

        while (hectareasPendientes > EPSILON) {
            val areaObjetivo = min(areaMaximaPorVuelo, hectareasPendientes)
            val pasadasNecesarias = if (areaPasadaCompleta > EPSILON) {
                ceil(areaObjetivo / areaPasadaCompleta).toInt()
            } else {
                1
            }
            val pasadasPlanificadas = min(maxPasadasPorVuelo, maxOf(1, pasadasNecesarias))

            var areaAcumulada = 0.0
            var distanciaAcumulada = 0.0
            var tiempoAcumulado = 0.0
            var productoAcumulado = 0.0
            var ultimoOffset = 0.0
            var ultimaLongitud = longitudPasada

            repeat(pasadasPlanificadas) { index ->
                val areaRestanteVuelo = min(areaObjetivo, hectareasPendientes) - areaAcumulada
                if (areaRestanteVuelo <= EPSILON) return@repeat

                val areaEstaPasada = min(areaPasadaCompleta, areaRestanteVuelo)
                val factor = if (areaPasadaCompleta > EPSILON) areaEstaPasada / areaPasadaCompleta else 1.0
                val longitudEstaPasada = longitudPasada * factor
                val tiempoEstaPasada = tiempoPasadaCompleta * factor
                val productoEstaPasada = areaEstaPasada * input.caudalLitrosHa

                if (index > 0) {
                    tiempoAcumulado += tiempoGiroSeg / 60.0
                }

                tiempoAcumulado += tiempoEstaPasada
                distanciaAcumulada += longitudEstaPasada
                productoAcumulado += productoEstaPasada
                areaAcumulada += areaEstaPasada
                ultimoOffset = index * interlineado
                ultimaLongitud = longitudEstaPasada
            }

            val areaRestanteTrasVuelo = (hectareasPendientes - areaAcumulada).coerceAtLeast(0.0)
            val alcanzoLimiteBateria = maxPasadasBateria == pasadasPlanificadas && areaRestanteTrasVuelo > EPSILON
            val alcanzoLimiteTanque = maxPasadasTanque == pasadasPlanificadas && areaRestanteTrasVuelo > EPSILON

            val tipoReabastecimiento = when {
                alcanzoLimiteBateria && alcanzoLimiteTanque -> "AMBOS"
                alcanzoLimiteBateria -> "BATERIA"
                alcanzoLimiteTanque -> "PRODUCTO"
                else -> null
            }

            val offsetLat = when (direccion) {
                FlightDirection.NORTE_SUR -> 0.0
                FlightDirection.ESTE_OESTE -> (ultimoOffset / METERS_PER_DEGREE_LAT)
            }
            val offsetLng = when (direccion) {
                FlightDirection.NORTE_SUR -> (ultimoOffset / metrosPorGradoLng)
                FlightDirection.ESTE_OESTE -> 0.0
            }

            val halfLengthLat = when (direccion) {
                FlightDirection.NORTE_SUR -> (ultimaLongitud / 2.0) / METERS_PER_DEGREE_LAT
                FlightDirection.ESTE_OESTE -> 0.0
            }
            val halfLengthLng = when (direccion) {
                FlightDirection.NORTE_SUR -> 0.0
                FlightDirection.ESTE_OESTE -> (ultimaLongitud / 2.0) / metrosPorGradoLng
            }

            val inicioLat = centroLat + offsetLat + halfLengthLat
            val inicioLng = centroLng + offsetLng - halfLengthLng
            val finLat = centroLat + offsetLat - halfLengthLat
            val finLng = centroLng + offsetLng + halfLengthLng

            val comentario = String.format(
                Locale.US,
                "Pasadas: %d | Tiempo exacto: %.1f min",
                pasadasPlanificadas,
                tiempoAcumulado
            )

            segments.add(
                FlightSegment(
                    workPlanId = 0,
                    ordenVuelo = vueloIndex,
                    latInicio = inicioLat,
                    lngInicio = inicioLng,
                    latFin = finLat,
                    lngFin = finLng,
                    distancia = distanciaAcumulada,
                    tiempoVuelo = maxOf(1, tiempoAcumulado.roundToInt()),
                    areaCubierta = areaAcumulada,
                    productoPulverizado = productoAcumulado,
                    requiereReabastecimiento = tipoReabastecimiento != null,
                    tipoReabastecimiento = tipoReabastecimiento,
                    comentario = comentario
                )
            )

            hectareasPendientes -= areaAcumulada
            vueloIndex++
        }

        return segments
    }

    private fun calculateMaxPasadasPorBateria(
        autonomiaMin: Double,
        tiempoPasadaMin: Double,
        tiempoGiroMin: Double
    ): Int {
        if (tiempoPasadaMin <= EPSILON) {
            return 1
        }
        var tiempoAcumulado = 0.0
        var pasadas = 0
        while (true) {
            val tiempoNuevaPasada = tiempoPasadaMin + if (pasadas > 0) tiempoGiroMin else 0.0
            if (tiempoAcumulado + tiempoNuevaPasada > autonomiaMin + EPSILON) {
                break
            }
            tiempoAcumulado += tiempoNuevaPasada
            pasadas++
            if (pasadas > 1000) break
        }
        return maxOf(1, pasadas)
    }

    private fun calculateTotalTime(
        segments: List<FlightSegment>,
        tiempoReabastecimientoMin: Double,
        numeroDrones: Int
    ): Int {
        val tiempoVuelo = segments.sumOf { it.tiempoVuelo }
        val reabastecimientos = segments.count { it.requiereReabastecimiento }
        val tiempoReab = (tiempoReabastecimientoMin * reabastecimientos).roundToInt()
        val total = tiempoVuelo + tiempoReab
        return ceil(total / max(1.0, numeroDrones.toDouble())).toInt()
    }

    fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun encodeBoundary(points: List<Pair<Double, Double>>): String {
        val jsonArray = JSONArray()
        points.forEach { (lat, lng) ->
            val obj = JSONObject()
            obj.put("lat", lat)
            obj.put("lng", lng)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    private fun computeBoundaryCentroid(points: List<Pair<Double, Double>>): Pair<Double, Double> {
        if (points.isEmpty()) return 0.0 to 0.0
        val latAverage = points.map { it.first }.average()
        val lngAverage = points.map { it.second }.average()
        return latAverage to lngAverage
    }
}

