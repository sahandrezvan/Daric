package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.model.TransactionType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val nameFa: String,
    val type: TransactionType,
    val iconName: String,
    val colorHex: Long,
    val isDefault: Boolean = true
)
