package com.example.allote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ApplicationType {
    PULVERIZACION,
    ESPARCIDO,
    AMBOS // Nuevo: para productos sin tipo específico
}

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombreComercial: String,
    val tipo: String, // Corresponde a "aptitudes" del CSV
    val applicationType: String,
    val principioActivo: String? = null, // Corresponde a "activos" del CSV
    val formulacionId: Int? = null,
    val numeroRegistroSenasa: String? = null, // Nuevo campo del CSV
    val concentracion: String? = null,
    val fabricante: String? = null, // Corresponde a "marca" del CSV
    val bandaToxicologica: String? = null, // Del CSV
    val modoAccion: String? = null,
    val isFromVademecum: Boolean = false // Para identificar productos importados
)
