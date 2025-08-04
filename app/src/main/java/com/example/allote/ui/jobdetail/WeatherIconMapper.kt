package com.example.allote.ui.jobdetail

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.allote.data.WeatherType

object WeatherIconMapper {
    fun getIcon(weatherType: WeatherType): ImageVector {
        return when (weatherType) {
            is WeatherType.ClearSky -> Icons.Default.WbSunny
            is WeatherType.MainlyClear -> Icons.Default.WbSunny
            is WeatherType.PartclyCloudy -> Icons.Default.FilterDrama
            is WeatherType.Overcast -> Icons.Default.Cloud
            is WeatherType.Foggy -> Icons.Default.Cloud
            is WeatherType.Drizzle -> Icons.Default.Grain
            is WeatherType.Rain -> Icons.Default.Grain
            is WeatherType.Snow -> Icons.Default.AcUnit
            is WeatherType.Thunderstorm -> Icons.Default.Thunderstorm
        }
    }
}
