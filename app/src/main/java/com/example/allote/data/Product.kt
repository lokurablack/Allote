package com.example.allote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ApplicationType {
    PULVERIZACION,
    ESPARCIDO
}

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombreComercial: String,
    val tipo: String, // Para PULVERIZACION: Herbicida, etc. Para ESPARCIDO: Semilla, Fertilizante, Cebo
    val applicationType: ApplicationType,
    val principioActivo: String? = null,
    val formulacionId: Int? = null,
    val numeroRegistroSenasa: String? = null,
    val concentracion: String? = null,
    val fabricante: String? = null,
    val bandaToxicologica: String? = null,
    val modoAccion: String? = null
)
