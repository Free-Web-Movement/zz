package io.github.freewebmovement.zz.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.ui.content.ServicePlaceholderScreen
import io.github.freewebmovement.zz.ui.content.StaticFileScreen
import io.github.freewebmovement.zz.ui.content.user.SettingsScreen
import io.github.freewebmovement.zz.ui.content.user.TrojanScreen
import io.github.freewebmovement.zz.ui.i18n.AppLang
import io.github.freewebmovement.zz.ui.i18n.AppLangProvider
import io.github.freewebmovement.zz.ui.i18n.LocalAppStrings
import io.github.freewebmovement.zz.ui.theme.AppTheme
import io.github.freewebmovement.zz.ui.theme.ZzTheme

/** Per-service settings page opened from the background control bar. */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preference = MainApplication.getApp().preference
        AppTheme.load(preference)
        AppLang.load(preference)
        val page = intent?.getStringExtra("page") ?: "node"
        setContent {
            ZzTheme {
                AppLangProvider {
                    val s = LocalAppStrings.current
                    val back = { finish() }
                    when (page) {
                        "web" -> StaticFileScreen(onBack = back)
                        "socks" -> ServicePlaceholderScreen(s.service.socksProxy, onBack = back)
                        "frp" -> ServicePlaceholderScreen(s.service.frp, onBack = back)
                        "trojan" -> TrojanScreen(onBack = back)
                        else -> SettingsScreen(onBack = back)
                    }
                }
            }
        }
    }
}
