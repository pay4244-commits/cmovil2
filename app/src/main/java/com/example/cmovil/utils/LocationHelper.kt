package com.example.cmovil.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocationHelper(private val context: Context) {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        return withContext(Dispatchers.IO) {
            try {
                // Try to get last known location first
                var location = Tasks.await(fusedLocationClient.lastLocation)
                
                if (location == null) {
                     // If null, try to get current location (might take longer)
                     // Note: For a background worker, getCurrentLocation might be tricky without a foreground service running
                     // But we will use Priority.PRIORITY_BALANCED_POWER_ACCURACY
                     val task = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                     location = Tasks.await(task)
                }
                location
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
