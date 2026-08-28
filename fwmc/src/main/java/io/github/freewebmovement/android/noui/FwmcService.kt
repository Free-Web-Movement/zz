package io.github.freewebmovement.android.noui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import io.github.freewebmovement.fwmc.R
import io.github.freewebmovement.peer.system.KVSettings
import io.github.freewebmovement.peer.system.Preference
import com.russhwolf.settings.Settings

class FwmcService : Service() {

    private var controller: FwmcNodeController? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
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
        startForeground(NOTIFICATION_ID, buildNotification("FWMC 节点运行中"))

        val dataDir = MyApp.getStoragePath(applicationContext)
        val settings = KVSettings(Preference(Settings()))
        val port = if (settings.network.port > 1024) settings.network.port else (1025..65535).random()

        val ctrl = FwmcNodeController(dataDir)
        val bound = ctrl.start(port)
        controller = ctrl
        MyApp.getApp().setFwmc(ctrl)
        if (bound > 0) {
            notifyText("FWMC 节点运行中 · 端口 $bound")
        } else {
            notifyText("FWMC 节点启动失败")
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

    private fun notifyText(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun stopNode() {
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

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "FWMC 节点",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "FWMC 节点后台运行通知"
            setSound(null, null)
            enableVibration(false)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val stopIntent = Intent(this, FwmcService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FreeWebMovement")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_fwmc)
            .setColor(0xFF1E88E5.toInt())
            .setOngoing(true)
            .setShowWhen(true)
            .addAction(android.R.drawable.ic_media_pause, "停止", stopPending)
            .build()
    }

    companion object {
        // v2：提高通道重要性到 DEFAULT 以在状态栏显示图标（旧 fwmc_service 通道保留 LOW，无法覆盖）
        private const val CHANNEL_ID = "fwmc_service_v2"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "io.github.freewebmovement.fwmc.STOP"

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
