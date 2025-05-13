package ru.yandex.buggyweatherapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WeatherApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Hilt обеспечит инициализацию всех зависимостей
    }
}