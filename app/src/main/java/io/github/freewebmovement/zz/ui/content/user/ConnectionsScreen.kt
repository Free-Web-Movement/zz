package io.github.freewebmovement.zz.ui.content.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.freewebmovement.zz.ui.common.Divider
import io.github.freewebmovement.zz.ui.common.MonoText
import io.github.freewebmovement.zz.ui.common.SectionCard
import io.github.freewebmovement.zz.ui.common.StatusDot
import io.github.freewebmovement.zz.ui.theme.CardBg
import io.github.freewebmovement.zz.ui.theme.OnlineGreen
import io.github.freewebmovement.zz.ui.theme.TextMuted
import io.github.freewebmovement.zz.ui.theme.TextPrimary
import io.github.freewebmovement.zz.ui.theme.TextSecondary
import io.github.freewebmovement.zz.ui.theme.WxBg
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import rs.zz.coin.FwmcApi

private data class PeerConn(
    val nodeId: String,
    val ip: String,
    val allIps: List<String>,
    val inboundRemotePort: Int,
    val inboundLocalPort: Int,
    val outboundRemotePort: Int,
    val outboundLocalPort: Int,
    val connected: Boolean,
) {
    val bidirectional: Boolean get() = connected
}

/** 一个已知（发现到的）节点，来源 `getNodes()`；address 可作为聊天对象 contact。 */
private data class NodeRow(
    val address: String,
    val ip: String,
    val isConnected: Boolean,
)

/** 按对端节点聚合后的一条连接记录。 */
private data class ConnRow(
    val nodeId: String,
    val ip: String,
    val allIps: List<String>,
    val outboundLocal: String,
    val outboundRemote: String,
    val inboundLocal: String,
    val inboundRemote: String,
    val bidirectional: Boolean,
)

private const val PAGE_SIZE = 20

@Composable
fun ConnectionsScreen(onBack: () -> Unit = {}, onChat: (String, String) -> Unit = { _, _ -> }) {
    val s = io.github.freewebmovement.zz.ui.i18n.LocalAppStrings.current
    var rows by remember { mutableStateOf<List<ConnRow>>(emptyList()) }
    var nodes by remember { mutableStateOf<List<NodeRow>>(emptyList()) }
    var currentPage by remember { mutableIntStateOf(0) }
    var errMsg by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            runCatching {
                val obj = JSONObject(FwmcApi.getConnections())
                if (obj.optBoolean("success", false)) {
                    errMsg = ""
                    val peers = parsePeers(obj.optJSONArray("peers"))
                    rows = peers.map { it.toConnRow() }
                } else {
                    errMsg = obj.optString("error", s.connections.loadFailed)
                }
            }
            runCatching {
                val no = JSONObject(FwmcApi.getNodes())
                if (no.optBoolean("success", false)) {
                    nodes = parseNodes(no.optJSONArray("nodes"))
                }
            }
            if (rows.isEmpty()) rows = demoRows()
            delay(5000)
        }
    }

    val chatAll = { addr: String ->
        if (addr.isNotEmpty()) onChat(addr, shortName(addr))
    }

    val totalPages = maxOf(1, (rows.size + PAGE_SIZE - 1) / PAGE_SIZE)
    val safePage = currentPage.coerceIn(0, totalPages - 1)
    val pageItems = rows.drop(safePage * PAGE_SIZE).take(PAGE_SIZE)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WxBg)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(CardBg)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.common.back)
            }
            Text(s.connections.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }

        val okCount = rows.count { it.bidirectional }
        val failCount = rows.size - okCount
        SectionCard(title = s.connections.p2pConnections.format(rows.size)) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(s.connections.outbound, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                    Text(s.connections.localOutbound, fontSize = 9.sp, color = TextMuted)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(s.connections.inbound, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                    Text(s.connections.localListen, fontSize = 9.sp, color = TextMuted)
                }
                Text(s.connections.status, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
            }
            Divider()

            if (pageItems.isEmpty()) {
                Text(
                    if (errMsg.isNotEmpty()) errMsg else s.connections.none,
                    fontSize = 13.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                pageItems.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (row.outboundRemote.isNotEmpty()) {
                                MonoText(text = "${row.outboundLocal} → ${row.outboundRemote}", fontSize = 9, color = TextPrimary)
                            } else {
                                MonoText(text = "—", fontSize = 9, color = TextMuted)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            if (row.inboundRemote.isNotEmpty()) {
                                MonoText(text = "${row.inboundLocal} ← ${row.inboundRemote}", fontSize = 9, color = TextPrimary)
                            } else {
                                MonoText(text = "—", fontSize = 9, color = TextMuted)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusDot(active = row.bidirectional)
                            Text(
                                if (row.bidirectional) s.connections.bidirectional else s.connections.unidirectional,
                                fontSize = 10.sp,
                                color = if (row.bidirectional) OnlineGreen else TextMuted,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                            if (row.nodeId.isNotEmpty()) {
                                IconButton(onClick = { chatAll(row.nodeId) }) {
                                    Icon(
                                        Icons.Filled.Send,
                                        contentDescription = s.connections.chat,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = shortAddr(row.nodeId),
                        fontSize = 9.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    if (row.ip.isNotEmpty()) {
                        Text(
                            text = row.ip,
                            fontSize = 9.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                }
            }

            if (rows.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(s.connections.okCount.format(okCount), fontSize = 12.sp, color = OnlineGreen)
                    Text(s.connections.failCount.format(failCount), fontSize = 12.sp, color = TextMuted)
                }
            }

            if (totalPages > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (safePage > 0) s.connections.prev else "          ",
                        fontSize = 13.sp,
                        color = if (safePage > 0) MaterialTheme.colorScheme.primary else TextMuted,
                        modifier = Modifier.clickable(enabled = safePage > 0) { currentPage = safePage - 1 },
                    )
                    Text("  ${safePage + 1} / $totalPages  ", fontSize = 12.sp, color = TextMuted)
                    Text(
                        text = if (safePage < totalPages - 1) s.connections.next else "          ",
                        fontSize = 13.sp,
                        color = if (safePage < totalPages - 1) MaterialTheme.colorScheme.primary else TextMuted,
                        modifier = Modifier.clickable(enabled = safePage < totalPages - 1) { currentPage = safePage + 1 },
                    )
                }
            }
        }
        SectionCard(title = s.connections.nodes.format(nodes.size)) {
            if (nodes.isEmpty()) {
                Text(
                    s.connections.nodesNone,
                    fontSize = 13.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                nodes.forEach { n ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            MonoText(text = shortAddr(n.address), fontSize = 10, color = TextPrimary)
                            Text(
                                text = n.ip.ifEmpty { "—" },
                                fontSize = 9.sp,
                                color = TextMuted,
                            )
                        }
                        StatusDot(active = n.isConnected)
                        Text(
                            text = if (n.isConnected) s.connections.connected else s.connections.disconnected,
                            fontSize = 10.sp,
                            color = if (n.isConnected) OnlineGreen else TextMuted,
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                        )
                        IconButton(onClick = { chatAll(n.address) }) {
                            Icon(
                                Icons.Filled.Send,
                                contentDescription = s.connections.chat,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Divider()
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun parsePeers(arr: JSONArray?): List<PeerConn> {
    arr ?: return emptyList()
    val list = mutableListOf<PeerConn>()
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val nodeId = o.optString("node_id", "")
        if (nodeId.isEmpty()) continue
        val allIps = mutableListOf<String>()
        o.optJSONArray("all_ips")?.let { a ->
            for (j in 0 until a.length()) allIps.add(a.getString(j))
        }
        val inbound = o.optJSONObject("inbound")
        val outbound = o.optJSONObject("outbound")
        list.add(
            PeerConn(
                nodeId = nodeId,
                ip = o.optString("ip", ""),
                allIps = allIps,
                inboundRemotePort = inbound?.optInt("remotePort", 0) ?: 0,
                inboundLocalPort = inbound?.optInt("localPort", 0) ?: 0,
                outboundRemotePort = outbound?.optInt("remotePort", 0) ?: 0,
                outboundLocalPort = outbound?.optInt("localPort", 0) ?: 0,
                connected = o.optBoolean("connected", false),
            ),
        )
    }
    return list
}

/** 把统一 `peers` 记录转为展示行：端口对 + ip + connected 状态。 */
private fun PeerConn.toConnRow(): ConnRow = ConnRow(
    nodeId = nodeId,
    ip = ip,
    allIps = allIps,
    outboundLocal = if (outboundLocalPort > 0) outboundLocalPort.toString() else "",
    outboundRemote = if (outboundRemotePort > 0) outboundRemotePort.toString() else "",
    inboundLocal = if (inboundLocalPort > 0) inboundLocalPort.toString() else "",
    inboundRemote = if (inboundRemotePort > 0) inboundRemotePort.toString() else "",
    bidirectional = connected,
)

private fun shortAddr(addr: String, keep: Int = 14): String =
    if (addr.length <= keep * 2) addr else addr.take(keep) + ".." + addr.takeLast(6)

/** 短地址作为聊天会话显示名。 */
private fun shortName(addr: String): String = shortAddr(addr, 8)

/** 解析 `getNodes()` 返回的其它节点列表；address 为 FWMC 节点地址，可作聊天对象。 */
private fun parseNodes(arr: JSONArray?): List<NodeRow> {
    arr ?: return emptyList()
    val list = mutableListOf<NodeRow>()
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val address = o.optString("address", "")
        if (address.isEmpty()) continue
        val ips = mutableListOf<String>()
        o.optJSONArray("intranet_ips").let { a ->
            if (a != null) for (j in 0 until a.length()) ips.add(a.getString(j))
        }
        o.optJSONArray("wan_ips").let { a ->
            if (a != null) for (j in 0 until a.length()) ips.add(a.getString(j))
        }
        list.add(
            NodeRow(
                address = address,
                ip = ips.joinToString(" "),
                isConnected = o.optBoolean("is_connected", false),
            ),
        )
    }
    return list
}

/** 演示数据：模拟 3 个远程节点，2 个双向成功、1 个单向失败。 */
private fun demoRows(): List<ConnRow> = listOf(
    ConnRow(
        nodeId = "FWMC:Zz:6Hk3Qp9Tf2VxLmRbNc1JdXa4ZsE8Wy0uIgKvOe5Tn",
        ip = "10.0.0.5:20260",
        allIps = listOf("10.0.0.5:20260", "192.168.1.5:20260"),
        outboundLocal = "54321",
        outboundRemote = "20260",
        inboundLocal = "20260",
        inboundRemote = "51234",
        bidirectional = true,
    ),
    ConnRow(
        nodeId = "FWMC:Zz:7AbC8dEfGhIjKlMnOpQrStUvWxYz1234567890AbCdEfGh",
        ip = "10.0.0.8:20260",
        allIps = listOf("10.0.0.8:20260"),
        outboundLocal = "55678",
        outboundRemote = "20260",
        inboundLocal = "20260",
        inboundRemote = "50001",
        bidirectional = true,
    ),
    ConnRow(
        nodeId = "FWMC:Zz:3MnBvCxZaLkQwErTyUiOpAsDfGhJkLzxCvBnM1234567890",
        ip = "172.16.5.9:20260",
        allIps = listOf("172.16.5.9:20260"),
        outboundLocal = "60123",
        outboundRemote = "20260",
        inboundLocal = "",
        inboundRemote = "",
        bidirectional = false,
    ),
)
