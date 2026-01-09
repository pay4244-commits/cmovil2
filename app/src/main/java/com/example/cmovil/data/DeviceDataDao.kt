package com.example.cmovil.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface DeviceDataDao {
    @Insert
    suspend fun insert(deviceData: DeviceData)

    @Query("SELECT * FROM device_data WHERE isSynced = 0")
    suspend fun getUnsyncedData(): List<DeviceData>

    @Update
    suspend fun update(deviceData: DeviceData)

    @Query("DELETE FROM device_data WHERE isSynced = 1")
    suspend fun deleteSyncedData()
}
