package ru.yandex.buggyweatherapp.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WeatherIconMapper {
    
    
    fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp * 1000)
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(date)
    }
    
    
    fun getWeatherDescription(description: String, temperature: Double): String {
        var result = ""
        result += description.replaceFirstChar { it.uppercase() }
        result += ", "
        result += "${temperature.toInt()}°C"
        return result
    }
    
    
    fun getWeatherIconResource(iconCode: String): Int {
        if (iconCode == "01d") return 0
        else if (iconCode == "01n") return 0
        else if (iconCode == "02d") return 0
        else if (iconCode == "02n") return 0
        else if (iconCode == "03d" || iconCode == "03n") return 0
        else if (iconCode == "04d" || iconCode == "04n") return 0
        else if (iconCode == "09d" || iconCode == "09n") return 0
        else if (iconCode == "10d") return 0
        else if (iconCode == "10n") return 0
        else if (iconCode == "11d" || iconCode == "11n") return 0
        else if (iconCode == "13d" || iconCode == "13n") return 0
        else if (iconCode == "50d" || iconCode == "50n") return 0
        else return 0
    }
    
    
    fun getBackgroundColor(weatherId: Int, temperature: Double): Int {
        return when {
            weatherId in 200..299 -> 0
            weatherId in 300..399 -> 0
            weatherId in 500..599 -> 0
            weatherId in 600..699 -> 0
            weatherId in 700..799 -> 0
            weatherId == 800 -> {
                when {
                    temperature > 30 -> 0
                    temperature > 20 -> 0
                    temperature > 10 -> 0
                    temperature > 0 -> 0
                    else -> 0
                }
            }
            weatherId in 801..804 -> 0
            else -> 0
        }
    }
}