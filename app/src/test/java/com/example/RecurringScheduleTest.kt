package com.example

import com.example.core.model.RecurringInterval
import com.example.core.util.RecurringSchedule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class RecurringScheduleTest {
    @Test fun monthlyScheduleKeepsCalendarSemantics() {
        val start = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 15, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val next = Calendar.getInstance().apply {
            timeInMillis = RecurringSchedule.next(start.timeInMillis, RecurringInterval.MONTHLY)
        }
        assertEquals(Calendar.FEBRUARY, next.get(Calendar.MONTH))
        assertEquals(15, next.get(Calendar.DAY_OF_MONTH))
    }

    @Test fun weeklyScheduleAddsSevenDays() {
        val start = 1_800_000_000_000L
        assertEquals(start + 7L * 24 * 60 * 60 * 1000, RecurringSchedule.next(start, RecurringInterval.WEEKLY))
    }
}
