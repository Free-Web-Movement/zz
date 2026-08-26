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
    val direction: String,
    val localAddr: String,
    val remoteAddr: String,
    val nodeId: String,
)

/** 按 node_id 聚合后的一条连接记录。 */
private data class ConnRow(
    val nodeId: String,
    val outboundLocal: String,
    val outboundRemote: String,
    val inboundLocal: String,
    val inboundRemote: String,
    val bidirectional: Boolean,
)

private const val PAGE_SIZE = 20

@Composable
fun ConnectionsScreen(onBack: () -> Unit = {}) {
    var rows by remember { mutableStateOf<List<ConnRow>>(emptyList()) }
    var currentPage by remember { mutableIntStateOf(0) }
    var errMsg by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            runCatching {
                val obj = JSONObject(FwmcApi.getConnections())
                if (obj.optBoolean("success", false)) {
                    errMsg = ""
                    val inbound = parsePeers(obj.optJSONArray("inbound"))
                    val outbound = parsePeers(obj.optJSONArray("outbound"))
                    rows = buildRows(inbound, outbound)
                } else {
                    errMsg = obj.optString("error", "加载失败")
                }
            }
            if (rows.isEmpty()) rows = demoRows()
            delay(5000)
        }
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text("节点连接情况", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }

        val okCount = rows.count { it.bidirectional }
        val failCount = rows.size - okCount
        SectionCard(title = "P2P 连接 (${rows.size})") {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("连出", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                    Text("本地:连出 → 远程:侦听", fontSize = 9.sp, color = TextMuted)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("连入", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                    Text("本地:侦听 ← 远程:连出", fontSize = 9.sp, color = TextMuted)
                }
                Text("状态", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
            }
            Divider()

            if (pageItems.isEmpty()) {
                Text(
                    if (errMsg.isNotEmpty()) errMsg else "暂无连接",
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
                                if (row.bidirectional) "双向" else "单向",
                                fontSize = 10.sp,
                                color = if (row.bidirectional) OnlineGreen else TextMuted,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                    Text(
                        text = shortAddr(row.nodeId),
                        fontSize = 9.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }

            if (rows.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("✓ 双向 $okCount", fontSize = 12.sp, color = OnlineGreen)
                    Text("✗ 单向 $failCount", fontSize = 12.sp, color = TextMuted)
                }
            }

            if (totalPages > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (safePage > 0) "< 上一页" else "          ",
                        fontSize = 13.sp,
                        color = if (safePage > 0) MaterialTheme.colorScheme.primary else TextMuted,
                        modifier = Modifier.clickable(enabled = safePage > 0) { currentPage = safePage - 1 },
                    )
                    Text("  ${safePage + 1} / $totalPages  ", fontSize = 12.sp, color = TextMuted)
                    Text(
                        text = if (safePage < totalPages - 1) "下一页 >" else "          ",
                        fontSize = 13.sp,
                        color = if (safePage < totalPages - 1) MaterialTheme.colorScheme.primary else TextMuted,
                        modifier = Modifier.clickable(enabled = safePage < totalPages - 1) { currentPage = safePage + 1 },
                    )
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
        list.add(
            PeerConn(
                direction = o.optString("direction", ""),
                localAddr = o.optString("local_addr", ""),
                remoteAddr = o.optString("addr", ""),
                nodeId = o.optString("node_id", ""),
            ),
        )
    }
    return list
}

/**
 * 按 node_id 聚合连入/连出，判断双向连接是否成立。
 *
 * 连出: 本地IP:连出端口 → 远程IP:侦听端口  (local_addr = 本地连出, addr = 远程侦听)
 * 连入: 本地IP:侦听端口 ← 远程IP:连出端口  (local_addr = 本地侦听, addr = 远程连出)
 *
 * 双向 = 同一 node_id 同时有连出和连入。
 */
private fun buildRows(inbound: List<PeerConn>, outbound: List<PeerConn>): List<ConnRow> {
    val byNode = mutableMapOf<String, ConnRow>()
    for (p in outbound) {
        if (p.nodeId.isEmpty()) continue
        val cur = byNode.getOrPut(p.nodeId) {
            ConnRow(p.nodeId, "", "", "", "", false)
        }
        byNode[p.nodeId] = cur.copy(
            outboundLocal = p.localAddr,
            outboundRemote = p.remoteAddr,
        )
    }
    for (p in inbound) {
        if (p.nodeId.isEmpty()) continue
        val cur = byNode.getOrPut(p.nodeId) {
            ConnRow(p.nodeId, "", "", "", "", false)
        }
        byNode[p.nodeId] = cur.copy(
            inboundLocal = p.localAddr,
            inboundRemote = p.remoteAddr,
        )
    }
    return byNode.values
        .map { it.copy(bidirectional = it.outboundRemote.isNotEmpty() && it.inboundRemote.isNotEmpty()) }
        .sortedBy { extractIp(it.outboundRemote.ifEmpty { it.inboundRemote }) }
}

private fun extractIp(addr: String): String {
    val idx = addr.lastIndexOf(':')
    return if (idx > 0) addr.substring(0, idx) else addr
}

private fun shortAddr(addr: String, keep: Int = 14): String =
    if (addr.length <= keep * 2) addr else addr.take(keep) + ".." + addr.takeLast(6)

/** 演示数据：模拟 3 个远程节点，2 个双向成功、1 个单向失败。 */
private fun demoRows(): List<ConnRow> = listOf(
    ConnRow(
        nodeId = "FWMC:Zz:6Hk3Qp9Tf2VxLmRbNc1JdXa4ZsE8Wy0uIgKvOe5Tn",
        outboundLocal = "192.168.3.27:54321",
        outboundRemote = "10.0.0.5:20260",
        inboundLocal = "192.168.3.27:20260",
        inboundRemote = "10.0.0.5:51234",
        bidirectional = true,
    ),
    ConnRow(
        nodeId = "FWMC:Zz:7AbC8dEfGhIjKlMnOpQrStUvWxYz1234567890AbCdEfGh",
        outboundLocal = "192.168.3.27:55678",
        outboundRemote = "10.0.0.8:20260",
        inboundLocal = "192.168.3.27:20260",
        inboundRemote = "10.0.0.8:50001",
        bidirectional = true,
    ),
    ConnRow(
        nodeId = "FWMC:Zz:3MnBvCxZaLkQwErTyUiOpAsDfGhJkLzxCvBnM1234567890",
        outboundLocal = "192.168.3.27:60123",
        outboundRemote = "172.16.5.9:20260",
        inboundLocal = "",
        inboundRemote = "",
        bidirectional = false,
    ),
)
