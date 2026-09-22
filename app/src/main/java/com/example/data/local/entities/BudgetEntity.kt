package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val monthlyLimit: Long,
    val monthYear: String, // e.g. "1405-07" or "2026-09"
    val alertThreshold: Int = 80 // alert at 80% and 100%
)
