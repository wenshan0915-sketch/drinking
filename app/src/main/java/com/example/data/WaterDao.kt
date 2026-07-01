package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_records WHERE date = :date")
    fun getRecordByDate(date: String): Flow<WaterRecord?>

    @Query("SELECT * FROM water_records WHERE date = :date")
    suspend fun getRecordByDateSync(date: String): WaterRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRecord(record: WaterRecord)

    @Query("SELECT * FROM water_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<WaterRecord>>

    @Query("DELETE FROM water_records")
    suspend fun clearAll()
}
