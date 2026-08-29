package com.autobook.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

val PrimaryGreen = Color(0xFF00B578)
val ExpenseRed = Color(0xFFFF4D4F)
val IncomeBlue = Color(0xFF2F80ED)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    // small 形状被全部 Chip 组件（FilterChip 等）使用，全圆角统一胶囊风格
    small = RoundedCornerShape(50),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

fun darker(color: Color, factor: Float = 0.78f): Color =
    Color(color.red * factor, color.green * factor, color.blue * factor)

// tnum 让数字等宽，金额列纵向对齐，滚动时不抖动
private fun TextStyle.tabular() = copy(fontFeatureSettings = "tnum")

val AppTypography = Typography().run {
    copy(
        displayLarge = displayLarge.tabular(),
        displayMedium = displayMedium.tabular(),
        displaySmall = displaySmall.tabular(),
        headlineLarge = headlineLarge.tabular(),
        headlineMedium = headlineMedium.tabular(),
        headlineSmall = headlineSmall.tabular(),
        titleLarge = titleLarge.tabular(),
        titleMedium = titleMedium.tabular(),
        titleSmall = titleSmall.tabular(),
        bodyLarge = bodyLarge.tabular(),
        bodyMedium = bodyMedium.tabular(),
        bodySmall = bodySmall.tabular(),
        labelLarge = labelLarge.tabular(),
        labelMedium = labelMedium.tabular(),
        labelSmall = labelSmall.tabular()
    )
}

private fun buildLightScheme(seed: Color) = lightColorScheme(
    primary = seed,
    onPrimary = Color.White,
    primaryContainer = seed.copy(alpha = 0.16f),
    onPrimaryContainer = darker(seed, 0.55f),
    secondary = seed.copy(alpha = 0.75f),
    background = Color(0xFFF5F7F6),
    onBackground = Color(0xFF1A211D),
    surface = Color.White,
    onSurface = Color(0xFF1A211D),
    surfaceVariant = Color(0xFFF0F3F1),
    onSurfaceVariant = Color(0xFF8A9490),
    outline = Color(0xFFE3E8E5),
    error = ExpenseRed
)

private fun buildDarkScheme(seed: Color) = darkColorScheme(
    primary = Color(
        (seed.red + 1f) / 2f,
        (seed.green + 1f) / 2f,
        (seed.blue + 1f) / 2f
    ),
    onPrimary = Color(0xFF00331F),
    primaryContainer = seed.copy(alpha = 0.3f),
    onPrimaryContainer = Color.White,
    background = Color(0xFF0F1311),
    onBackground = Color(0xFFE4E9E6),
    surface = Color(0xFF171C19),
    onSurface = Color(0xFFE4E9E6),
    surfaceVariant = Color(0xFF222825),
    onSurfaceVariant = Color(0xFF9AA39E),
    outline = Color(0xFF2C332F),
    error = Color(0xFFFF6B6E)
)

@Composable
private fun animatedColorScheme(
    darkTheme: Boolean,
    seed: Color
): androidx.compose.material3.ColorScheme {
    val light = buildLightScheme(seed)
    val dark = buildDarkScheme(seed)
    val progress = androidx.compose.runtime.remember {
        androidx.compose.animation.core.Animatable(if (darkTheme) 1f else 0f)
    }
    androidx.compose.runtime.LaunchedEffect(darkTheme) {
        progress.animateTo(
            if (darkTheme) 1f else 0f,
            androidx.compose.animation.core.tween(500)
        )
    }
    fun lerp(a: Color, b: Color): Color =
        androidx.compose.ui.graphics.lerp(a, b, progress.value)

    val base = if (progress.value < 0.5f) light else dark
    return base.copy(
        primary = lerp(light.primary, dark.primary),
        onPrimary = lerp(light.onPrimary, dark.onPrimary),
        primaryContainer = lerp(light.primaryContainer, dark.primaryContainer),
        onPrimaryContainer = lerp(light.onPrimaryContainer, dark.onPrimaryContainer),
        secondary = lerp(light.secondary, dark.secondary),
        onSecondary = lerp(light.onSecondary, dark.onSecondary),
        secondaryContainer = lerp(light.secondaryContainer, dark.secondaryContainer),
        onSecondaryContainer = lerp(light.onSecondaryContainer, dark.onSecondaryContainer),
        background = lerp(light.background, dark.background),
        onBackground = lerp(light.onBackground, dark.onBackground),
        surface = lerp(light.surface, dark.surface),
        onSurface = lerp(light.onSurface, dark.onSurface),
        surfaceVariant = lerp(light.surfaceVariant, dark.surfaceVariant),
        onSurfaceVariant = lerp(light.onSurfaceVariant, dark.onSurfaceVariant),
        outline = lerp(light.outline, dark.outline),
        error = lerp(light.error, dark.error),
        onError = lerp(light.onError, dark.onError)
    )
}

@Composable
fun AutoBookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    seed: Color = PrimaryGreen,
    content: @Composable () -> Unit
) {
    val animatedSeed by androidx.compose.animation.animateColorAsState(
        targetValue = seed,
        animationSpec = androidx.compose.animation.core.tween(500),
        label = "seed_anim"
    )
    MaterialTheme(
        colorScheme = animatedColorScheme(darkTheme, animatedSeed),
        shapes = AppShapes,
        typography = AppTypography,
        content = content
    )
}
