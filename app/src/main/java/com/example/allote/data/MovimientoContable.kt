package com.example.allote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movimientos_contables")
data class MovimientoContable(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // <-- Valor por defecto

    val clientId: Int = 0, // <-- Valor por defecto
    val jobId: Int? = null,
    val fecha: Long = 0L, // <-- Valor por defecto
    val descripcion: String = "", // <-- Valor por defecto
    val debe: Double = 0.0, // <-- Valor por defecto
    val haber: Double = 0.0, // <-- Valor por defecto
    val tipoMovimiento: String = "", // <-- Valor por defecto
    val detallesPago: String? = null,
    val documentoUri: String? = null,
    val esAprobadoGeneral: Boolean = false,
    val estadoAprobacion: AprobacionStatus = AprobacionStatus.PENDIENTE
)