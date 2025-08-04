package com.example.allote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "job_parametros")
data class JobParametros(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val jobId: Int,
    val dosis: Double?,
    val tamanoGota: Double?,
    val interlineado: Double?,
    val velocidad: Double?,
    val altura: Double?,
    val discoUtilizado: String? = null,
    val revoluciones: Double? = null
)
