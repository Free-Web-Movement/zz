package io.github.freewebmovement.zz.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import io.github.freewebmovement.zz.ui.content.FwmcSession
import io.github.freewebmovement.zz.ui.content.LoginScreen
import io.github.freewebmovement.zz.ui.theme.AppTheme
import io.github.freewebmovement.zz.ui.theme.ZzTheme

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preference = io.github.freewebmovement.zz.MainApplication.getApp().preference
        AppTheme.load(preference)
        setContent {
            ZzTheme {
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