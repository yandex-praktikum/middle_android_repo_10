package ru.yandex.buggyweatherapp.viewmodel

// Удалено использование LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.yandex.buggyweatherapp.model.Location
import ru.yandex.buggyweatherapp.repository.ILocationRepository
import ru.yandex.buggyweatherapp.ui.state.UiState
import javax.inject.Inject

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val locationRepository: ILocationRepository
) : ViewModel() {
    
    // UI состояние для местоположения
    private val _locationState = MutableStateFlow<UiState<Location>>(UiState.Loading)
    val locationState: StateFlow<UiState<Location>> = _locationState
    
    // Название города для текущего местоположения - используем StateFlow вместо LiveData
    private val _cityName = MutableStateFlow<String>("")
    val cityName: StateFlow<String> = _cityName
    
    // Получение текущего местоположения
    fun getCurrentLocation() {
        viewModelScope.launch {
            _locationState.value = UiState.Loading
            
            try {
                val result = locationRepository.getCurrentLocation()
                
                result.fold(
                    onSuccess = { location ->
                        _locationState.value = UiState.Success(location)
                        
                        // Получение названия города для местоположения
                        getCityName(location)
                    },
                    onFailure = { e ->
                        _locationState.value = UiState.Error("Не удалось получить местоположение: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                _locationState.value = UiState.Error("Ошибка: ${e.message}")
            }
        }
    }
    
    // Получение названия города по координатам
    private fun getCityName(location: Location) {
        viewModelScope.launch {
            try {
                val result = locationRepository.getCityNameFromLocation(location)
                
                result.fold(
                    onSuccess = { city ->
                        _cityName.value = city
                    },
                    onFailure = { e ->
                        // Если не удалось получить название города, используем координаты
                        _cityName.value = "${location.latitude}, ${location.longitude}"
                    }
                )
            } catch (e: Exception) {
                // В случае ошибки используем координаты
                _cityName.value = "${location.latitude}, ${location.longitude}"
            }
        }
    }
    
    // Запуск отслеживания местоположения
    fun startLocationTracking() {
        locationRepository.startLocationTracking()
    }
    
    // Остановка отслеживания местоположения
    fun stopLocationTracking() {
        locationRepository.stopLocationTracking()
    }
    
    override fun onCleared() {
        super.onCleared()
        // Остановка отслеживания при уничтожении ViewModel
        stopLocationTracking()
    }
}