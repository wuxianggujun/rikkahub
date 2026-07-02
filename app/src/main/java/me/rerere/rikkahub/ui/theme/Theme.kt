package me.rerere.rikkahub.ui.theme

import android.app.Activity
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography as MaterialTypography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlinx.serialization.Serializable

private val ExtendLightColors = lightExtendColors()
private val ExtendDarkColors = darkExtendColors()
val LocalExtendColors = compositionLocalOf { ExtendLightColors }

val LocalDarkMode = compositionLocalOf { false }

@Serializable
enum class ColorMode {
    SYSTEM,
    LIGHT,
    DARK
}

@Composable
fun RikkahubTheme(
    colorMode: ColorMode = ColorMode.SYSTEM,
    colorScheme: ColorScheme? = null,
    darkMode: Boolean? = null,
    typography: MaterialTypography? = null,
    syncSystemBars: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = darkMode ?: when (colorMode) {
        ColorMode.SYSTEM -> isSystemInDarkTheme()
        ColorMode.LIGHT -> false
        ColorMode.DARK -> true
    }
    val resolvedColorScheme = colorScheme ?: PresetThemes.first().getColorScheme(dark = darkTheme)
    val extendColors = if (darkTheme) ExtendDarkColors else ExtendLightColors

    // 更新状态栏图标颜色
    val view = LocalView.current
    if (syncSystemBars && !view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkMode provides darkTheme,
        LocalExtendColors provides extendColors,
        LocalOverscrollFactory provides null
    ) {
        MaterialExpressiveTheme(
            colorScheme = resolvedColorScheme,
            typography = typography ?: Typography,
            content = content,
            motionScheme = MotionScheme.expressive()
        )
    }
}

val MaterialTheme.extendColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendColors.current
