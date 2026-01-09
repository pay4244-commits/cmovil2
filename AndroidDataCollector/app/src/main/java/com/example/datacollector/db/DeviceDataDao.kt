package com.example.datacollector.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.datacollector.model.DeviceData

@Dao
interface DeviceDataDao {
    @Insert
    suspend fun insert(data: DeviceData)

    @Query("SELECT * FROM device_data WHERE isSynced = 0")
    suspend fun getUnsyncedData(): List<DeviceData>

    @Query("UPDATE device_data SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Int)
    
    @Query("DELETE FROM device_data WHERE isSynced = 1")
    suspend fun deleteSynced()
}
