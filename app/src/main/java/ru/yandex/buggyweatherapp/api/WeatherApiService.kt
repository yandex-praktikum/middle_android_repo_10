package ru.yandex.buggyweatherapp.api

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import ru.yandex.buggyweatherapp.BuildConfig

/**
 * Интерфейс для взаимодействия с API OpenWeatherMap.
 * 
 * Изменения:
 * 1. Использование HTTPS вместо HTTP для безопасных запросов
 * 2. Использование BuildConfig для хранения API ключа вместо жесткого кодирования
 * 3. Методы API переделаны в suspend-функции для использования с корутинами
 * 4. Добавлена типизация возвращаемых значений
 * 5. Параметры разделены для лучшей читаемости и возможности замены значений по умолчанию
 */
interface WeatherApiService {
    
    companion object {
        /**
         * API ключ, получаемый из BuildConfig для безопасности.
         * Избегаем хранения ключа в коде.
         */
        const val API_KEY = BuildConfig.WEATHER_API_KEY
        
        /**
         * Базовый URL API с HTTPS для безопасных соединений.
         * Ранее использовался небезопасный HTTP.
         */
        const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
    }
    
    /**
     * Получает текущую погоду по координатам.
     * 
     * @param latitude Широта местоположения
     * @param longitude Долгота местоположения
     * @param apiKey API ключ OpenWeatherMap
     * @param units Единицы измерения (metric = градусы Цельсия)
     * @return Ответ API с данными о погоде в формате JsonObject
     */
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String = API_KEY,
        @Query("units") units: String = "metric"
    ): Response<JsonObject>
    
    /**
     * Получает текущую погоду по названию города.
     * 
     * @param cityName Название города
     * @param apiKey API ключ OpenWeatherMap
     * @param units Единицы измерения (metric = градусы Цельсия)
     * @return Ответ API с данными о погоде в формате JsonObject
     */
    @GET("weather")
    suspend fun getWeatherByCity(
        @Query("q") cityName: String,
        @Query("appid") apiKey: String = API_KEY,
        @Query("units") units: String = "metric"
    ): Response<JsonObject>
    
    /**
     * Получает прогноз погоды на несколько дней по координатам.
     * 
     * @param latitude Широта местоположения
     * @param longitude Долгота местоположения
     * @param apiKey API ключ OpenWeatherMap
     * @param units Единицы измерения (metric = градусы Цельсия)
     * @return Ответ API с данными о прогнозе в формате JsonObject
     */
    @GET("forecast")
    suspend fun getForecast(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String = API_KEY,
        @Query("units") units: String = "metric"
    ): Response<JsonObject>
}