package com.example.allote.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "work_plans",
    foreignKeys = [
        ForeignKey(
            entity = Job::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["jobId"])]
)
data class WorkPlan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val jobId: Int,
    val loteId: Int? = null,
    val fechaCreacion: Long,
    val fechaModificacion: Long,
    val autonomiaBateria: Int,
    val capacidadTanque: Double,
    val interlineado: Double,
    val velocidadTrabajo: Double,
    val tiempoReabastecimiento: Double,
    val caudalAplicacion: Double,
    val extensionEsteOeste: Double,
    val extensionNorteSur: Double,
    val hectareasTotales: Double,
    val latReabastecedor: Double,
    val lngReabastecedor: Double,
    val direccionViento: Double,
    val velocidadViento: Double,
    val totalVuelos: Int,
    val tiempoTotalEstimado: Int,
    val distanciaTotalRecorrida: Double,
    val numeroReabastecimientos: Int,
    val numeroDrones: Int,
    val direccionPasadas: String,
    val ordenPasadas: String,
    val boundaryGeoJson: String? = null
)
