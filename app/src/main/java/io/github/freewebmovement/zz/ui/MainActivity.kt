package io.github.freewebmovement.zz.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
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
import io.github.freewebmovement.zz.ui.content.FwmcSession
import io.github.freewebmovement.zz.ui.content.LoginScreen
import io.github.freewebmovement.zz.ui.theme.AppTheme
import io.github.freewebmovement.zz.ui.theme.ZzTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result ignored – node runs regardless */ }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermission()

        val preference = io.github.freewebmovement.zz.MainApplication.getApp().preference
        AppTheme.load(preference)
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