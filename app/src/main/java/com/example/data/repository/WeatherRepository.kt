package com.example.data.repository

import com.example.core.model.WeatherInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WeatherRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val _weather = MutableStateFlow(WeatherInfo())
    val weather: StateFlow<WeatherInfo> = _weather.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://api.open-meteo.com/v1/forecast?latitude=35.6892&longitude=51.3890&current=temperature_2m,apparent_temperature,weather_code&timezone=Asia%2FTehran")
                .header("Accept", "application/json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("weather http ${response.code}")
                val current = JSONObject(response.body?.string().orEmpty()).getJSONObject("current")
                _weather.value = WeatherInfo(
                    temperature = current.getDouble("temperature_2m"),
                    apparentTemperature = current.optDouble("apparent_temperature"),
                    weatherCode = current.getInt("weather_code"),
                    updatedAt = System.currentTimeMillis()
                )
            }
        }
    }
}
