package com.example.datacollector.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "device_data")
data class DeviceData(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    @SerializedName("device_id")
    val deviceId: String,
    
    @SerializedName("phone_number")
    val phoneNumber: String?,
    
    @SerializedName("model")
    val model: String,
    
    @SerializedName("brand")
    val brand: String,
    
    @SerializedName("os_version")
    val osVersion: String,
    
    @SerializedName("battery_level")
    val batteryLevel: Int,
    
    @SerializedName("is_charging")
    val isCharging: Boolean,
    
    @SerializedName("latitude")
    val latitude: Double?,
    
    @SerializedName("longitude")
    val longitude: Double?,
    
    @SerializedName("altitude")
    val altitude: Double?,
    
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    // Flag to check if synced to server
    val isSynced: Boolean = false
)
