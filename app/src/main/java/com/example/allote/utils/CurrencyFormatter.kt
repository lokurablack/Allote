package com.example.allote.utils

import com.example.allote.data.CurrencySettings
import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    fun format(
        baseValueInUsd: Double,
        settings: CurrencySettings
    ): String {
        val valueToDisplay: Double
        val currencySymbol: String

        // 1. Determina el valor a mostrar y el símbolo correcto según la configuración.
        if (settings.displayCurrency == "ARS" && settings.exchangeRate > 0) {
            valueToDisplay = baseValueInUsd * settings.exchangeRate
            currencySymbol = "$"
        } else {
            valueToDisplay = baseValueInUsd
            currencySymbol = "U$"
        }

        // 2. Formatea el número para que siempre sea un entero, usando separadores de miles.
        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 0 // <-- La única regla necesaria: sin decimales.
        }
        val formattedNumber = numberFormat.format(valueToDisplay)

        // 3. Devuelve la cadena final con un espacio no rompible (\u00A0) para evitar saltos de línea.
        return "$currencySymbol\u00A0$formattedNumber"
    }
}