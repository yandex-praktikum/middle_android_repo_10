package ru.yandex.buggyweatherapp.api

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    
    // ОШИБКА: Захардкоженный API-ключ как константа в интерфейсе
    // (Проблема безопасности - раскрытие API-ключа в исходном коде)
    companion object {
        const val API_KEY = "8fd9a0f2216e2bc16a09102e2af8ab1d"
        const val BASE_URL = "http://api.openweathermap.org/data/2.5/" // ОШИБКА: Использование HTTP вместо HTTPS
    }
    
    // ОШИБКА: Использование Call<> вместо suspend-функций
    @GET("weather")
    fun getCurrentWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String = API_KEY,
        @Query("units") units: String = "metric"
    ): Call<JsonObject>
    
    @GET("weather")
    fun getWeatherByCity(
        @Query("q") cityName: String,
        @Query("appid") apiKey: String = API_KEY,
        @Query("units") units: String = "metric"
    ): Call<JsonObject>
    
    @GET("forecast")
    fun getForecast(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String = API_KEY,
        @Query("units") units: String = "metric"
    ): Call<JsonObject>
}