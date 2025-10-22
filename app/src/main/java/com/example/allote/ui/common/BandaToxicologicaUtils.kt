package com.example.allote.ui.common

import androidx.compose.ui.graphics.Color

private val toxicBandColors = mapOf(
    "IA" to Color(0xFFD32F2F),
    "IB" to Color(0xFFD32F2F),
    "LA" to Color(0xFFD32F2F),
    "LB" to Color(0xFFD32F2F),
    "II" to Color(0xFFFBC02D),
    "III" to Color(0xFF1976D2),
    "IV" to Color(0xFF388E3C)
)

/**
 * Normalises the toxicological band value coming from the data source.
 *
 * Some rows imported from the SENASA vademecum CSV arrive with null characters or
 * extra descriptors (e.g. "ROJA (IA)", "I-B") that would otherwise prevent the
 * UI from identifying the correct colour. This helper strips any characters that
 * are not relevant to the band code and returns it in uppercase.
 */
fun sanitizeBandaToxicologica(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val cleaned = buildString {
        raw.forEach { char ->
            val upper = char.uppercaseChar()
            if (upper == 'I' || upper == 'V' || upper == 'A' || upper == 'B' || upper == 'L') {
                append(upper)
            }
        }
    }
    return cleaned.takeIf { it.isNotEmpty() }
}

/**
 * Resolves the colour associated to a toxicological band after normalisation.
 */
fun toxicBandColor(raw: String?, defaultColor: Color = Color.Gray): Color {
    val code = sanitizeBandaToxicologica(raw)
    return toxicBandColors[code] ?: defaultColor
}
