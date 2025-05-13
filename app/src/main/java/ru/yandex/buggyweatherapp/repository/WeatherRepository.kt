package ru.yandex.buggyweatherapp.repository

import android.util.Log
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import ru.yandex.buggyweatherapp.api.ApiException
import ru.yandex.buggyweatherapp.api.NetworkException
import ru.yandex.buggyweatherapp.api.TimeoutException
import ru.yandex.buggyweatherapp.api.WeatherApiService
import ru.yandex.buggyweatherapp.model.Location
import ru.yandex.buggyweatherapp.model.WeatherData
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Типы ошибок, которые могут возникнуть при получении данных о погоде
 */
sealed class WeatherError : Exception() {
    data class NetworkError(override val message: String) : WeatherError()
    data class ServerError(override val message: String, val code: Int) : WeatherError()
    data class LocationError(override val message: String) : WeatherError()
    data class CacheError(override val message: String) : WeatherError()
    data class UnknownError(override val message: String, override val cause: Throwable? = null) :
        WeatherError()
}

/**
 * Интерфейс для WeatherRepository согласно принципу инверсии зависимостей
 */
interface IWeatherRepository {
    /**
     * Получение данных о погоде для указанного местоположения
     * @param location местоположение
     * @return Result с данными о погоде или ошибкой
     */
    suspend fun getWeatherData(location: Location): Result<WeatherData>

    /**
     * Получение данных о погоде по названию города
     * @param cityName название города
     * @return Result с данными о погоде или ошибкой
     */
    suspend fun getWeatherByCity(cityName: String): Result<WeatherData>

    /**
     * Получение данных о погоде для указанного местоположения в виде Flow
     * @param location местоположение
     * @return Flow с данными о погоде
     */
    fun getWeatherDataAsFlow(location: Location): Flow<Result<WeatherData>>

    /**
     * Получение данных о погоде по названию города в виде Flow
     * @param cityName название города
     * @return Flow с данными о погоде
     */
    fun getWeatherByCityAsFlow(cityName: String): Flow<Result<WeatherData>>

    /**
     * Очистка кэша данных о погоде
     */
    fun clearCache()
}

/**
 * Реализация репозитория погоды с поддержкой кэширования и потоковой обработки данных.
 *
 * Обновление: Репозиторий теперь использует suspend функции Retrofit напрямую вместо
 * вызовов execute(). Это упрощает код, делает его более читаемым и позволяет избежать
 * избыточного кода для обработки ответов. Retrofit автоматически обработает успешные ответы
 * и выбросит HttpException при ошибках, что упрощает обработку ошибок.
 */
class WeatherRepository @Inject constructor(
    private val weatherApi: WeatherApiService
) : IWeatherRepository {

    companion object {
        private const val TAG = "WeatherRepository"
        private const val CACHE_EXPIRATION_MINUTES = 15L
    }

    // Использование потокобезопасного кэша с временем жизни
    private val cacheExpirationTime = TimeUnit.MINUTES.toMillis(CACHE_EXPIRATION_MINUTES)

    private data class CachedData(val data: WeatherData, val timestamp: Long)

    private val cache = ConcurrentHashMap<String, CachedData>()

    override suspend fun getWeatherData(location: Location): Result<WeatherData> {
        return withContext(Dispatchers.IO) {
            try {
                // Проверка кэша
                val cacheKey = "location_${location.latitude}_${location.longitude}"
                val cachedData = cache[cacheKey]

                if (cachedData != null && (System.currentTimeMillis() - cachedData.timestamp) < cacheExpirationTime) {
                    return@withContext Result.success(cachedData.data)
                }

                // Выполнение запроса на IO потоке с использованием suspend функции
                try {
                    // Прямой вызов suspend функции вместо execute()
                    val jsonResponse = weatherApi.getCurrentWeather(
                        location.latitude,
                        location.longitude
                    )

                    // Парсинг данных о погоде из ответа
                    val weatherData = parseWeatherData(jsonResponse, location)

                    // Обновление кэша
                    cache[cacheKey] = CachedData(weatherData, System.currentTimeMillis())

                    Result.success(weatherData)
                } catch (e: HttpException) {
                    // Обработка HTTP ошибок 
                    val errorCode = e.code()
                    val errorMessage = when (errorCode) {
                        401 -> "API key is invalid or missing"
                        404 -> "Location not found"
                        429 -> "API limit reached"
                        in 500..599 -> "Server error (code: $errorCode)"
                        else -> "Unknown error (code: $errorCode)"
                    }
                    Log.e("WeatherRepository", "API Error: $errorMessage", e)
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: ApiException) {
                Log.e("WeatherRepository", "API Exception: ${e.message}, Code: ${e.code}", e)
                Result.failure(Exception(e.message))
            } catch (e: NetworkException) {
                Log.e("WeatherRepository", "Network exception: ${e.message}", e)
                // Возвращаем кэшированные данные, если они есть, даже если устарели
                val cachedData = cache["location_${location.latitude}_${location.longitude}"]
                if (cachedData != null) {
                    Result.success(cachedData.data)
                } else {
                    Result.failure(Exception("Network error, check your connection"))
                }
            } catch (e: TimeoutException) {
                Log.e("WeatherRepository", "Timeout exception: ${e.message}", e)
                // Возвращаем кэшированные данные, если они есть, даже если устарели
                val cachedData = cache["location_${location.latitude}_${location.longitude}"]
                if (cachedData != null) {
                    Result.success(cachedData.data)
                } else {
                    Result.failure(Exception("Request timeout, check your connection"))
                }
            } catch (e: Exception) {
                Log.e("WeatherRepository", "Error fetching weather", e)
                Result.failure(Exception("Unexpected error: ${e.message}"))
            }
        }
    }

    override suspend fun getWeatherByCity(cityName: String): Result<WeatherData> {
        return withContext(Dispatchers.IO) {
            try {
                // Проверка кэша
                val cacheKey = "city_$cityName"
                val cachedData = cache[cacheKey]

                if (cachedData != null && (System.currentTimeMillis() - cachedData.timestamp) < cacheExpirationTime) {
                    return@withContext Result.success(cachedData.data)
                }

                // Выполнение запроса на IO потоке с использованием suspend функции
                try {
                    // Прямой вызов suspend функции вместо execute()
                    val json = weatherApi.getWeatherByCity(cityName)
                    val location = extractLocationFromResponse(json)
                    val weatherData = parseWeatherData(json, location)

                    // Обновление кэша
                    cache[cacheKey] = CachedData(weatherData, System.currentTimeMillis())

                    Result.success(weatherData)
                } catch (e: HttpException) {
                    // Обработка HTTP ошибок
                    val errorCode = e.code()
                    val errorMessage = when (errorCode) {
                        401 -> "API key is invalid or missing"
                        404 -> "City '$cityName' not found"
                        429 -> "API limit reached"
                        in 500..599 -> "Server error (code: $errorCode)"
                        else -> "Unknown error (code: $errorCode)"
                    }
                    Log.e("WeatherRepository", "API Error: $errorMessage", e)
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: ApiException) {
                Log.e("WeatherRepository", "API Exception: ${e.message}, Code: ${e.code}", e)
                Result.failure(Exception(e.message))
            } catch (e: NetworkException) {
                Log.e("WeatherRepository", "Network exception: ${e.message}", e)
                // Возвращаем кэшированные данные, если они есть, даже если устарели
                val cachedData = cache["city_$cityName"]
                if (cachedData != null) {
                    Result.success(cachedData.data)
                } else {
                    Result.failure(Exception("Network error, check your connection"))
                }
            } catch (e: TimeoutException) {
                Log.e("WeatherRepository", "Timeout exception: ${e.message}", e)
                // Возвращаем кэшированные данные, если они есть, даже если устарели
                val cachedData = cache["city_$cityName"]
                if (cachedData != null) {
                    Result.success(cachedData.data)
                } else {
                    Result.failure(Exception("Request timeout, check your connection"))
                }
            } catch (e: Exception) {
                Log.e("WeatherRepository", "Error fetching weather by city", e)
                Result.failure(Exception("Unexpected error: ${e.message}"))
            }
        }
    }

    // Парсинг данных выполняется на IO потоке
    private fun parseWeatherData(json: JsonObject, location: Location): WeatherData {
        val main = json.getAsJsonObject("main")
        val wind = json.getAsJsonObject("wind")
        val sys = json.getAsJsonObject("sys")
        val weather = json.getAsJsonArray("weather").get(0).asJsonObject
        val clouds = json.getAsJsonObject("clouds")

        // Извлекаем координаты из JSON или используем переданные
        val coord = json.getAsJsonObject("coord")
        val latitude =
            if (coord != null && coord.has("lat")) coord.get("lat").asDouble else location.latitude
        val longitude =
            if (coord != null && coord.has("lon")) coord.get("lon").asDouble else location.longitude

        return WeatherData(
            cityName = json.get("name").asString,
            country = sys.get("country").asString,
            latitude = latitude,
            longitude = longitude,
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

    /**
     * Получение данных о погоде для указанного местоположения в виде потока событий.
     * Сначала вернет кэшированные данные (если есть), затем делает запрос к API.
     */
    override fun getWeatherDataAsFlow(location: Location): Flow<Result<WeatherData>> = flow {
        // Проверяем кэш и возвращаем кэшированные данные, если они есть и не устарели
        val cacheKey = "location_${location.latitude}_${location.longitude}"
        val cachedData = cache[cacheKey]

        if (cachedData != null && (System.currentTimeMillis() - cachedData.timestamp) < cacheExpirationTime) {
            // Возвращаем кэшированные данные
            emit(Result.success(cachedData.data))
        }

        // В любом случае делаем запрос к API для получения свежих данных с использованием suspend функции
        try {
            // Прямой вызов suspend функции
            val jsonResponse = weatherApi.getCurrentWeather(
                location.latitude,
                location.longitude
            )
            
            // Парсинг и обработка результата
            val weatherData = parseWeatherData(jsonResponse, location)

            // Обновляем кэш
            cache[cacheKey] = CachedData(weatherData, System.currentTimeMillis())

            // Отправляем свежие данные
            emit(Result.success(weatherData))
        } catch (e: HttpException) {
            val errorCode = e.code()
            val errorMessage = createErrorMessage(errorCode)

            Log.e(TAG, "API Error: $errorMessage", e)
            emit(Result.failure(WeatherError.ServerError(errorMessage, errorCode)))
        } catch (e: ApiException) {
            Log.e(TAG, "API Exception: ${e.message}, Code: ${e.code}", e)
            emit(Result.failure(WeatherError.ServerError(e.message ?: "Unknown API error", e.code)))
        } catch (e: NetworkException) {
            Log.e(TAG, "Network exception: ${e.message}", e)
            // Возвращаем кэшированные данные, если они есть, даже если устарели
            if (cachedData != null) {
                emit(Result.success(cachedData.data))
            } else {
                emit(Result.failure(WeatherError.NetworkError("Network error, check your connection")))
            }
        } catch (e: TimeoutException) {
            Log.e(TAG, "Timeout exception: ${e.message}", e)
            // Возвращаем кэшированные данные, если они есть, даже если устарели
            if (cachedData != null) {
                emit(Result.success(cachedData.data))
            } else {
                emit(Result.failure(WeatherError.NetworkError("Request timeout, check your connection")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching weather", e)
            emit(Result.failure(WeatherError.UnknownError("Unexpected error", e)))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Получение данных о погоде по названию города в виде потока событий.
     * Сначала вернет кэшированные данные (если есть), затем делает запрос к API.
     */
    override fun getWeatherByCityAsFlow(cityName: String): Flow<Result<WeatherData>> = flow {
        // Проверяем кэш и возвращаем кэшированные данные, если они есть и не устарели
        val cacheKey = "city_$cityName"
        val cachedData = cache[cacheKey]

        if (cachedData != null && (System.currentTimeMillis() - cachedData.timestamp) < cacheExpirationTime) {
            // Возвращаем кэшированные данные
            emit(Result.success(cachedData.data))
        }

        // В любом случае делаем запрос к API для получения свежих данных с использованием suspend функции
        try {
            // Прямой вызов suspend функции
            val json = weatherApi.getWeatherByCity(cityName)
            val location = extractLocationFromResponse(json)
            val weatherData = parseWeatherData(json, location)

            // Обновляем кэш
            cache[cacheKey] = CachedData(weatherData, System.currentTimeMillis())

            // Отправляем свежие данные
            emit(Result.success(weatherData))
        } catch (e: HttpException) {
            val errorCode = e.code()
            val errorMessage = if (errorCode == 404) {
                "City '$cityName' not found"
            } else {
                createErrorMessage(errorCode)
            }

            Log.e(TAG, "API Error: $errorMessage", e)
            emit(Result.failure(WeatherError.ServerError(errorMessage, errorCode)))
        } catch (e: ApiException) {
            Log.e(TAG, "API Exception: ${e.message}, Code: ${e.code}", e)
            emit(Result.failure(WeatherError.ServerError(e.message ?: "Unknown API error", e.code)))
        } catch (e: NetworkException) {
            Log.e(TAG, "Network exception: ${e.message}", e)
            // Возвращаем кэшированные данные, если они есть, даже если устарели
            if (cachedData != null) {
                emit(Result.success(cachedData.data))
            } else {
                emit(Result.failure(WeatherError.NetworkError("Network error, check your connection")))
            }
        } catch (e: TimeoutException) {
            Log.e(TAG, "Timeout exception: ${e.message}", e)
            // Возвращаем кэшированные данные, если они есть, даже если устарели
            if (cachedData != null) {
                emit(Result.success(cachedData.data))
            } else {
                emit(Result.failure(WeatherError.NetworkError("Request timeout, check your connection")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching weather by city", e)
            emit(Result.failure(WeatherError.UnknownError("Unexpected error", e)))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Создает сообщение об ошибке по коду ответа API
     */
    private fun createErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            401 -> "API key is invalid or missing"
            404 -> "Location not found"
            429 -> "API limit reached"
            in 500..599 -> "Server error (code: $errorCode)"
            else -> "Unknown error (code: $errorCode)"
        }
    }

    /**
     * Очистка кэша данных о погоде
     */
    override fun clearCache() {
        Log.d(TAG, "Clearing weather data cache")
        cache.clear()
    }
}