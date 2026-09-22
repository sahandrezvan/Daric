package com.example.core.util

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

data class JalaliDate(
    val year: Int,
    val month: Int, // 1 to 12
    val day: Int    // 1 to 31
)

object JalaliCalendar {

    private val persianMonthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    private val englishMonthNames = listOf(
        "Farvardin", "Ordibehesht", "Khordad",
        "Tir", "Mordad", "Shahrivar",
        "Mehr", "Aban", "Azar",
        "Dey", "Bahman", "Esfand"
    )

    fun getMonthName(month: Int, isFarsi: Boolean = true): String {
        val index = (month - 1).coerceIn(0, 11)
        return if (isFarsi) persianMonthNames[index] else englishMonthNames[index]
    }

    /**
     * Converts a timestamp (millis) to a JalaliDate
     */
    fun fromTimestamp(millis: Long): JalaliDate {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"))
        cal.timeInMillis = millis
        return gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Converts Jalali Date back to timestamp (at start of day)
     */
    fun toTimestamp(year: Int, month: Int, day: Int): Long {
        val (gy, gm, gd) = jalaliToGregorian(year, month, day)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"))
        cal.set(gy, gm - 1, gd, 12, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Algorithmic conversion Gregorian -> Jalali
     */
    fun gregorianToJalali(gYear: Int, gMonth: Int, gDay: Int): JalaliDate {
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        val gy = gYear - 1600
        val gm = gMonth - 1
        val gd = gDay - 1

        var gDayNo = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400
        for (i in 0 until gm) {
            gDayNo += gDaysInMonth[i]
        }
        if (gm > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
            gDayNo++
        }
        gDayNo += gd

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        for (i in 0..11) {
            if (jDayNo < jDaysInMonth[i]) {
                jm = i + 1
                break
            }
            jDayNo -= jDaysInMonth[i]
        }
        val jd = jDayNo + 1

        return JalaliDate(jy, jm, jd)
    }

    /**
     * Algorithmic conversion Jalali -> Gregorian
     */
    fun jalaliToGregorian(jYear: Int, jMonth: Int, jDay: Int): Triple<Int, Int, Int> {
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        val jy = jYear - 979
        val jm = jMonth - 1
        val jd = jDay - 1

        var jDayNo = 365 * jy + (jy / 33) * 8 + (jy % 33 + 3) / 4
        for (i in 0 until jm) {
            jDayNo += jDaysInMonth[i]
        }
        jDayNo += jd

        var gDayNo = jDayNo + 79
        var gy = 1600 + 400 * (gDayNo / 146097)
        gDayNo %= 146097

        var leap = true
        if (gDayNo >= 36525) {
            gDayNo--
            gy += 100 * (gDayNo / 36524)
            gDayNo %= 36524

            if (gDayNo >= 365) {
                gDayNo++
            } else {
                leap = false
            }
        }

        gy += 4 * (gDayNo / 1461)
        gDayNo %= 1461

        if (gDayNo >= 366) {
            leap = false
            gDayNo--
            gy += gDayNo / 365
            gDayNo %= 365
        }

        var gm = 0
        while (gm < 12) {
            val dim = if (gm == 1 && leap) 29 else gDaysInMonth[gm]
            if (gDayNo < dim) break
            gDayNo -= dim
            gm++
        }
        val gd = gDayNo + 1

        return Triple(gy, gm + 1, gd)
    }

    /**
     * Formats date based on calendar choice
     */
    fun formatDate(millis: Long, isShamsi: Boolean = true, isFarsi: Boolean = true): String {
        return if (isShamsi) {
            val j = fromTimestamp(millis)
            val monthName = getMonthName(j.month, isFarsi)
            "${j.day} $monthName ${j.year}"
        } else {
            val cal = Calendar.getInstance()
            cal.timeInMillis = millis
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            val d = cal.get(Calendar.DAY_OF_MONTH)
            "$y-$m-$d"
        }
    }

    fun isToday(millis: Long): Boolean {
        val now = System.currentTimeMillis()
        val d1 = fromTimestamp(millis)
        val d2 = fromTimestamp(now)
        return d1 == d2
    }

    fun isYesterday(millis: Long): Boolean {
        val yesterday = System.currentTimeMillis() - 86400000L
        val d1 = fromTimestamp(millis)
        val d2 = fromTimestamp(yesterday)
        return d1 == d2
    }

    /**
     * Adds n months to a timestamp using Jalali Calendar rules
     */
    fun addMonths(millis: Long, monthsToAdd: Int): Long {
        val j = fromTimestamp(millis)
        val totalMonths = (j.year * 12 + (j.month - 1)) + monthsToAdd
        val newYear = totalMonths / 12
        val newMonth = (totalMonths % 12) + 1
        val maxDay = when {
            newMonth in 1..6 -> 31
            newMonth in 7..11 -> 30
            else -> 29
        }
        val newDay = j.day.coerceAtMost(maxDay)
        return toTimestamp(newYear, newMonth, newDay)
    }
}
