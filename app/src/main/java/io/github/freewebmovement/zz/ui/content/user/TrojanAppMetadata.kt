package io.github.freewebmovement.zz.ui.content.user

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import java.net.URLEncoder

/**
 * Best-effort online lookup of an app's developer country.
 *
 * The lookup first tries the Play Store page and then APKPure. If the Trojan
 * service is running, requests go through its local SOCKS5 port so the lookup
 * itself can use the proxy. Failures are ignored and the local certificate /
 * installer / package heuristics remain in effect.
 */
internal object TrojanAppMetadata {
    suspend fun lookup(packageName: String, socksPort: Int?): String? = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(packageName, "UTF-8")
        val urls = listOf(
            "https://play.google.com/store/apps/details?id=$encoded&hl=en&gl=US",
            "https://apkpure.com/search?q=$encoded",
        )
        for (url in urls) {
            val country = fetchCountry(url, socksPort)
            if (country != null) return@withContext country
        }
        null
    }

    private fun fetchCountry(url: String, socksPort: Int?): String? = runCatching {
        val proxy = socksPort?.let {
            Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", it))
        }
        val connection = (if (proxy != null) URL(url).openConnection(proxy) else URL(url).openConnection())
            as HttpURLConnection
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36",
        )
        val html = connection.inputStream.bufferedReader().use { it.readText().take(400_000) }
        connection.disconnect()
        parseCountry(html)
    }.getOrNull()

    private fun parseCountry(html: String): String? {
        val addressSnippet = sequenceOf(
            html.substringAfter("Developer address", ""),
            html.substringAfter("\"address\"", ""),
        ).firstOrNull { it.isNotBlank() }?.take(2_000)

        val snippet = addressSnippet ?: html.take(400_000)
        for ((needle, code) in COUNTRY_HINTS) {
            if (snippet.contains(needle, ignoreCase = true)) return code
        }
        return null
    }

    private val COUNTRY_HINTS = listOf(
        "China" to "CN", "Beijing" to "CN", "Shanghai" to "CN", "Shenzhen" to "CN",
        "Hangzhou" to "CN", "Guangzhou" to "CN", "Chengdu" to "CN",
        "Russia" to "RU", "Moscow" to "RU",
        "Iran" to "IR", "Tehran" to "IR",
        "North Korea" to "KP",
        "Belarus" to "BY", "Minsk" to "BY",
        "Syria" to "SY", "Venezuela" to "VE", "Cuba" to "CU", "Myanmar" to "MM",
        "United States" to "US", "California" to "US", "New York" to "US",
        "United Kingdom" to "GB", "London" to "GB",
        "Germany" to "DE", "Berlin" to "DE",
        "Japan" to "JP", "Tokyo" to "JP",
        "South Korea" to "KR", "Seoul" to "KR",
        "Taiwan" to "TW", "Taipei" to "TW",
        "Singapore" to "SG",
        "Canada" to "CA",
        "Australia" to "AU",
        "France" to "FR", "Paris" to "FR",
        "Netherlands" to "NL", "Amsterdam" to "NL",
        "Sweden" to "SE", "Stockholm" to "SE",
        "Switzerland" to "CH", "Zurich" to "CH",
    )
}
