package ru.yandex.buggyweatherapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import ru.yandex.buggyweatherapp.ui.screens.WeatherScreen
import ru.yandex.buggyweatherapp.ui.theme.BuggyWeatherAppTheme
import ru.yandex.buggyweatherapp.utils.PermissionHandler
import ru.yandex.buggyweatherapp.viewmodel.LocationViewModel
import ru.yandex.buggyweatherapp.viewmodel.WeatherViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    // ViewModel предоставляется через Hilt
    private val weatherViewModel: WeatherViewModel by viewModels()
    private val locationViewModel: LocationViewModel by viewModels()
    
    // Инъекция обработчика разрешений
    @Inject
    lateinit var permissionHandler: PermissionHandler

    // Launcher для запроса разрешений
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var hasLocationPermission = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Регистрация launcher для разрешений
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            hasLocationPermission = fineLocationGranted || coarseLocationGranted

            if (hasLocationPermission) {
                // Инициализация получения данных о погоде при наличии разрешений
                locationViewModel.getCurrentLocation()
                weatherViewModel.fetchCurrentLocationWeather()
            }
        }
        
        // Проверка наличия разрешений
        hasLocationPermission = permissionHandler.hasLocationPermissions(this)
        
        // Запрос разрешений при необходимости
        if (!hasLocationPermission) {
            requestLocationPermissions()
        }
        
        enableEdgeToEdge()

        setContent {
            BuggyWeatherAppTheme {
                var showPermissionDialog by remember { mutableStateOf(!hasLocationPermission) }

                // Effect для обновления диалога при изменении состояния разрешений
                LaunchedEffect(hasLocationPermission) {
                    showPermissionDialog = !hasLocationPermission
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Показ диалога при отсутствии разрешений
                    if (showPermissionDialog) {
                        PermissionRequestDialog(
                            onGrantRequest = { requestLocationPermissions() },
                            onDismiss = { showPermissionDialog = false }
                        )
                    }

                    WeatherScreen(
                        viewModel = weatherViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        // Запуск получения данных при наличии разрешений
        if (hasLocationPermission) {
            locationViewModel.getCurrentLocation()
            weatherViewModel.fetchCurrentLocationWeather()
            weatherViewModel.startAutoRefresh()
        }
    }
    
    private fun requestLocationPermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    
    // Остановка обновления данных и освобождение ресурсов при уничтожении активити
    override fun onDestroy() {
        super.onDestroy()
        weatherViewModel.stopAutoRefresh()
        locationViewModel.stopLocationTracking()
    }
}

@Composable
fun PermissionRequestDialog(
    onGrantRequest: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Требуется разрешение") },
        text = { Text("Для работы приложения необходим доступ к местоположению. Пожалуйста, предоставьте разрешение.") },
        confirmButton = {
            Button(onClick = {
                onGrantRequest()
                onDismiss()
            }) {
                Text("Предоставить разрешение")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun WeatherAppPreview() {
    BuggyWeatherAppTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Weather App Preview")
        }
    }
}