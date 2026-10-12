package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.FraudAlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FraudAlertDao {

    @Query("SELECT * FROM fraud_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<FraudAlertEntity>>

    @Query("SELECT * FROM fraud_alerts WHERE isResolved = 0 ORDER BY timestamp DESC")
    fun getUnresolvedAlerts(): Flow<List<FraudAlertEntity>>

    @Query("SELECT COUNT(*) FROM fraud_alerts WHERE isResolved = 0")
    fun getUnresolvedAlertCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: FraudAlertEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<FraudAlertEntity>): List<Long>

    @Update
    suspend fun update(alert: FraudAlertEntity)

    @Delete
    suspend fun delete(alert: FraudAlertEntity)

    @Query("DELETE FROM fraud_alerts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM fraud_alerts")
    suspend fun deleteAll()

    @Query("UPDATE fraud_alerts SET isResolved = 1 WHERE id = :id")
    suspend fun resolveAlert(id: Long)
}
