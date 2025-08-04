package com.example.allote.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class WeatherType(
    val weatherDesc: String,
    val icon: ImageVector
) {
    object ClearSky : WeatherType("Cielo Despejado", Icons.Default.WbSunny)
    object MainlyClear : WeatherType("Mayormente Despejado", Icons.Default.WbSunny)
    object PartclyCloudy : WeatherType("Parcialmente Nublado", Icons.Default.FilterDrama)
    object Overcast : WeatherType("Nublado", Icons.Default.Cloud)
    object Foggy : WeatherType("Niebla", Icons.Default.Cloud)
    object Drizzle : WeatherType("Llovizna", Icons.Default.Grain)
    object Rain : WeatherType("Lluvia", Icons.Default.Grain)
    object Snow : WeatherType("Nieve", Icons.Default.AcUnit)
    object Thunderstorm : WeatherType("Tormenta", Icons.Default.Thunderstorm)

    companion object {
        fun fromWMO(code: Int): WeatherType {
            return when(code) {
                0 -> ClearSky
                1 -> MainlyClear
                2 -> PartclyCloudy
                3 -> Overcast
                45, 48 -> Foggy
                51, 53, 55, 56, 57 -> Drizzle
                61, 63, 65, 66, 67 -> Rain
                71, 73, 75, 77 -> Snow
                80, 81, 82 -> Rain
                95, 96, 99 -> Thunderstorm
                else -> ClearSky
            }
        }
    }
}
