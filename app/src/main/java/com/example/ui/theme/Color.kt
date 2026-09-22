package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ── Semantic financial colors (muted, professional) ──────────────────────────
val IncomeGreen = Color(0xFF2D9B6F)
val ExpenseRed = Color(0xFFD64545)
val TransferBlue = Color(0xFF4A7FC1)
val WarningAmber = Color(0xFFC9922A)
val GoldAsset = Color(0xFFB8952A)

// ── Shared neutrals ──────────────────────────────────────────────────────────
val InkBlack = Color(0xFF0E1110)
val InkSoft = Color(0xFF1A1F1D)
val PaperWhite = Color(0xFFFAFAF9)
val MistGray = Color(0xFFF2F3F2)

/**
 * Full surface + accent palette for one curated visual theme.
 * Each style ships coordinated light and dark variants.
 */
data class ThemeSurfaces(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val outline: Color,
    val onBackground: Color,
    val onSurfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color
)

data class ThemeStyleDefinition(
    val light: ThemeSurfaces,
    val dark: ThemeSurfaces,
    val previewPrimary: Color,
    val previewBg: Color
)

// ── 1. Obsidian — AMOLED black + soft mint ───────────────────────────────────
private val ObsidianDark = ThemeSurfaces(
    background = Color(0xFF050505),
    surface = Color(0xFF111111),
    surfaceVariant = Color(0xFF1A1A1A),
    outline = Color(0xFF2A2A2A),
    onBackground = Color(0xFFF2F2F2),
    onSurfaceVariant = Color(0xFF8A8A8A),
    primary = Color(0xFF3DCF9A),
    onPrimary = Color(0xFF003822)
)
private val ObsidianLight = ThemeSurfaces(
    background = Color(0xFFF7F9F8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEEF2F0),
    outline = Color(0xFFD8E0DC),
    onBackground = Color(0xFF121816),
    onSurfaceVariant = Color(0xFF5C6B64),
    primary = Color(0xFF0D8F63),
    onPrimary = Color.White
)

// ── 2. Pearl — clean paper + charcoal ink ────────────────────────────────────
private val PearlLight = ThemeSurfaces(
    background = Color(0xFFF8F8F7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0F0EE),
    outline = Color(0xFFE2E2DF),
    onBackground = Color(0xFF161616),
    onSurfaceVariant = Color(0xFF6B6B68),
    primary = Color(0xFF2C2C2A),
    onPrimary = Color.White
)
private val PearlDark = ThemeSurfaces(
    background = Color(0xFF0C0C0B),
    surface = Color(0xFF161615),
    surfaceVariant = Color(0xFF222220),
    outline = Color(0xFF333330),
    onBackground = Color(0xFFF4F4F2),
    onSurfaceVariant = Color(0xFF9A9A96),
    primary = Color(0xFFE8E8E4),
    onPrimary = Color(0xFF1A1A18)
)

// ── 3. Midnight — deep navy + soft sky ───────────────────────────────────────
private val MidnightDark = ThemeSurfaces(
    background = Color(0xFF080B12),
    surface = Color(0xFF101522),
    surfaceVariant = Color(0xFF181E2E),
    outline = Color(0xFF252D42),
    onBackground = Color(0xFFE8ECF4),
    onSurfaceVariant = Color(0xFF8B95AD),
    primary = Color(0xFF6B9FD4),
    onPrimary = Color(0xFF001A33)
)
private val MidnightLight = ThemeSurfaces(
    background = Color(0xFFF4F6FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8ECF4),
    outline = Color(0xFFD0D7E4),
    onBackground = Color(0xFF12161F),
    onSurfaceVariant = Color(0xFF5A6578),
    primary = Color(0xFF2F5F98),
    onPrimary = Color.White
)

// ── 4. Nordic — cool fog + steel blue ────────────────────────────────────────
private val NordicLight = ThemeSurfaces(
    background = Color(0xFFF5F6F8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEBEDF1),
    outline = Color(0xFFD5D9E0),
    onBackground = Color(0xFF1A1D24),
    onSurfaceVariant = Color(0xFF636B7A),
    primary = Color(0xFF4A6278),
    onPrimary = Color.White
)
private val NordicDark = ThemeSurfaces(
    background = Color(0xFF0A0C10),
    surface = Color(0xFF13161C),
    surfaceVariant = Color(0xFF1C2028),
    outline = Color(0xFF2C323C),
    onBackground = Color(0xFFECEEF2),
    onSurfaceVariant = Color(0xFF9098A8),
    primary = Color(0xFF8AA0B4),
    onPrimary = Color(0xFF0E141A)
)

// ── 5. Graphite — charcoal + muted teal ──────────────────────────────────────
private val GraphiteDark = ThemeSurfaces(
    background = Color(0xFF0A0B0C),
    surface = Color(0xFF141617),
    surfaceVariant = Color(0xFF1E2123),
    outline = Color(0xFF2E3336),
    onBackground = Color(0xFFF0F1F1),
    onSurfaceVariant = Color(0xFF8E9498),
    primary = Color(0xFF4DB6A0),
    onPrimary = Color(0xFF002820)
)
private val GraphiteLight = ThemeSurfaces(
    background = Color(0xFFF6F7F7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFECF0EF),
    outline = Color(0xFFD4DCDA),
    onBackground = Color(0xFF141616),
    onSurfaceVariant = Color(0xFF5A6361),
    primary = Color(0xFF1F7A68),
    onPrimary = Color.White
)

// ── 6. Mono — pure grayscale, ultra-minimal ──────────────────────────────────
private val MonoLight = ThemeSurfaces(
    background = Color(0xFFF9F9F9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0F0F0),
    outline = Color(0xFFE0E0E0),
    onBackground = Color(0xFF111111),
    onSurfaceVariant = Color(0xFF666666),
    primary = Color(0xFF111111),
    onPrimary = Color.White
)
private val MonoDark = ThemeSurfaces(
    background = Color(0xFF000000),
    surface = Color(0xFF0F0F0F),
    surfaceVariant = Color(0xFF1A1A1A),
    outline = Color(0xFF2C2C2C),
    onBackground = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF888888),
    primary = Color(0xFFF5F5F5),
    onPrimary = Color(0xFF000000)
)

// ── 7. Sand — warm neutral + soft bronze (restrained) ────────────────────────
private val SandLight = ThemeSurfaces(
    background = Color(0xFFF7F5F2),
    surface = Color(0xFFFFFDFB),
    surfaceVariant = Color(0xFFEEEAE4),
    outline = Color(0xFFDDD7CE),
    onBackground = Color(0xFF1C1915),
    onSurfaceVariant = Color(0xFF6E675C),
    primary = Color(0xFF8B7355),
    onPrimary = Color.White
)
private val SandDark = ThemeSurfaces(
    background = Color(0xFF0E0C0A),
    surface = Color(0xFF171412),
    surfaceVariant = Color(0xFF221E1A),
    outline = Color(0xFF342E28),
    onBackground = Color(0xFFF3EFE9),
    onSurfaceVariant = Color(0xFFA0988C),
    primary = Color(0xFFC4A882),
    onPrimary = Color(0xFF1C1408)
)

val ThemeStyleCatalog: Map<com.example.core.model.AccentColorChoice, ThemeStyleDefinition> = mapOf(
    com.example.core.model.AccentColorChoice.EMERALD to ThemeStyleDefinition(
        light = ObsidianLight, dark = ObsidianDark,
        previewPrimary = ObsidianDark.primary, previewBg = ObsidianDark.background
    ),
    com.example.core.model.AccentColorChoice.SAPPHIRE to ThemeStyleDefinition(
        light = MidnightLight, dark = MidnightDark,
        previewPrimary = MidnightDark.primary, previewBg = MidnightDark.background
    ),
    com.example.core.model.AccentColorChoice.AMBER to ThemeStyleDefinition(
        light = SandLight, dark = SandDark,
        previewPrimary = SandLight.primary, previewBg = SandLight.background
    ),
    com.example.core.model.AccentColorChoice.RUBY to ThemeStyleDefinition(
        light = PearlLight, dark = PearlDark,
        previewPrimary = PearlLight.primary, previewBg = PearlLight.background
    ),
    com.example.core.model.AccentColorChoice.VIOLET to ThemeStyleDefinition(
        light = NordicLight, dark = NordicDark,
        previewPrimary = NordicLight.primary, previewBg = NordicLight.background
    ),
    com.example.core.model.AccentColorChoice.GRAPHITE to ThemeStyleDefinition(
        light = GraphiteLight, dark = GraphiteDark,
        previewPrimary = GraphiteDark.primary, previewBg = GraphiteDark.background
    ),
    com.example.core.model.AccentColorChoice.MONO to ThemeStyleDefinition(
        light = MonoLight, dark = MonoDark,
        previewPrimary = MonoDark.primary, previewBg = MonoDark.background
    )
)

// Legacy aliases used by older screens / settings previews
val AccentEmerald = ObsidianDark.primary
val AccentSapphire = MidnightDark.primary
val AccentAmber = SandLight.primary
val AccentRuby = PearlLight.primary
val AccentViolet = NordicLight.primary
val AccentGraphite = GraphiteDark.primary
val AccentMono = MonoDark.primary

val DaricEmerald = AccentEmerald
val DaricEmeraldDark = Color(0xFF006B43)
val DaricEmeraldLight = Color(0xFF4EE8AA)

val DarkBackground = ObsidianDark.background
val DarkSurface = ObsidianDark.surface
val DarkSurfaceVariant = ObsidianDark.surfaceVariant
val DarkSurfaceBorder = ObsidianDark.outline
val DarkTextPrimary = ObsidianDark.onBackground
val DarkTextSecondary = ObsidianDark.onSurfaceVariant

val LightBackground = PearlLight.background
val LightSurface = PearlLight.surface
val LightSurfaceVariant = PearlLight.surfaceVariant
val LightSurfaceBorder = PearlLight.outline
val LightTextPrimary = PearlLight.onBackground
val LightTextSecondary = PearlLight.onSurfaceVariant
