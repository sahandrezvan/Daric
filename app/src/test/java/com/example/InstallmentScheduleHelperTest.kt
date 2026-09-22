package com.example

import com.example.core.util.InstallmentScheduleHelper
import com.example.core.util.JalaliCalendar
import com.example.data.local.entities.InstallmentEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallmentScheduleHelperTest {

    @Test
    fun calculatesMonthlyAmountAndAdjustsFinalPayment() {
        val installment = InstallmentEntity(
            title = "وام تست",
            totalAmount = 100L,
            totalInstallments = 3,
            installmentAmount = InstallmentScheduleHelper.installmentAmount(100L, 3),
            firstDueDate = JalaliCalendar.toTimestamp(1405, 1, 15),
            accountId = 1L
        )

        assertEquals(34L, installment.installmentAmount)
        assertEquals(34L, InstallmentScheduleHelper.amountFor(installment, 1))
        assertEquals(32L, InstallmentScheduleHelper.amountFor(installment, 3))
    }

    @Test
    fun payingAnItemKeepsOriginalMonthlySchedule() {
        val start = JalaliCalendar.toTimestamp(1405, 1, 31)
        val installment = InstallmentEntity(
            title = "وام تست",
            totalAmount = 300L,
            totalInstallments = 3,
            installmentAmount = 100L,
            firstDueDate = start,
            scheduleStartDate = start,
            accountId = 1L
        )

        val updated = InstallmentScheduleHelper.toggleItem(installment, 1, true, 123L)
        val schedule = InstallmentScheduleHelper.generateSchedule(updated)

        assertTrue(schedule[0].isPaid)
        assertFalse(schedule[1].isPaid)
        assertEquals(start, schedule[0].scheduledDueDate)
        assertEquals(JalaliCalendar.addMonths(start, 1), schedule[1].scheduledDueDate)
        assertEquals(schedule[1].scheduledDueDate, updated.firstDueDate)
    }
}
