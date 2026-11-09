package io.github.freewebmovement.android.noui

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import io.github.freewebmovement.peer.system.KVSettings
import io.github.freewebmovement.peer.system.Preference
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import rs.zz.coin.FwmcApi

class FwmcService : Service() {

    private var controller: FwmcNodeController? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var statsJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        SystemStatusNotifier.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                SystemStatusNotifier.setNode(this, false, "已停止")
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> if (controller == null) startNode()
            else -> startNode()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopNode()
        super.onDestroy()
    }

    private fun startNode() {
        acquireWakeLock()
        ensureBatteryOptimizationExempt()
        SystemStatusNotifier.setNode(this, true, "启动中…")
        startForeground(SystemStatusNotifier.NOTIFICATION_ID, SystemStatusNotifier.build(this))

        val dataDir = MyApp.getStoragePath(applicationContext)
        val settings = KVSettings(Preference(Settings()))
        val port = if (settings.network.port > 1024) settings.network.port else (1025..65535).random()
        // 纪元运行模式：测试模式按配置的每 tick 秒数加速；生产模式 15 分钟/tick。
        val tickMs = if (settings.debug.epochTestMode) {
            settings.debug.epochTickSeconds.coerceAtLeast(1).toLong() * 1000L
        } else {
            0L
        }
        val startTick = if (settings.debug.epochTestMode) {
            settings.debug.epochStartTick.coerceIn(0, 95).toLong()
        } else {
            0L
        }

        val ctrl = FwmcNodeController(dataDir)
        val bound = ctrl.start(port, tickMs, startTick)
        controller = ctrl
        MyApp.getApp().setFwmc(ctrl)
        if (bound > 0) {
            SystemStatusNotifier.setNode(this, true, "运行中")
            SystemStatusNotifier.setNodeDetails(this, bound, 0, 0, 0, 0)
            startStatsPolling(bound)
        } else {
            SystemStatusNotifier.setNode(this, false, "启动失败")
        }
    }

    private fun startStatsPolling(port: Int) {
        statsJob?.cancel()
        statsJob = CoroutineScope(Dispatchers.IO).launch {
            val interval = if (KVSettings(Preference(Settings())).debug.epochTestMode) {
                1000L
            } else {
                5000L
            }
            while (isActive) {
                runCatching {
                    val obj = JSONObject(FwmcApi.getData())
                    if (obj.optBoolean("success", false)) {
                        SystemStatusNotifier.setNodeDetails(
                            applicationContext,
                            port,
                            obj.optLong("epoch", 0),
                            obj.optLong("epoch_tick", 0),
                            obj.optLong("ticks_per_epoch", 0),
                            obj.optLong("tick_count", 0),
                        )
                    }
                }
                delay(interval)
            }
        }
    }

    /**
     * 确保本 app 在「忽略电池优化」白名单内，避免 Doze 模式下节点被挂起。
     * 若未豁免，尝试通过系统弹窗请求（需用户确认；部分厂商会直接拒绝后台拉起）。
     */
    private fun ensureBatteryOptimizationExempt() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val pkg = packageName
        if (pm.isIgnoringBatteryOptimizations(pkg)) return
        try {
            val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = android.net.Uri.parse("package:$pkg")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // 部分 ROM 禁止后台弹窗，忽略
        }
    }

    private fun stopNode() {
        statsJob?.cancel()
        statsJob = null
        SystemStatusNotifier.setNode(this, false, "已停止")
        releaseWakeLock()
        controller?.destroy()
        controller = null
        MyApp.getApp().setFwmc(null)
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "freewebmovement:fwmc_node"
            ).apply {
                acquire(24 * 60 * 60 * 1000L) // 24 hours max
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    companion object {
        // v2：提高通道重要性到 DEFAULT 以在状态栏显示图标（旧 fwmc_service 通道保留 LOW，无法覆盖）
        const val ACTION_START = "io.github.freewebmovement.fwmc.START"
        const val ACTION_STOP = "io.github.freewebmovement.fwmc.STOP"

        fun start(context: Context) {
            val intent = Intent(context, FwmcService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, FwmcService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
