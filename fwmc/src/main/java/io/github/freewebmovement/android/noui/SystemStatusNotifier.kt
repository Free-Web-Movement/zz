package io.github.freewebmovement.android.noui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.russhwolf.settings.Settings
import io.github.freewebmovement.fwmc.R
import io.github.freewebmovement.peer.system.KVSettings
import io.github.freewebmovement.peer.system.Preference

/**
 * Single ongoing control-bar notification shared by the fwmc node and the
 * Trojan VPN. The expanded view lists every service on its own row, with the
 * node listening port / current epoch and per-service quick controls.
 */
object SystemStatusNotifier {
    const val NOTIFICATION_ID = 100

    private const val CHANNEL_ID = "zz_status_v5"
    private const val LEGACY_NODE_ID = 1
    private const val LEGACY_TROJAN_ID = 2

    private const val COLOR_RUNNING = 0xFF2E7D32.toInt()
    private const val COLOR_STOPPED = 0xFF8A8A8A.toInt()
    private const val COLOR_UNAVAILABLE = 0xFFA0A0A0.toInt()
    private const val DEFAULT_ACCENT = 0xFF9C27B0.toInt()
    private const val KEY_THEME_COLOR = "ui_theme_primary_color"

    @Volatile
    private var nodeRunning = false

    @Volatile
    private var nodeStatus = "已停止"

    @Volatile
    private var nodePort = 0

    @Volatile
    private var epoch = 0L

    @Volatile
    private var epochTick = 0L

    @Volatile
    private var ticksPerEpoch = 0L

    @Volatile
    private var tickCount = 0L

    @Volatile
    private var trojanRunning = false

    @Volatile
    private var trojanStatus = "已停止"

    @Volatile
    private var trojanDetail = ""

    @Volatile
    private var keepIdle = false

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Zz 后台服务",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "节点与 Trojan 的运行状态和快捷控制"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
        nm.cancel(LEGACY_NODE_ID)
        nm.cancel(LEGACY_TROJAN_ID)
    }

    /** Posts the control bar even before a service starts. */
    fun showIdle(context: Context) {
        keepIdle = true
        ensureChannel(context)
        refresh(context)
    }

    fun setNode(context: Context, running: Boolean, status: String) {
        nodeRunning = running
        nodeStatus = status
        refresh(context)
    }

    fun setNodeDetails(
        context: Context,
        port: Int,
        epoch: Long,
        epochTick: Long,
        ticksPerEpoch: Long,
        tickCount: Long,
    ) {
        if (nodePort == port &&
            this.epoch == epoch &&
            this.epochTick == epochTick &&
            this.ticksPerEpoch == ticksPerEpoch &&
            this.tickCount == tickCount
        ) {
            return
        }
        nodePort = port
        this.epoch = epoch
        this.epochTick = epochTick
        this.ticksPerEpoch = ticksPerEpoch
        this.tickCount = tickCount
        refresh(context)
    }

    fun setTrojan(context: Context, running: Boolean, status: String, detail: String = "") {
        trojanRunning = running
        trojanStatus = status
        trojanDetail = detail
        refresh(context)
    }

    fun isNodeRunning(): Boolean = nodeRunning

    fun isTrojanRunning(): Boolean = trojanRunning

    fun nodePortValue(): Int = nodePort

    fun epochValue(): Long = epoch

    fun nodeStatusValue(): String = nodeStatus

    fun trojanStatusValue(): String = trojanStatus

    fun refresh(context: Context) {
        ensureChannel(context)
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nodeRunning || trojanRunning || keepIdle) {
            runCatching { nm.notify(NOTIFICATION_ID, build(context)) }
        } else {
            runCatching { nm.cancel(NOTIFICATION_ID) }
        }
    }

    fun build(context: Context): Notification {
        ensureChannel(context)

        val webEnabled = readWebEnabled()
        val webRunning = webEnabled && nodeRunning
        val zzSummary = summaryText(webRunning)
        val summary = overallSummary(zzSummary)
        val accent = accentColor()

        val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val openPending = openIntent?.let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        val collapsed = RemoteViews(context.packageName, R.layout.notification_zz_control_collapsed).apply {
            setTextViewText(R.id.zz_collapsed_summary, summary)
            setTextColor(R.id.zz_collapsed_summary, accent)
        }
        val expanded = buildExpanded(context, webEnabled, webRunning, zzSummary, accent)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_fwmc)
            .setContentTitle("Zz 服务")
            .setContentText(summary)
            .setColor(accent)
            .setOngoing(nodeRunning || trojanRunning)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openPending)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsed)
            .setCustomBigContentView(expanded)
            .build()
    }

    private fun buildExpanded(
        context: Context,
        webEnabled: Boolean,
        webRunning: Boolean,
        zzSummary: String,
        accent: Int,
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.notification_zz_control)
        rv.setTextViewText(R.id.zz_title, "Zz 服务")
        rv.setTextColor(R.id.zz_title, accent)
        rv.setTextViewText(R.id.zz_summary, zzSummary)
        rv.setTextViewText(R.id.zz_external_title, "外接服务")
        rv.setTextColor(R.id.zz_external_title, accent)
        val socksRunning = false
        val frpRunning = false
        val anyZzAddon = socksRunning || frpRunning
        rv.setViewVisibility(R.id.zz_addon_label, if (anyZzAddon) View.VISIBLE else View.GONE)
        rv.setViewVisibility(R.id.zz_socks_row, if (socksRunning) View.VISIBLE else View.GONE)
        rv.setViewVisibility(R.id.zz_frp_row, if (frpRunning) View.VISIBLE else View.GONE)
        rv.setViewVisibility(
            R.id.zz_external_box,
            if (trojanRunning) View.VISIBLE else View.GONE,
        )

        rv.setInt(R.id.zz_node_action, "setBackgroundColor", accent)
        rv.setInt(R.id.zz_web_action, "setBackgroundColor", accent)
        rv.setInt(R.id.zz_socks_action, "setBackgroundColor", accent)
        rv.setInt(R.id.zz_frp_action, "setBackgroundColor", accent)
        rv.setInt(R.id.zz_trojan_action, "setBackgroundColor", accent)

        bindSettings(rv, context, R.id.zz_node_settings, "node", 5, accent, "设置基础节点")
        bindSettings(rv, context, R.id.zz_web_settings, "web", 6, accent, "设置 Web")
        bindSettings(rv, context, R.id.zz_socks_settings, "socks", 7, accent, "设置 SOCKS")
        bindSettings(rv, context, R.id.zz_frp_settings, "frp", 8, accent, "设置 FRP")
        bindSettings(rv, context, R.id.zz_trojan_settings, "trojan", 9, accent, "设置 Trojan")

        // Zz base node: listening port + current epoch.
        rv.setTextViewText(R.id.zz_node_status, nodeStatus)
        rv.setTextColor(R.id.zz_node_status, if (nodeRunning) COLOR_RUNNING else COLOR_STOPPED)
        rv.setTextViewText(R.id.zz_node_info, nodeInfo())
        rv.setImageViewResource(
            R.id.zz_node_action,
            if (nodeRunning) R.drawable.ic_zz_pause else R.drawable.ic_zz_play,
        )
        rv.setContentDescription(R.id.zz_node_action, if (nodeRunning) "停止节点" else "启动节点")
        rv.setOnClickPendingIntent(
            R.id.zz_node_action,
            servicePending(context, nodeRunning, node = true),
        )

        // Web is the HTTP service and shares the node port.
        rv.setTextViewText(R.id.zz_web_status, if (webRunning) "运行中" else "已停止")
        rv.setTextColor(R.id.zz_web_status, if (webRunning) COLOR_RUNNING else COLOR_STOPPED)
        rv.setTextViewText(
            R.id.zz_web_info,
            "HTTP · 端口 ${if (nodePort > 0) nodePort else "—"}",
        )
        rv.setImageViewResource(
            R.id.zz_web_action,
            if (webEnabled) R.drawable.ic_zz_pause else R.drawable.ic_zz_play,
        )
        rv.setContentDescription(R.id.zz_web_action, if (webEnabled) "停止 Web" else "启动 Web")
        rv.setOnClickPendingIntent(R.id.zz_web_action, webPending(context))

        // Not-yet-integrated value-added services.
        rv.setTextViewText(R.id.zz_socks_status, "未接入")
        rv.setTextColor(R.id.zz_socks_status, COLOR_UNAVAILABLE)
        rv.setTextViewText(R.id.zz_socks_info, "")
        rv.setViewVisibility(R.id.zz_socks_info, View.GONE)
        rv.setViewVisibility(R.id.zz_socks_action, View.GONE)

        rv.setTextViewText(R.id.zz_frp_status, "未接入")
        rv.setTextColor(R.id.zz_frp_status, COLOR_UNAVAILABLE)
        rv.setTextViewText(R.id.zz_frp_info, "")
        rv.setViewVisibility(R.id.zz_frp_info, View.GONE)
        rv.setViewVisibility(R.id.zz_frp_action, View.GONE)

        // Trojan (value-added service, independent from Zz).
        rv.setTextViewText(R.id.zz_trojan_status, trojanStatus)
        rv.setTextColor(R.id.zz_trojan_status, if (trojanRunning) COLOR_RUNNING else COLOR_STOPPED)
        rv.setTextViewText(R.id.zz_trojan_info, trojanDetail)
        rv.setImageViewResource(
            R.id.zz_trojan_action,
            if (trojanRunning) R.drawable.ic_zz_pause else R.drawable.ic_zz_play,
        )
        rv.setContentDescription(
            R.id.zz_trojan_action,
            if (trojanRunning) "停止 Trojan" else "启动 Trojan",
        )
        rv.setOnClickPendingIntent(
            R.id.zz_trojan_action,
            servicePending(context, trojanRunning, node = false),
        )

        return rv
    }

    private fun summaryText(webRunning: Boolean): String = buildString {
        append("节点 ")
        append(nodeStatus)
        append(" · Epoch ")
        append(epoch)
        append(" · Web ")
        append(if (webRunning) "运行中" else "已停止")
    }

    private fun overallSummary(zzSummary: String): String =
        if (trojanRunning) "$zzSummary · 外接：Trojan 运行中" else zzSummary

    private fun nodeInfo(): String = buildString {
        append("监听 ")
        append(if (nodePort > 0) nodePort else "—")
        append(" · Epoch ")
        append(epoch)
        if (ticksPerEpoch > 0) {
            append(" · Tick ")
            append(epochTick)
            append("/")
            append(ticksPerEpoch)
        }
    }

    private fun readWebEnabled(): Boolean =
        runCatching { KVSettings(Preference(Settings())).network.staticFileEnabled }.getOrDefault(false)

    private fun accentColor(): Int =
        runCatching {
            Preference(Settings()).read(KEY_THEME_COLOR, DEFAULT_ACCENT)
        }.getOrDefault(DEFAULT_ACCENT)

    private fun bindSettings(
        rv: RemoteViews,
        context: Context,
        viewId: Int,
        page: String,
        requestCode: Int,
        accent: Int,
        description: String,
    ) {
        rv.setInt(viewId, "setBackgroundColor", accent)
        rv.setContentDescription(viewId, description)
        rv.setOnClickPendingIntent(viewId, settingsPending(context, page, requestCode))
    }

    private fun settingsPending(context: Context, page: String, requestCode: Int): PendingIntent {
        val intent = Intent().apply {
            setClassName(context.packageName, "io.github.freewebmovement.zz.ui.SettingsActivity")
            putExtra("page", page)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun webPending(context: Context): PendingIntent {
        val intent = Intent(context, ZzControlReceiver::class.java).apply {
            action = ZzControlReceiver.ACTION_TOGGLE_WEB
        }
        return PendingIntent.getBroadcast(
            context,
            4,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun servicePending(context: Context, running: Boolean, node: Boolean): PendingIntent {
        val intent = if (node) {
            Intent(context, FwmcService::class.java).apply {
                action = if (running) FwmcService.ACTION_STOP else FwmcService.ACTION_START
            }
        } else {
            Intent(context, TrojanService::class.java).apply {
                action = if (running) TrojanService.ACTION_STOP else TrojanService.ACTION_START
            }
        }
        val requestCode = if (node) 2 else 3
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (!running && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(context, requestCode, intent, flags)
        } else {
            PendingIntent.getService(context, requestCode, intent, flags)
        }
    }
}
