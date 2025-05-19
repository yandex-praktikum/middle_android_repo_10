package ru.yandex.buggyweatherapp.utils

/**
 * Sealed класс для представления различных состояний UI
 */
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}