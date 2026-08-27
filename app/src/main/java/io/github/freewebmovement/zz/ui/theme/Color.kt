package io.github.freewebmovement.zz.ui.theme

import androidx.compose.ui.graphics.Color

// ---- 中性色（所有主题共享，微信风格） ----
val WxBg = Color(0xFFF2F2F2)
val CardBg = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF191919)
val TextSecondary = Color(0xFF888888)
val TextMuted = Color(0xFFB0B0B0)
val LineColor = Color(0xFFE0E0E0)
val BadgeRed = Color(0xFFF44336)
val IncomeGreen = Color(0xFF1AAD19)
val OnlineGreen = Color(0xFF10B981)

/** 一整套主题配色模板 */
data class ThemePreset(
    val id: String,
    val label: String,
    val primary: Color,      // 主色（导航、按钮、强调）
    val primaryDark: Color,  // 渐变深端
    val bubble: Color,       // 聊天气泡（自己发送）
)

/** 内置主题：绿 / 蓝 / 粉 / 紫 / 橙 */
val THEMES = listOf(
    ThemePreset("purple", "经典紫", Color(0xFF9C27B0), Color(0xFF7B1FA2), Color(0xFFE9D5F5)),
    ThemePreset("green", "清新绿", Color(0xFF07C160), Color(0xFF06AD56), Color(0xFF95EC69)),
    ThemePreset("blue", "清爽蓝", Color(0xFF1677FF), Color(0xFF0E5FD8), Color(0xFFC9E2FF)),
    ThemePreset("pink", "樱花粉", Color(0xFFFF5A8F), Color(0xFFE8386D), Color(0xFFFFD9E5)),
    ThemePreset("purple", "浪漫紫", Color(0xFF7C4DFF), Color(0xFF651FFF), Color(0xFFE3D7FF)),
    ThemePreset("orange", "活力橙", Color(0xFFF57C00), Color(0xFFE65100), Color(0xFFFFE3BF)),
)

/** 主题名的本地化显示。 */
@androidx.compose.runtime.Composable
fun localizedPresetLabel(index: Int): String {
    val s = io.github.freewebmovement.zz.ui.i18n.LocalAppStrings.current
    return when (index.coerceIn(THEMES.indices)) {
        0 -> s.settings.themeClassicPurple
        1 -> s.settings.themeGreen
        2 -> s.settings.themeBlue
        3 -> s.settings.themePink
        4 -> s.settings.themeRomanticPurple
        else -> s.settings.themeOrange
    }
}

// 兼容旧引用
val green = THEMES[0].primary
val black = TextPrimary
val backColor = CardBg
