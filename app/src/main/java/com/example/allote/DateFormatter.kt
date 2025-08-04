package com.example.allote

import java.text.SimpleDateFormat
import java.util.*

object DateFormatter {
    private val originalFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat  = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    /**
     * Formatea una fecha en cadena yyyy-MM-dd a dd/MM/yyyy.
     */
    fun formatDate(dateString: String): String {
        return try {
            val date = originalFormat.parse(dateString)
            date?.let { displayFormat.format(it) } ?: dateString
        } catch (_: Exception) {
            dateString
        }
    }

    /**
     * Formatea milisegundos a cadena dd/MM/yyyy.
     */
    fun formatMillis(ms: Long): String {
        return if (ms <= 0L) {
            ""
        } else {
            displayFormat.format(Date(ms))
        }
    }
}
