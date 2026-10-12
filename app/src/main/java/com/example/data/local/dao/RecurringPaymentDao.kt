package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.RecurringPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringPaymentDao {

    @Query("SELECT * FROM recurring_payments ORDER BY nextDueTimestamp ASC")
    fun getAllRecurringPayments(): Flow<List<RecurringPaymentEntity>>

    @Query("SELECT * FROM recurring_payments WHERE status = 'ACTIVE' ORDER BY nextDueTimestamp ASC")
    fun getActiveRecurringPayments(): Flow<List<RecurringPaymentEntity>>

    @Query("SELECT * FROM recurring_payments WHERE id = :id LIMIT 1")
    fun getById(id: Long): Flow<RecurringPaymentEntity?>

    @Query("SELECT * FROM recurring_payments WHERE LOWER(merchantName) = LOWER(:merchant) LIMIT 1")
    suspend fun findByMerchant(merchant: String): RecurringPaymentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurringPayment: RecurringPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<RecurringPaymentEntity>): List<Long>

    @Update
    suspend fun update(recurringPayment: RecurringPaymentEntity)

    @Delete
    suspend fun delete(recurringPayment: RecurringPaymentEntity)

    @Query("DELETE FROM recurring_payments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recurring_payments")
    suspend fun deleteAll()

    @Query("SELECT COALESCE(SUM(expectedAmountPaise), 0) FROM recurring_payments WHERE status = 'ACTIVE'")
    fun getTotalMonthlyRecurringCommitmentPaise(): Flow<Long>
}
