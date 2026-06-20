package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

data class WeatherCondition(
    val main: String, // "Sunny", "Cloudy", "Rain", "Storm", "Snow", "Night"
    val description: String,
    val iconId: String
)

data class CurrentWeather(
    val cityName: String,
    val temp: Float,
    val feelsLike: Float,
    val high: Float,
    val low: Float,
    val condition: WeatherCondition,
    val humidity: Int, // %
    val windSpeed: Float, // km/h or mph
    val pressure: Int, // hPa
    val visibility: Float, // km or miles
    val uvIndex: Int,
    val airQuality: String, // Good, Fair, Moderate, Poor, Very Poor
    val sunrise: String,
    val sunset: String,
    val moonPhase: String,
    val dewPoint: Float
)

data class HourlyForecast(
    val time: String,
    val temp: Float,
    val condition: WeatherCondition,
    val chanceOfRain: Int // %
)

data class DailyForecast(
    val dayOfWeek: String, // e.g. "Monday"
    val condition: WeatherCondition,
    val high: Float,
    val low: Float,
    val chanceOfRain: Int // %
)

data class WeatherReport(
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>
)

@Entity(tableName = "saved_cities")
data class SavedCity(
    @PrimaryKey
    val cityName: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val currentTemp: Float,
    val conditionMain: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class UserSettings(
    val isDarkMode: Boolean = true, // Deep premium translucent is dark by default
    val useSystemTheme: Boolean = false,
    val tempUnit: String = "°C", // "°C" or "°F"
    val windUnit: String = "km/h", // "km/h", "mph", "m/s"
    val language: String = "English",
    val notificationsEnabled: Boolean = true,
    val locationAllowed: Boolean = false,
    val alertsEnabled: Boolean = true
)
