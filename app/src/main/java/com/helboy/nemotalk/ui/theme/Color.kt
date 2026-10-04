package com.helboy.nemotalk.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Core Nvidia ──────────────────────────────────────────────
val NvidiaGreen = Color(0xFF76B900)
val NvidiaNeon = Color(0xFF00E676)
val NvidiaDarkGreen = Color(0xFF385700)
val NvidiaGreenMuted = Color(0xFF4A7300)

// ── Terracotta system (awesome-terracotta) ──────────────────
// Warm editorial palette — cream paper, ink text, terracotta accent.
// Kept alongside Nvidia green so the brand stays, but surfaces breathe.
val Terracotta = Color(0xFFC2724E)
val TerracottaLight = Color(0xFFE8A082)
val TerracottaDark = Color(0xFF8B4A2E)
val Cream = Color(0xFFFFF8F0)
val CreamMuted = Color(0xFFF5EDE3)
val Ink = Color(0xFF1A1410)
val InkMuted = Color(0xFF4A3F38)

// ── Obsidian base (existing, preserved for compat) ───────────
val DarkBackground = Color(0xFF0A0E17)
val DarkSurface = Color(0xFF141A24)
val DarkSurfaceElevated = Color(0xFF1C2433)
val DarkBorder = Color(0xFF283447)
val DarkScrim = Color(0x990A0E17)

// ── Surface tokens (Minis parity) ────────────────────────────
val Surface = DarkSurface
val SurfaceElevated = DarkSurfaceElevated
val SurfaceGlass = Color(0x1AFFFFFF)          // glassmorphism base
val SurfaceGlassStrong = Color(0x2AFFFFFF)
val BorderGlass = Color(0x1FFFFFFF)           // luminous border
val BorderSubtle = Color(0xFF1E293B)
val BorderStrong = Color(0xFF334155)

// ── Bubbles ──────────────────────────────────────────────────
val UserBubble = Color(0xFF1F2B40)
val AiBubble = Color(0xFF121822)
val UserBubbleGlass = Color(0xFF1E3A2E)       // green-tinted for user
val AiBubbleElevated = Color(0xFF1A2332)

// ── Text ─────────────────────────────────────────────────────
val TextPrimary = Color(0xFFF3F6FA)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)
val TextOnTerracotta = Color(0xFFFFFBF7)
val TextOnGlass = Color(0xFFF8FAFC)

// ── Status ───────────────────────────────────────────────────
val ErrorRed = Color(0xFFEF4444)
val ErrorBg = Color(0xFF450A0A)
val SuccessGreen = Color(0xFF10B981)
val SuccessBg = Color(0xFF052E1C)
val WarningAmber = Color(0xFFF59E0B)
val WarningBg = Color(0xFF451A03)
val InfoBlue = Color(0xFF38BDF8)
val InfoBg = Color(0xFF0C2A3A)

// ── Gradients ────────────────────────────────────────────────
val GradientNvidia = Brush.linearGradient(listOf(NvidiaGreen, NvidiaNeon))
val GradientTerracotta = Brush.linearGradient(listOf(Terracotta, TerracottaLight))
val GradientGlass = Brush.linearGradient(listOf(SurfaceGlass, Color.Transparent))
val GradientScrimTop = Brush.verticalGradient(listOf(DarkScrim, Color.Transparent))
val GradientScrimBottom = Brush.verticalGradient(listOf(Color.Transparent, DarkScrim))

// ── Shimmer / skeleton ───────────────────────────────────────
val ShimmerBase = Color(0xFF1E293B)
val ShimmerHighlight = Color(0xFF334155)
