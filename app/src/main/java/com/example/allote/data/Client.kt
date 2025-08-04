package com.example.allote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val lastname: String,
    val phone: String? = null,
    val email: String? = null,
    val cuit: String? = null,
    val localidad: String? = null,
    val direccion: String? = null
)