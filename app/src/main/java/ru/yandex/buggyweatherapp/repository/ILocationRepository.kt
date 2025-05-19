package ru.yandex.buggyweatherapp.repository

import kotlinx.coroutines.flow.Flow
import ru.yandex.buggyweatherapp.model.Location

/**
 * Интерфейс для репозитория локации
 */
interface ILocationRepository {
    suspend fun getCurrentLocation(): Result<Location>
    suspend fun getCityNameFromLocation(location: Location): Result<String?>
    fun startLocationUpdates()
    fun stopLocationUpdates()
}