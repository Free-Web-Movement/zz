package io.github.freewebmovement.zz.ui.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.freewebmovement.zz.ui.common.ContentType
import io.github.freewebmovement.zz.ui.common.PageType
import io.github.freewebmovement.zz.ui.common.rememberFwmcNodeSnapshot
import io.github.freewebmovement.zz.ui.content.peer.AddPeer
import io.github.freewebmovement.zz.ui.theme.backColor
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
)

@Composable
fun PeerContent(page: PageType, updater: (page: PageType, value: ContentType) -> Unit) {
    when (page) {
        PageType.PeerAdd -> AddPeer {
            updater(it, ContentType.NonStacked)
        }
        else -> NetworkDashboard()
    }
}

@Composable
private fun NetworkDashboard() {
    val node = rememberFwmcNodeSnapshot()
    val running = node.running
    var myAddress by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf(0L) }
    var epoch by remember { mutableStateOf(0L) }
    var tickCount by remember { mutableStateOf(0L) }
    var nodes by remember { mutableStateOf<List<NodeRow>>(emptyList()) }
    var seeds by remember { mutableStateOf<List<SeedRow>>(emptyList()) }
    var errorMsg by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(running, refresh) {
        if (!running) return@LaunchedEffect
        runCatching {
            val obj = JSONObject(FwmcApi.getData())
            if (!obj.optBoolean("success", false)) {
                errorMsg = obj.optString("error", "加载失败")
                return@LaunchedEffect
            }
            errorMsg = ""
            myAddress = obj.optString("my_address")
            balance = obj.optLong("my_balance", 0)
            epoch = obj.optLong("epoch", 0)
            tickCount = obj.optLong("tick_count", 0)
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
                    )
                }
            } ?: emptyList()
        }.onFailure { errorMsg = it.message ?: "加载失败" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        ) {
            Text(text = "网络", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { refresh++ }) { Text(text = "刷新") }
        }
        when {
            !running -> SectionCard { Text("fwmc 节点未运行，请在“我的”页启动节点", color = Color.Gray) }
            errorMsg.isNotEmpty() -> SectionCard { Text(errorMsg, color = MaterialTheme.colorScheme.error) }
            else -> {
                SectionCard(title = "本机") {
                    SelectionContainer {
                        Text(text = myAddress, fontSize = 12.sp)
                    }
                    Text(text = "余额: ${balance / 100}.${balance % 100}", fontSize = 14.sp)
                    Text(text = "Epoch: $epoch   Tick: $tickCount", fontSize = 14.sp)
                }
                SectionCard(title = "节点 (${nodes.size})") {
                    if (nodes.isEmpty()) Text("无", color = Color.Gray)
                    nodes.forEach { n ->
                        Column(modifier = Modifier.padding(vertical = 2.dp)) {
                            Row {
                                Text(
                                    text = if (n.connected) "●" else "○",
                                    color = if (n.connected) Color(0xFF2E7D32) else Color.Gray,
                                    fontSize = 13.sp,
                                )
                                Text(
                                    text = n.address,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                            val ips = (n.wanIps + n.intranetIps).joinToString(", ")
                            if (ips.isNotEmpty()) {
                                Text(text = ips, fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
                SectionCard(title = "种子 (${seeds.size})") {
                    if (seeds.isEmpty()) Text("无", color = Color.Gray)
                    seeds.forEach { s ->
                        Text(
                            text = "${s.address}:${s.port} [${s.protocol}${if (s.active) "" else ", inactive"}]",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun toStringList(arr: JSONArray?): List<String> {
    arr ?: return emptyList()
    return (0 until arr.length()).mapNotNull { i -> runCatching { arr.getString(i) }.getOrNull() }
}

@Composable
private fun SectionCard(title: String? = null, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = backColor,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (title != null) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    text = "",
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            content()
        }
    }
}
