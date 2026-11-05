package io.github.freewebmovement.android.noui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import io.github.freewebmovement.fwmc.R

/**
 * Media-style ongoing notification for the Trojan service.
 *
 * It lives in the notification shade (pulled down from the top status bar) and
 * exposes a compact play/pause action, much like a media player's mini bar.
 */
object TrojanStatusNotifier {
    const val NOTIFICATION_ID = 2
    private const val CHANNEL_ID = "trojan_status_v2"

    private var mediaSession: MediaSessionCompat? = null

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Trojan 状态",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Trojan 服务状态与快捷开关"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun build(context: Context, running: Boolean, text: String): Notification {
        ensureChannel(context)
        val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val openPending = openIntent?.let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        val actionIntent = Intent(context, TrojanService::class.java).apply {
            action = if (running) TrojanService.ACTION_STOP else TrojanService.ACTION_START
        }
        val actionPending = if (!running && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                context,
                1,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        } else {
            PendingIntent.getService(
                context,
                1,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_fwmc)
            .setContentTitle("Trojan")
            .setContentText(text)
            .setColor(0xFF1E88E5.toInt())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPending)
            .addAction(
                if (running) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (running) "停止" else "启动",
                actionPending,
            )
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession(context).sessionToken)
                    .setShowActionsInCompactView(0)
            )
            .build()
    }

    fun showStopped(context: Context) {
        post(context, build(context, running = false, text = "Trojan 已停止"))
    }

    fun showRunning(context: Context, text: String = "Trojan 运行中") {
        post(context, build(context, running = true, text = text))
    }

    fun cancel(context: Context) {
        runCatching {
            context.getSystemService(NotificationManager::class.java)
                .cancel(NOTIFICATION_ID)
        }
    }

    @Synchronized
    private fun mediaSession(context: Context): MediaSessionCompat {
        var session = mediaSession
        if (session == null) {
            session = MediaSessionCompat(context.applicationContext, "TrojanStatus").apply {
                setFlags(
                    MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
                )
                isActive = true
            }
            mediaSession = session
        }
        return session
    }

    private fun post(context: Context, notification: Notification) {
        runCatching {
            context.getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, notification)
        }
    }
}
