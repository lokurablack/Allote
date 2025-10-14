package com.example.allote.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "flight_segments",
    foreignKeys = [
        ForeignKey(
            entity = WorkPlan::class,
            parentColumns = ["id"],
            childColumns = ["workPlanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["workPlanId"])]
)
data class FlightSegment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workPlanId: Int,
    val ordenVuelo: Int, // Orden secuencial del vuelo

    // Coordenadas del segmento
    val latInicio: Double,
    val lngInicio: Double,
    val latFin: Double,
    val lngFin: Double,

    // Detalles del vuelo
    val distancia: Double, // metros
    val tiempoVuelo: Int, // minutos
    val areaCubierta: Double, // hectáreas
    val productoPulverizado: Double, // litros o kg

    // Estado
    val requiereReabastecimiento: Boolean,
    val tipoReabastecimiento: String? = null, // "BATERIA", "PRODUCTO", "AMBOS"
    val comentario: String? = null
)
