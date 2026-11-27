package com.example.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@Immutable
data class EditorialColors(
    val isDark: Boolean,
    val acidLime: Color,
    val onAcidLime: Color,
    val acidLimeMuted: Color,
    val cardSurface: Color,
    val elevatedSurface: Color,
    val borderSubtle: Color,
    val explanationSurface: Color,
    val correctText: Color,
    val correctBg: Color,
    val wrongText: Color,
    val wrongBg: Color,
    val warningText: Color,
    val reviewText: Color,
    val accentDustyRoseText: Color,
    val unvisitedSwatch: Color
) {
    /** High-contrast foreground accent safe for text/icons on card surfaces in both Light and Dark themes. */
    val acidLimeForeground: Color get() = if (isDark) acidLime else accentDustyRoseText
}

private val LightEditorialColors = EditorialColors(
    isDark = false,
    acidLime = AcidLime,
    onAcidLime = PureBlack,
    acidLimeMuted = AcidLimeMutedLight,
    cardSurface = LightCardSurface,
    elevatedSurface = LightElevatedSurface,
    borderSubtle = LightBorder,
    explanationSurface = Color(0xFFF6F6F6),
    correctText = SemanticCorrectTextLight,
    correctBg = SemanticCorrectBgLight,
    wrongText = SemanticWrongTextLight,
    wrongBg = SemanticWrongBgLight,
    warningText = SemanticWarningTextLight,
    reviewText = SemanticReviewTextLight,
    accentDustyRoseText = LightTextPrimary,
    unvisitedSwatch = MediumGrey
)

private val DarkEditorialColors = EditorialColors(
    isDark = true,
    acidLime = AcidLime,
    onAcidLime = PureBlack,
    acidLimeMuted = AcidLimeMutedDark,
    cardSurface = AmoledCardPrimary,
    elevatedSurface = AmoledCardElevated,
    borderSubtle = AmoledBorder,
    explanationSurface = Color(0xFF141414),
    correctText = SemanticCorrectTextDark,
    correctBg = SemanticCorrectBgDark,
    wrongText = SemanticWrongTextDark,
    wrongBg = SemanticWrongBgDark,
    warningText = SemanticWarningTextDark,
    reviewText = SemanticReviewTextDark,
    accentDustyRoseText = AcidLime,
    unvisitedSwatch = Color(0xFF737373)
)

private val LocalEditorialColors = staticCompositionLocalOf { LightEditorialColors }

val MaterialTheme.editorialColors: EditorialColors
    @Composable
    @ReadOnlyComposable
    get() = LocalEditorialColors.current

private val DarkColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = PureBlack,
    primaryContainer = AmoledCardElevated,
    onPrimaryContainer = AmoledTextPrimary,
    secondary = AcidLime,
    onSecondary = PureBlack,
    secondaryContainer = AcidLime,
    onSecondaryContainer = PureBlack,
    tertiary = Graphite,
    onTertiary = AmoledTextPrimary,
    tertiaryContainer = SecondaryGraphite,
    onTertiaryContainer = AmoledTextPrimary,
    background = AmoledBackground,
    onBackground = AmoledTextPrimary,
    surface = AmoledCardPrimary,
    onSurface = AmoledTextPrimary,
    surfaceVariant = AmoledCardElevated,
    onSurfaceVariant = AmoledTextSecondary,
    surfaceContainerLowest = AmoledBackground,
    surfaceContainerLow = AmoledCardPrimary,
    surfaceContainer = AmoledCardPrimary,
    surfaceContainerHigh = AmoledCardElevated,
    surfaceContainerHighest = SecondaryGraphite,
    outline = AmoledBorder,
    outlineVariant = AmoledBorder,
    error = SemanticWrongTextDark,
    onError = PureBlack,
    errorContainer = SemanticWrongBgDark,
    onErrorContainer = Color(0xFFFCA5A5)
)

private val LightColorScheme = lightColorScheme(
    primary = PureBlack,
    onPrimary = PureWhite,
    primaryContainer = LightGrey,
    onPrimaryContainer = LightTextPrimary,
    secondary = AcidLime,
    onSecondary = PureBlack,
    secondaryContainer = AcidLime,
    onSecondaryContainer = PureBlack,
    tertiary = Graphite,
    onTertiary = PureWhite,
    tertiaryContainer = SoftSurfaceGrey,
    onTertiaryContainer = LightTextPrimary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightCardSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = SoftSurfaceGrey,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = PureWhite,
    surfaceContainer = SoftSurfaceGrey,
    surfaceContainerHigh = LightGrey,
    surfaceContainerHighest = LightGrey,
    outline = LightBorder,
    outlineVariant = LightBorder,
    error = SemanticWrongFill,
    onError = PureWhite,
    errorContainer = SemanticWrongBgLight,
    onErrorContainer = SemanticWrongTextLight
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RrbNursingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Strictly preserve Monochrome + Acid Lime brand palette across all Android versions
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val editorial = if (darkTheme) DarkEditorialColors else LightEditorialColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivityWindow()
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalEditorialColors provides editorial) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

private tailrec fun Context.findActivityWindow(): android.view.Window? = when (this) {
    is Activity -> window
    is ContextWrapper -> baseContext.findActivityWindow()
    else -> null
}
