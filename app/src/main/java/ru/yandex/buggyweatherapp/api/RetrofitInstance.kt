package ru.yandex.buggyweatherapp.api

import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// Класс ошибки для API
class ApiException(message: String, val code: Int = 0, val errorBody: String? = null) : IOException(message)
class NetworkException(message: String) : IOException(message)
class TimeoutException(message: String) : IOException(message)

// Интерцептор для обработки ошибок сети
class ErrorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        try {
            val request = chain.request()
            val response = chain.proceed(request)

            // Проверка на ошибки сервера
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                val errorMessage = when (response.code) {
                    401 -> "Unauthorized: API key is invalid"
                    404 -> "City or location not found"
                    429 -> "Too many requests: API limit reached"
                    500, 502, 503, 504 -> "Server error: ${response.code}"
                    else -> "API Error: ${response.code}"
                }
                throw ApiException(errorMessage, response.code, errorBody)
            }

            return response
        } catch (e: SocketTimeoutException) {
            throw TimeoutException("Connection timed out")
        } catch (e: IOException) {
            if (e is ApiException) throw e // пробрасываем уже созданную ошибку API
            throw NetworkException("Network error: ${e.message}")
        }
    }
}

@Singleton
class RetrofitInstance @Inject constructor() {
    companion object {
        // Ленивая инициализация - переименовываем переменную, чтобы избежать конфликта
        private val weatherApiInstance: WeatherApiService by lazy {
            createRetrofit().create(WeatherApiService::class.java)
        }

        // Получить экземпляр API сервиса
        fun getWeatherApi(): WeatherApiService = weatherApiInstance

        // Создать настроенный OkHttpClient с таймаутами и интерцепторами для обработки ошибок
        private fun createOkHttpClient(): OkHttpClient {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .addInterceptor(ErrorInterceptor())
                .retryOnConnectionFailure(true)
                .build()
        }

        // Создать настроенный Retrofit с поддержкой Gson и безопасного соединения
        private fun createRetrofit(): Retrofit {
            return Retrofit.Builder()
                .baseUrl(WeatherApiService.BASE_URL)
                .client(createOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create(Gson()))
                .build()
        }
    }
    
    // Публичный метод для получения API сервиса
    fun getWeatherApiService(): WeatherApiService = Companion.getWeatherApi()
}