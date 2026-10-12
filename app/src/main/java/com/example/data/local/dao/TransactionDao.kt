package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE isExcluded = 0 ORDER BY timestamp DESC")
    fun getAllActiveTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :start AND timestamp <= :end AND isExcluded = 0 ORDER BY timestamp DESC")
    fun getTransactionsBetween(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun getTransactionById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun findByFingerprint(fingerprint: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT DISTINCT bankName FROM transactions WHERE bankName != '' ORDER BY bankName ASC")
    fun getDistinctBanks(): Flow<List<String>>

    @Query("SELECT DISTINCT category FROM transactions WHERE category != '' ORDER BY category ASC")
    fun getDistinctCategories(): Flow<List<String>>

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM transactions WHERE type = 'CREDIT' AND status = 'COMPLETED' AND isExcluded = 0 AND timestamp >= :startTime AND timestamp <= :endTime")
    fun getTotalIncomePaise(startTime: Long, endTime: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM transactions WHERE type = 'DEBIT' AND status = 'COMPLETED' AND isExcluded = 0 AND timestamp >= :startTime AND timestamp <= :endTime")
    fun getTotalExpensePaise(startTime: Long, endTime: Long): Flow<Long>
}
