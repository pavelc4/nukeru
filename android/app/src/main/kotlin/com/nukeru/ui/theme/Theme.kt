package com.nukeru.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.graphics.ColorUtils

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA8CD94), // Sage Green
    onPrimary = Color(0xFF11380A),
    primaryContainer = Color(0xFF284F22),
    onPrimaryContainer = Color(0xFFC4ECC0),
    secondary = Color(0xFFBDCBB6),
    onSecondary = Color(0xFF283425),
    secondaryContainer = Color(0xFF3E4A3A),
    onSecondaryContainer = Color(0xFFD9E7D2),
    background = Color(0xFF121411), // Clean Charcoal/Obsidian AOSP
    surface = Color(0xFF121411),
    onBackground = Color(0xFFE2E3DE),
    onSurface = Color(0xFFE2E3DE),
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = Color(0xFFC2C9BD),
    outline = Color(0xFF8C9388),
    inverseOnSurface = Color(0xFF121411),
    inverseSurface = Color(0xFFE2E3DE),
    inversePrimary = Color(0xFF3B693A)
)

@Composable
fun NukeruAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    colorIndex: Int = 0,
    styleMode: Int = 1, // 0 = Muted, 1 = Expressive, 2 = Vibrant
    content: @Composable () -> Unit
) {
    val presetC1 = when(colorIndex) {
        0 -> Color(0xFFC5E384) // Pistachio
        1 -> Color(0xFFC2D8C4) // Matcha
        2 -> Color(0xFF700143) // Tyrian
        3 -> Color(0xFF385144) // Moss
        4 -> Color(0xFF006C4C) // KSU Mint (KernelSU Signature Green)
        5 -> Color(0xFF1A73E8) // Pixel Blue (Google Pixel Brand Blue)
        6 -> Color(0xFF6750A4) // Lavender (Google M3 Default Purple)
        7 -> Color(0xFFB85C38) // Terracotta (Sunset Orange)
        else -> md_theme_light_primary
    }
    val presetC2 = Color(0xFFFFFFFF)

    val customColorScheme = if (darkTheme) {
        DarkColorScheme.copy(
            primary = presetC1,
            onPrimary = presetC2,
            primaryContainer = presetC1.copy(alpha = 0.15f),
            onPrimaryContainer = presetC1,
            secondaryContainer = presetC1.copy(alpha = 0.12f),
            onSecondaryContainer = presetC1,
            background = Color(0xFF0F110D),
            surface = Color(0xFF0F110D)
        )
    } else {
        LightColorScheme.copy(
            primary = presetC1,
            onPrimary = presetC2,
            primaryContainer = presetC1,
            onPrimaryContainer = presetC2,
            secondaryContainer = presetC1,
            onSecondaryContainer = presetC2,
            tertiaryContainer = presetC1,
            onTertiaryContainer = presetC2,
            surfaceTint = presetC1
        )
    }

    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> customColorScheme
    }

    val colorScheme = baseScheme.applyStyleMode(styleMode)
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private fun Color.adjustSaturation(factor: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    hsl[1] = (hsl[1] * factor).coerceIn(0f, 1f)
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun ColorScheme.applyStyleMode(styleMode: Int): ColorScheme {
    val factor = when(styleMode) {
        0 -> 0.4f  // Muted
        1 -> 1.0f  // Expressive
        2 -> 1.35f // Vibrant
        else -> 1.0f
    }
    if (factor == 1.0f) return this
    
    return this.copy(
        primary = this.primary.adjustSaturation(factor),
        primaryContainer = this.primaryContainer.adjustSaturation(factor),
        onPrimaryContainer = this.onPrimaryContainer.adjustSaturation(factor),
        secondary = this.secondary.adjustSaturation(factor),
        secondaryContainer = this.secondaryContainer.adjustSaturation(factor),
        onSecondaryContainer = this.onSecondaryContainer.adjustSaturation(factor),
        tertiary = this.tertiary.adjustSaturation(factor),
        tertiaryContainer = this.tertiaryContainer.adjustSaturation(factor),
        onTertiaryContainer = this.onTertiaryContainer.adjustSaturation(factor),
        surfaceTint = this.surfaceTint.adjustSaturation(factor)
    )
}
