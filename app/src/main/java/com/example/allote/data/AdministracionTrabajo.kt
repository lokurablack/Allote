package com.example.allote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class AdministracionTrabajo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val jobId: Int,
    val facturaUri: String? = null,
    val costoPorHectarea: Double = 0.0,
    val totalSinIVA: Double = 0.0,
    val totalConIVA: Double = 0.0,
    val aplicaIVA: Boolean = false,
    val documentUri: String? = null
)
