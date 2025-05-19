package ru.yandex.buggyweatherapp.repository

import android.util.Log
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.yandex.buggyweatherapp.api.WeatherApiService
import ru.yandex.buggyweatherapp.model.Location
import ru.yandex.buggyweatherapp.model.WeatherData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторий для работы с данными о погоде.
 * 
 * Изменения:
 * 1. Реализация интерфейса IWeatherRepository для тестируемости и соблюдения SOLID
 * 2. Использование Dispatchers.IO для операций ввода-вывода
 * 3. Использование suspend-функций и корутин вместо колбэков
 * 4. Правильная обработка ошибок с использованием Result
 * 5. Улучшенная обработка JSON с проверками на null и значениями по умолчанию
 * 6. Логирование ошибок
 * 7. Добавление кэширования данных
 */
@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApi: WeatherApiService
) : IWeatherRepository {
    
    /**
     * Кэш для последних полученных данных о погоде.
     * Позволяет избежать повторных запросов при повороте экрана и других пересозданиях UI.
     */
    private var cachedWeatherData: WeatherData? = null
    
    /**
     * Получает данные о погоде по координатам местоположения.
     * Выполняется в IO-потоке для избежания блокировки главного потока.
     * 
     * @param location Местоположение для получения погоды
     * @return Result с данными о погоде или ошибкой
     */
    override suspend fun getWeatherData(location: Location): Result<WeatherData> = 
        withContext(Dispatchers.IO) {
            try {
                val response = weatherApi.getCurrentWeather(location.latitude, location.longitude)
                
                if (response.isSuccessful && response.body() != null) {
                    val weatherData = parseWeatherData(response.body()!!, location)
                    cachedWeatherData = weatherData
                    Result.success(weatherData)
                } else {
                    Result.failure(Exception("API Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e("WeatherRepository", "Error fetching weather", e)
                Result.failure(e)
            }
        }
    
    /**
     * Получает данные о погоде по названию города.
     * Выполняется в IO-потоке для избежания блокировки главного потока.
     * 
     * @param cityName Название города для поиска
     * @return Result с данными о погоде или ошибкой
     */
    override suspend fun getWeatherByCity(cityName: String): Result<WeatherData> = 
        withContext(Dispatchers.IO) {
            try {
                val response = weatherApi.getWeatherByCity(cityName)
                
                if (response.isSuccessful && response.body() != null) {
                    val json = response.body()!!
                    val location = extractLocationFromResponse(json)
                    val weatherData = parseWeatherData(json, location)
                    Result.success(weatherData)
                } else {
                    Result.failure(Exception("Error fetching weather data: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e("WeatherRepository", "Error fetching weather by city", e)
                Result.failure(e)
            }
        }
    
    /**
     * Разбирает данные JSON, полученные от API, в объект WeatherData.
     * Включает множество проверок на null и пустые значения для предотвращения исключений.
     * 
     * @param json JSON-ответ от API
     * @param location Данные о местоположении
     * @return Объект WeatherData с данными о погоде
     * @throws Exception если обязательные поля отсутствуют или некорректны
     */
    private fun parseWeatherData(json: JsonObject, location: Location): WeatherData {
        try {
            // Проверяем наличие основных объектов
            val main = json.getAsJsonObject("main") ?: throw Exception("Missing 'main' object in response")
            val wind = json.getAsJsonObject("wind") ?: JsonObject()
            val sys = json.getAsJsonObject("sys") ?: JsonObject()
            
            // Проверяем наличие массива weather и берем первый элемент, если он есть
            val weatherArray = json.getAsJsonArray("weather")
            if (weatherArray == null || weatherArray.size() == 0) {
                throw Exception("Missing 'weather' array in response")
            }
            val weather = weatherArray.get(0).asJsonObject
            
            val clouds = json.getAsJsonObject("clouds") ?: JsonObject()
            
            // Безопасно получаем значения с заменой по умолчанию, если поле отсутствует
            fun JsonObject.getStringOrDefault(key: String, default: String): String {
                return if (this.has(key) && !this.get(key).isJsonNull) this.get(key).asString else default
            }
            
            fun JsonObject.getDoubleOrDefault(key: String, default: Double): Double {
                return if (this.has(key) && !this.get(key).isJsonNull) this.get(key).asDouble else default
            }
            
            fun JsonObject.getIntOrDefault(key: String, default: Int): Int {
                return if (this.has(key) && !this.get(key).isJsonNull) this.get(key).asInt else default
            }
            
            fun JsonObject.getLongOrDefault(key: String, default: Long): Long {
                return if (this.has(key) && !this.get(key).isJsonNull) this.get(key).asLong else default
            }
            
            return WeatherData(
                cityName = json.getStringOrDefault("name", location.name ?: "Unknown"),
                country = sys.getStringOrDefault("country", ""),
                temperature = main.getDoubleOrDefault("temp", 0.0),
                feelsLike = main.getDoubleOrDefault("feels_like", 0.0),
                minTemp = main.getDoubleOrDefault("temp_min", 0.0),
                maxTemp = main.getDoubleOrDefault("temp_max", 0.0),
                humidity = main.getIntOrDefault("humidity", 0),
                pressure = main.getIntOrDefault("pressure", 0),
                windSpeed = wind.getDoubleOrDefault("speed", 0.0),
                windDirection = wind.getIntOrDefault("deg", 0),
                description = weather.getStringOrDefault("description", "Нет данных"),
                icon = weather.getStringOrDefault("icon", "01d"), // Дефолтное значение для иконки
                cloudiness = clouds.getIntOrDefault("all", 0),
                sunriseTime = sys.getLongOrDefault("sunrise", 0L),
                sunsetTime = sys.getLongOrDefault("sunset", 0L),
                timezone = json.getIntOrDefault("timezone", 0),
                timestamp = json.getLongOrDefault("dt", System.currentTimeMillis() / 1000),
                rawApiData = json.toString(),
                rain = if (json.has("rain") && json.getAsJsonObject("rain").has("1h") && 
                        !json.getAsJsonObject("rain").get("1h").isJsonNull) 
                        json.getAsJsonObject("rain").get("1h").asDouble else null,
                snow = if (json.has("snow") && json.getAsJsonObject("snow").has("1h") && 
                        !json.getAsJsonObject("snow").get("1h").isJsonNull)
                        json.getAsJsonObject("snow").get("1h").asDouble else null
            )
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error parsing weather data", e)
            throw e
        }
    }
    
    /**
     * Извлекает данные о местоположении из JSON-ответа API.
     * Включает проверки на null и корректность данных.
     * 
     * @param json JSON-ответ от API
     * @return Объект Location с извлеченными данными или значениями по умолчанию в случае ошибки
     */
    private fun extractLocationFromResponse(json: JsonObject): Location {
        try {
            // Проверяем наличие координат
            val coord = json.getAsJsonObject("coord") ?: throw Exception("Missing 'coord' object in response")
            
            // Безопасно получаем значения с заменой по умолчанию
            fun JsonObject.getDoubleOrDefault(key: String, default: Double): Double {
                return if (this.has(key) && !this.get(key).isJsonNull) this.get(key).asDouble else default
            }
            
            fun JsonObject.getStringOrDefault(key: String, default: String): String {
                return if (this.has(key) && !this.get(key).isJsonNull) this.get(key).asString else default
            }
            
            val lat = coord.getDoubleOrDefault("lat", 0.0)
            val lon = coord.getDoubleOrDefault("lon", 0.0)
            val name = json.getStringOrDefault("name", "Unknown")
            
            return Location(lat, lon, name)
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error extracting location from response", e)
            // Возвращаем дефолтную локацию в случае ошибки
            return Location(0.0, 0.0, "Unknown")
        }
    }
}