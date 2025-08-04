package com.example.allote.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "documentos_movimiento",
    foreignKeys = [
        ForeignKey(
            entity = MovimientoContable::class,
            parentColumns = ["id"],
            childColumns = ["movimientoId"],
            onDelete = ForeignKey.CASCADE // Si se borra el movimiento, se borran sus documentos
        )
    ]
)
data class DocumentoMovimiento(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val movimientoId: Int,
    val uri: String,
    val mimeType: String?,
    val fileName: String?
)