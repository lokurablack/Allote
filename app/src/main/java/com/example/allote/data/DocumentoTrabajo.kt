package com.example.allote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documento_trabajo")
data class DocumentoTrabajo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val jobId: Int,
    val documentUri: String
)