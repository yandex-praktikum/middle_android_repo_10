package ru.yandex.buggyweatherapp.model

/**
 * Класс данных для представления информации о погоде.
 * Содержит только данные о погоде, без состояния UI.
 */
data class WeatherData(
    // Основная информация о местоположении
    val cityName: String,
    val country: String,
    val latitude: Double? = null,
    val longitude: Double? = null,

    // Основные данные о погоде
    val temperature: Double,
    val feelsLike: Double,
    val minTemp: Double,
    val maxTemp: Double,
    val humidity: Int,
    val pressure: Int,

    // Данные о ветре
    val windSpeed: Double,
    val windDirection: Int,

    // Описательные данные
    val description: String,
    val icon: String,

    // Осадки
    val rain: Double? = null,
    val snow: Double? = null,
    val cloudiness: Int,

    // Временные данные
    val sunriseTime: Long,
    val sunsetTime: Long,
    val timezone: Int,
    val timestamp: Long,
    
    // Сырые данные для отладки (можно удалить в production)
    val rawApiData: String
) {
    /**
     * Преобразует данные о погоде в UI модель.
     * @param isSelected флаг выбранности
     * @return WeatherUiModel для отображения в UI
     */
    fun toUiModel(isSelected: Boolean = false): WeatherUiModel {
        return WeatherUiModel(
            weatherData = this,
            isSelected = isSelected
        )
    }
}

/**
 * UI модель для погоды, которая содержит как данные о погоде,
 * так и состояние UI (выбранность и т.д.).
 */
data class WeatherUiModel(
    val weatherData: WeatherData,
    var isSelected: Boolean = false
) {
    // Делегирование свойств данных о погоде
    val cityName: String get() = weatherData.cityName
    val country: String get() = weatherData.country
    val latitude: Double? get() = weatherData.latitude
    val longitude: Double? get() = weatherData.longitude
    val temperature: Double get() = weatherData.temperature
    val feelsLike: Double get() = weatherData.feelsLike
    val minTemp: Double get() = weatherData.minTemp
    val maxTemp: Double get() = weatherData.maxTemp
    val humidity: Int get() = weatherData.humidity
    val pressure: Int get() = weatherData.pressure
    val windSpeed: Double get() = weatherData.windSpeed
    val windDirection: Int get() = weatherData.windDirection
    val description: String get() = weatherData.description
    val icon: String get() = weatherData.icon
    val rain: Double? get() = weatherData.rain
    val snow: Double? get() = weatherData.snow
    val cloudiness: Int get() = weatherData.cloudiness
    val sunriseTime: Long get() = weatherData.sunriseTime
    val sunsetTime: Long get() = weatherData.sunsetTime
    val timezone: Int get() = weatherData.timezone
    val timestamp: Long get() = weatherData.timestamp

    /**
     * Создает копию UI модели с обновленными UI состояниями.
     * @param isSelected новое значение для выбранности (null - не изменять)
     * @return Новый экземпляр WeatherUiModel с обновленными значениями
     */
    fun copy(isSelected: Boolean? = null): WeatherUiModel {
        return WeatherUiModel(
            weatherData = this.weatherData,
            isSelected = isSelected ?: this.isSelected
        )
    }
}