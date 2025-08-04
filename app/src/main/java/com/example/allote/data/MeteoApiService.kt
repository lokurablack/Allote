package com.example.allote.data

import retrofit2.http.GET
import retrofit2.http.Query

interface MeteoApiService {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,weathercode,wind_speed_10m",
        @Query("daily") daily: String = "weathercode,temperature_2m_max,temperature_2m_min,precipitation_sum,wind_speed_10m_max,wind_gusts_10m_max,wind_direction_10m_dominant",
        @Query("hourly") hourly: String = "temperature_2m,relative_humidity_2m,precipitation_probability,weathercode,wind_speed_10m,wind_direction_10m,wind_gusts_10m", // Añadido weathercode
        @Query("timezone") timezone: String = "auto"
    ): ForecastResponse
}

data class ForecastResponse(
    val current: CurrentWeatherResponse,
    val daily: DailyForecast,
    val hourly: HourlyForecast
)

data class CurrentWeatherResponse(
    val time: String,
    val temperature_2m: Double,
    val weathercode: Int,
    val wind_speed_10m: Double
)

data class DailyForecast(
    val time: List<String>,
    val weathercode: List<Int>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>,
    val precipitation_sum: List<Double>,
    val wind_speed_10m_max: List<Double>,
    val wind_gusts_10m_max: List<Double>,
    val wind_direction_10m_dominant: List<Int>
)

data class HourlyForecast(
    val time: List<String>,
    val temperature_2m: List<Double>,
    val relative_humidity_2m: List<Double>,
    val precipitation_probability: List<Int>,
    val weathercode: List<Int>, // Añadido weathercode
    val wind_speed_10m: List<Double>,
    val wind_direction_10m: List<Int>,
    val wind_gusts_10m: List<Double>
)
