package com.tms.os.launcher.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

/** Next hours and days, as shown by the medium and large iOS Weather widgets. */
data class WeatherForecast(
    val hourly: List<HourForecast>,
    val daily: List<DayForecast>
)
data class HourForecast(val epochMillis: Long, val tempCelsius: Int, val weatherCode: Int)
data class DayForecast(val epochMillis: Long, val highCelsius: Int, val lowCelsius: Int, val weatherCode: Int)

data class LiveWeatherData(
    val resolvedCity: String,
    val tempCelsius: Int,
    val highCelsius: Int,
    val lowCelsius: Int,
    val weatherCode: Int,
    val conditionDe: String,
    val forecast: WeatherForecast? = null
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
                    "&hourly=temperature_2m,weather_code" +
                    "&daily=temperature_2m_max,temperature_2m_min,weather_code" +
                    "&timezone=auto&forecast_days=7&timeformat=unixtime"

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

            val forecast = runCatching {
                val now = System.currentTimeMillis()
                val h = wRoot.getJSONObject("hourly")
                val ht = h.getJSONArray("time"); val htemp = h.getJSONArray("temperature_2m"); val hcode = h.getJSONArray("weather_code")
                val hours = (0 until ht.length()).map { i ->
                    HourForecast(ht.getLong(i) * 1000, htemp.getDouble(i).roundToInt(), hcode.getInt(i))
                }.filter { it.epochMillis >= now - 3_600_000 }.take(6)
                val d = wRoot.getJSONObject("daily")
                val dt = d.getJSONArray("time"); val dmax = d.getJSONArray("temperature_2m_max")
                val dmin = d.getJSONArray("temperature_2m_min"); val dcode = d.getJSONArray("weather_code")
                val days = (0 until dt.length()).map { i ->
                    DayForecast(dt.getLong(i) * 1000, dmax.getDouble(i).roundToInt(), dmin.getDouble(i).roundToInt(), dcode.getInt(i))
                }
                WeatherForecast(hours, days)
            }.getOrNull()

            LiveWeatherData(
                forecast = forecast,
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
