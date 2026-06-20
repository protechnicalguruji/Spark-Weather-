package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.CurrentWeather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiSummaryService {
    private const val TAG = "GeminiSummaryService"
    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Call the Gemini API to get an elegant weather briefing
     */
    suspend fun fetchWeatherSummary(weather: CurrentWeather, tempUnit: String): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is unconfigured. Returning premium fallback summary.")
            return@withContext getPremiumFallbackSummary(weather, tempUnit)
        }

        val prompt = """
            You are "Spark Station Weather's" elite AI weather system assistant.
            Provide a luxurious, elegant, extremely poetic yet scientific 2-3 sentence overview of the current weather condition in ${weather.cityName}.
            Make it sound like a premium concierge service or a high-end futuristic OS (inspired by Apple design and VisionOS luxury).
            
            Current details:
            - City: ${weather.cityName}
            - Temp: ${weather.temp}$tempUnit (Feels like: ${weather.feelsLike}$tempUnit)
            - Conditions: ${weather.condition.main} (${weather.condition.description})
            - Humidity: ${weather.humidity}%
            - Wind Speed: ${weather.windSpeed}
            - UV Index: ${weather.uvIndex}
            - Air Quality: ${weather.airQuality}
            - Moon Phase: ${weather.moonPhase}
            - Dew Point: ${weather.dewPoint}$tempUnit
            
            Speak directly to the observer. Use phrases that evoke light, glass, atmosphere, and state of nature.
            Do not include Markdown bold asterisks inside the sentences. Keep the summary incredibly clean and concise.
        """.trimIndent()

        try {
            // Build direct REST payload
            val root = JSONObject()
            val contentsArr = JSONArray()
            val contentObj = JSONObject()
            val partsArr = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArr.put(partObj)
            contentObj.put("parts", partsArr)
            contentsArr.put(contentObj)
            root.put("contents", contentsArr)

            // Generation config
            val generationConfig = JSONObject()
            generationConfig.put("temperature", 0.7)
            generationConfig.put("maxOutputTokens", 250)
            root.put("generationConfig", generationConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = root.toString().toRequestBody(mediaType)

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Request failed: Code ${response.code}, Body: $errBody")
                    return@withContext getPremiumFallbackSummary(weather, tempUnit)
                }

                val respStr = response.body?.string() ?: ""
                val respJson = JSONObject(respStr)
                val candidates = respJson.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                val text = parts.getJSONObject(0).getString("text")
                
                return@withContext text.trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Gemini summary: ${e.message}", e)
            return@withContext getPremiumFallbackSummary(weather, tempUnit)
        }
    }

    private fun getPremiumFallbackSummary(weather: CurrentWeather, tempUnit: String): String {
        return when (weather.condition.main) {
            "Sunny" -> "The solar atmosphere washes over ${weather.cityName} in clean radiance with a comfortable ${weather.feelsLike}$tempUnit perception. Ideal conditions register a balanced UV Index of ${weather.uvIndex}, inviting tranquil outdoor excursions beneath a pristine clear canopy."
            "Cloudy" -> "Soft frosted vapor screens the skies of ${weather.cityName}, casting a diffuse, elegant light. A gentle ${weather.windSpeed} breeze maintains a quiet harmony, ideal for peaceful reflection as the dew point hovers near ${String.format("%.1f", weather.dewPoint)}$tempUnit."
            "Rain" -> "A serene liquid rainfall embraces ${weather.cityName}, draping the streets in reflecting glass. High moisture content at ${weather.humidity}% nourishes the surrounding flora under a peaceful, overcast atmospheric calm."
            "Storm" -> "A majestic, highly charged electric tempest moves through the region. Dynamic pressure vectors drop to ${weather.pressure} hPa, bringing dramatic energy patterns and sweeping, refreshing winds that thoroughly cleanse the localized air quality."
            "Snow" -> "A silent, pristine cascade of powder snow layers ${weather.cityName} in soft white insulation. Thermal index reads a crisp ${weather.temp}$tempUnit under a still, crystalline winter sky."
            "Night" -> "A majestic lunar glow illuminates ${weather.cityName} tonight, displaying a prominent ${weather.moonPhase}. Clear stargazing coordinates are enhanced by an ultra-premium visibility of ${weather.visibility} km."
            else -> "A balanced atmospheric serenity graces ${weather.cityName} today with a current thermal state of ${weather.temp}$tempUnit. Air currents remain soft and refreshing, keeping the regional layout pleasant and clean."
        }
    }
}
