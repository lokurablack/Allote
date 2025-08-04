package com.example.allote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lotes")
data class Lote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val jobId: Int,
    val nombre: String,
    val hectareas: Double,
    val hectareasReales: Double? = null, // <-- CAMPO AÑADIDO
    val latitude: Double? = null,
    val longitude: Double? = null
)