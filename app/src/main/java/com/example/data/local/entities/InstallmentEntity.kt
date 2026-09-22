package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.model.InstallmentStatus

@Entity(tableName = "installments")
data class InstallmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val totalAmount: Long,
    val totalInstallments: Int,
    val paidInstallments: Int = 0,
    val installmentAmount: Long,
    val firstDueDate: Long,
    val accountId: Long,
    val status: InstallmentStatus = InstallmentStatus.PENDING,
    val note: String = "",
    val paidIndicesWithDates: String = ""
)
