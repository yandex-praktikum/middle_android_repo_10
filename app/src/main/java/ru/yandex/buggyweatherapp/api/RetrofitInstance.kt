package ru.yandex.buggyweatherapp.api

import com.google.gson.Gson
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// ОШИБКА: Реализация паттерна Singleton с потенциальными проблемами многопоточности
object RetrofitInstance {
    // ОШИБКА: Отсутствует конфигурация OkHttpClient (нет таймаутов, нет интерцепторов)
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(WeatherApiService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
    }
    
    // ОШИБКА: Нетерпеливая (eager) инициализация вместо ленивой (lazy)
    val weatherApi: WeatherApiService = retrofit.create(WeatherApiService::class.java)
    
    // ОШИБКА: Отсутствует обработка ошибок и механизм повторных попыток
}