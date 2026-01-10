package com.example.datacollector.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.datacollector.R
import com.example.datacollector.api.RetrofitClient
import com.example.datacollector.db.AppDatabase
import com.example.datacollector.model.DeviceData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.*

class DataCollectionService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var db: AppDatabase
    
    // Interval: 5 minutes
    private val INTERVAL = 5 * 60 * 1000L 

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        db = AppDatabase.getDatabase(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(1, notification)

        startCollectionLoop()

        return START_STICKY
    }

    private fun startCollectionLoop() {
        serviceScope.launch {
            while (isActive) {
                collectAndSendData()
                // Try to sync offline data
                syncOfflineData()
                delay(INTERVAL)
            }
        }
    }

    private suspend fun collectAndSendData() {
        try {
            val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            val model = Build.MODEL
            val brand = Build.MANUFACTURER
            val osVersion = Build.VERSION.RELEASE
            
            val batteryStatus = getBatteryStatus()
            val phoneNumber = getPhoneNumber()
            val location = getLocation()

            val data = DeviceData(
                deviceId = deviceId,
                phoneNumber = phoneNumber,
                model = model,
                brand = brand,
                osVersion = osVersion,
                batteryLevel = batteryStatus.first,
                isCharging = batteryStatus.second,
                latitude = location?.latitude,
                longitude = location?.longitude,
                altitude = location?.altitude
            )

            // Try to send to API
            try {
                val response = RetrofitClient.instance.sendData(data)
                if (response.isSuccessful) {
                    Log.d("DataService", "Data sent successfully")
                    // If successful, we don't strictly need to save it locally unless we want a full log history.
                    // Requirement says "Almacenarse en una base de datos MySQL", implying the server is the source of truth.
                    // But for offline-first, we save if fail.
                } else {
                    Log.e("DataService", "Server error, saving locally")
                    db.deviceDataDao().insert(data)
                }
            } catch (e: Exception) {
                Log.e("DataService", "Network error, saving locally: ${e.message}")
                db.deviceDataDao().insert(data)
            }

        } catch (e: Exception) {
            Log.e("DataService", "Error collecting data: ${e.message}")
        }
    }

    private suspend fun syncOfflineData() {
        val unsynced = db.deviceDataDao().getUnsyncedData()
        for (item in unsynced) {
            try {
                val response = RetrofitClient.instance.sendData(item)
                if (response.isSuccessful) {
                    db.deviceDataDao().markAsSynced(item.id)
                    // Or delete: db.deviceDataDao().delete(item)
                }
            } catch (e: Exception) {
                // Keep for next try
            }
        }
        // Cleanup synced data to save space
        db.deviceDataDao().deleteSynced()
    }

    private fun getBatteryStatus(): Pair<Int, Boolean> {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val status = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return Pair(level, isCharging)
    }

    private fun getPhoneNumber(): String? {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        // Note: This often returns null or empty string on modern devices/carriers
        try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            return tm.line1Number
        } catch (e: SecurityException) {
            return null
        }
    }

    private suspend fun getLocation(): Location? {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        
        return suspendCancellableCoroutine { cont ->
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    cont.resume(location, null)
                }
                .addOnFailureListener {
                    cont.resume(null, null)
                }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "DataCollectionChannel",
                "Data Collection Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "DataCollectionChannel")
            .setContentTitle("Data Collector Running")
            .setContentText("Collecting device data every 5 minutes")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
