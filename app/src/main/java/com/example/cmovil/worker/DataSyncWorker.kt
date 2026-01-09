package com.example.cmovil.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cmovil.data.AppDatabase
import com.example.cmovil.data.DeviceData
import com.example.cmovil.network.ApiService
import com.example.cmovil.utils.BatteryHelper
import com.example.cmovil.utils.DeviceHelper
import com.example.cmovil.utils.LocationHelper
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DataSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val locationHelper = LocationHelper(context)
    private val batteryHelper = BatteryHelper(context)
    private val deviceHelper = DeviceHelper(context)
    private val database = AppDatabase.getDatabase(context)

    override suspend fun doWork(): Result {
        return try {
            // 1. Capture Data
            val location = locationHelper.getCurrentLocation()
            
            if (location == null) {
                Log.e("DataSyncWorker", "Location is null, retrying later")
                return Result.retry()
            }

            val deviceData = DeviceData(
                deviceId = deviceHelper.getDeviceId(),
                phoneNumber = deviceHelper.getPhoneNumber(),
                model = deviceHelper.getModel(),
                brand = deviceHelper.getBrand(),
                osVersion = deviceHelper.getOsVersion(),
                batteryLevel = batteryHelper.getBatteryLevel(),
                isCharging = batteryHelper.isCharging(),
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                isSynced = false
            )

            // 2. Save to Local DB
            database.deviceDataDao().insert(deviceData)

            // 3. Try to Send to Server
            // IMPORTANT: Replace BASE_URL with your actual server IP
            // If using Emulator, use http://10.0.2.2/cmovil1/
            // If using Real Device, use your PC's LAN IP, e.g., http://192.168.1.X/cmovil1/
            val retrofit = Retrofit.Builder()
                .baseUrl("http://10.0.2.2/cmovil1/") 
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiService = retrofit.create(ApiService::class.java)

            // Get all unsynced data
            val unsyncedList = database.deviceDataDao().getUnsyncedData()

            for (data in unsyncedList) {
                try {
                    val response = apiService.sendDeviceData(data)
                    if (response.isSuccessful) {
                        // Update status
                        val syncedData = data.copy(isSynced = true)
                        database.deviceDataDao().update(syncedData)
                    } else {
                        Log.e("DataSyncWorker", "Server error: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e("DataSyncWorker", "Network error: ${e.message}")
                }
            }
            
            // Optional: Clean up synced data to save space
            database.deviceDataDao().deleteSyncedData()

            Result.success()
        } catch (e: Exception) {
            Log.e("DataSyncWorker", "Error: ${e.message}")
            Result.failure()
        }
    }
}
