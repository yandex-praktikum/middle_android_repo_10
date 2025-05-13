package ru.yandex.buggyweatherapp.api

import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Query
import ru.yandex.buggyweatherapp.BuildConfig

/**
 * Интерфейс для работы с API погоды.
 * Обновлено: заменены Call<> на suspend функции для более естественной работы с корутинами.
 */
interface WeatherApiService {

    companion object {
        // API ключ берется из BuildConfig, который получает значение из local.properties
        // Эти свойства закрыты и не хранятся в системе контроля версий
        private const val API_KEY = BuildConfig.WEATHER_API_KEY
        const val BASE_URL = "https://api.openweathermap.org/data/2.5/"

        // Публичный метод для получения API ключа, который можно использовать только изнутри
        fun getApiKey(): String {
            return API_KEY
        }
    }
    
    /**
     * Получение текущей погоды по координатам.
     * Преобразовано в suspend функцию для прямой интеграции с корутинами.
     */
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String = getApiKey(),
        @Query("units") units: String = "metric"
    ): JsonObject
    
    /**
     * Получение погоды по названию города.
     * Преобразовано в suspend функцию для прямой интеграции с корутинами.
     */
    @GET("weather")
    suspend fun getWeatherByCity(
        @Query("q") cityName: String,
        @Query("appid") apiKey: String = getApiKey(),
        @Query("units") units: String = "metric"
    ): JsonObject
    
    /**
     * Получение прогноза погоды по координатам.
     * Преобразовано в suspend функцию для прямой интеграции с корутинами.
     */
    @GET("forecast")
    suspend fun getForecast(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String = getApiKey(),
        @Query("units") units: String = "metric"
    ): JsonObject
}