package com.example.cmovil

import android.content.Context
import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DataRepository(private val context: Context, private val db: AppDatabase) {

    // IMPORTANT: Change this URL to your computer's IP if running on a real device
    // 10.0.2.2 is localhost for Android Emulator
    private val BASE_URL = "http://10.114.78.5/cmovil/" 

    private val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    suspend fun collectAndSendData(data: DeviceData) {
        try {
            val response = api.sendData(data)
            if (response.isSuccessful) {
                Log.d("DataRepository", "Data sent successfully: ${response.body()?.message}")
                // Also try to send pending data if any
                syncPendingData()
            } else {
                Log.e("DataRepository", "Failed to send data: ${response.code()}")
                saveToLocal(data)
            }
        } catch (e: Exception) {
            Log.e("DataRepository", "Network error: ${e.message}")
            saveToLocal(data)
        }
    }

    private suspend fun saveToLocal(data: DeviceData) {
        db.deviceDataDao().insert(data)
        Log.d("DataRepository", "Data saved locally")
    }

    suspend fun syncPendingData() {
        val pendingData = db.deviceDataDao().getAll()
        if (pendingData.isNotEmpty()) {
            Log.d("DataRepository", "Syncing ${pendingData.size} pending records...")
            for (data in pendingData) {
                try {
                    val response = api.sendData(data)
                    if (response.isSuccessful) {
                        db.deviceDataDao().delete(data)
                        Log.d("DataRepository", "Synced record ${data.id}")
                    }
                } catch (e: Exception) {
                    // Stop syncing if network fails again
                    Log.e("DataRepository", "Sync failed: ${e.message}")
                    break
                }
            }
        }
    }
}
