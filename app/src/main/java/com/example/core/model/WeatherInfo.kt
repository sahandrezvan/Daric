package com.example.core.model

data class WeatherInfo(
    val city: String = "تهران",
    val temperature: Double? = null,
    val apparentTemperature: Double? = null,
    val weatherCode: Int? = null,
    val updatedAt: Long = 0L
)
