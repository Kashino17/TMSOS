package com.tms.os.launcher.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

data class LiveWeatherData(
    val resolvedCity: String,
    val tempCelsius: Int,
    val highCelsius: Int,
    val lowCelsius: Int,
    val weatherCode: Int,
    val conditionDe: String
)

object WeatherService {

    suspend fun fetchLiveWeatherForCity(cityQuery: String): LiveWeatherData? = withContext(Dispatchers.IO) {
        try {
            val cleanCity = cityQuery.trim().ifEmpty { "Berlin" }
            val encoded = URLEncoder.encode(cleanCity, "UTF-8")

            // 1. Geocode city name via Open-Meteo Geocoding API
            val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=de&format=json"
            val geoJsonStr = httpGet(geoUrl) ?: return@withContext null
            val geoRoot = JSONObject(geoJsonStr)
            val results = geoRoot.optJSONArray("results") ?: return@withContext null
            if (results.length() == 0) return@withContext null

            val firstPlace = results.getJSONObject(0)
            val lat = firstPlace.getDouble("latitude")
            val lon = firstPlace.getDouble("longitude")
            val resolvedName = firstPlace.optString("name", cleanCity)

            // 2. Fetch current weather & daily high/low from Open-Meteo Forecast API
            val weatherUrl =
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,weather_code" +
                    "&daily=temperature_2m_max,temperature_2m_min" +
                    "&timezone=auto&forecast_days=1"

            val weatherJsonStr = httpGet(weatherUrl) ?: return@withContext null
            val wRoot = JSONObject(weatherJsonStr)
            val current = wRoot.optJSONObject("current") ?: return@withContext null
            val daily = wRoot.optJSONObject("daily")

            val temp = current.optDouble("temperature_2m", 21.0).roundToInt()
            val code = current.optInt("weather_code", 1)

            val maxArr = daily?.optJSONArray("temperature_2m_max")
            val minArr = daily?.optJSONArray("temperature_2m_min")
            val high = if (maxArr != null && maxArr.length() > 0) maxArr.getDouble(0).roundToInt() else temp + 3
            val low = if (minArr != null && minArr.length() > 0) minArr.getDouble(0).roundToInt() else temp - 5

            LiveWeatherData(
                resolvedCity = resolvedName,
                tempCelsius = temp,
                highCelsius = high,
                lowCelsius = low,
                weatherCode = code,
                conditionDe = wmoCodeToGerman(code)
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun httpGet(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    fun wmoCodeToGerman(code: Int): String = when (code) {
        0 -> "Klar & Sonnig"
        1 -> "Überwiegend Klar"
        2 -> "Teilweise Bewölkt"
        3 -> "Bedeckt"
        45, 48 -> "Neblig"
        51, 53, 55 -> "Nieselregen"
        61, 63, 65 -> "Regen"
        71, 73, 75, 77 -> "Schneefall"
        80, 81, 82 -> "Regenschauer"
        95, 96, 99 -> "Gewitter"
        else -> "Heiter"
    }
}
