package com.example.allote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "formulaciones")
data class Formulacion(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    var ordenMezcla: Int,
    val tipoUnidad: String // "SOLIDO" o "LIQUIDO"
)
