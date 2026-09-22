package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.model.AccentColorChoice
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.core.model.CalendarType
import com.example.core.model.DigitFormat

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val userName: String = "کاربر داریک",
    val currency: String = "تومان",
    val language: AppLanguage = AppLanguage.FA,
    val digitFormat: DigitFormat = DigitFormat.PERSIAN,
    val calendarType: CalendarType = CalendarType.SHAMSI,
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val accentColor: AccentColorChoice = AccentColorChoice.EMERALD,
    val isPinEnabled: Boolean = false,
    val pinCode: String = "",
    val isBiometricEnabled: Boolean = false,
    val isOnboardingCompleted: Boolean = false,
    val lastBackupTimestamp: Long = 0L,
    val autoBackupFrequency: String = "خاموش" // خاموش, روزانه, هفتگی, ماهانه
)
