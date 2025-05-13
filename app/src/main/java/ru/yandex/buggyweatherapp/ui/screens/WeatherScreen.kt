package ru.yandex.buggyweatherapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import ru.yandex.buggyweatherapp.R
import ru.yandex.buggyweatherapp.model.WeatherUiModel
import ru.yandex.buggyweatherapp.ui.components.DetailedWeatherCard
import ru.yandex.buggyweatherapp.ui.state.UiState
import ru.yandex.buggyweatherapp.viewmodel.WeatherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(viewModel: WeatherViewModel, modifier: Modifier = Modifier) {
    // Запуск автообновления при появлении экрана и остановка при его закрытии
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Привязка к жизненному циклу для автоматического управления ресурсами
    LaunchedEffect(Unit) {
        viewModel.startAutoRefresh()
        
        // Остановка автообновления при уходе экрана с активного состояния
        val lifecycle = lifecycleOwner.lifecycle
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycle.addObserver(object : androidx.lifecycle.LifecycleEventObserver {
                override fun onStateChanged(source: androidx.lifecycle.LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_STOP) {
                        viewModel.stopAutoRefresh()
                    } else if (event == Lifecycle.Event.ON_START) {
                        viewModel.startAutoRefresh()
                    }
                }
            })
        }
    }

    // Получение состояния из ViewModel с учетом жизненного цикла
    val weatherStateFlow = remember(viewModel, lifecycleOwner) {
        viewModel.weatherState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val weatherState by weatherStateFlow.collectAsState(initial = UiState.Loading)

    // Наблюдение за состоянием сети
    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsState()
    
    // Состояние для отслеживания ввода в поле поиска
    var searchText by remember { mutableStateOf("") }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text(stringResource(R.string.search_city)) },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Search field for city name" },
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (searchText.isNotBlank()) {
                            viewModel.searchWeatherByCity(searchText)
                        }
                    },
                    modifier = Modifier.semantics { contentDescription = "Search button" }
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { 
                if (searchText.isNotBlank()) {
                    viewModel.searchWeatherByCity(searchText)
                }
            })
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Отображение состояния UI
        when (val state = weatherState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Success -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    DetailedWeatherCard(
                        weather = state.data,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { viewModel.fetchCurrentLocationWeather() },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .semantics { contentDescription = "Refresh weather button" }
                    ) {
                        Text(stringResource(R.string.refresh_weather))
                    }
                }
            }
            is UiState.Error -> {
                ErrorView(
                    message = state.message,
                    onRetry = { viewModel.fetchCurrentLocationWeather() }
                )
            }
        }

        // Индикатор состояния сети
        if (!isNetworkAvailable) {
            Text(
                text = stringResource(R.string.no_network_connection),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier.semantics { contentDescription = "Retry button" }
        ) {
            Text(stringResource(R.string.retry))
        }
    }
}

