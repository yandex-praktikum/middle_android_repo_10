package ru.yandex.buggyweatherapp.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import javax.inject.Inject

class PermissionHandler @Inject constructor() {
    
    private var locationPermissionCallback: ((Boolean) -> Unit)? = null
    
    fun registerLocationPermissions(activity: AppCompatActivity): ActivityResultLauncher<Array<String>> {
        return activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            
            val hasLocationPermission = fineLocationGranted || coarseLocationGranted
            locationPermissionCallback?.invoke(hasLocationPermission)
        }
    }
    
    fun requestLocationPermissions(
        activity: Activity, 
        permissionLauncher: ActivityResultLauncher<Array<String>>,
        callback: (Boolean) -> Unit
    ) {
        locationPermissionCallback = callback
        
        when {
            // Проверяем, есть ли разрешение на точное местоположение
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                callback(true)
            }
            
            // Проверяем, есть ли разрешение на приблизительное местоположение
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                callback(true)
            }
            
            // Запрашиваем разрешения
            else -> {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
    
    fun hasLocationPermissions(context: Context): Boolean {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        return hasFineLocation || hasCoarseLocation
    }
}