package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PaymentReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentReminderDao {

    @Query("SELECT * FROM payment_reminders ORDER BY dueDateTimestamp ASC")
    fun getAllReminders(): Flow<List<PaymentReminderEntity>>

    @Query("SELECT * FROM payment_reminders WHERE isPaid = 0 ORDER BY dueDateTimestamp ASC")
    fun getPendingReminders(): Flow<List<PaymentReminderEntity>>

    @Query("SELECT * FROM payment_reminders WHERE id = :id LIMIT 1")
    fun getById(id: Long): Flow<PaymentReminderEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: PaymentReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<PaymentReminderEntity>): List<Long>

    @Update
    suspend fun update(reminder: PaymentReminderEntity)

    @Delete
    suspend fun delete(reminder: PaymentReminderEntity)

    @Query("DELETE FROM payment_reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM payment_reminders")
    suspend fun deleteAll()

    @Query("UPDATE payment_reminders SET isPaid = 1 WHERE id = :id")
    suspend fun markAsPaid(id: Long)

    @Query("UPDATE payment_reminders SET isSnoozed = 1, reminderTimestamp = :newReminderTime WHERE id = :id")
    suspend fun snoozeReminder(id: Long, newReminderTime: Long)
}
