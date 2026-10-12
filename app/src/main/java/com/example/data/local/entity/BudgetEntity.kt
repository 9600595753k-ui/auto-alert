package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val category: String, // e.g. "Food & Dining"
    val monthlyLimitPaise: Long, // e.g. 1500000L = ₹15,000.00
    val alertThresholdPercent: Int = 80 // alert at 80% usage
)
