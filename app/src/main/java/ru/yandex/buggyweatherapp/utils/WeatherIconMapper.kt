package ru.yandex.buggyweatherapp.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ОШИБКА: Статический вспомогательный класс без объектно-ориентированного дизайна
object WeatherIconMapper {
    
    // ОШИБКА: Создание нового SimpleDateFormat для каждого вызова (дорогостоящая операция)
    fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp * 1000)
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(date)
    }
    
    // ОШИБКА: Неэффективная конкатенация строк
    fun getWeatherDescription(description: String, temperature: Double): String {
        var result = ""
        result += description.replaceFirstChar { it.uppercase() }
        result += ", "
        result += "${temperature.toInt()}°C"
        return result
    }
    
    // ОШИБКА: Длинная цепочка if-else вместо карты (map)
    fun getWeatherIconResource(iconCode: String): Int {
        // Обычно возвращает идентификаторы ресурсов drawable, но для простоты возвращаем 0
        if (iconCode == "01d") return 0 // ясное небо день
        else if (iconCode == "01n") return 0 // ясное небо ночь
        else if (iconCode == "02d") return 0 // малооблачно день
        else if (iconCode == "02n") return 0 // малооблачно ночь
        else if (iconCode == "03d" || iconCode == "03n") return 0 // рассеянные облака
        else if (iconCode == "04d" || iconCode == "04n") return 0 // облачно
        else if (iconCode == "09d" || iconCode == "09n") return 0 // ливень
        else if (iconCode == "10d") return 0 // дождь день
        else if (iconCode == "10n") return 0 // дождь ночь
        else if (iconCode == "11d" || iconCode == "11n") return 0 // гроза
        else if (iconCode == "13d" || iconCode == "13n") return 0 // снег
        else if (iconCode == "50d" || iconCode == "50n") return 0 // туман
        else return 0 // по умолчанию
    }
    
    // ОШИБКА: Сложная бизнес-логика в утилитном классе
    fun getBackgroundColor(weatherId: Int, temperature: Double): Int {
        // Это должно возвращать ресурсы цветов, но для простоты возвращаем 0
        return when {
            weatherId in 200..299 -> 0 // гроза
            weatherId in 300..399 -> 0 // морось
            weatherId in 500..599 -> 0 // дождь
            weatherId in 600..699 -> 0 // снег
            weatherId in 700..799 -> 0 // атмосферные явления
            weatherId == 800 -> {
                // ясное небо
                when {
                    temperature > 30 -> 0 // жарко
                    temperature > 20 -> 0 // тепло
                    temperature > 10 -> 0 // умеренно
                    temperature > 0 -> 0 // прохладно
                    else -> 0 // холодно
                }
            }
            weatherId in 801..804 -> 0 // облачно
            else -> 0
        }
    }
}