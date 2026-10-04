package com.helboy.nemotalk.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ── Light / Dark schemes — Minis parity ─────────────────────
private val DarkColorScheme = darkColorScheme(
    primary = NvidiaGreen,
    onPrimary = DarkBackground,
    primaryContainer = NvidiaDarkGreen,
    onPrimaryContainer = TextPrimary,
    secondary = Terracotta,
    onSecondary = TextOnTerracotta,
    secondaryContainer = TerracottaDark,
    onSecondaryContainer = Cream,
    tertiary = NvidiaNeon,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = SurfaceElevated,
    surfaceContainerHigh = DarkSurfaceElevated,
    outline = DarkBorder,
    outlineVariant = BorderSubtle,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorBg,
    onErrorContainer = ErrorRed,
    scrim = DarkScrim
)

private val LightColorScheme = lightColorScheme(
    primary = NvidiaGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6F4CC),
    onPrimaryContainer = Color(0xFF1A3000),
    secondary = Terracotta,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBCF),
    onSecondaryContainer = Color(0xFF3A0B00),
    background = Cream,
    onBackground = Ink,
    surface = Cream,
    onSurface = Ink,
    surfaceVariant = CreamMuted,
    onSurfaceVariant = InkMuted,
    outline = Color(0xFFE2D9D0),
    outlineVariant = Color(0xFFF0E8E0),
    error = ErrorRed,
    scrim = Color(0x66000000)
)

// ── Glass locals — glassmorphism ────────────────────────────
data class GlassColors(
    val surfaceGlass: Color = SurfaceGlass,
    val surfaceGlassStrong: Color = SurfaceGlassStrong,
    val borderGlass: Color = BorderGlass
)

val LocalGlassColors = staticCompositionLocalOf { GlassColors() }

@Composable
fun NeMoTalkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalGlassColors provides GlassColors()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
