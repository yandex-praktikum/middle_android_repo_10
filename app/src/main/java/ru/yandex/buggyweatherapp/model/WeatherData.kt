package ru.yandex.buggyweatherapp.model

// ОШИБКА: Класс данных со слишком большим количеством полей (Божественный объект)
data class WeatherData(
    val cityName: String,
    val country: String,
    val temperature: Double,
    val feelsLike: Double,
    val minTemp: Double,
    val maxTemp: Double,
    val humidity: Int,
    val pressure: Int,
    val windSpeed: Double,
    val windDirection: Int,
    val description: String,
    val icon: String,
    val rain: Double? = null,
    val snow: Double? = null,
    val cloudiness: Int,
    val sunriseTime: Long,
    val sunsetTime: Long,
    val timezone: Int,
    val timestamp: Long,
    
    // ОШИБКА: Сырые данные API без правильной обработки или форматирования
    val rawApiData: String,
    
    // ОШИБКА: Смешивание данных и состояния UI в одном классе
    var isFavorite: Boolean = false,
    var isSelected: Boolean = false
)