package io.github.freewebmovement.zz.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.freewebmovement.peer.interfaces.IPreference
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 语言偏好：0=跟随系统，1=中文，2=English */
object AppLang {
    private const val KEY = "ui_lang"

    var mode by mutableIntStateOf(0)
        private set

    fun load(preference: IPreference?) {
        mode = preference?.read(KEY, 0) ?: 0
    }

    fun select(mode: Int, preference: IPreference?) {
        this.mode = mode.coerceIn(0, 2)
        preference?.save(KEY, this.mode)
        syncWebLang()
    }

    /** 将当前语言同步到 Rust Web UI（网页刷新即生效）。 */
    fun syncWebLang() {
        val code = if (this.mode == 2 || (this.mode == 0 && Locale.getDefault().language.startsWith("en"))) "en" else "zh"
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { rs.zz.coin.FwmcApi.setLang(code) }
        }
    }

    val strings: AppStrings
        get() = when (mode) {
            1 -> StringsZh
            2 -> StringsEn
            else -> systemStrings()
        }

    private fun systemStrings(): AppStrings {
        val lang = Locale.getDefault().language
        return if (lang.startsWith("zh")) StringsZh
        else if (lang.startsWith("en")) StringsEn
        else StringsZh
    }

    /** 当前有效语言代号（同步 Rust Web UI 用）。 */
    val localeCode: String
        get() = if (strings === StringsEn) "en" else "zh"
}

val LocalAppStrings = staticCompositionLocalOf { StringsZh }

/** 语言切换立即生效：读取 AppLang.mode 触发重组并重设 CompositionLocal。 */
@Composable
fun AppLangProvider(content: @Composable () -> Unit) {
    AppLang.mode // 读取以订阅变化
    val strings = AppLang.strings
    CompositionLocalProvider(LocalAppStrings provides strings, content = content)
}