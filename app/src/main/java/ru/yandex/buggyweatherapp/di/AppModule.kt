package ru.yandex.buggyweatherapp.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.yandex.buggyweatherapp.api.RetrofitInstance
import ru.yandex.buggyweatherapp.api.WeatherApiService
import ru.yandex.buggyweatherapp.repository.ILocationRepository
import ru.yandex.buggyweatherapp.repository.IWeatherRepository
import ru.yandex.buggyweatherapp.repository.LocationRepository
import ru.yandex.buggyweatherapp.repository.WeatherRepository
import javax.inject.Singleton

/**
 * Модуль Hilt для предоставления зависимостей приложения.
 * ImageLoader удален, так как для загрузки изображений используется Coil (AsyncImage).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideWeatherApiService(retrofitInstance: RetrofitInstance): WeatherApiService {
        return retrofitInstance.getWeatherApiService()
    }
    
    @Provides
    @Singleton
    fun provideWeatherRepository(weatherApiService: WeatherApiService): IWeatherRepository {
        return WeatherRepository(weatherApiService)
    }
    
    @Provides
    @Singleton
    fun provideLocationRepository(
        application: Application
    ): ILocationRepository {
        return LocationRepository(application)
    }
    
    // Метод provideWeatherFormatter удален, так как теперь используются строковые ресурсы
}