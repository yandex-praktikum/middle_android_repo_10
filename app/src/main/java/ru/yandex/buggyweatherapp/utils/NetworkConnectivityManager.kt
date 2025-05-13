package ru.yandex.buggyweatherapp.utils

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkConnectivityManager @Inject constructor(
    private val application: Application
) {
    private companion object {
        private const val TAG = "NetworkConnManager"
    }
    
    private val connectivityManager = 
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
    // StateFlow для текущего состояния подключения с начальным значением
    private val _isNetworkAvailable = MutableStateFlow(checkNetworkAvailability())
    
    // Публичный StateFlow с текущим состоянием сети
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable
    
    // Реактивный Flow для подробных событий подключения
    val networkEvents: Flow<NetworkState> = createNetworkStateFlow()
    
    init {
        // Регистрируем колбэк сразу при создании
        setupNetworkCallback()
        Log.d(TAG, "NetworkConnectivityManager initialized, current state: ${_isNetworkAvailable.value}")
    }
    
    /**
     * Настройка колбэка для мониторинга изменений подключения
     */
    private fun setupNetworkCallback() {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available")
                _isNetworkAvailable.value = true
            }
            
            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost")
                _isNetworkAvailable.value = false
            }
            
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                )
                val hasValidated = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )
                
                Log.d(TAG, "Network capabilities changed: internet=$hasInternet, validated=$hasValidated")
                _isNetworkAvailable.value = hasInternet && hasValidated
            }
            
            override fun onUnavailable() {
                Log.d(TAG, "Network unavailable")
                _isNetworkAvailable.value = false
            }
        }
        
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) // Убедиться, что есть доступ в интернет
            .build()
            
        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering network callback", e)
        }
    }
    
    /**
     * Создает реактивный Flow для мониторинга детальных событий подключения
     */
    private fun createNetworkStateFlow(): Flow<NetworkState> = callbackFlow {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(NetworkState.Available)
            }
            
            override fun onLosing(network: Network, maxMsToLive: Int) {
                trySend(NetworkState.Losing(maxMsToLive))
            }
            
            override fun onLost(network: Network) {
                trySend(NetworkState.Lost)
            }
            
            override fun onUnavailable() {
                trySend(NetworkState.Unavailable)
            }
            
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val state = NetworkState.CapabilitiesChanged(
                    hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
                    hasValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                    hasCellular = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR),
                    hasWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
                    hasEthernet = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                )
                trySend(state)
            }
        }
        
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering network callback for flow", e)
        }
        
        // Сразу отправляем текущее состояние
        val initialState = if (checkNetworkAvailability()) {
            NetworkState.Available
        } else {
            NetworkState.Unavailable
        }
        trySend(initialState)
        
        // Закрываем колбэк при отмене Flow
        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }.distinctUntilChanged() // Выдаем только изменившиеся значения
    
    /**
     * Синхронно проверяет доступность сети
     */
    fun isNetworkAvailable(): Boolean {
        return checkNetworkAvailability()
    }
    
    /**
     * Проверяет текущее состояние подключения к сети
     * Реализация зависит от версии Android
     */
    private fun checkNetworkAvailability(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected == true
        }
    }
}

/**
 * Sealed класс для представления различных состояний сети
 */
sealed class NetworkState {
    object Available : NetworkState()
    object Unavailable : NetworkState()
    object Lost : NetworkState()
    data class Losing(val maxMsToLive: Int) : NetworkState()
    data class CapabilitiesChanged(
        val hasInternet: Boolean,
        val hasValidated: Boolean,
        val hasCellular: Boolean,
        val hasWifi: Boolean,
        val hasEthernet: Boolean
    ) : NetworkState()
}