package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.core.model.AccentColorChoice
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode

fun getPrimaryColor(accent: AccentColorChoice): Color {
    return when (accent) {
        AccentColorChoice.EMERALD -> AccentEmerald
        AccentColorChoice.SAPPHIRE -> AccentSapphire
        AccentColorChoice.AMBER -> AccentAmber
        AccentColorChoice.RUBY -> AccentRuby
        AccentColorChoice.VIOLET -> AccentViolet
    }
}

@Composable
fun DaricTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    accentColor: AccentColorChoice = AccentColorChoice.EMERALD,
    language: AppLanguage = AppLanguage.FA,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    val primary = getPrimaryColor(accentColor)

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color.Black,
            primaryContainer = primary.copy(alpha = 0.2f),
            onPrimaryContainer = primary,
            background = DarkBackground,
            onBackground = DarkTextPrimary,
            surface = DarkSurface,
            onSurface = DarkTextPrimary,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = DarkTextSecondary,
            outline = DarkSurfaceBorder,
            error = ExpenseRed
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = 0.15f),
            onPrimaryContainer = primary,
            background = LightBackground,
            onBackground = LightTextPrimary,
            surface = LightSurface,
            onSurface = LightTextPrimary,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = LightTextSecondary,
            outline = LightSurfaceBorder,
            error = ExpenseRed
        )
    }

    val layoutDirection = if (language == AppLanguage.FA) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Backward compatibility alias for tests
@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    DaricTheme(content = content)
}
