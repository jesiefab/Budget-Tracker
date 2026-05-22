package com.mystegui.budgettracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Custom color set per theme ─────────────────────────────────────────────────

data class AppColors(
    val bg: Color,
    val card: Color,
    val input: Color,
    val accent: Color,
    val success: Color,
    val danger: Color,
    val warning: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val border: Color
)

val DarkColors = AppColors(
    bg          = Color(0xFF16161E),
    card        = Color(0xFF20202C),
    input       = Color(0xFF2C2C3C),
    accent      = Color(0xFF6EA8D2),
    success     = Color(0xFF55B48C),
    danger      = Color(0xFFC85F5F),
    warning     = Color(0xFFCDA54B),
    textPrimary = Color(0xFFD7D7E4),
    textMuted   = Color(0xFF82829A),
    border      = Color(0xFF3A3A4E)
)

val LightColors = AppColors(
    bg          = Color(0xFFEEEEF5),
    card        = Color(0xFFFAFAFE),
    input       = Color(0xFFE4E4F2),
    accent      = Color(0xFF5A82BE),
    success     = Color(0xFF4B9E69),
    danger      = Color(0xFFBC4B4B),
    warning     = Color(0xFFB48228),
    textPrimary = Color(0xFF282837),
    textMuted   = Color(0xFF6E6E82),
    border      = Color(0xFFD0D0DB)
)

val MidnightBlueColors = AppColors(
    bg          = Color(0xFF12163A),
    card        = Color(0xFF191E41),
    input       = Color(0xFF202A52),
    accent      = Color(0xFF6E9BD2),
    success     = Color(0xFF50AF8C),
    danger      = Color(0xFFC36464),
    warning     = Color(0xFFC8A54B),
    textPrimary = Color(0xFFC8D2EB),
    textMuted   = Color(0xFF788592),
    border      = Color(0xFF2D3A69)
)

val ForestGreenColors = AppColors(
    bg          = Color(0xFF162018),
    card        = Color(0xFF1E2C20),
    input       = Color(0xFF263A2A),
    accent      = Color(0xFF5FAF78),
    success     = Color(0xFF78BE91),
    danger      = Color(0xFFBE5F5A),
    warning     = Color(0xFFBEA54B),
    textPrimary = Color(0xFFC8E2CC),
    textMuted   = Color(0xFF789E80),
    border      = Color(0xFF324E38)
)

val WarmSunsetColors = AppColors(
    bg          = Color(0xFF201612),
    card        = Color(0xFF2E1A0E),
    input       = Color(0xFF3C261C),
    accent      = Color(0xFFCD8050),
    success     = Color(0xFF64AF7D),
    danger      = Color(0xFFC3645F),
    warning     = Color(0xFFC8AA50),
    textPrimary = Color(0xFFEEDCC8),
    textMuted   = Color(0xFFA58069),
    border      = Color(0xFF583626)
)

fun appColorsForTheme(theme: String): AppColors = when (theme) {
    "Light"        -> LightColors
    "Midnight Blue" -> MidnightBlueColors
    "Forest Green" -> ForestGreenColors
    "Warm Sunset"  -> WarmSunsetColors
    else           -> DarkColors
}

val LocalAppColors = staticCompositionLocalOf { DarkColors }

@Composable
fun BudgetTrackerTheme(
    appColors: AppColors = DarkColors,
    content: @Composable () -> Unit
) {
    val colorScheme = if (appColors == LightColors) {
        lightColorScheme(
            primary   = appColors.accent,
            background = appColors.bg,
            surface   = appColors.card,
        )
    } else {
        darkColorScheme(
            primary   = appColors.accent,
            background = appColors.bg,
            surface   = appColors.card,
        )
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalAppColors provides appColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content     = content
        )
    }
}