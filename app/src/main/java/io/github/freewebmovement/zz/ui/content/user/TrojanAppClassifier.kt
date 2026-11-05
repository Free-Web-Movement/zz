package io.github.freewebmovement.zz.ui.content.user

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/** App grouping used by the per-app proxy selector. */
internal enum class AppCategory {
    SYSTEM,
    NON_FREE,
    FREE,
}

internal data class AppClassification(
    val category: AppCategory,
    val country: String?,
)

/**
 * Classifies an app using (in order):
 * 1. preinstalled / updated-system flag -> SYSTEM;
 * 2. cached online country / signing-certificate country / installer store;
 * 3. curated package prefixes from known non-free-country vendors;
 * 4. otherwise FREE.
 */
internal fun classifyApp(
    app: ApplicationInfo,
    pm: PackageManager,
    onlineCountries: Map<String, String>,
): AppClassification {
    val systemFlags = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
    if (app.flags and systemFlags != 0) {
        return AppClassification(AppCategory.SYSTEM, null)
    }

    val pkg = app.packageName
    val country = onlineCountries[pkg]
        ?: certificateCountry(pm, pkg)
        ?: installerCountry(pm, pkg)

    if (country != null) {
        val category = if (country in NON_FREE_COUNTRIES) {
            AppCategory.NON_FREE
        } else {
            AppCategory.FREE
        }
        return AppClassification(category, country)
    }

    val lower = pkg.lowercase()
    return if (NON_FREE_APP_PREFIXES.any { lower == it || lower.startsWith("$it.") }) {
        AppClassification(AppCategory.NON_FREE, null)
    } else {
        AppClassification(AppCategory.FREE, null)
    }
}

private val NON_FREE_COUNTRIES = setOf(
    "CN", "RU", "IR", "KP", "BY", "SY", "VE", "CU", "MM",
)

private val CHINESE_STORES = setOf(
    "com.xiaomi.market", "com.huawei.appmarket", "com.oppo.market",
    "com.heytap.market", "com.bbk.appstore", "com.tencent.android.qqdownloader",
    "com.qihoo.appstore", "com.baidu.appsearch", "com.taobao.appcenter",
    "com.wandoujia.phoenix2", "com.lenovo.leos.appstore", "com.meizu.mstore",
    "com.coolapk.market",
)

private val VENDOR_COUNTRIES = mapOf(
    "tencent" to "CN",
    "alibaba" to "CN",
    "baidu" to "CN",
    "xiaomi" to "CN",
    "huawei" to "CN",
    "bytedance" to "CN",
    "netease" to "CN",
    "sina" to "CN",
    "jd.com" to "CN",
    "meituan" to "CN",
    "qihoo" to "CN",
    "yandex" to "RU",
    "mail.ru" to "RU",
    "vk.com" to "RU",
)

private fun installerCountry(pm: PackageManager, pkg: String): String? {
    val installer = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pm.getInstallSourceInfo(pkg).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(pkg)
        }
    }.getOrNull() ?: return null
    return if (installer in CHINESE_STORES) "CN" else null
}

private fun certificateCountry(pm: PackageManager, pkg: String): String? {
    val der = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
            info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
        } else {
            @Suppress("DEPRECATION")
            val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES)
            @Suppress("DEPRECATION")
            info.signatures?.firstOrNull()?.toByteArray()
        }
    }.getOrNull() ?: return null

    val cert = runCatching {
        CertificateFactory.getInstance("X.509")
            .generateCertificate(der.inputStream()) as X509Certificate
    }.getOrNull() ?: return null

    val subject = cert.subjectX500Principal.name
    val country = Regex("(?:^|,)\\s*C=([A-Za-z]{2})")
        .find(subject)
        ?.groupValues
        ?.get(1)
        ?.uppercase()
    if (country != null) return country

    val organization = Regex("(?:^|,)\\s*O=([^,]+)")
        .find(subject)
        ?.groupValues
        ?.get(1)
        ?.lowercase()
        ?: return null
    return VENDOR_COUNTRIES.entries
        .firstOrNull { organization.contains(it.key) }
        ?.value
}

private val NON_FREE_APP_PREFIXES = listOf(
    "com.tencent", "com.taobao", "com.alibaba", "com.alipay",
    "com.eg.android.alipaygphone", "com.baidu", "com.jd", "com.jingdong",
    "com.xiaomi", "com.huawei", "com.hihonor", "com.oppo", "com.coloros",
    "com.vivo", "com.bbk", "com.meituan", "com.sankuai", "com.dianping",
    "com.bytedance", "com.ss.android", "com.zhiliaoapp", "com.sina",
    "com.weibo", "com.zhihu", "com.netease", "com.qihoo", "com.sohu",
    "com.youku", "com.iqiyi", "com.ctrip", "com.qunar", "com.unionpay",
    "com.icbc", "com.cmbchina", "com.pingan", "com.chinatelecom",
    "com.chinaunicom", "com.chinamobile", "com.xingin", "com.douyin",
    "com.kuaishou", "com.smile.gifmaker", "com.ucmobile", "com.UCMobile",
    "com.quark", "com.alivc", "com.taobao.taobao", "com.jingdong.app.mall",
    "ru.yandex", "com.vk", "ru.mail", "ru.ok", "ru.sberbank", "ru.alfabank",
    "ru.gosuslugi", "ru.mos", "ru.tinkoff", "com.idamob.tinkoff",
    "ir.",
)
