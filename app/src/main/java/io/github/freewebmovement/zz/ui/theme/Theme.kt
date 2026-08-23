package io.github.freewebmovement.zz.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import io.github.freewebmovement.peer.interfaces.IPreference

/** 当前主题状态（全局单例），切换即时生效并持久化。 */
object AppTheme {
    private const val KEY = "ui_theme_index"

    var index by mutableIntStateOf(0)
        private set

    val preset: ThemePreset
        get() = THEMES[index.coerceIn(THEMES.indices)]

    fun load(preference: IPreference?) {
        index = preference?.read(KEY, 0)?.coerceIn(THEMES.indices) ?: 0
    }

    fun select(i: Int, preference: IPreference?) {
        index = i.coerceIn(THEMES.indices)
        preference?.save(KEY, index)
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

@Composable
fun ZzTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = schemeOf(AppTheme.preset),
        typography = Typography,
        content = content
    )
}
