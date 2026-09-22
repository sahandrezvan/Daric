package com.example.core.util

import com.example.core.model.InstallmentStatus
import com.example.data.local.entities.InstallmentEntity

data class InstallmentTaskItem(
    val index: Int, // 1 to totalInstallments
    val scheduledDueDate: Long,
    val isPaid: Boolean,
    val paidDate: Long? = null
)

object InstallmentScheduleHelper {

    fun scheduleStart(inst: InstallmentEntity): Long {
        if (inst.scheduleStartDate > 0L) return inst.scheduleStartDate
        return JalaliCalendar.addMonths(inst.firstDueDate, -inst.paidInstallments.coerceAtLeast(0))
    }

    fun installmentAmount(totalAmount: Long, totalInstallments: Int): Long {
        if (totalAmount <= 0L || totalInstallments <= 0) return 0L
        return (totalAmount + totalInstallments - 1L) / totalInstallments
    }

    fun amountFor(inst: InstallmentEntity, itemIndex: Int): Long {
        if (itemIndex !in 1..inst.totalInstallments) return 0L
        if (itemIndex < inst.totalInstallments) return inst.installmentAmount
        return (inst.totalAmount - inst.installmentAmount * (inst.totalInstallments - 1L))
            .coerceAtLeast(0L)
    }

    fun parsePaidMap(paidIndicesWithDates: String, paidInstallments: Int): Map<Int, Long> {
        val map = mutableMapOf<Int, Long>()
        if (paidIndicesWithDates.isNotBlank()) {
            paidIndicesWithDates.split(",").forEach { entry ->
                val parts = entry.split(":")
                if (parts.isNotEmpty()) {
                    val idx = parts[0].trim().toIntOrNull()
                    if (idx != null) {
                        val ts = if (parts.size > 1) parts[1].trim().toLongOrNull() ?: 0L else 0L
                        map[idx] = ts
                    }
                }
            }
        } else {
            // fallback: items 1..paidInstallments are considered paid
            for (i in 1..paidInstallments) {
                map[i] = 0L
            }
        }
        return map
    }

    fun serializePaidMap(map: Map<Int, Long>): String {
        return map.entries.sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value}" }
    }

    fun generateSchedule(inst: InstallmentEntity): List<InstallmentTaskItem> {
        val paidMap = parsePaidMap(inst.paidIndicesWithDates, inst.paidInstallments)
        val anchor = scheduleStart(inst)
        val items = mutableListOf<InstallmentTaskItem>()
        for (i in 1..inst.totalInstallments) {
            val scheduledDate = JalaliCalendar.addMonths(anchor, i - 1)
            val isPaid = paidMap.containsKey(i)
            val paidTs = paidMap[i]?.takeIf { it > 0 }
            items.add(
                InstallmentTaskItem(
                    index = i,
                    scheduledDueDate = scheduledDate,
                    isPaid = isPaid,
                    paidDate = paidTs
                )
            )
        }
        return items
    }

    fun nextUnpaid(inst: InstallmentEntity): InstallmentTaskItem? =
        generateSchedule(inst).firstOrNull { !it.isPaid }

    /**
     * Toggles an installment item (paid / unpaid) and returns updated InstallmentEntity
     */
    fun toggleItem(
        inst: InstallmentEntity,
        itemIndex: Int,
        isPaid: Boolean,
        paidTimestamp: Long = System.currentTimeMillis()
    ): InstallmentEntity {
        val map = parsePaidMap(inst.paidIndicesWithDates, inst.paidInstallments).toMutableMap()
        if (isPaid) {
            map[itemIndex] = paidTimestamp
        } else {
            map.remove(itemIndex)
        }
        val newPaidCount = map.size
        val isFinished = newPaidCount >= inst.totalInstallments
        val serialized = serializePaidMap(map)

        // Find the earliest unpaid item due date
        val anchor = scheduleStart(inst)
        var nextDue = inst.firstDueDate
        for (i in 1..inst.totalInstallments) {
            if (!map.containsKey(i)) {
                nextDue = JalaliCalendar.addMonths(anchor, i - 1)
                break
            }
        }

        return inst.copy(
            paidInstallments = newPaidCount,
            firstDueDate = nextDue,
            scheduleStartDate = anchor,
            status = if (isFinished) InstallmentStatus.PAID else InstallmentStatus.PENDING,
            paidIndicesWithDates = serialized
        )
    }
}
