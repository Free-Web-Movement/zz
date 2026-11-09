package io.github.freewebmovement.zz.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import io.github.freewebmovement.android.noui.TrojanService
import io.github.freewebmovement.zz.ui.content.FwmcSession
import io.github.freewebmovement.zz.ui.content.LoginScreen
import io.github.freewebmovement.zz.ui.theme.AppTheme
import io.github.freewebmovement.zz.ui.theme.ZzTheme

class MainActivity : ComponentActivity() {

    private var vpnPromptPending = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && !io.github.freewebmovement.android.noui.TrojanService.running) {
            io.github.freewebmovement.android.noui.SystemStatusNotifier.showIdle(this)
        }
    }

    private val vpnLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        vpnPromptPending = false
        if (result.resultCode == RESULT_OK) TrojanService.start(this)
    }

    override fun onResume() {
        super.onResume()
        maybeAutoStart()
    }

    private fun maybeAutoStart() {
        if (vpnPromptPending || TrojanService.running) return
        val setting = io.github.freewebmovement.zz.MainApplication.getApp().settings.trojan
        if (!setting.autoOpen) return
        val server = setting.currentServer() ?: return
        if (server.remoteAddr.isBlank() || server.password.isBlank()) return
        val prepare = runCatching { VpnService.prepare(this) }.getOrNull()
        if (prepare == null) {
            TrojanService.start(this)
        } else {
            vpnPromptPending = true
            vpnLauncher.launch(prepare)
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermission()

        val preference = io.github.freewebmovement.zz.MainApplication.getApp().preference
        AppTheme.load(preference)
        io.github.freewebmovement.android.noui.SystemStatusNotifier.showIdle(this)
        io.github.freewebmovement.zz.ui.i18n.AppLang.load(preference)
        io.github.freewebmovement.zz.ui.i18n.AppLang.syncWebLang()
        setContent {
            ZzTheme {
                io.github.freewebmovement.zz.ui.i18n.AppLangProvider {
                    LaunchedEffect(Unit) { FwmcSession.refresh() }
                    Scaffold(modifier = Modifier.fillMaxSize()) {
                        if (FwmcSession.current == null) {
                            LoginScreen()
                        } else {
                            TabView()
                        }
                    }
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}