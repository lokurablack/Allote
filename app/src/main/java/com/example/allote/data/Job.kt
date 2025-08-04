package com.example.allote.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "jobs",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Job(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val clientId: Int = 0, // No necesitas @JvmField, es un detalle menor
    val clientName: String = "",
    // ...resto de campos con sus valores por defecto y nulabilidad correcta...
    val description: String? = null,
    val date: String = "",
    var status: String = "Pendiente",
    val startDate: Long = 0L,
    var endDate: Long? = null,
    val surface: Double? = 0.0,
    val valuePerHectare: Double? = 0.0,
    var billingStatus: String = "No Facturado",
    val notes: String? = null,
    val tipoAplicacion: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
) : Parcelable