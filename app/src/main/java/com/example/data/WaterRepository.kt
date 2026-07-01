package com.example.data

import kotlinx.coroutines.flow.Flow

class WaterRepository(private val waterDao: WaterDao) {
    fun getRecord(date: String): Flow<WaterRecord?> = waterDao.getRecordByDate(date)

    suspend fun getRecordSync(date: String): WaterRecord? = waterDao.getRecordByDateSync(date)

    suspend fun saveRecord(record: WaterRecord) = waterDao.insertOrUpdateRecord(record)

    val allRecords: Flow<List<WaterRecord>> = waterDao.getAllRecords()

    suspend fun clear() = waterDao.clearAll()
}
