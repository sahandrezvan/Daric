package com.example.core.util

import com.example.core.model.RecurringInterval
import java.util.Calendar

object RecurringSchedule {
    fun next(timestamp: Long, interval: RecurringInterval): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        when (interval) {
            RecurringInterval.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            RecurringInterval.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RecurringInterval.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            RecurringInterval.YEARLY -> calendar.add(Calendar.YEAR, 1)
            RecurringInterval.NONE -> return timestamp
        }
        return calendar.timeInMillis
    }
}
