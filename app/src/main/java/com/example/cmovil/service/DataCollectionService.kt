package com.example.cmovil.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.cmovil.R
import com.example.cmovil.data.AppDatabase
import com.example.cmovil.data.DeviceData
import com.example.cmovil.network.ApiService
import com.example.cmovil.utils.BatteryHelper
import com.example.cmovil.utils.DeviceHelper
import com.example.cmovil.utils.LocationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DataCollectionService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private val CHANNEL_ID = "DataCollectionChannel"

    private lateinit var locationHelper: LocationHelper
    private lateinit var batteryHelper: BatteryHelper
    private lateinit var deviceHelper: DeviceHelper
    private lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        locationHelper = LocationHelper(this)
        batteryHelper = BatteryHelper(this)
        deviceHelper = DeviceHelper(this)
        database = AppDatabase.getDatabase(this)
        
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(1, notification)

        startDataCollectionLoop()

        return START_STICKY
    }

    private fun startDataCollectionLoop() {
        serviceScope.launch(Dispatchers.IO) {
            while (true) {
                collectAndSendData()
                delay(5 * 60 * 1000) // 5 Minutes
            }
        }
    }

    private suspend fun collectAndSendData() {
        try {
            Log.d("DataCollectionService", "Collecting data...")
            // 1. Capture Data
            val location = locationHelper.getCurrentLocation()
            
            if (location == null) {
                Log.e("DataCollectionService", "Location is null")
                return
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
            // Use 10.0.2.2 for Emulator to Localhost
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
                        val syncedData = data.copy(isSynced = true)
                        database.deviceDataDao().update(syncedData)
                    } else {
                        Log.e("DataCollectionService", "Server error: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e("DataCollectionService", "Network error: ${e.message}")
                }
            }
            
            // Cleanup
            database.deviceDataDao().deleteSyncedData()

        } catch (e: Exception) {
            Log.e("DataCollectionService", "Error: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Data Collection Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Data Collection Active")
            .setContentText("Collecting device data every 5 minutes")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Ensure you have this or use default
            .build()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
