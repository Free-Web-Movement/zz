package io.github.freewebmovement.zz.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import io.github.freewebmovement.peer.interfaces.IPreference

/** 当前主题状态（全局单例），切换即时生效并持久化。 */
object AppTheme {
    private const val KEY = "ui_theme_index"
    private const val COLOR_KEY = "ui_theme_primary_color"

    var index by mutableIntStateOf(0)
        private set

    val preset: ThemePreset
        get() = THEMES[index.coerceIn(THEMES.indices)]

    fun load(preference: IPreference?) {
        index = preference?.read(KEY, 0)?.coerceIn(THEMES.indices) ?: 0
        saveColor(preference)
    }

    fun select(i: Int, preference: IPreference?) {
        index = i.coerceIn(THEMES.indices)
        preference?.save(KEY, index)
        saveColor(preference)
        io.github.freewebmovement.zz.MainApplication.instance?.let {
            io.github.freewebmovement.android.noui.SystemStatusNotifier.refresh(it)
        }
    }

    private fun saveColor(preference: IPreference?) {
        preference?.save(COLOR_KEY, preset.primary.toArgb())
    }
}

private fun schemeOf(p: ThemePreset) = lightColorScheme(
    primary = p.primary,
    onPrimary = Color.White,
    primaryContainer = CardBg,
    onPrimaryContainer = TextPrimary,
    secondary = p.primaryDark,
    background = WxBg,
    onBackground = TextPrimary,
    surface = CardBg,
    onSurface = TextPrimary,
    surfaceVariant = WxBg,
    onSurfaceVariant = TextSecondary,
    error = BadgeRed,
)

private const val UI_SCALE = 0.85f

@Composable
fun ZzTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density * UI_SCALE,
            fontScale = density.fontScale,
        )
    ) {
        MaterialTheme(
            colorScheme = schemeOf(AppTheme.preset),
            typography = Typography,
            content = content
        )
    }
}
