package ru.yandex.buggyweatherapp.repository

import android.util.Log
import com.google.gson.JsonObject
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.yandex.buggyweatherapp.api.RetrofitInstance
import ru.yandex.buggyweatherapp.model.Location
import ru.yandex.buggyweatherapp.model.WeatherData
import java.util.Date

// ОШИБКА: Не определен интерфейс (нарушение SOLID - отсутствие абстракции)
class WeatherRepository {
    
    // ОШИБКА: Отсутствует внедрение зависимостей, прямое использование синглтона
    private val weatherApi = RetrofitInstance.weatherApi
    
    // ОШИБКА: Нет потокобезопасности, кэширования или механизма времени жизни данных
    private var cachedWeatherData: WeatherData? = null
    
    // ОШИБКА: Архитектура на основе колбэков вместо корутин с правильной обработкой ошибок
    fun getWeatherData(location: Location, callback: (WeatherData?, Exception?) -> Unit) {
        // ОШИБКА: Отсутствует проверка подключения перед API-вызовом
        val call = weatherApi.getCurrentWeather(location.latitude, location.longitude)
        
        // ОШИБКА: Прямое выполнение сетевого вызова без правильной многопоточности
        try {
            // ОШИБКА: Синхронный вызов в основном потоке, что вызовет ANR
            val response = call.execute()
            
            if (response.isSuccessful) {
                val weatherData = parseWeatherData(response.body()!!, location)
                cachedWeatherData = weatherData
                callback(weatherData, null)
            } else {
                // ОШИБКА: Нет сообщения об ошибке, просто null
                callback(null, Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            // ОШИБКА: Проглатывание исключений с простым логированием
            Log.e("WeatherRepository", "Error fetching weather", e)
            callback(null, e)
        }
    }
    
    fun getWeatherByCity(cityName: String, callback: (WeatherData?, Exception?) -> Unit) {
        weatherApi.getWeatherByCity(cityName).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful && response.body() != null) {
                    try {
                        val json = response.body()!!
                        val location = extractLocationFromResponse(json)
                        val weatherData = parseWeatherData(json, location)
                        callback(weatherData, null)
                    } catch (e: Exception) {
                        // ОШИБКА: Тихая обработка всех исключений
                        callback(null, e)
                    }
                } else {
                    callback(null, Exception("Error fetching weather data"))
                }
            }
            
            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                callback(null, Exception(t))
            }
        })
    }
    
    // ОШИБКА: Обработка в UI-потоке
    private fun parseWeatherData(json: JsonObject, location: Location): WeatherData {
        // ОШИБКА: Неэффективная логика парсинга JSON
        val main = json.getAsJsonObject("main")
        val wind = json.getAsJsonObject("wind")
        val sys = json.getAsJsonObject("sys")
        val weather = json.getAsJsonArray("weather").get(0).asJsonObject
        val clouds = json.getAsJsonObject("clouds")
        
        return WeatherData(
            cityName = json.get("name").asString,
            country = sys.get("country").asString,
            temperature = main.get("temp").asDouble,
            feelsLike = main.get("feels_like").asDouble,
            minTemp = main.get("temp_min").asDouble,
            maxTemp = main.get("temp_max").asDouble,
            humidity = main.get("humidity").asInt,
            pressure = main.get("pressure").asInt,
            windSpeed = wind.get("speed").asDouble,
            windDirection = if (wind.has("deg")) wind.get("deg").asInt else 0,
            description = weather.get("description").asString,
            icon = weather.get("icon").asString,
            cloudiness = clouds.get("all").asInt,
            sunriseTime = sys.get("sunrise").asLong,
            sunsetTime = sys.get("sunset").asLong,
            timezone = json.get("timezone").asInt,
            timestamp = json.get("dt").asLong,
            rawApiData = json.toString(),
            rain = if (json.has("rain") && json.getAsJsonObject("rain").has("1h")) 
                    json.getAsJsonObject("rain").get("1h").asDouble else null,
            snow = if (json.has("snow") && json.getAsJsonObject("snow").has("1h"))
                    json.getAsJsonObject("snow").get("1h").asDouble else null
        )
    }
    
    private fun extractLocationFromResponse(json: JsonObject): Location {
        val coord = json.getAsJsonObject("coord")
        val lat = coord.get("lat").asDouble
        val lon = coord.get("lon").asDouble
        val name = json.get("name").asString
        
        return Location(lat, lon, name)
    }
}