package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Immutable
data class EditorialColors(
    val correctText: Color,
    val wrongText: Color,
    val warningText: Color,
    val reviewText: Color,
    val accentDustyRoseText: Color,
    val unvisitedSwatch: Color
)

private val LightEditorialColors = EditorialColors(
    correctText = CalmSageTextLight,
    wrongText = MutedCrimsonTextLight,
    warningText = WarmOchreTextLight,
    reviewText = MutedPlumTextLight,
    accentDustyRoseText = Color(0xFF7D5242),
    unvisitedSwatch = UnvisitedNeutral
)

private val DarkEditorialColors = EditorialColors(
    correctText = CalmSageTextDark,
    wrongText = MutedCrimsonTextDark,
    warningText = WarmOchreTextDark,
    reviewText = MutedPlumTextDark,
    accentDustyRoseText = DustyRoseSecondaryDark,
    unvisitedSwatch = Color(0xFF938B94)
)

private val LocalEditorialColors = staticCompositionLocalOf { LightEditorialColors }

val MaterialTheme.editorialColors: EditorialColors
    @Composable
    @ReadOnlyComposable
    get() = LocalEditorialColors.current

private val DarkColorScheme = darkColorScheme(
    primary = BurgundyPrimaryDark,
    onPrimary = OnBurgundyPrimaryDark,
    primaryContainer = BurgundyContainerDark,
    onPrimaryContainer = OnBurgundyContainerDark,
    secondary = DustyRoseSecondaryDark,
    onSecondary = OnDustyRoseDark,
    secondaryContainer = DustyRoseContainerDark,
    onSecondaryContainer = OnDustyRoseContainerDark,
    tertiary = CharcoalAccentDark,
    onTertiary = OnCharcoalAccentDark,
    tertiaryContainer = CharcoalContainerDark,
    onTertiaryContainer = OnCharcoalContainerDark,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = MutedCrimsonTextDark,
    onError = Color(0xFF361016),
    errorContainer = MutedCrimsonContainerDark,
    onErrorContainer = OnMutedCrimsonContainerDark
)

private val LightColorScheme = lightColorScheme(
    primary = BurgundyPrimary,
    onPrimary = WarmWhite,
    primaryContainer = BurgundyContainerLight,
    onPrimaryContainer = OnBurgundyContainerLight,
    secondary = DustyRoseSecondary,
    onSecondary = OnDustyRoseLight,
    secondaryContainer = DustyRoseContainerLight,
    onSecondaryContainer = OnDustyRoseContainerLight,
    tertiary = CharcoalAccent,
    onTertiary = WarmWhite,
    tertiaryContainer = CharcoalContainerLight,
    onTertiaryContainer = OnCharcoalContainerLight,
    background = CreamBackground,
    onBackground = LightTextPrimary,
    surface = CreamSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = WarmLinenSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = WarmSandOutline,
    outlineVariant = WarmSandOutlineVariant,
    error = MutedCrimsonFill,
    onError = WarmWhite,
    errorContainer = MutedCrimsonContainerLight,
    onErrorContainer = OnMutedCrimsonContainerLight
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RrbNursingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val editorial = if (darkTheme) DarkEditorialColors else LightEditorialColors

    CompositionLocalProvider(LocalEditorialColors provides editorial) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
