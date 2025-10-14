package com.example.allote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val jobId: Int, // Mantener por compatibilidad con trabajos sin lotes
    val loteId: Int? = null, // Nueva relación: una receta pertenece a un lote específico
    val hectareas: Double,
    val caudal: Double,
    val caldoPorTachada: Double = 0.0,
    val totalCaldo: Double,
    val fechaCreacion: Long,
    val resumen: String = ""
)
