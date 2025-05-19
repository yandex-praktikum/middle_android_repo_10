package ru.yandex.buggyweatherapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.yandex.buggyweatherapp.ui.components.ErrorView
import ru.yandex.buggyweatherapp.ui.components.LoadingView
import ru.yandex.buggyweatherapp.ui.components.LocationSearch
import ru.yandex.buggyweatherapp.ui.components.WeatherCard
import ru.yandex.buggyweatherapp.utils.UiState
import ru.yandex.buggyweatherapp.viewmodel.WeatherViewModel

/**
 * Главный экран приложения, отображающий данные о погоде.
 * 
 * Изменения:
 * 1. Использование hiltViewModel() для получения ViewModel через DI
 * 2. Использование StateFlow с collectAsState() вместо LiveData
 * 3. Разделение UI на отдельные компоненты для улучшения читаемости и поддержки
 * 4. Использование состояний UiState для управления отображением (Loading, Success, Error)
 * 5. Добавление отдельных состояний для различных типов данных
 * 6. Добавление компонентов для обработки загрузки и ошибок
 */

/**
 * Composable-функция для отображения экрана погоды.
 * Отвечает за координацию отображения поиска и данных о погоде.
 *
 * @param modifier Модификатор для настройки внешнего вида
 * @param viewModel ViewModel для получения данных о погоде
 */
@Composable
fun WeatherScreen(
    modifier: Modifier = Modifier,
    viewModel: WeatherViewModel = hiltViewModel()
) {
    // Получаем состояния из ViewModel через StateFlow
    val weatherUiState by viewModel.weatherUiState.collectAsState()
    val cityName by viewModel.cityNameState.collectAsState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Поиск по городу
        LocationSearch(
            onCitySearch = { city -> 
                viewModel.searchWeatherByCity(city) 
            },
            onLocationRequest = { 
                viewModel.fetchCurrentLocationWeather() 
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Отображение соответствующего контента в зависимости от состояния
        when (val state = weatherUiState) {
            is UiState.Loading -> {
                LoadingView()
            }
            
            is UiState.Success -> {
                WeatherCard(
                    weatherData = state.data,
                    cityName = cityName
                )
            }
            
            is UiState.Error -> {
                ErrorView(
                    message = state.message,
                    onRetry = { viewModel.fetchCurrentLocationWeather() }
                )
            }
        }
    }
}