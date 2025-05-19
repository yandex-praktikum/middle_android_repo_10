package ru.yandex.buggyweatherapp.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WeatherIconMapper {
    
    // ThreadLocal для безопасности использования в многопоточной среде
    private val dateFormatter = ThreadLocal.withInitial {
        SimpleDateFormat("HH:mm", Locale.getDefault())
    }
    
    fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp * 1000)
        return dateFormatter.get().format(date)
    }
    
    fun getWeatherDescription(description: String, temperature: Double): String {
        return buildString {
            append(description.replaceFirstChar { it.uppercase() })
            append(", ")
            append("${temperature.toInt()}°C")
        }
    }
    
    fun getIconUrl(iconCode: String): String {
        return "https://openweathermap.org/img/wn/$iconCode@2x.png"
    }
}