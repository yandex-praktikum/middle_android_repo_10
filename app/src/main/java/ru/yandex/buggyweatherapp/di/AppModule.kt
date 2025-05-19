package ru.yandex.buggyweatherapp.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.yandex.buggyweatherapp.api.WeatherApiService
import ru.yandex.buggyweatherapp.repository.ILocationRepository
import ru.yandex.buggyweatherapp.repository.IWeatherRepository
import ru.yandex.buggyweatherapp.repository.LocationRepository
import ru.yandex.buggyweatherapp.repository.WeatherRepository
import javax.inject.Singleton

/**
 * Hilt модуль для внедрения зависимостей на уровне приложения
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideWeatherRepository(weatherApi: WeatherApiService): IWeatherRepository {
        return WeatherRepository(weatherApi)
    }
    
    @Provides
    @Singleton
    fun provideLocationRepository(application: Application): ILocationRepository {
        return LocationRepository(application)
    }
}