package com.example.data.local

import androidx.room.TypeConverter
import com.example.core.model.AccentColorChoice
import com.example.core.model.AccountType
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.core.model.CalendarType
import com.example.core.model.DebtType
import com.example.core.model.DigitFormat
import com.example.core.model.InstallmentStatus
import com.example.core.model.RecurringInterval
import com.example.core.model.TransactionType

class Converters {

    @TypeConverter
    fun fromAccountType(value: AccountType): String = value.name

    @TypeConverter
    fun toAccountType(value: String): AccountType = try {
        AccountType.valueOf(value)
    } catch (e: Exception) {
        AccountType.OTHER
    }

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = try {
        TransactionType.valueOf(value)
    } catch (e: Exception) {
        TransactionType.EXPENSE
    }

    @TypeConverter
    fun fromRecurringInterval(value: RecurringInterval): String = value.name

    @TypeConverter
    fun toRecurringInterval(value: String): RecurringInterval = try {
        RecurringInterval.valueOf(value)
    } catch (e: Exception) {
        RecurringInterval.NONE
    }

    @TypeConverter
    fun fromInstallmentStatus(value: InstallmentStatus): String = value.name

    @TypeConverter
    fun toInstallmentStatus(value: String): InstallmentStatus = try {
        InstallmentStatus.valueOf(value)
    } catch (e: Exception) {
        InstallmentStatus.PENDING
    }

    @TypeConverter
    fun fromDebtType(value: DebtType): String = value.name

    @TypeConverter
    fun toDebtType(value: String): DebtType = try {
        DebtType.valueOf(value)
    } catch (e: Exception) {
        DebtType.DEBTOR
    }

    @TypeConverter
    fun fromThemeMode(value: AppThemeMode): String = value.name

    @TypeConverter
    fun toThemeMode(value: String): AppThemeMode = try {
        AppThemeMode.valueOf(value)
    } catch (e: Exception) {
        AppThemeMode.SYSTEM
    }

    @TypeConverter
    fun fromAccentColor(value: AccentColorChoice): String = value.name

    @TypeConverter
    fun toAccentColor(value: String): AccentColorChoice = try {
        AccentColorChoice.normalize(AccentColorChoice.valueOf(value))
    } catch (e: Exception) {
        AccentColorChoice.EMERALD
    }

    @TypeConverter
    fun fromAppLanguage(value: AppLanguage): String = value.name

    @TypeConverter
    fun toAppLanguage(value: String): AppLanguage = try {
        AppLanguage.valueOf(value)
    } catch (e: Exception) {
        AppLanguage.FA
    }

    @TypeConverter
    fun fromCalendarType(value: CalendarType): String = value.name

    @TypeConverter
    fun toCalendarType(value: String): CalendarType = try {
        CalendarType.valueOf(value)
    } catch (e: Exception) {
        CalendarType.SHAMSI
    }

    @TypeConverter
    fun fromDigitFormat(value: DigitFormat): String = value.name

    @TypeConverter
    fun toDigitFormat(value: String): DigitFormat = try {
        DigitFormat.valueOf(value)
    } catch (e: Exception) {
        DigitFormat.PERSIAN
    }
}
