package com.example.core.util

import com.example.core.model.DigitFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {

    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun toPersianDigits(text: String): String {
        val sb = java.lang.StringBuilder()
        for (c in text) {
            if (c in '0'..'9') {
                sb.append(persianDigits[c - '0'])
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    fun toEnglishDigits(text: String): String {
        val sb = java.lang.StringBuilder()
        for (c in text) {
            val idx = persianDigits.indexOf(c)
            if (idx != -1) {
                sb.append(('0' + idx))
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    fun format(
        amount: Long,
        currency: String = "تومان",
        digitFormat: DigitFormat = DigitFormat.PERSIAN,
        includeCurrency: Boolean = true
    ): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        symbols.groupingSeparator = ','
        val formatter = DecimalFormat("#,###", symbols)
        val formattedNumber = formatter.format(amount)

        val result = if (digitFormat == DigitFormat.PERSIAN) {
            toPersianDigits(formattedNumber)
        } else {
            formattedNumber
        }

        return if (includeCurrency) {
            "$result $currency"
        } else {
            result
        }
    }

    fun formatPercentage(
        value: Double,
        digitFormat: DigitFormat = DigitFormat.PERSIAN
    ): String {
        val formatted = String.format(Locale.US, "%.1f%%", value)
        return if (digitFormat == DigitFormat.PERSIAN) {
            toPersianDigits(formatted)
        } else {
            formatted
        }
    }
}
