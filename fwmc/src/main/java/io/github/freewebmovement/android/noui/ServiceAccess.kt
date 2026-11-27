package io.github.freewebmovement.android.noui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import org.json.JSONObject
import rs.zz.coin.FwmcService

/**
 * Unified service access for Android frontends.
 *
 * - Server/managed services: delegated to fwmc via [FwmcService] JNI.
 * - Client/external-app services: resolved by package id and opened via Intent;
 *   zz does not embed the client implementation.
 *
 * Response shape (JSON string):
 *   {"success":true/false, "name":"...", "action":"...", ...}
 */
object ServiceAccess {

    /** Well-known external client apps. */
    const val PKG_IGNITER = "io.github.freewebmovement.igniter"
    const val SERVICE_TROJAN = "trojan"
    const val SERVICE_TROJAN_CLIENT = "trojan-client"

    data class ExternalApp(
        val serviceName: String,
        val packageId: String,
        val label: String,
    )

    val knownExternalClients = listOf(
        ExternalApp(SERVICE_TROJAN_CLIENT, PKG_IGNITER, "Igniter"),
    )

    fun list(): String = runCatching { FwmcService.list() }
        .getOrElse { err("list", it) }

    fun status(name: String): String = runCatching { FwmcService.status(name) }
        .getOrElse { err(name, it) }

    /**
     * Start/open a service. For external-app clients, [context] is required to
     * launch the installed app; [configJson] is ignored for external apps.
     */
    fun start(context: Context?, name: String, configJson: String = ""): String {
        return runCatching {
            val raw = FwmcService.start(name, configJson)
            val obj = JSONObject(raw)
            if (obj.optString("action") == "open_external_app") {
                val pkg = obj.optJSONObject("endpoint")?.optString("package_id")
                    ?.ifBlank { externalPackageFor(name) }
                    ?: externalPackageFor(name)
                if (pkg.isNullOrBlank()) return raw
                val ctx = context ?: return JSONObject()
                    .put("success", false)
                    .put("name", name)
                    .put("error", "context required to open external app")
                    .put("package_id", pkg)
                    .toString()
                return openExternalApp(ctx, name, pkg)
            }
            raw
        }.getOrElse { err(name, it) }
    }

    fun stop(name: String): String = runCatching { FwmcService.stop(name) }
        .getOrElse { err(name, it) }

    fun isAppInstalled(context: Context, packageId: String): Boolean = try {
        context.packageManager.getPackageInfo(packageId, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    } catch (_: Throwable) {
        false
    }

    /**
     * Open an installed external service app by package id. No embedding.
     * Falls back to market/details page if not installed.
     */
    fun openExternalApp(context: Context, serviceName: String, packageId: String): String {
        return runCatching {
            val pm = context.packageManager
            val launch = pm.getLaunchIntentForPackage(packageId)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
                JSONObject()
                    .put("success", true)
                    .put("name", serviceName)
                    .put("action", "opened_external_app")
                    .put("package_id", packageId)
                    .toString()
            } else {
                // Not installed: try to open app details/market.
                val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageId"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val details = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageId"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val opened = runCatching { context.startActivity(market); true }
                    .getOrElse { runCatching { context.startActivity(details); true }.getOrDefault(false) }
                JSONObject()
                    .put("success", false)
                    .put("name", serviceName)
                    .put("action", if (opened) "opened_app_store" else "app_not_found")
                    .put("package_id", packageId)
                    .put("error", "app not installed: $packageId")
                    .toString()
            }
        }.getOrElse { err(serviceName, it) }
    }

    private fun externalPackageFor(name: String): String? =
        knownExternalClients.firstOrNull { it.serviceName == name }?.packageId

    private fun err(name: String, t: Throwable): String = JSONObject()
        .put("success", false)
        .put("name", name)
        .put("error", t.message ?: t.toString())
        .toString()
}
