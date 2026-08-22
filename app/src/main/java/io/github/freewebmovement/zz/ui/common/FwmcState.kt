package io.github.freewebmovement.zz.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.freewebmovement.zz.MainApplication
import kotlinx.coroutines.delay

/**
 * Snapshot of the embedded fwmc node state, refreshed every [periodMs] ms so
 * Compose reacts when the node finishes starting/stopping in background.
 */
data class FwmcNodeSnapshot(
    val running: Boolean,
    val port: Int,
    val address: String,
)

@Composable
fun rememberFwmcNodeSnapshot(periodMs: Long = 2000): FwmcNodeSnapshot {
    var snap by remember {
        mutableStateOf(FwmcNodeSnapshot(running = false, port = 0, address = ""))
    }
    LaunchedEffect(periodMs) {
        while (true) {
            val c = MainApplication.getApp().fwmc
            snap = FwmcNodeSnapshot(
                running = c?.isRunning == true,
                port = c?.port ?: 0,
                address = c?.address ?: "",
            )
            delay(periodMs)
        }
    }
    return snap
}
