package io.github.freewebmovement.android.noui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
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
        startForeground(NOTIFICATION_ID, buildNotification("FWMC 节点运行中"))

        val dataDir = MyApp.getStoragePath(applicationContext)
        val settings = KVSettings(Preference(Settings()))
        val port = if (settings.network.port > 1024) settings.network.port else (1025..65535).random()

        val ctrl = FwmcNodeController(dataDir)
        ctrl.start(port)
        controller = ctrl
        MyApp.getApp().setFwmc(ctrl)
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
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "FWMC 节点后台运行通知"
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
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "停止", stopPending)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "fwmc_service"
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
