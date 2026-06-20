package com.example.data.api

import com.example.data.model.CurrentWeather
import com.example.data.model.DailyForecast
import com.example.data.model.HourlyForecast
import com.example.data.model.WeatherCondition
import com.example.data.model.WeatherReport
import kotlin.math.abs
import kotlin.math.sin
import java.util.Calendar

object WeatherApiService {

    // Preset list of ultimate elite high-performance search cities
    val popularCities = listOf(
        PresetCity("Tokyo", "Japan", 35.6762, 139.6503, "Cloudy"),
        PresetCity("Reykjavik", "Iceland", 64.1466, -21.9426, "Snow"),
        PresetCity("Zermatt", "Switzerland", 46.0207, 7.7491, "Snow"),
        PresetCity("San Francisco", "United States", 37.7749, -122.4194, "Sunny"),
        PresetCity("Paris", "France", 48.8566, 2.3522, "Cloudy"),
        PresetCity("Sydney", "Australia", -33.8688, 151.2093, "Sunny"),
        PresetCity("London", "United Kingdom", 51.5074, -0.1278, "Rain"),
        PresetCity("Cairo", "Egypt", 30.0444, 31.2357, "Sunny"),
        PresetCity("New York", "United States", 40.7128, -74.0060, "Storm"),
        PresetCity("Aurora Station", "Lofoten", 68.1670, 13.7129, "Night") // Night + potential aurora
    )

    data class PresetCity(
        val name: String,
        val country: String,
        val lat: Double,
        val lon: Double,
        val defaultCondition: String
    )

    fun searchCities(query: String): List<PresetCity> {
        if (query.trim().isEmpty()) return popularCities
        return (popularCities + extraCities).filter {
            it.name.contains(query, ignoreCase = true) || it.country.contains(query, ignoreCase = true)
        }
    }

    private val extraCities = listOf(
        PresetCity("Singapore", "Singapore", 1.3521, 103.8198, "Rain"),
        PresetCity("Dubai", "United Arab Emirates", 25.2048, 55.2708, "Sunny"),
        PresetCity("Cape Town", "South Africa", -33.9249, 18.4241, "Sunny"),
        PresetCity("Rio de Janeiro", "Brazil", -22.9068, -43.1729, "Cloudy"),
        PresetCity("Mumbai", "India", 19.0760, 72.8777, "Storm"),
        PresetCity("Vancouver", "Canada", 49.2827, -123.1207, "Rain"),
        PresetCity("Athens", "Greece", 37.9838, 23.7275, "Sunny"),
        PresetCity("Rome", "Italy", 41.9028, 12.4964, "Sunny"),
        PresetCity("Honolulu", "Hawaii", 21.3069, -157.8583, "Sunny"),
        PresetCity("Tromsø", "Norway", 69.6492, 18.9553, "Night")
    )

    /**
     * Compute realistic weather report on-the-fly dynamically based on coordinate, time parameter, and default condition
     */
    fun getWeatherReport(cityName: String, lat: Double, lon: Double): WeatherReport {
        // Choose weather condition based on coordinate hashing or seed
        val baseCondition = getConditionFromCoordinate(cityName, lat, lon)
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val isNight = hour < 6 || hour > 19

        val finalCondition = if (isNight && baseCondition == "Sunny") "Night" else baseCondition

        // Base values influenced by latitude
        val baseTemp = 28f - abs(lat.toFloat()) * 0.4f // Colder near poles
        val tempMultiplier = when (finalCondition) {
            "Sunny" -> 1.1f
            "Storm" -> 0.8f
            "Rain" -> 0.85f
            "Snow" -> 0.2f
            "Cloudy" -> 0.95f
            else -> 0.9f
        }
        val currentTemp = baseTemp * tempMultiplier
        val high = currentTemp + 3f
        val low = currentTemp - 4f

        val weatherCondition = when (finalCondition) {
            "Sunny" -> WeatherCondition("Sunny", "Glorious Clear Sky", "01d")
            "Cloudy" -> WeatherCondition("Cloudy", "Overcast Frosted Clouds", "03d")
            "Rain" -> WeatherCondition("Rain", "Serene Liquid Rainfall", "09d")
            "Storm" -> WeatherCondition("Storm", "Cinematic Electric Tempest", "11d")
            "Snow" -> WeatherCondition("Snow", "Pristine Powder Snowflakes", "13d")
            "Night" -> WeatherCondition("Night", "Celestial Moonlit Canopy", "01n")
            else -> WeatherCondition("Sunny", "Clear Sky", "01d")
        }

        // Generate variables
        val humidity = when (finalCondition) {
            "Rain", "Storm" -> 88 + (cityName.length % 10)
            "Snow" -> 72 + (cityName.length % 10)
            "Cloudy" -> 60 + (cityName.length % 10)
            else -> 42 + (cityName.length % 10)
        }

        val windSpeed = 8.5f + (cityName.length % 15).toFloat() * 1.5f + if (finalCondition == "Storm") 20f else 0f
        val pressure = 1013 - (cityName.length % 12) + if (finalCondition == "Storm") -18 else if (finalCondition == "Sunny") 6 else 0
        val visibility = when (finalCondition) {
            "Storm" -> 4.2f
            "Rain" -> 8.0f
            "Snow" -> 6.5f
            "Cloudy" -> 12.0f
            else -> 16.0f
        }

        val uvIndex = when (finalCondition) {
            "Sunny" -> 8
            "Cloudy" -> 3
            "Night" -> 0
            else -> 1
        }

        val airQuality = when {
            windSpeed > 30f -> "Fair"
            cityName.length % 3 == 0 -> "Good"
            cityName.length % 3 == 1 -> "Fair"
            else -> "Moderate"
        }

        val sunrise = "05:42 AM"
        val sunset = "08:14 PM"

        val moonPhase = when (calendar.get(Calendar.DAY_OF_MONTH) % 8) {
            0 -> "New Moon"
            1 -> "Waxing Crescent"
            2 -> "First Quarter"
            3 -> "Waxing Gibbous"
            4 -> "Full Moon"
            5 -> "Waning Gibbous"
            6 -> "Third Quarter"
            else -> "Waning Crescent"
        }

        val dewPoint = currentTemp - ((100f - humidity) / 5f)

        val current = CurrentWeather(
            cityName = cityName,
            temp = currentTemp,
            feelsLike = currentTemp - 0.5f + (cityName.length % 3) - if (windSpeed > 15) 1.5f else 0f,
            high = high,
            low = low,
            condition = weatherCondition,
            humidity = humidity,
            windSpeed = windSpeed,
            pressure = pressure,
            visibility = visibility,
            uvIndex = uvIndex,
            airQuality = airQuality,
            sunrise = sunrise,
            sunset = sunset,
            moonPhase = moonPhase,
            dewPoint = dewPoint
        )

        // Generate 24 Hourly Forecast (stretching out from current hour)
        val hourly = (0..23).map { index ->
            val forecastHour = (hour + index) % 24
            val forecastHourStr = when {
                forecastHour == 0 -> "12 AM"
                forecastHour == 12 -> "12 PM"
                forecastHour > 12 -> "${forecastHour - 12} PM"
                else -> "$forecastHour AM"
            }
            val hourDist = abs(forecastHour - 14) // Distance from peak heat at 2 PM
            val tempVariation = currentTemp + (4f - hourDist * 0.8f) + (sin(index.toDouble() / 2.0) * 1.5).toFloat()
            
            val forecastCondition = if (forecastHour > 19 || forecastHour < 6) {
                if (weatherCondition.main == "Sunny") "Night" else weatherCondition.main
            } else {
                if (weatherCondition.main == "Night") "Sunny" else weatherCondition.main
            }

            val iconCode = when (forecastCondition) {
                "Sunny" -> "01d"
                "Cloudy" -> "03d"
                "Rain" -> "09d"
                "Storm" -> "11d"
                "Snow" -> "13d"
                "Night" -> "01n"
                else -> "02d"
            }

            HourlyForecast(
                time = if (index == 0) "Now" else forecastHourStr,
                temp = tempVariation,
                condition = WeatherCondition(forecastCondition, weatherCondition.description, iconCode),
                chanceOfRain = when (forecastCondition) {
                    "Rain" -> 80 + (index % 5) * 4
                    "Storm" -> 90 + (index % 3) * 5
                    "Snow" -> 40 + (index % 4) * 10
                    "Cloudy" -> 15 + (index % 6) * 5
                    else -> 0
                }
            )
        }

        // Generate 10 Day Forecast
        val days = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val dayIndex = calendar.get(Calendar.DAY_OF_WEEK) - 1
        
        val daily = (1..10).map { index ->
            val dayName = if (index == 1) "Today" else days[(dayIndex + index) % 7]
            val randomOffset = (sin(index.toDouble() + cityName.length) * 2.5).toFloat()
            val dayHigh = high + randomOffset
            val dayLow = low + randomOffset - 2f

            val dayConditionStr = when ((index + cityName.length) % 6) {
                0 -> "Sunny"
                1 -> "Cloudy"
                2 -> "Rain"
                3 -> "Storm"
                4 -> "Snow"
                else -> baseCondition
            }

            val dayIconCode = when (dayConditionStr) {
                "Sunny" -> "01d"
                "Cloudy" -> "03d"
                "Rain" -> "09d"
                "Storm" -> "11d"
                "Snow" -> "13d"
                else -> "02d"
            }

            DailyForecast(
                dayOfWeek = dayName,
                condition = WeatherCondition(dayConditionStr, "Slightly varied", dayIconCode),
                high = dayHigh,
                low = dayLow,
                chanceOfRain = when (dayConditionStr) {
                    "Rain", "Storm" -> 70 + (index * 3) % 25
                    "Snow" -> 30 + (index * 7) % 30
                    "Cloudy" -> 10 + (index * 5) % 20
                    else -> 0
                }
            )
        }

        return WeatherReport(current, hourly, daily)
    }

    private fun getConditionFromCoordinate(cityName: String, lat: Double, lon: Double): String {
        return when {
            cityName.contains("Iceland", ignoreCase = true) || cityName.contains("Reykjavik", ignoreCase = true) || cityName.contains("Zermatt", ignoreCase = true) || lat > 60 -> "Snow"
            cityName.contains("London", ignoreCase = true) || cityName.contains("Singapore", ignoreCase = true) || cityName.contains("Vancouver", ignoreCase = true) -> "Rain"
            cityName.contains("Mumbai", ignoreCase = true) || cityName.contains("tempest", ignoreCase = true) || cityName.contains("Storm", ignoreCase = true) -> "Storm"
            cityName.contains("Tromsø", ignoreCase = true) || cityName.contains("Aurora", ignoreCase = true) -> "Night"
            cityName.length % 5 == 0 -> "Cloudy"
            cityName.length % 5 == 1 -> "Sunny"
            cityName.length % 5 == 2 -> "Rain"
            cityName.length % 5 == 3 -> "Storm"
            else -> "Sunny"
        }
    }
}
