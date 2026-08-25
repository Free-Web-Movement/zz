package io.github.freewebmovement.zz.ui.content

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
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

private data class NodeRow(
    val address: String,
    val intranetIps: List<String>,
    val wanIps: List<String>,
    val connected: Boolean,
)

private data class SeedRow(
    val address: String,
    val port: Int,
    val protocol: String,
    val active: Boolean,
    val lastSeen: String,
)

private data class ConnGroup(val nodeId: String, val addrs: String, val passed: String)

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
    val todayTick: Long = 0,
)

@Composable
fun PeerContent() {
    ServerDashboard()
}

/**
 * 网络 tab = 服务器管理 + 网络状态总览（超越 WebUI 的服务器控制能力）。
 */
@Composable
private fun ServerDashboard() {
    val node = rememberFwmcNodeSnapshot()
    val running = node.running

    var myAddress by remember { mutableStateOf("") }
    var balance by remember { mutableLongStateOf(0L) }
    var witness by remember { mutableStateOf(WitnessData()) }
    var inbound by remember { mutableStateOf<List<ConnGroup>>(emptyList()) }
    var outbound by remember { mutableStateOf<List<ConnGroup>>(emptyList()) }
    var nodes by remember { mutableStateOf<List<NodeRow>>(emptyList()) }
    var seeds by remember { mutableStateOf<List<SeedRow>>(emptyList()) }
    var errorMsg by remember { mutableStateOf("") }

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
                balance = obj.optLong("my_balance", 0)
                witness = parseWitness(obj)
                inbound = parseConnGroups(obj.optJSONArray("inbound_connections"))
                outbound = parseConnGroups(obj.optJSONArray("outbound_connections"))
                nodes = obj.optJSONArray("nodes")?.let { arr ->
                    (0 until arr.length()).map { i ->
                        val o = arr.getJSONObject(i)
                        NodeRow(
                            address = o.optString("address"),
                            intranetIps = toStringList(o.optJSONArray("intranet_ips")),
                            wanIps = toStringList(o.optJSONArray("wan_ips")),
                            connected = o.optBoolean("is_connected", false),
                        )
                    }
                } ?: emptyList()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(io.github.freewebmovement.zz.ui.theme.WxBg)
            .verticalScroll(rememberScrollState()),
    ) {
        ServerControlCard(running = running, port = node.port, address = myAddress)
        when {
            !running -> SectionCard { Text("节点已停止。点击上方「启动节点」开始服务。", color = TextSecondary) }
            errorMsg.isNotEmpty() -> SectionCard { Text(errorMsg, color = MaterialTheme.colorScheme.error) }
            else -> {
                // ① 节点本身信息
                StatusCard(witness, balance, myAddress)
                SectionHeader("Peer 节点")
                // ② Peer 节点
                NodesCard(nodes)
                ConnectionsCard(inbound, outbound)
                SeedsCard(seeds, onChanged = { /* refreshed on next poll */ })
                // ③ 见证环
                RingCard(witness)
                WeightsCard()
                // ④ 区块浏览
                ExplorerCard(myAddress)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

// ============================================================
//  服务器管理（启动/停止/端口/Web 分享）
// ============================================================

@Composable
private fun ServerControlCard(running: Boolean, port: Int, address: String) {
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
                    if (running) app.fwmc?.stop() else app.restartFwmcNode(app.settings.network.port)
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
                Text(
                    text = if (running && port > 0) "http://<本机IP>:$port/ 局域网可访问" else "节点启动后提供 Web UI",
                    fontSize = 11.sp,
                    color = TextMuted,
                )
            }
            if (running && port > 0) {
                Text(
                    text = "分享",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "http://:${port}/")
                        }
                        context.startActivity(Intent.createChooser(send, "分享 fwmc Web UI"))
                    }.padding(6.dp),
                )
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
                app.restartFwmcNode(newPort)
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
private fun StatusCard(w: WitnessData, balance: Long, myAddress: String) {
    SectionCard(title = "节点信息") {
        InfoGrid(
            listOf(
                "Tick" to w.tickCount.toString(),
                "纪元" to w.epoch.toString(),
                "纪元进度" to "${w.epochTick} / ${w.ticksPerEpoch}",
            )
        )
        InfoGrid(
            listOf(
                "今日 Tick" to w.todayTick.toString(),
                "下次 Tick" to "${w.nextTickSeconds}s",
                "我的余额" to formatAmount(balance),
            )
        )
        if (myAddress.isNotEmpty()) {
            Divider()
            Row(verticalAlignment = Alignment.Top) {
                Text("本机地址 ", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
                SelectionContainer {
                    MonoText(
                        text = myAddress,
                        fontSize = 10,
                        color = TextSecondary,
                        maxLines = 2,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionsCard(inbound: List<ConnGroup>, outbound: List<ConnGroup>) {
    SectionCard(title = "连接 (${inbound.size} 入站 / ${outbound.size} 出站)") {
        GroupTable("入站连接", inbound)
        Spacer(modifier = Modifier.height(8.dp))
        GroupTable("出站连接", outbound)
    }
}

@Composable
private fun GroupTable(title: String, groups: List<ConnGroup>) {
    Text(text = title, fontSize = 13.sp, color = TextSecondary)
    if (groups.isEmpty()) {
        EmptyHint("暂无$title")
    } else {
        Row(modifier = Modifier.padding(vertical = 2.dp)) {
            MonoText(text = "Node ID", fontSize = 10, color = TextMuted, maxLines = 1)
        }
        groups.forEach { g ->
            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                MonoText(text = g.nodeId, fontSize = 11, color = TextPrimary, maxLines = 1)
                Text(
                    text = "已连接 ${g.addrs} · 通过 ${g.passed}",
                    fontSize = 10.sp,
                    color = TextMuted,
                )
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
private fun NodesCard(nodes: List<NodeRow>) {
    SectionCard(title = "注册节点 (${nodes.size})") {
        if (nodes.isEmpty()) {
            EmptyHint("暂无注册节点")
        } else {
            nodes.forEach { n ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDotBig(n.connected)
                        MonoText(
                            text = n.address,
                            fontSize = 11,
                            color = TextPrimary,
                            maxLines = 1,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                    val lan = n.intranetIps.joinToString(", ").ifEmpty { "无" }
                    val wan = n.wanIps.joinToString(", ").ifEmpty { "无" }
                    Text(text = "内网 $lan", fontSize = 10.sp, color = TextMuted)
                    Text(text = "外网 $wan", fontSize = 10.sp, color = TextMuted)
                }
            }
        }
    }
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

/** ③ 见证环：活跃环 / 锁定环 / 见证链全量信息。 */
@Composable
private fun RingCard(w: WitnessData) {
    SectionCard(title = "见证环") {
        InfoGrid(
            listOf(
                "活跃纪元" to w.ringActiveEpoch.toString(),
                "成员数" to w.ringActiveMembers.size.toString(),
                "" to "",
            )
        )
        Row(verticalAlignment = Alignment.Top) {
            Text("环哈希 ", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
            SelectionContainer {
                MonoText(
                    text = w.ringActiveHash.ifEmpty { "-" },
                    fontSize = 10,
                    color = TextSecondary,
                    maxLines = 2,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
        if (w.ringActiveMembers.isNotEmpty()) {
            Divider()
            Text("环成员", fontSize = 12.sp, color = TextSecondary)
            w.ringActiveMembers.forEach { m ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp),
                ) {
                    StatusDotBig(m.active)
                    MonoText(
                        text = "${m.ip}:${m.port}",
                        fontSize = 10,
                        color = TextSecondary,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 6.dp).weight(1f, fill = false),
                    )
                }
            }
        }
        if (w.ringLockedHash.isNotEmpty()) {
            Divider()
            Text("锁定环（纪元 \${w.ringLockedEpoch}）", fontSize = 12.sp, color = TextSecondary)
            SelectionContainer {
                MonoText(text = w.ringLockedHash, fontSize = 10, color = TextMuted, maxLines = 2)
            }
            if (w.ringLockedMembers.isNotEmpty()) {
                Text("成员 \${w.ringLockedMembers.size} 个", fontSize = 10.sp, color = TextMuted)
            }
        }
        if (w.chain.isNotEmpty()) {
            Divider()
            Text("见证链", fontSize = 12.sp, color = TextSecondary)
            w.chain.forEach { c ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDotBig(c.online)
                        MonoText(
                            text = c.address,
                            fontSize = 10,
                            color = TextPrimary,
                            maxLines = 1,
                            modifier = Modifier.padding(start = 6.dp).weight(1f, fill = false),
                        )
                        if (c.isCurrent) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            ) {
                                Text(
                                    "本机",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }
                    Text(
                        text = "在线 ${c.onlineMinutes} 分钟 · Tick ${c.tickCount} · 权重 ${c.weight}",
                        fontSize = 10.sp,
                        color = TextMuted,
                    )
                }
            }
        }
    }
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
        todayTick = obj.optLong("today_tick", 0),
        ringActiveHash = ra?.optString("ring_hash") ?: "",
        ringActiveEpoch = ra?.optLong("epoch", 0) ?: 0,
        ringActiveMembers = members(ra?.optJSONArray("members")),
        ringLockedHash = rl?.optString("ring_hash") ?: "",
        ringLockedEpoch = rl?.optLong("epoch", 0) ?: 0,
        ringLockedMembers = members(rl?.optJSONArray("members")),
        chain = chain,
    )
}

private fun parseConnGroups(arr: JSONArray?): List<ConnGroup> =
    arr?.let { a ->
        (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            ConnGroup(
                nodeId = o.optString("node_id"),
                addrs = o.optString("addrs"),
                passed = o.optString("passed"),
            )
        }
    } ?: emptyList()

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
