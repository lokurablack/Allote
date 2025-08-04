package com.example.allote.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeteoRepository @Inject constructor(
    private val meteoApiService: MeteoApiService
) {
    suspend fun getWeatherReport(latitude: Double, longitude: Double): WeatherReport? {
        return try {
            val response = meteoApiService.getForecast(latitude, longitude)
            val dailyForecast = mapResponseToDailyWeather(response)
            val currentWeather = CurrentWeather(
                temperature = response.current.temperature_2m,
                windSpeed = response.current.wind_speed_10m,
                weatherType = WeatherType.fromWMO(response.current.weathercode)
            )
            WeatherReport(current = currentWeather, daily = dailyForecast)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun mapResponseToDailyWeather(response: ForecastResponse): List<DailyWeather> {
        val dailyData = response.daily
        val hourlyData = response.hourly

        val allHourlyWeather = hourlyData.time.indices.mapNotNull { index ->
            hourlyData.time.getOrNull(index)?.let { time ->
                HourlyWeather(
                    time = time,
                    temperature = hourlyData.temperature_2m.getOrElse(index) { 0.0 },
                    humidity = hourlyData.relative_humidity_2m.getOrElse(index) { 0.0 },
                    precipitationProbability = hourlyData.precipitation_probability.getOrElse(index) { 0 },
                    weatherType = WeatherType.fromWMO(hourlyData.weathercode.getOrElse(index) { 0 }),
                    windSpeed = hourlyData.wind_speed_10m.getOrElse(index) { 0.0 },
                    windDirection = degreesToCardinal(hourlyData.wind_direction_10m.getOrElse(index) { 0 }),
                    windGusts = hourlyData.wind_gusts_10m.getOrElse(index) { 0.0 }
                )
            }
        }

        val hourlyWeatherByDay = allHourlyWeather.groupBy { it.time.substring(0, 10) }

        return dailyData.time.indices.mapNotNull { index ->
            dailyData.time.getOrNull(index)?.let { date ->
                DailyWeather(
                    date = date,
                    weatherType = WeatherType.fromWMO(dailyData.weathercode.getOrElse(index) { 0 }),
                    maxTemp = dailyData.temperature_2m_max.getOrElse(index) { 0.0 },
                    minTemp = dailyData.temperature_2m_min.getOrElse(index) { 0.0 },
                    precipitationSum = dailyData.precipitation_sum.getOrElse(index) { 0.0 },
                    maxWindSpeed = dailyData.wind_speed_10m_max.getOrElse(index) { 0.0 },
                    maxWindGusts = dailyData.wind_gusts_10m_max.getOrElse(index) { 0.0 },
                    dominantWindDirection = degreesToCardinal(dailyData.wind_direction_10m_dominant.getOrElse(index) { 0 }),
                    hourly = hourlyWeatherByDay[date] ?: emptyList()
                )
            }
        }
    }

    private fun degreesToCardinal(d: Int): String {
        val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = ((d + 22.5) / 45).toInt() % 8
        return directions[index]
    }
}

// --- NUEVOS MODELOS DE DATOS PARA LA UI ---

data class WeatherReport(
    val current: CurrentWeather,
    val daily: List<DailyWeather>
)

data class CurrentWeather(
    val temperature: Double,
    val windSpeed: Double,
    val weatherType: WeatherType
)

data class DailyWeather(
    val date: String,
    val weatherType: WeatherType,
    val maxTemp: Double,
    val minTemp: Double,
    val precipitationSum: Double,
    val maxWindSpeed: Double,
    val maxWindGusts: Double,
    val dominantWindDirection: String,
    val hourly: List<HourlyWeather>
)

data class HourlyWeather(
    val time: String,
    val temperature: Double,
    val humidity: Double,
    val precipitationProbability: Int,
    val weatherType: WeatherType,
    val windSpeed: Double,
    val windDirection: String,
    val windGusts: Double
)

