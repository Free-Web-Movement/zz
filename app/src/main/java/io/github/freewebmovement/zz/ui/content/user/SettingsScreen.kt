package io.github.freewebmovement.zz.ui.content.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.freewebmovement.zz.ui.common.Divider
import io.github.freewebmovement.zz.ui.common.SectionCard
import io.github.freewebmovement.zz.ui.theme.TextMuted
import io.github.freewebmovement.zz.ui.theme.TextPrimary
import io.github.freewebmovement.zz.ui.theme.TextSecondary
import io.github.freewebmovement.zz.ui.theme.WxBg
import io.github.freewebmovement.zz.ui.theme.AppTheme
import kotlinx.coroutines.launch
import org.json.JSONObject
import rs.zz.coin.FwmcApi

@Composable
fun SettingsScreen(onBack: () -> Unit = {}) {
    var showTheme by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var config by remember { mutableStateOf<JSONObject?>(null) }
    var maxWpl by remember { mutableStateOf("") }
    var cpuCores by remember { mutableStateOf("") }
    var cpuGhz by remember { mutableStateOf("") }
    var memGb by remember { mutableStateOf("") }
    var storageTb by remember { mutableStateOf("") }
    var bandwidth by remember { mutableStateOf("") }
    var apiRequests by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var ticksPerEpoch by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching {
            val raw = FwmcApi.getConfig()
            val obj = JSONObject(raw)
            if (obj.optBoolean("success", false)) {
                config = obj
                maxWpl = obj.optString("max_witness_per_layer", "")
                cpuCores = obj.optString("cpu_cores", "")
                cpuGhz = obj.optString("cpu_ghz", "")
                memGb = obj.optString("memory_gb", "")
                storageTb = obj.optString("storage_tb", "")
                bandwidth = obj.optString("bandwidth_gbps", "")
                apiRequests = obj.optString("api_requests", "")
                port = obj.optString("p2p_port", "")
                ticksPerEpoch = obj.optString("ticks_per_epoch", "")
            }
        }
    }

    fun save() {
        scope.launch {
            val json = JSONObject().apply {
                put("witness.max_per_layer", maxWpl)
                put("resource.cpu_cores", cpuCores)
                put("resource.cpu_ghz", cpuGhz)
                put("resource.memory_gb", memGb)
                put("resource.storage_tb", storageTb)
                put("resource.bandwidth_gbps", bandwidth)
                put("resource.api_requests", apiRequests)
                put("network.p2p_port", port)
                put("epoch.ticks_per_epoch", ticksPerEpoch)
            }
            runCatching {
                val raw = FwmcApi.setConfig(json.toString())
                val obj = JSONObject(raw)
                saved = obj.optBoolean("success", false)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WxBg)
            .verticalScroll(rememberScrollState()),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(io.github.freewebmovement.zz.ui.theme.CardBg)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text("设置", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }

        // fwmc 节点配置
        SectionCard(title = "节点配置") {
            ConfigRow("见证环最大节点数", maxWpl) { maxWpl = it }
            ConfigRow("CPU 核心数", cpuCores) { cpuCores = it }
            ConfigRow("CPU 频率 (GHz)", cpuGhz) { cpuGhz = it }
            ConfigRow("内存 (GB)", memGb) { memGb = it }
            ConfigRow("存储 (TB)", storageTb) { storageTb = it }
            ConfigRow("带宽 (Gbps)", bandwidth) { bandwidth = it }
            ConfigRow("API 请求数", apiRequests) { apiRequests = it }
            ConfigRow("P2P 端口", port) { port = it }
            ConfigRow("每纪元 Tick 数", ticksPerEpoch) { ticksPerEpoch = it }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { save() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                enabled = true,
            ) {
                Text(if (saved) "已保存 ✓" else "保存配置")
            }
        }

        Divider()

        // 主题设置
        SectionCard(title = "外观") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                    .clickable { showTheme = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("主题配色", fontSize = 15.sp, color = TextPrimary)
                    Text("绿 / 蓝 / 粉 / 紫 / 橙", fontSize = 11.sp, color = TextMuted)
                }
                Text(AppTheme.preset.label, fontSize = 13.sp, color = TextSecondary)
                Text("  ›", color = TextMuted)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showTheme) {
        ThemeDialog(onDismiss = { showTheme = false })
    }
}

@Composable
private fun ConfigRow(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = {
            onValueChange(it)
        },
        label = { Text(label, fontSize = 13.sp) },
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    )
}
