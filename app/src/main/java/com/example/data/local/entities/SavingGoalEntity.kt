package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saving_goals")
data class SavingGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val targetAmount: Long,
    val currentAmount: Long = 0,
    val targetDate: Long,
    val colorHex: Long = 0xFF00A86B,
    val iconName: String = "flag",
    val isCompleted: Boolean = false
)
