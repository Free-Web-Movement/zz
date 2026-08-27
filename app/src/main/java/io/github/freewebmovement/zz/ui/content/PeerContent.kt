package io.github.freewebmovement.zz.ui.content

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.ui.common.Divider
import io.github.freewebmovement.zz.ui.common.EmptyHint
import io.github.freewebmovement.zz.ui.common.InfoGrid
import io.github.freewebmovement.zz.ui.common.MonoText
import io.github.freewebmovement.zz.ui.common.SectionCard
import io.github.freewebmovement.zz.ui.common.StatusText
import io.github.freewebmovement.zz.ui.common.formatAmount
import io.github.freewebmovement.zz.ui.common.rememberFwmcNodeSnapshot
import io.github.freewebmovement.zz.ui.common.shortAddr
import io.github.freewebmovement.zz.ui.theme.CardBg
import io.github.freewebmovement.zz.ui.theme.TextMuted
import io.github.freewebmovement.zz.ui.theme.OnlineGreen
import io.github.freewebmovement.zz.ui.theme.TextPrimary
import io.github.freewebmovement.zz.ui.theme.TextSecondary
import io.github.freewebmovement.zz.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import rs.zz.coin.FwmcApi

private data class SeedRow(
    val address: String,
    val port: Int,
    val protocol: String,
    val active: Boolean,
    val lastSeen: String,
)

private data class ChainRow(
    val address: String,
    val online: Boolean,
    val onlineMinutes: Long,
    val tickCount: Long,
    val weight: String,
    val isCurrent: Boolean,
)

private data class RingMember(val ip: String, val port: Int, val nodeId: String, val active: Boolean)

private data class WitnessData(
    val tickCount: Long = 0,
    val epoch: Long = 0,
    val epochTick: Long = 0,
    val ticksPerEpoch: Long = 0,
    val nextTickSeconds: Long = 0,
    val ringActiveHash: String = "",
    val ringActiveEpoch: Long = 0,
    val ringActiveMembers: List<RingMember> = emptyList(),
    val ringLockedHash: String = "",
    val ringLockedEpoch: Long = 0,
    val ringLockedMembers: List<RingMember> = emptyList(),
    val chain: List<ChainRow> = emptyList(),
    val tickRecords: List<TickRecord> = emptyList(),
    val tickMax: Long = 0,
    val tickRings: List<TickRing> = emptyList(),
)

private data class TickRecord(
    val address: String,
    val tickCount: Long,
    val isFullTime: Boolean,
)

/** 每个 tick 保存的锁定环快照。 */
private data class TickRing(
    val tickIndex: Long,
    val ringHash: String,
    val members: List<String>,
)

/** 子页面通用头部：返回箭头 + 标题。 */
@Composable
private fun SubPageHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(CardBg)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
        }
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

/**
 * 网络 tab = 公共网络信息：链状态 / Peer 节点 / 见证环 / 区块浏览。
 * 本机服务器相关入口见「我的」页。
 */
@Composable
fun PeerContent() {
    val node = rememberFwmcNodeSnapshot()
    val running = node.running

    var myAddress by remember { mutableStateOf("") }
    var witness by remember { mutableStateOf(WitnessData()) }
    var seeds by remember { mutableStateOf<List<SeedRow>>(emptyList()) }
    var errorMsg by remember { mutableStateOf("") }
    var showTickList by remember { mutableStateOf(false) }
    var showRingDetail by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(running) {
        while (running) {
            runCatching {
                val obj = JSONObject(FwmcApi.getData())
                if (!obj.optBoolean("success", false)) {
                    errorMsg = obj.optString("error", "加载失败")
                    return@runCatching
                }
                errorMsg = ""
                myAddress = obj.optString("my_address")
                witness = parseWitness(obj)
                seeds = obj.optJSONArray("seeds")?.let { arr ->
                    (0 until arr.length()).map { i ->
                        val o = arr.getJSONObject(i)
                        SeedRow(
                            address = o.optString("address"),
                            port = o.optInt("port", 0),
                            protocol = o.optString("protocol"),
                            active = o.optBoolean("is_active", false),
                            lastSeen = o.optString("last_seen"),
                        )
                    }
                } ?: emptyList()
            }
            delay(5000)
        }
    }

    if (showTickList) {
        TickRingListScreen(witness = witness, onBack = { showTickList = false })
        return
    }
    showRingDetail?.let { which ->
        RingDetailScreen(which = which, witness = witness, onBack = { showRingDetail = null })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(io.github.freewebmovement.zz.ui.theme.WxBg)
            .verticalScroll(rememberScrollState()),
    ) {
        when {
            !running -> SectionCard { Text("节点未运行。可在「我的 → 服务器」中启动。", color = TextSecondary) }
            errorMsg.isNotEmpty() -> SectionCard { Text(errorMsg, color = MaterialTheme.colorScheme.error) }
            else -> {
                // ① 纪元（含纪元状态、见证环、Tick 列表入口）
                SectionHeader("纪元")
                EpochCard(
                    witness,
                    onOpenTickList = { showTickList = true },
                    onOpenRingMembers = { which -> showRingDetail = which },
                )

                // ② 区块链信息
                SectionHeader("区块链信息")
                GenesisCard()
                ExplorerCard(myAddress)

                // ③ 种子服务器
                SectionHeader("种子服务器")
                SeedsCard(seeds, onChanged = { /* refreshed on next poll */ })
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

// ============================================================
//  我的 · 服务器子页（节点控制 + 余额/地址）
// ============================================================

/** 「我的 → 服务器」：节点控制 + 网络信息 + 位置 + 服务列表。 */
@Composable
fun ServerScreen(onBack: () -> Unit = {}) {
    val app = MainApplication.getApp()
    val ctx = LocalContext.current
    val node = rememberFwmcNodeSnapshot()
    val running = node.running

    var publicIps by remember { mutableStateOf<List<String>>(emptyList()) }
    var privateIps by remember { mutableStateOf<List<String>>(emptyList()) }
    var locationEnabled by remember { mutableStateOf(app.settings.network.locationEnabled) }
    var locationText by remember { mutableStateOf("") }

    LaunchedEffect(running) {
        while (running) {
            try {
                val raw = FwmcApi.getWeights()
                val obj = JSONObject(raw)
                if (obj.optBoolean("success", false)) {
                    publicIps = toStringList(obj.optJSONArray("external_ips"))
                    privateIps = toStringList(obj.optJSONArray("inner_ips"))
                }
                // JNI get_all_ips 在 Android 上可能返回空（ip addr show 无权限），回退到 Java NetworkInterface
                if (privateIps.isEmpty()) {
                    try {
                        val ips = java.net.NetworkInterface.getNetworkInterfaces()?.toList()
                            ?.filter { !it.isLoopback && it.isUp && it.inetAddresses.hasMoreElements() }
                            ?.flatMap { it.inetAddresses.toList() }
                            ?.filter { it is java.net.Inet4Address && !it.isLoopbackAddress }
                            ?.map { it.hostAddress ?: "" }
                            ?.filter { it.isNotEmpty() }
                            ?: emptyList()
                        if (ips.isNotEmpty()) {
                            privateIps = ips.map { it }
                        }
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                android.util.Log.w("ServerScreen", "getWeights failed", e)
            }
            try {
                val obj = JSONObject(FwmcApi.getData())
                if (obj.optBoolean("success", false)) {
                    locationText = obj.optString("location", "")
                }
            } catch (e: Exception) {
                android.util.Log.w("ServerScreen", "getData failed", e)
            }
            delay(5000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(io.github.freewebmovement.zz.ui.theme.WxBg)
            .verticalScroll(rememberScrollState()),
    ) {
        SubPageHeader("服务器", onBack)

        // 节点控制（启动/停止/端口）
        ServerControlCard(running = running, port = node.port, address = "", privateIps = privateIps)

        if (running) {
            // 网络信息
            SectionCard(title = "网络信息") {
                InfoGrid(listOf("运行端口" to "${node.port}"))
                Divider()
                Text("公网 IP", fontSize = 12.sp, color = TextSecondary)
                if (publicIps.isNotEmpty()) {
                    publicIps.forEach { ip ->
                        MonoText(text = ip.split(":").first(), fontSize = 11, color = TextPrimary)
                    }
                } else {
                    Text("  无", fontSize = 12.sp, color = TextMuted)
                }
                Divider()
                Text("内网 IP", fontSize = 12.sp, color = TextSecondary)
                if (privateIps.isNotEmpty()) {
                    privateIps.forEach { ip ->
                        MonoText(text = ip.split(":").first(), fontSize = 11, color = TextPrimary)
                    }
                } else {
                    Text("  无", fontSize = 12.sp, color = TextMuted)
                }
            }

            // 位置信息
            SectionCard(title = "位置信息") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("位置服务", fontSize = 14.sp, color = TextPrimary)
                        Text(
                            text = if (locationEnabled) "已开启 · 用于节点定位" else "已关闭",
                            fontSize = 11.sp,
                            color = TextMuted,
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = locationEnabled,
                        onCheckedChange = {
                            locationEnabled = it
                            app.settings.network.locationEnabled = it
                        },
                    )
                }
                if (locationEnabled && locationText.isNotEmpty()) {
                    Divider()
                    Text(locationText, fontSize = 12.sp, color = TextSecondary)
                }
            }

            // 提供的服务
            SectionCard(title = "提供的服务") {
                ServiceRow("Web 静态服务器", app.settings.network.staticFileEnabled)
                Divider()
                ServiceRow("HTTP Proxy", false)
                Divider()
                ServiceRow("SOCKS Proxy", false)
                Divider()
                ServiceRow("FRP 服务", false)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ServiceRow(name: String, enabled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(name, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        StatusText(
            active = enabled,
            activeLabel = "运行中",
            inactiveLabel = "未启用",
        )
    }
}

// ============================================================
//  我的 · 静态服务器配置子页
// ============================================================

/** 「我的 → 静态服务器配置」：启用开关 + 根目录选择。 */
@Composable
fun StaticFileScreen(onBack: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(io.github.freewebmovement.zz.ui.theme.WxBg)
            .verticalScroll(rememberScrollState()),
    ) {
        SubPageHeader("静态服务器配置", onBack)
        StaticFileCard()
        Spacer(modifier = Modifier.height(12.dp))
    }
}

/** 静态文件服务器设置：与节点共用同一端口；默认目录 = fwmc 数据目录/www。 */
@Composable
private fun StaticFileCard() {
    val app = MainApplication.getApp()
    val ctx = LocalContext.current
    var enabled by remember { mutableStateOf(app.settings.network.staticFileEnabled) }
    var rootDir by remember {
        mutableStateOf(app.settings.network.staticFileRoot.ifEmpty { defaultWwwDir(ctx) })
    }
    var showTip by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            ctx.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        val path = treeUriToPath(uri)
        if (path != null) {
            rootDir = path
            app.settings.network.staticFileRoot = path
            showTip = "已选择: $path"
        } else {
            showTip = "该存储暂不支持，请选择内部存储目录"
        }
    }

    SectionCard(title = "静态文件服务器") {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("启用", fontSize = 14.sp, color = TextPrimary)
                Text(
                    text = "与节点共用端口（单端口统一服务）",
                    fontSize = 11.sp,
                    color = TextMuted,
                )
            }
            androidx.compose.material3.Switch(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    app.settings.network.staticFileEnabled = it
                    if (it && rootDir.isEmpty()) {
                        rootDir = defaultWwwDir(ctx)
                        app.settings.network.staticFileRoot = rootDir
                    }
                },
            )
        }
        Divider()
        Row(verticalAlignment = Alignment.Top) {
            Text("目录 ", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
            SelectionContainer {
                MonoText(
                    text = rootDir.ifEmpty { defaultWwwDir(ctx) },
                    fontSize = 10,
                    color = TextSecondary,
                    maxLines = 3,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
        Row(modifier = Modifier.padding(top = 6.dp)) {
            Text(
                text = "选择目录",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { picker.launch(null) }.padding(end = 18.dp),
            )
            Text(
                text = "恢复默认目录",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    rootDir = defaultWwwDir(ctx)
                    app.settings.network.staticFileRoot = rootDir
                    showTip = "已恢复默认目录"
                },
            )
        }
        if (showTip.isNotEmpty()) {
            Text(showTip, fontSize = 10.sp, color = TextMuted, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** 默认静态目录：fwmc 数据目录的 www 子目录（自动创建）。 */
private fun defaultWwwDir(ctx: android.content.Context): String {
    val d = java.io.File(ctx.filesDir, "fwmc/www")
    d.mkdirs()
    return d.absolutePath
}

/** SAF tree URI → 真实文件路径（仅支持内部存储 primary）。 */
private fun treeUriToPath(uri: android.net.Uri): String? {
    val docId = runCatching {
        android.provider.DocumentsContract.getTreeDocumentId(uri)
    }.getOrNull() ?: return null
    if (!docId.startsWith("primary:")) return null
    val rel = docId.removePrefix("primary:")
    val base = android.os.Environment.getExternalStorageDirectory().absolutePath
    return if (rel.isEmpty()) base else "$base/$rel"
}

// ============================================================
//  我的 · 资源与权重配置子页
// ============================================================

/** 「我的 → 资源与权重配置」：手机硬件资源分配 + IP 权重。 */
@Composable
fun WeightsScreen(onBack: () -> Unit = {}) {
    val app = MainApplication.getApp()
    val ctx = LocalContext.current

    // 设备真实硬件数据
    var totalDisk by remember { mutableLongStateOf(0L) }
    var availDisk by remember { mutableLongStateOf(0L) }
    var cpuCores by remember { mutableIntStateOf(0) }
    var cpuFreq by remember { mutableStateOf("") }
    var totalMem by remember { mutableLongStateOf(0L) }
    var availMem by remember { mutableLongStateOf(0L) }

    // 用户分配量（持久化到 KVSettings）
    var diskAlloc by remember { mutableIntStateOf(app.settings.network.diskAllocation) }
    var cpuAlloc by remember { mutableIntStateOf(app.settings.network.cpuAllocation) }
    var memAlloc by remember { mutableIntStateOf(app.settings.network.memAllocation) }
    var gpuAlloc by remember { mutableIntStateOf(app.settings.network.gpuAllocation) }
    var bleAlloc by remember { mutableIntStateOf(app.settings.network.bleAllocation) }
    var wifiAlloc by remember { mutableIntStateOf(app.settings.network.wifiAllocation) }

    var ipData by remember { mutableStateOf<JSONObject?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            // 磁盘
            val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
            totalDisk = stat.totalBytes / (1024 * 1024)
            availDisk = stat.availableBytes / (1024 * 1024)
            // CPU
            cpuCores = Runtime.getRuntime().availableProcessors()
            cpuFreq = runCatching {
                java.io.File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
                    .readText().trim().toLongOrNull()?.let { "${it / 1000} MHz" } ?: "未知"
            }.getOrDefault("未知")
            // 内存
            val mi = android.os.Debug.MemoryInfo()
            android.os.Debug.getMemoryInfo(mi)
            val r = Runtime.getRuntime()
            totalMem = r.maxMemory() / (1024 * 1024)
            availMem = r.freeMemory() / (1024 * 1024)
        }
        runCatching {
            val obj = JSONObject(FwmcApi.getWeights())
            if (obj.optBoolean("success", false)) {
                ipData = obj
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(io.github.freewebmovement.zz.ui.theme.WxBg)
            .verticalScroll(rememberScrollState()),
    ) {
        SubPageHeader("资源与权重配置", onBack)

        // 节点权重总览
        val wr = ipData?.optJSONObject("resources")
        SectionCard(title = "节点权重") {
            if (wr == null) {
                EmptyHint("暂无数据")
            } else {
                val totalWeight = wr.optString("composite_weight", "0")
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("总权重", fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                    Text(totalWeight, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Divider()
                WeightDetailRow("公网 IPv4", wr.optString("public_ip_count", "0"), wr.optString("public_ip_weight", "0"))
                WeightDetailRow("私网 IP", wr.optString("private_ip_count", "0"), wr.optString("private_ip_weight", "0"))
                WeightDetailRow("公网 IPv6", wr.optString("public_ipv6_count", "0"), wr.optString("public_ipv6_weight", "0"))
                WeightDetailRow("存储", "${wr.optDouble("storage_tb", 0.0)} TB", wr.optString("storage_weight", "0"))
                WeightDetailRow("带宽", "${wr.optDouble("bandwidth_gbps", 0.0)} Gbps", wr.optString("bandwidth_weight", "0"))
                val cpuTicks = wr.optLong("cpu_time_ticks", 0)
                val cpuMin = cpuTicks / 6000
                WeightDetailRow("CPU 时间", "$cpuMin 分钟", wr.optString("cpu_weight", "0"))
                val memKb = wr.optLong("memory_kb", 0)
                WeightDetailRow("内存", "${memKb / 1024} MB", wr.optString("memory_weight", "0"))
                WeightDetailRow("API", "${wr.optLong("api_requests", 0)} 次/天", wr.optString("api_weight", "0"))
                Divider()
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("见证资格", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                    StatusText(
                        active = wr.optBoolean("witness_participation", false),
                        activeLabel = "有资格",
                        inactiveLabel = "无公网出口",
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 磁盘
        SectionCard(title = "磁盘") {
            InfoGrid(listOf(
                "总容量" to "$totalDisk MB",
                "可用" to "$availDisk MB",
            ))
            Divider()
            EditableResourceRow(
                label = "分配容量",
                value = diskAlloc.toLong(),
                unit = "MB",
                maxValue = totalDisk,
                onValueChanged = {
                    diskAlloc = it.toInt()
                    app.settings.network.diskAllocation = it.toInt()
                },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // CPU
        SectionCard(title = "CPU") {
            InfoGrid(listOf(
                "核心数" to "$cpuCores 核心",
                "频率" to cpuFreq,
            ))
            Divider()
            EditableResourceRow(
                label = "分配核心",
                value = cpuAlloc.toLong(),
                unit = "核",
                maxValue = cpuCores.toLong(),
                onValueChanged = {
                    cpuAlloc = it.toInt()
                    app.settings.network.cpuAllocation = it.toInt()
                },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 内存
        SectionCard(title = "内存") {
            InfoGrid(listOf(
                "总容量" to "$totalMem MB",
                "可用" to "$availMem MB",
            ))
            Divider()
            EditableResourceRow(
                label = "分配容量",
                value = memAlloc.toLong(),
                unit = "MB",
                maxValue = totalMem,
                onValueChanged = {
                    memAlloc = it.toInt()
                    app.settings.network.memAllocation = it.toInt()
                },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // GPU
        SectionCard(title = "GPU") {
            val gpuName = remember {
                runCatching {
                    val pm = ctx.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
                    val gl = pm.isPowerSaveMode // placeholder — real GPU name needs GL surface
                    "图形处理器"
                }.getOrDefault("图形处理器")
            }
            InfoGrid(listOf("能力" to "图形计算 / 渲染"))
            Divider()
            EditableResourceRow(
                label = "分配",
                value = gpuAlloc.toLong(),
                unit = "核",
                maxValue = 16,
                onValueChanged = {
                    gpuAlloc = it.toInt()
                    app.settings.network.gpuAllocation = it.toInt()
                },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 蓝牙
        SectionCard(title = "蓝牙") {
            val bleSupported = remember {
                runCatching { ctx.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE) }.getOrDefault(false)
            }
            val bleEnabled = remember {
                runCatching {
                    val bm = ctx.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
                    bm?.adapter?.isEnabled == true
                }.getOrDefault(false)
            }
            InfoGrid(listOf(
                "BLE 支持" to if (bleSupported) "是" else "否",
                "当前状态" to if (bleEnabled) "已开启" else "已关闭",
            ))
            Divider()
            EditableResourceRow(
                label = "分配通道",
                value = bleAlloc.toLong(),
                unit = "个",
                maxValue = 8,
                onValueChanged = {
                    bleAlloc = it.toInt()
                    app.settings.network.bleAllocation = it.toInt()
                },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // WiFi
        SectionCard(title = "WiFi") {
            val wifiInfo = remember {
                runCatching {
                    val wm = ctx.applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
                    val info = wm?.connectionInfo
                    val ssid = info?.ssid?.removePrefix("\"")?.removeSuffix("\"") ?: "未连接"
                    val speed = info?.linkSpeed ?: 0
                    Pair(ssid, speed)
                }.getOrDefault(Pair("未知", 0))
            }
            InfoGrid(listOf(
                "网络" to wifiInfo.first,
                "速率" to if (wifiInfo.second > 0) "${wifiInfo.second} Mbps" else "未知",
            ))
            Divider()
            EditableResourceRow(
                label = "分配带宽",
                value = wifiAlloc.toLong(),
                unit = "Mbps",
                maxValue = 1000,
                onValueChanged = {
                    wifiAlloc = it.toInt()
                    app.settings.network.wifiAllocation = it.toInt()
                },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // IP 资源
        val r = ipData?.optJSONObject("resources")
        SectionCard(title = "IP 资源") {
            if (r == null) {
                EmptyHint("暂无数据")
            } else {
                val publicCount = r.optLong("public_ip_count")
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("公网", fontSize = 13.sp, color = TextPrimary, modifier = Modifier.width(64.dp))
                    Text(
                        text = if (publicCount > 0) "有（$publicCount 个）" else "无",
                        fontSize = 12.sp,
                        color = if (publicCount > 0) OnlineGreen else TextMuted,
                        modifier = Modifier.weight(1f),
                    )
                    Text("权重 ${r.optString("public_ip_weight")}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
                Divider()
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("私网", fontSize = 13.sp, color = TextPrimary, modifier = Modifier.width(64.dp))
                    Text(
                        text = if (r.optLong("private_ip_count") > 0) "有" else "无",
                        fontSize = 12.sp,
                        color = if (r.optLong("private_ip_count") > 0) OnlineGreen else TextMuted,
                        modifier = Modifier.weight(1f),
                    )
                    Text("权重 ${r.optString("private_ip_weight")}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
                Divider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("见证资格", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                    StatusText(
                        active = r.optBoolean("witness_participation", false),
                        activeLabel = "有资格",
                        inactiveLabel = "无公网出口",
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun EditableResourceRow(
    label: String,
    value: Long,
    unit: String,
    maxValue: Long,
    onValueChanged: (Long) -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf(value.toString()) }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(64.dp))
        if (editing) {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it.filter { c -> c.isDigit() } },
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                singleLine = true,
            )
            Text(unit, fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(start = 4.dp))
            Text(
                text = "保存",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable {
                        val v = textValue.toLongOrNull()?.coerceIn(0, maxValue) ?: 0
                        onValueChanged(v)
                        editing = false
                    },
            )
        } else {
            Text(
                text = "$value $unit",
                fontSize = 13.sp,
                color = TextPrimary,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        textValue = value.toString()
                        editing = true
                    },
            )
            Text("编辑", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    textValue = value.toString()
                    editing = true
                })
        }
    }
}

@Composable
private fun WeightDetailRow(label: String, value: String, weight: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 2.dp)) {
        Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.width(80.dp))
        Text(value, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Text("权重 $weight", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
    }
}

// ============================================================
//  服务器管理（启动/停止/端口/Web 分享）
// ============================================================

@Composable
private fun ServerControlCard(running: Boolean, port: Int, address: String, privateIps: List<String> = emptyList()) {
    val app = MainApplication.getApp()
    val context = LocalContext.current
    var bump by remember { mutableIntStateOf(0) }
    var showPortEditor by remember { mutableStateOf(false) }

    SectionCard(title = "服务器管理") {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            StatusText(active = running, activeLabel = "运行中", inactiveLabel = "未运行")
            Text(
                text = if (running && port > 0) "  端口 $port" else "",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    if (running) io.github.freewebmovement.android.noui.FwmcService.stop(context)
                    else io.github.freewebmovement.android.noui.FwmcService.start(context)
                    bump++
                },
                colors = if (running) {
                    ButtonDefaults.buttonColors(containerColor = Color(0xFFE64340))
                } else {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                },
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(if (running) "停止节点" else "启动节点")
            }
        }
        if (address.isNotEmpty()) {
            Divider()
            Row(verticalAlignment = Alignment.Top) {
                Text("地址 ", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
                SelectionContainer {
                    MonoText(
                        text = address,
                        fontSize = 11,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                val clip = androidx.compose.ui.platform.LocalClipboardManager.current
                val ctx = LocalContext.current
                Icon(
                    painter = painterResource(id = io.github.freewebmovement.zz.R.drawable.ic_copy),
                    contentDescription = "copy",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(18.dp)
                        .clickable {
                            clip.setText(androidx.compose.ui.text.AnnotatedString(address))
                            android.widget.Toast.makeText(ctx, ctx.getString(io.github.freewebmovement.zz.R.string.copied), android.widget.Toast.LENGTH_SHORT).show()
                        },
                )
            }
        }
        Divider()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { showPortEditor = true },
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("服务端口", fontSize = 14.sp, color = TextPrimary)
                Text(
                    text = "当前 ${if (port > 0) port else app.settings.network.port}，点击修改并重启",
                    fontSize = 11.sp,
                    color = TextMuted,
                )
            }
            Text("修改 ›", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
        }
        Divider()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Web 服务", fontSize = 14.sp, color = TextPrimary)
                val lanIp = privateIps.firstOrNull()?.split(":")?.firstOrNull() ?: ""
                Text(
                    text = if (running && port > 0 && lanIp.isNotEmpty()) "http://$lanIp:$port/ 局域网可访问" else if (running && port > 0) "http://<本机IP>:$port/" else "节点启动后提供 Web UI",
                    fontSize = 11.sp,
                    color = TextMuted,
                )
            }
            if (running && port > 0) {
                Row {
                    Text(
                        text = "浏览器",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            val lanIp = privateIps.firstOrNull()?.split(":")?.firstOrNull() ?: ""
                            if (lanIp.isNotEmpty()) {
                                val uri = android.net.Uri.parse("http://$lanIp:$port/")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }
                        }.padding(6.dp),
                    )
                    Text(
                        text = "分享",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            val lanIp = privateIps.firstOrNull()?.split(":")?.firstOrNull() ?: ""
                            val url = if (lanIp.isNotEmpty()) "http://$lanIp:$port/" else "http://:${port}/"
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, url)
                            }
                            context.startActivity(Intent.createChooser(send, "分享 fwmc Web UI"))
                        }.padding(6.dp),
                    )
                }
            }
        }
    }

    if (showPortEditor) {
        PortEditDialog(
            current = if (port > 0) port.toString() else app.settings.network.port.toString(),
            onDismiss = { showPortEditor = false },
            onApply = { newPort ->
                showPortEditor = false
                app.settings.network.port = newPort
                val ctx = io.github.freewebmovement.android.noui.MyApp.getContext()
                io.github.freewebmovement.android.noui.FwmcService.stop(ctx)
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    kotlinx.coroutines.delay(500)
                    io.github.freewebmovement.android.noui.FwmcService.start(ctx)
                }
                bump++
            },
        )
    }
}

@Composable
private fun PortEditDialog(current: String, onDismiss: () -> Unit, onApply: (Int) -> Unit) {
    var portText by remember { mutableStateOf(current) }
    // idle=无效输入 checking=检测中 free=可用 busy=被占用 self=未变更
    var state by remember { mutableStateOf("idle") }

    LaunchedEffect(portText) {
        val p = portText.toIntOrNull()
        when {
            p == null || p <= 1024 || p > 65535 || p.toString() != portText -> state = "idle"
            current.toIntOrNull() != null && p == current.toIntOrNull() -> state = "self"
            else -> {
                state = "checking"
                delay(400)
                val free = runCatching {
                    org.json.JSONObject(rs.zz.coin.FwmcApi.checkPort(p)).optBoolean("success", false)
                }.getOrDefault(false)
                state = if (free) "free" else "busy"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改服务端口") },
        text = {
            Column {
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text("端口 (1025-65535)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                val hint = when (state) {
                    "checking" -> "正在检测端口占用…"
                    "free" -> "✓ 端口可用"
                    "busy" -> "✗ 端口已被占用，请更换"
                    "self" -> "当前运行中的端口（未变更）"
                    else -> ""
                }
                if (hint.isNotEmpty()) {
                    Text(
                        hint,
                        fontSize = 12.sp,
                        color = when (state) {
                            "free" -> Color(0xFF2E7D32)
                            "busy" -> Color(0xFFC62828)
                            else -> TextSecondary
                        },
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Text(
                    text = "恢复默认端口 ${io.github.freewebmovement.peer.system.NetworkSetting.DEFAULT_SERVER_PORT}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable {
                            portText = io.github.freewebmovement.peer.system.NetworkSetting.DEFAULT_SERVER_PORT.toString()
                        },
                )
            }
        },
        confirmButton = {
            val enabled = state == "free" || state == "self"
            TextButton(
                onClick = {
                    val p = portText.toIntOrNull() ?: return@TextButton
                    if ((p > 1024 && p < 65536) && enabled) onApply(p)
                },
                enabled = enabled,
            ) {
                Text("保存并重启", color = if (enabled) MaterialTheme.colorScheme.primary else TextSecondary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

// ============================================================
//  状态 / 连接 / 见证 / 节点 / 种子 / 权重
// ============================================================

@Composable
private fun GenesisCard() {
    var genesis by remember { mutableStateOf<JSONObject?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch {
            runCatching {
                val raw = FwmcApi.getGenesis()
                val obj = JSONObject(raw)
                if (obj.optBoolean("success", false)) genesis = obj
            }
        }
    }
    SectionCard(title = "创世纪") {
        val g = genesis
        if (g == null) {
            EmptyHint("加载中…")
        } else {
            InfoGrid(
                listOf(
                    "币种" to g.optString("coin_symbol", ""),
                    "总发行量" to formatAmount(g.optLong("total_supply", 0)),
                    "创世分配" to "${g.optString("genesis_dev_ratio", "")}（开发者）",
                    "释放周期" to "${g.optLong("release_years", 0)} 年",
                )
            )
            Divider()
            Text("初始分配", fontSize = 12.sp, color = TextSecondary)
            val allocs = g.optJSONArray("allocations")
            if (allocs != null) {
                (0 until allocs.length()).forEach { i ->
                    val a = allocs.getJSONObject(i)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    ) {
                        MonoText(text = a.optString("address"), fontSize = 10, color = TextPrimary, maxLines = 1, modifier = Modifier.weight(1f))
                        MonoText(text = "${a.optString("percentage")}%", fontSize = 10, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
/** 纪元卡片：嵌套 纪元状态 / 见证环 / Tick 列表入口（均为当前纪元的衍生物）。 */
private fun EpochCard(
    w: WitnessData,
    onOpenTickList: () -> Unit,
    onOpenRingMembers: (which: String) -> Unit,
) {
    SectionCard(title = "纪元 ${w.epoch}") {
        // 纪元进度
        val epochPct = if (w.ticksPerEpoch > 0) w.epochTick.toFloat() / w.ticksPerEpoch else 0f
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text("当前 epoch (${w.epochTick}/${w.ticksPerEpoch})", fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { epochPct },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }

        // Tick 状态（当前 tick 内的进度）
        val tickInterval = 900L // 15 min default
        val secondsIntoTick = tickInterval - w.nextTickSeconds.coerceIn(0, tickInterval)
        val tickMinutes = secondsIntoTick / 60
        val tickPct = if (tickInterval > 0) secondsIntoTick.toFloat() / tickInterval else 0f
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text("当前 tick 状态 (${tickMinutes}/15)", fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { tickPct },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }

        Divider()

        // 见证环（当前 tick 的节点组合）——成员多，入口进入子页查看完整列表
        Text("见证环", fontSize = 12.sp, color = TextSecondary)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { onOpenRingMembers("active") })
                .padding(vertical = 4.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("活跃环", fontSize = 11.sp, color = TextSecondary)
                SelectionContainer {
                    MonoText(
                        text = w.ringActiveHash.ifEmpty { "-" },
                        fontSize = 10,
                        color = TextSecondary,
                        maxLines = 1,
                    )
                }
                Text("成员 ${w.ringActiveMembers.size} 个", fontSize = 10.sp, color = TextMuted)
            }
            Text("查看  ›", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
        }
        if (w.ringLockedHash.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { onOpenRingMembers("locked") })
                    .padding(vertical = 4.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("锁定环（纪元 ${w.ringLockedEpoch}）", fontSize = 11.sp, color = TextSecondary)
                    SelectionContainer {
                        MonoText(text = w.ringLockedHash, fontSize = 10, color = TextMuted, maxLines = 1)
                    }
                    Text("成员 ${w.ringLockedMembers.size} 个", fontSize = 10.sp, color = TextMuted)
                }
                Text("查看  ›", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
        }

        if (w.chain.isNotEmpty()) {
            Divider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { onOpenRingMembers("chain") })
                    .padding(vertical = 4.dp),
            ) {
                Text("见证链 (${w.chain.size})", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                Text("查看  ›", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
        }

        Divider()

        // Tick 列表入口（完整列表在子页显示，因为一天数据量大且每个环有很多地址）
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenTickList)
                .padding(vertical = 4.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Tick 列表", fontSize = 12.sp, color = TextSecondary)
                Text(
                    text = "当前 ${w.epochTick}/${w.ticksPerEpoch} · 已记录 ${w.tickRings.size} 个环",
                    fontSize = 10.sp,
                    color = TextMuted,
                )
            }
            Text("查看全部  ›", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
        }
        if (w.tickRings.isNotEmpty()) {
            val latest = w.tickRings.last()
            Text(
                text = "最新 #${latest.tickIndex} · ${latest.members.size} 个成员",
                fontSize = 10.sp,
                color = TextMuted,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

/** Tick 列表子页：显示当前纪元所有 tick 入口，点击进入该 tick 的成员列表。 */
@Composable
private fun TickRingListScreen(witness: WitnessData, onBack: () -> Unit) {
    var selectedTick by remember { mutableStateOf<TickRing?>(null) }

    if (selectedTick != null) {
        TickMemberScreen(tick = selectedTick!!, onBack = { selectedTick = null })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(io.github.freewebmovement.zz.ui.theme.WxBg)
            .verticalScroll(rememberScrollState()),
    ) {
        SubPageHeader(title = "Tick 列表", onBack = onBack)

        if (witness.tickRings.isEmpty()) {
            SectionCard { Text("暂无 Tick 记录", fontSize = 13.sp, color = TextSecondary) }
        } else {
            SectionCard(title = "Tick 记录 (${witness.tickRings.size})") {
                witness.tickRings.forEach { tr ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTick = tr }
                            .padding(vertical = 6.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tick #${tr.tickIndex}", fontSize = 13.sp, color = TextPrimary)
                            Text("成员 ${tr.members.size} 个", fontSize = 10.sp, color = TextMuted)
                        }
                        Text("查看  ›", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** 单个 tick 的成员列表子页（复用统一成员列表模板）。 */
@Composable
private fun TickMemberScreen(tick: TickRing, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(io.github.freewebmovement.zz.ui.theme.WxBg)
            .verticalScroll(rememberScrollState()),
    ) {
        SubPageHeader(title = "Tick #${tick.tickIndex}", onBack = onBack)
        MemberListCard(
            title = "锁定环成员 · ${tick.members.size} 个",
            items = tick.members.map { m -> MemberItem(label = "", address = m) },
            emptyHint = "（空）",
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** 可复制的地址行：点击复制完整地址。 */
@Composable
private fun CopyableAddress(address: String) {
    val ctx = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("address", address))
                android.widget.Toast.makeText(ctx, "已复制", android.widget.Toast.LENGTH_SHORT).show()
            }
            .padding(vertical = 4.dp),
    ) {
        SelectionContainer {
            MonoText(text = address, fontSize = 11, color = TextPrimary, maxLines = 1)
        }
        Spacer(modifier = Modifier.weight(1f))
        Text("复制", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
    }
}

/** 环成员子页：活跃环 / 锁定环 / 见证链 共用统一的成员列表模板。 */
@Composable
private fun RingDetailScreen(which: String, witness: WitnessData, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(io.github.freewebmovement.zz.ui.theme.WxBg)
            .verticalScroll(rememberScrollState()),
    ) {
        val title = when (which) {
            "active" -> "活跃环成员"
            "locked" -> "锁定环成员"
            "chain" -> "见证链"
            else -> "成员"
        }
        SubPageHeader(title = title, onBack = onBack)

        val items = when (which) {
            "active" -> witness.ringActiveMembers.map { m ->
                MemberItem(label = "${m.ip}:${m.port}", address = m.nodeId)
            }
            "locked" -> witness.ringLockedMembers.map { m ->
                MemberItem(label = "${m.ip}:${m.port}", address = m.nodeId)
            }
            "chain" -> witness.chain.map { c ->
                MemberItem(
                    label = c.address,
                    address = c.address,
                    subtitle = "在线 ${c.onlineMinutes} 分钟 · Tick ${c.tickCount} · 权重 ${c.weight}",
                )
            }
            else -> emptyList()
        }
        MemberListCard(
            title = "$title · ${items.size} 个",
            items = items,
            emptyHint = "（空）",
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** 成员条目：标签 + 可复制地址 + 可选副标题。 */
private data class MemberItem(
    val label: String,
    val address: String,
    val subtitle: String = "",
)

/** 统一的成员列表模板：每个条目显示 标签 + 可复制地址 + 可选副标题。 */
@Composable
private fun MemberListCard(
    title: String,
    items: List<MemberItem>,
    emptyHint: String = "（空）",
) {
    SectionCard(title = title) {
        if (items.isEmpty()) {
            Text(emptyHint, fontSize = 12.sp, color = TextMuted)
        } else {
            items.forEach { item ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    if (item.label.isNotEmpty()) {
                        Text(item.label, fontSize = 11.sp, color = TextPrimary)
                    }
                    CopyableAddress(address = item.address)
                    if (item.subtitle.isNotEmpty()) {
                        Text(
                            text = item.subtitle,
                            fontSize = 10.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeedsCard(seeds: List<SeedRow>, onChanged: () -> Unit) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<SeedRow?>(null) }

    SectionCard(
        title = "种子服务器 (${seeds.size})",
        trailing = {
            Text(
                text = "+ 添加",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { showAdd = true }.padding(4.dp),
            )
        },
    ) {
        if (seeds.isEmpty()) {
            EmptyHint("暂无种子，点击右上角添加")
        } else {
            seeds.forEach { s ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) {
                    StatusText(active = s.active)
                    Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                        MonoText(
                            text = "${s.address}:${s.port}",
                            fontSize = 12,
                            color = TextPrimary,
                            maxLines = 1,
                        )
                        Text(
                            text = protocolLabel(s.protocol) + if (s.lastSeen.isNotEmpty()) " · 最后活跃 ${s.lastSeen}" else "",
                            fontSize = 10.sp,
                            color = TextMuted,
                        )
                    }
                    Text(
                        text = "删除",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier
                            .clickable { deleteTarget = s }
                            .padding(start = 8.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
                    )
                }
            }
        }
    }

    if (showAdd) {
        SeedAddDialog(
            onDismiss = { showAdd = false },
            onConfirm = { ip, port ->
                showAdd = false
                scope.launch { FwmcApi.addSeed(ip, port); onChanged() }
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除种子") },
            text = { Text("确定删除 ${target.address}:${target.port} ？") },
            confirmButton = {
                TextButton(onClick = {
                    val t = target
                    deleteTarget = null
                    scope.launch { FwmcApi.deleteSeed(t.address, t.port); onChanged() }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun SeedAddDialog(onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加种子服务器") },
        text = {
            Column {
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it.trim() },
                    label = { Text("IP 地址") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text("端口") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val p = port.toIntOrNull() ?: return@TextButton
                if (ip.isNotBlank() && p > 0) onConfirm(ip, p)
            }) { Text("确定", color = MaterialTheme.colorScheme.primary) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun StatusDotBig(active: Boolean) {
    Surface(shape = RoundedCornerShape(4.dp), color = if (active) OnlineGreen.copy(alpha = 0.15f) else Color(0xFFE0E0E0)) {
        Text(
            text = if (active) "在线" else "离线",
            fontSize = 9.sp,
            color = if (active) OnlineGreen else TextMuted,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun WeightsCard() {
    var resourcesJson by remember { mutableStateOf<JSONObject?>(null) }
    var innerIps by remember { mutableStateOf<List<String>>(emptyList()) }
    var externalIps by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        runCatching {
            val obj = JSONObject(FwmcApi.getWeights())
            if (obj.optBoolean("success", false)) {
                resourcesJson = obj.optJSONObject("resources")
                innerIps = toStringList(obj.optJSONArray("inner_ips"))
                externalIps = toStringList(obj.optJSONArray("external_ips"))
            }
        }
    }

    val r = resourcesJson
    SectionCard(title = "资源权重") {
        if (r == null) {
            EmptyHint("暂无权重数据")
        } else {
            InfoGrid(
                listOf(
                    "公网 IPv4" to r.optString("public_ip_count"),
                    "内网 IPv4" to r.optString("private_ip_count"),
                    "公网 IPv6" to r.optString("public_ipv6_count"),
                )
            )
            Divider()
            InfoGrid(
                listOf(
                    "公网权重" to r.optString("public_ip_weight"),
                    "内网权重" to r.optString("private_ip_weight"),
                    "IPv6 权重" to r.optString("public_ipv6_weight"),
                )
            )
            Divider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "见证资格",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f),
                )
                StatusText(
                    active = r.optBoolean("witness_participation", false),
                    activeLabel = "有资格",
                    inactiveLabel = "无公网出口",
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "综合权重",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = r.optString("witness_composite_weight", "-"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (innerIps.isNotEmpty() || externalIps.isNotEmpty()) {
                Divider()
                Text("本机服务地址", fontSize = 12.sp, color = TextSecondary)
                (externalIps + innerIps).forEach { ip ->
                    MonoText(text = "http://$ip/", fontSize = 10, color = TextSecondary, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 2.dp),
    )
}

/** ④ 区块浏览：地址余额 + 交易记录（getAddressInfo）。 */
@Composable
private fun ExplorerCard(myAddress: String) {
    var query by remember(myAddress) { mutableStateOf(myAddress) }
    var result by remember { mutableStateOf<JSONObject?>(null) }
    var err by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    fun doQuery(addr: String) {
        if (addr.isBlank()) return
        loading = true; err = ""
        scope.launch {
            val r = runCatching { JSONObject(FwmcApi.getAddressInfo(addr.trim())) }
            loading = false
            r.onSuccess { obj ->
                // ok_json 不含 success 字段；err_json 才有 success=false
                if (obj.optBoolean("success", true) && !obj.has("error")) result = obj
                else err = obj.optString("error", "查询失败")
            }.onFailure { err = it.message ?: "查询失败" }
        }
    }

    SectionCard(title = "区块浏览") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("输入地址查询", fontSize = 12.sp) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { doQuery(query) },
                enabled = !loading && query.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(if (loading) "查询中" else "查询")
            }
        }
        if (myAddress.isNotEmpty() && query != myAddress) {
            Text(
                text = "查我的地址",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp).clickable { doQuery(myAddress) },
            )
        }
        if (err.isNotEmpty()) {
            Text(err, fontSize = 11.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp))
        }
        val r = result
        if (r != null) {
            Divider()
            InfoGrid(
                listOf(
                    "地址余额" to formatAmount(r.optLong("balance", 0)),
                    "交易数" to (r.optJSONArray("transactions")?.length() ?: 0).toString(),
                )
            )
            val txs = r.optJSONArray("transactions")
            if (txs != null && txs.length() > 0) {
                Divider()
                Text("交易记录", fontSize = 12.sp, color = TextSecondary)
                (0 until txs.length()).forEach { i ->
                    val t = txs.getJSONObject(i)
                    val amount = t.optString("amount")
                    val amtLong = amount.toLongOrNull() ?: 0
                    val dir = if (t.optString("from_address") == query.trim()) "-" else "+"
                    Column(modifier = Modifier.padding(vertical = 5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "\$dir\${formatAmount(amtLong)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (dir == "+") OnlineGreen else MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f),
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            ) {
                                Text(
                                    t.optString("tx_type", "tx"),
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                )
                            }
                        }
                        Text("自 ${t.optString("from_address")}", fontSize = 9.sp, color = TextMuted, maxLines = 1)
                        Text("至 ${t.optString("to_address")}", fontSize = 9.sp, color = TextMuted, maxLines = 1)
                        val ts = t.optLong("timestamp", 0)
                        if (ts > 0) {
                            Text(
                                text = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                    .format(java.util.Date(ts * 1000)),
                                fontSize = 9.sp,
                                color = TextMuted,
                            )
                        }
                        val hash = t.optString("hash")
                        if (hash.isNotEmpty()) {
                            SelectionContainer {
                                MonoText(text = "hash \${hash.take(24)}…", fontSize = 9, color = TextMuted, maxLines = 1)
                            }
                        }
                    }
                    if (i < txs.length() - 1) Divider()
                }
            } else {
                Divider()
                EmptyHint("该地址暂无交易记录")
            }
        }
    }
}

// ============================================================
//  Parsing helpers
// ============================================================

private fun parseWitness(obj: JSONObject): WitnessData {
    fun members(arr: JSONArray?): List<RingMember> =
        arr?.let { a ->
            (0 until a.length()).map { i ->
                val m = a.getJSONObject(i)
                RingMember(
                    ip = m.optString("ip"),
                    port = m.optInt("port", 0),
                    nodeId = m.optString("node_id"),
                    active = m.optBoolean("is_active", false),
                )
            }
        } ?: emptyList()

    val ra = obj.optJSONObject("witness_ring_active")
    val rl = obj.optJSONObject("witness_ring_locked")
    val tickRecords = obj.optJSONArray("witness_tick_records")?.let { arr ->
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            TickRecord(
                address = o.optString("address"),
                tickCount = o.optLong("tick_count", 0),
                isFullTime = o.optBoolean("is_full_time", false),
            )
        }
    } ?: emptyList()
    val chain = obj.optJSONObject("witness_chain")?.let { objChain ->
        objChain.keys().asSequence().mapNotNull { k ->
            val o = objChain.optJSONObject(k) ?: return@mapNotNull null
            ChainRow(
                address = o.optString("address", k),
                online = o.optBoolean("is_online", false),
                onlineMinutes = o.optLong("online_minutes", 0),
                tickCount = o.optLong("tick_count", 0),
                weight = o.optString("weight"),
                isCurrent = o.optBoolean("is_current", false),
            )
        }.toList()
    } ?: obj.optJSONArray("witness_chain")?.let { arr ->
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ChainRow(
                address = o.optString("address"),
                online = o.optBoolean("is_online", false),
                onlineMinutes = o.optLong("online_minutes", 0),
                tickCount = o.optLong("tick_count", 0),
                weight = o.optString("weight"),
                isCurrent = o.optBoolean("is_current", false),
            )
        }
    } ?: emptyList()

    return WitnessData(
        tickCount = obj.optLong("tick_count", 0),
        epoch = obj.optLong("epoch", 0),
        epochTick = obj.optLong("epoch_tick", 0),
        ticksPerEpoch = obj.optLong("ticks_per_epoch", 0),
        nextTickSeconds = obj.optLong("next_tick_seconds", 0),
        ringActiveHash = ra?.optString("ring_hash") ?: "",
        ringActiveEpoch = ra?.optLong("epoch", 0) ?: 0,
        ringActiveMembers = members(ra?.optJSONArray("members")),
        ringLockedHash = rl?.optString("ring_hash") ?: "",
        ringLockedEpoch = rl?.optLong("epoch", 0) ?: 0,
        ringLockedMembers = members(rl?.optJSONArray("members")),
        chain = chain,
        tickRecords = tickRecords,
        tickMax = obj.optLong("witness_tick_max", 0),
        tickRings = obj.optJSONArray("tick_rings")?.let { arr ->
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                TickRing(
                    tickIndex = o.optLong("tick_index", 0),
                    ringHash = o.optString("ring_hash"),
                    members = o.optJSONArray("members")?.let { mArr ->
                        (0 until mArr.length()).mapNotNull { j -> runCatching { mArr.getString(j) }.getOrNull() }
                    } ?: emptyList(),
                )
            }
        } ?: emptyList(),
    )
}

private fun toStringList(arr: JSONArray?): List<String> {
    arr ?: return emptyList()
    return (0 until arr.length()).mapNotNull { i -> runCatching { arr.getString(i) }.getOrNull() }
}

private fun protocolLabel(p: String): String = when (p) {
    "inner" -> "内网"
    "external" -> "外网"
    "ipv6" -> "IPv6"
    else -> p
}
