package ru.yandex.buggyweatherapp

import android.app.Application
import android.content.Context
import ru.yandex.buggyweatherapp.utils.ImageLoader
import ru.yandex.buggyweatherapp.utils.LocationTracker

class WeatherApplication : Application() {
    
    // ОШИБКА: Утечка памяти - статическая ссылка на контекст
    companion object {
        lateinit var appContext: Context
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // ОШИБКА: Утечка памяти - сохранение контекста приложения в статической переменной
        appContext = this
        
        // ОШИБКА: Ранняя инициализация компонентов, которые могут не понадобиться сразу
        // и без механизма правильной очистки ресурсов
        ImageLoader.initialize(this)
        LocationTracker.getInstance(this)
    }
}