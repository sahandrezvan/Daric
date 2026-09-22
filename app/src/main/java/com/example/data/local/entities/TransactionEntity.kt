package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.RecurringInterval
import com.example.core.model.TransactionType

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["accountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["isSoftDeleted"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    val amount: Long,
    val accountId: Long,
    val destinationAccountId: Long? = null,
    val categoryId: Long,
    val description: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val tags: String = "",
    val attachmentPath: String? = null,
    val isRecurring: Boolean = false,
    val recurringInterval: RecurringInterval = RecurringInterval.NONE,
    val isSoftDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
