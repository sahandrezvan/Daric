package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.model.AccountType

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val initialBalance: Long,
    val currentBalance: Long,
    val colorHex: Long = 0xFF00A86B,
    val iconName: String = "account_balance",
    val note: String = "",
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
