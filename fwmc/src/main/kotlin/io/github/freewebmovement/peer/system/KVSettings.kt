package io.github.freewebmovement.peer.system

import io.github.freewebmovement.peer.types.IPScopeType
import io.github.freewebmovement.peer.types.IPType
import io.github.freewebmovement.peer.interfaces.IPreference
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.reflect.KProperty

//enum class MessageType(i: Int) {
//    TEXT(0b1),
//    IMAGE(0b10),
//    VOICE(0b100),
//    VIDEO(0b1000)
//}
//
//enum class RealtimeType(i: Int) {
//    VOICE(0b1),
//    VIDEO(0b10)
//}

//private const val MESSAGE_PERSISTENCE_PERIOD = "MESSAGE_PERSISTENCE_PERIOD"
//private const val MESSAGE_TYPE = "MESSAGE_TYPE"

//private const val REALTIME_COMMUNICATION_TYPE = "REALTIME_COMMUNICATION_TYPE"

private const val PEER_IP_TYPE = "PEER_IP_TYPE"
private const val PEER_IP_SCOPE_TYPE = "PEER_IP_SCOPE_TYPE"
private const val LOCAL_SERVER_PORT = "LOCAL_SERVER_PORT"
private const val STATIC_FILE_ENABLED = "STATIC_FILE_ENABLED"
private const val STATIC_FILE_ROOT = "STATIC_FILE_ROOT"

private const val DISK_ALLOCATION = "DISK_ALLOCATION"
private const val CPU_ALLOCATION = "CPU_ALLOCATION"
private const val MEM_ALLOCATION = "MEM_ALLOCATION"
private const val GPU_ALLOCATION = "GPU_ALLOCATION"
private const val BLE_ALLOCATION = "BLE_ALLOCATION"
private const val WIFI_ALLOCATION = "WIFI_ALLOCATION"
private const val LOCATION_ENABLED = "LOCATION_ENABLED"

private const val MINE_PROFILE_IMAGE_URI = "MINE_PROFILE_IMAGE_URI"
private const val MINE_PROFILE_NICKNAME = "MINE_PROFILE_NICKNAME"
private const val MINE_PROFILE_INTRO = "MINE_PROFILE_INTRO"

private const val EPOCH_TEST_MODE = "EPOCH_TEST_MODE"
private const val EPOCH_TEST_TICK_SECONDS = "EPOCH_TEST_TICK_SECONDS"
private const val EPOCH_TEST_START_TICK = "EPOCH_TEST_START_TICK"

private const val TROJAN_REMOTE_ADDR = "TROJAN_REMOTE_ADDR"
private const val TROJAN_REMOTE_PORT = "TROJAN_REMOTE_PORT"
private const val TROJAN_PASSWORD = "TROJAN_PASSWORD"
private const val TROJAN_LOCAL_ADDR = "TROJAN_LOCAL_ADDR"
private const val TROJAN_LOCAL_PORT = "TROJAN_LOCAL_PORT"
private const val TROJAN_VERIFY_CERT = "TROJAN_VERIFY_CERT"
private const val TROJAN_SNI = "TROJAN_SNI"
private const val TROJAN_SERVERS = "TROJAN_SERVERS"
private const val TROJAN_SELECTED_INDEX = "TROJAN_SELECTED_INDEX"
private const val TROJAN_PROXY_ALL_APPS = "TROJAN_PROXY_ALL_APPS"
private const val TROJAN_PROXIED_APPS = "TROJAN_PROXIED_APPS"
private const val TROJAN_BYPASS_LAN = "TROJAN_BYPASS_LAN"
private const val TROJAN_CN_DIRECT = "TROJAN_CN_DIRECT"
private const val TROJAN_DIRECT_DOMAINS = "TROJAN_DIRECT_DOMAINS"
private const val TROJAN_PROXY_DOMAINS = "TROJAN_PROXY_DOMAINS"
private const val TROJAN_BLOCKED_DOMAINS = "TROJAN_BLOCKED_DOMAINS"
private const val TROJAN_UNREACHABLE_DOMAINS = "TROJAN_UNREACHABLE_DOMAINS"
private const val TROJAN_IPV6 = "TROJAN_IPV6"
private const val TROJAN_AUTO_START = "TROJAN_AUTO_START"
private const val TROJAN_APP_COUNTRIES = "TROJAN_APP_COUNTRIES"


class PreferenceAccessor<T>(
    private val preference: IPreference,
    private val key: String,
    private var field: T
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        field = preference.read(key, field)
        return field
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        preference.save(key, value)
        field = value
    }
}

data class ProfileSetting(
    private val preference: IPreference
) {
    var imageUri: String by PreferenceAccessor(
        preference, MINE_PROFILE_IMAGE_URI,
        field = ""
    )
    var nickname: String by PreferenceAccessor(
        preference, MINE_PROFILE_NICKNAME,
        field = ""
    )
    var intro: String by PreferenceAccessor(
        preference, MINE_PROFILE_INTRO,
        field = ""
    )
}

//data class MessageSetting(
//    private val preference: IPreference
//) {
//        var period: Int by PreferenceAccessor(
//        preference, MESSAGE_PERSISTENCE_PERIOD,
//        field = 0
//    )
//    var typeSupported: Int by PreferenceAccessor(
//        preference, MESSAGE_TYPE,
//        field = 0
//    )
//    var realtimeTypeSupported: Int by PreferenceAccessor(
//        preference, REALTIME_COMMUNICATION_TYPE,
//        field = 0
//    )
//}

data class NetworkSetting(
    private val preference: IPreference
) {
    companion object {
        /** fwmc 节点默认服务端口 */
        const val DEFAULT_SERVER_PORT = 20260
    }

    /** 恢复默认端口 */
    fun resetPort() {
        preference.save(LOCAL_SERVER_PORT, DEFAULT_SERVER_PORT)
    }

    var ipType: IPType by PreferenceAccessor(
        preference, PEER_IP_TYPE,
        field = IPType.IPV4
    )

    var ipScopeType: IPScopeType by PreferenceAccessor(
        preference, PEER_IP_SCOPE_TYPE,
        field = IPScopeType.LOCAL
    )

    var staticFileEnabled: Boolean by PreferenceAccessor(
        preference, STATIC_FILE_ENABLED, field = false
    )

    var staticFileRoot: String by PreferenceAccessor(
        preference, STATIC_FILE_ROOT, field = ""
    )

    var diskAllocation: Int by PreferenceAccessor(preference, DISK_ALLOCATION, field = 500)
    var cpuAllocation: Int by PreferenceAccessor(preference, CPU_ALLOCATION, field = 1)
    var memAllocation: Int by PreferenceAccessor(preference, MEM_ALLOCATION, field = 20)
    var gpuAllocation: Int by PreferenceAccessor(preference, GPU_ALLOCATION, field = 0)
    var bleAllocation: Int by PreferenceAccessor(preference, BLE_ALLOCATION, field = 1)
    var wifiAllocation: Int by PreferenceAccessor(preference, WIFI_ALLOCATION, field = 1)
    var locationEnabled: Boolean by PreferenceAccessor(preference, LOCATION_ENABLED, field = false)

    var port: Int = 0
        get() {
            field = preference.read(LOCAL_SERVER_PORT, DEFAULT_SERVER_PORT)
            if (field == 0) {
                val min = (1u shl 10) + 1u
                val max = (1u shl 16) - 1u
                field = (min..max).random().toInt()
                preference.save(LOCAL_SERVER_PORT, field)
            }
            return field
        }
        set(value) {
            if (value <= 1024 && value != 0) {
                throw Exception("Value must be larger than 1024!")
            }
            preference.save(LOCAL_SERVER_PORT, value)
            field = value
        }
}


/**
 * 纪元运行模式设置（我的 → 纪元设置）：
 *
 * - **生产模式**（默认）：真实墙钟，每 15 分钟一个 tick，96 tick = 1 天。
 * - **测试模式**：虚拟时钟加速，每个 tick 可自定义真实时长（秒），
 *   96 tick = 1 个 epoch，便于验证 epoch 切换。
 *
 * **「24h = 96 tick」比例不变**，被压缩的只是等待这些虚拟时间所需的真实时间。
 * 修改后需**重启节点**生效。
 */
data class DebugSetting(
    private val preference: IPreference
) {
    companion object {
        /** 测试模式默认每 tick 真实秒数（96 tick = 96 秒/epoch）。 */
        const val DEFAULT_TEST_TICK_SECONDS = 1
    }

    /** true = 测试模式（加速），false = 生产模式（15 分钟/tick）。 */
    var epochTestMode: Boolean by PreferenceAccessor(
        preference, EPOCH_TEST_MODE, field = false
    )

    /** 测试模式下每个 tick 的真实秒数（生产模式下无效）。 */
    var epochTickSeconds: Int by PreferenceAccessor(
        preference, EPOCH_TEST_TICK_SECONDS, field = DEFAULT_TEST_TICK_SECONDS
    )

    /** 测试模式下 TICK 的初始值（0..96-1）；达到 96 时发 EPOCH_NEW。生产模式无效。 */
    var epochStartTick: Int by PreferenceAccessor(
        preference, EPOCH_TEST_START_TICK, field = 0
    )
}

/** 一个 Trojan 服务器条目（与 igniter 的服务器列表对应）。 */
@Serializable
data class TrojanServer(
    val id: String,
    val name: String = "",
    val remoteAddr: String,
    val remotePort: Int = 443,
    val password: String = "",
    val localAddr: String = "127.0.0.1",
    val localPort: Int = 1080,
    val verifyCert: Boolean = true,
    val sni: String = "",
) {
    val displayName: String
        get() = name.ifBlank { remoteAddr }
}

private val TROJAN_JSON = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Trojan 客户端服务配置（我的 → 服务器 → 提供的服务 → Trojan）。
 *
 * 对应 trojan 的 client.json：本地 SOCKS5 监听 + 远程 trojan 服务器。
 * 支持多个服务器，`selectedIndex` 指向当前使用的服务器。
 */
data class TrojanSetting(
    private val preference: IPreference
) {
    companion object {
        const val DEFAULT_REMOTE_PORT = 443
        const val DEFAULT_LOCAL_ADDR = "127.0.0.1"
        const val DEFAULT_LOCAL_PORT = 1080
    }

    var remoteAddr: String by PreferenceAccessor(
        preference, TROJAN_REMOTE_ADDR, field = ""
    )

    var remotePort: Int by PreferenceAccessor(
        preference, TROJAN_REMOTE_PORT, field = DEFAULT_REMOTE_PORT
    )

    var password: String by PreferenceAccessor(
        preference, TROJAN_PASSWORD, field = ""
    )

    var localAddr: String by PreferenceAccessor(
        preference, TROJAN_LOCAL_ADDR, field = DEFAULT_LOCAL_ADDR
    )

    var localPort: Int by PreferenceAccessor(
        preference, TROJAN_LOCAL_PORT, field = DEFAULT_LOCAL_PORT
    )

    var verifyCert: Boolean by PreferenceAccessor(
        preference, TROJAN_VERIFY_CERT, field = true
    )

    var sni: String by PreferenceAccessor(
        preference, TROJAN_SNI, field = ""
    )

    // ---- 多服务器列表 ----

    var serversJson: String by PreferenceAccessor(
        preference, TROJAN_SERVERS, field = "[]"
    )

    var selectedIndex: Int by PreferenceAccessor(
        preference, TROJAN_SELECTED_INDEX, field = 0
    )

    fun loadServers(): List<TrojanServer> {
        val raw = serversJson
        val parsed = runCatching {
            TROJAN_JSON.decodeFromString<List<TrojanServer>>(raw)
        }.getOrElse { emptyList() }
        if (parsed.isNotEmpty()) return parsed
        return migrateLegacyServer()
    }

    fun saveServers(servers: List<TrojanServer>) {
        serversJson = TROJAN_JSON.encodeToString(servers)
        if (selectedIndex !in servers.indices) {
            selectedIndex = 0
        }
    }

    fun currentServer(): TrojanServer? {
        val list = loadServers()
        if (list.isEmpty()) return null
        return list.getOrNull(selectedIndex.coerceIn(0, list.lastIndex)) ?: list.first()
    }

    fun selectServer(id: String) {
        val index = loadServers().indexOfFirst { it.id == id }
        if (index >= 0) selectedIndex = index
    }

    fun upsertServer(server: TrojanServer) {
        val list = loadServers().toMutableList()
        val index = list.indexOfFirst { it.id == server.id }
        val isNew = index < 0
        if (isNew) list.add(server) else list[index] = server
        saveServers(list)
        if (isNew) selectedIndex = list.lastIndex
    }

    fun deleteServer(id: String) {
        saveServers(loadServers().filterNot { it.id == id })
    }

    // ---- 代理应用 / 分流 ----

    var proxyAllApps: Boolean by PreferenceAccessor(
        preference, TROJAN_PROXY_ALL_APPS, field = true
    )

    var proxiedAppsRaw: String by PreferenceAccessor(
        preference, TROJAN_PROXIED_APPS, field = ""
    )

    var bypassLan: Boolean by PreferenceAccessor(
        preference, TROJAN_BYPASS_LAN, field = false
    )

    var cnDirectEnabled: Boolean by PreferenceAccessor(
        preference, TROJAN_CN_DIRECT, field = true
    )

    var directDomainsRaw: String by PreferenceAccessor(
        preference, TROJAN_DIRECT_DOMAINS, field = ""
    )

    fun loadDirectDomains(): Set<String> =
        if (directDomainsRaw.isEmpty()) emptySet()
        else directDomainsRaw.split("||").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

    fun saveDirectDomains(domains: Set<String>) {
        directDomainsRaw = domains.joinToString("||")
    }

    var proxyDomainsRaw: String by PreferenceAccessor(
        preference, TROJAN_PROXY_DOMAINS, field = ""
    )

    var blockedDomainsRaw: String by PreferenceAccessor(
        preference, TROJAN_BLOCKED_DOMAINS, field = ""
    )

    fun loadProxyDomains(): Set<String> =
        if (proxyDomainsRaw.isEmpty()) emptySet()
        else proxyDomainsRaw.split("||").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

    fun saveProxyDomains(domains: Set<String>) {
        proxyDomainsRaw = domains.joinToString("||")
    }

    fun loadBlockedDomains(): Set<String> =
        if (blockedDomainsRaw.isEmpty()) emptySet()
        else blockedDomainsRaw.split("||").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

    fun saveBlockedDomains(domains: Set<String>) {
        blockedDomainsRaw = domains.joinToString("||")
    }

    var unreachableDomainsRaw: String by PreferenceAccessor(
        preference, TROJAN_UNREACHABLE_DOMAINS, field = ""
    )

    fun loadUnreachableDomains(): Set<String> =
        if (unreachableDomainsRaw.isEmpty()) emptySet()
        else unreachableDomainsRaw.split("||").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

    fun saveUnreachableDomains(domains: Set<String>) {
        unreachableDomainsRaw = domains.joinToString("||")
    }

    var enableIpv6: Boolean by PreferenceAccessor(
        preference, TROJAN_IPV6, field = false
    )

    var autoStart: Boolean by PreferenceAccessor(
        preference, TROJAN_AUTO_START, field = false
    )

    var appCountriesRaw: String by PreferenceAccessor(
        preference, TROJAN_APP_COUNTRIES, field = ""
    )

    fun loadAppCountries(): Map<String, String> {
        if (appCountriesRaw.isEmpty()) return emptyMap()
        return appCountriesRaw.split("||")
            .mapNotNull { entry ->
                val parts = entry.split(":", limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    parts[0] to parts[1].uppercase()
                } else {
                    null
                }
            }
            .toMap()
    }

    fun saveAppCountries(countries: Map<String, String>) {
        appCountriesRaw = countries.entries.joinToString("||") { "${it.key}:${it.value}" }
    }

    fun loadProxiedApps(): Set<String> =
        if (proxiedAppsRaw.isEmpty()) emptySet()
        else proxiedAppsRaw.split("||").filter { it.isNotEmpty() }.toSet()

    fun saveProxiedApps(apps: Set<String>) {
        proxiedAppsRaw = apps.joinToString("||")
    }

    val isConfigured: Boolean
        get() = currentServer() != null

    private fun legacyServer(): TrojanServer? {
        if (remoteAddr.isBlank() || password.isBlank()) return null
        return TrojanServer(
            id = UUID.randomUUID().toString(),
            name = remoteAddr,
            remoteAddr = remoteAddr,
            remotePort = remotePort,
            password = password,
            localAddr = localAddr,
            localPort = localPort,
            verifyCert = verifyCert,
            sni = sni,
        )
    }

    /** 把旧版单服务器配置迁移到列表，并清空旧字段避免重复迁移。 */
    private fun migrateLegacyServer(): List<TrojanServer> {
        val legacy = legacyServer() ?: return emptyList()
        remoteAddr = ""
        password = ""
        val list = listOf(legacy)
        saveServers(list)
        selectedIndex = 0
        return list
    }
}

class KVSettings(preference: IPreference) {
    // Message Persistence Period
//    val message = MessageSetting(preference)
    val profile = ProfileSetting(preference)
    val network = NetworkSetting(preference)
    val debug = DebugSetting(preference)
    val trojan = TrojanSetting(preference)
}