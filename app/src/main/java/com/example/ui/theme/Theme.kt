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
    val normalized = AccentColorChoice.normalize(accent)
    return ThemeStyleCatalog[normalized]?.previewPrimary ?: AccentEmerald
}

fun resolveThemeSurfaces(
    accent: AccentColorChoice,
    isDark: Boolean
): ThemeSurfaces {
    val normalized = AccentColorChoice.normalize(accent)
    val def = ThemeStyleCatalog[normalized] ?: ThemeStyleCatalog.getValue(AccentColorChoice.EMERALD)
    return if (isDark) def.dark else def.light
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

    val surfaces = resolveThemeSurfaces(accentColor, isDark)

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = surfaces.primary,
            onPrimary = surfaces.onPrimary,
            primaryContainer = surfaces.primary.copy(alpha = 0.16f),
            onPrimaryContainer = surfaces.primary,
            secondary = surfaces.onSurfaceVariant,
            onSecondary = surfaces.background,
            secondaryContainer = surfaces.surfaceVariant,
            onSecondaryContainer = surfaces.onBackground,
            tertiary = surfaces.primary,
            onTertiary = surfaces.onPrimary,
            background = surfaces.background,
            onBackground = surfaces.onBackground,
            surface = surfaces.surface,
            onSurface = surfaces.onBackground,
            surfaceVariant = surfaces.surfaceVariant,
            onSurfaceVariant = surfaces.onSurfaceVariant,
            outline = surfaces.outline,
            outlineVariant = surfaces.outline.copy(alpha = 0.6f),
            error = ExpenseRed,
            onError = Color.White,
            errorContainer = ExpenseRed.copy(alpha = 0.16f),
            onErrorContainer = ExpenseRed,
            inverseSurface = surfaces.onBackground,
            inverseOnSurface = surfaces.background,
            inversePrimary = surfaces.primary,
            scrim = Color.Black.copy(alpha = 0.5f)
        )
    } else {
        lightColorScheme(
            primary = surfaces.primary,
            onPrimary = surfaces.onPrimary,
            primaryContainer = surfaces.primary.copy(alpha = 0.10f),
            onPrimaryContainer = surfaces.primary,
            secondary = surfaces.onSurfaceVariant,
            onSecondary = Color.White,
            secondaryContainer = surfaces.surfaceVariant,
            onSecondaryContainer = surfaces.onBackground,
            tertiary = surfaces.primary,
            onTertiary = surfaces.onPrimary,
            background = surfaces.background,
            onBackground = surfaces.onBackground,
            surface = surfaces.surface,
            onSurface = surfaces.onBackground,
            surfaceVariant = surfaces.surfaceVariant,
            onSurfaceVariant = surfaces.onSurfaceVariant,
            outline = surfaces.outline,
            outlineVariant = surfaces.outline.copy(alpha = 0.7f),
            error = ExpenseRed,
            onError = Color.White,
            errorContainer = ExpenseRed.copy(alpha = 0.10f),
            onErrorContainer = ExpenseRed,
            inverseSurface = surfaces.onBackground,
            inverseOnSurface = surfaces.background,
            inversePrimary = surfaces.primary,
            scrim = Color.Black.copy(alpha = 0.4f)
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
            shapes = DaricShapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    DaricTheme(content = content)
}
