package io.github.freewebmovement.zz.ui.content

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.freewebmovement.zz.ui.common.EmptyHint
import io.github.freewebmovement.zz.ui.common.MonoText
import io.github.freewebmovement.zz.ui.common.SectionCard
import io.github.freewebmovement.zz.ui.common.SubTabs
import io.github.freewebmovement.zz.ui.common.formatAmount
import io.github.freewebmovement.zz.ui.common.rememberFwmcNodeSnapshot
import io.github.freewebmovement.zz.ui.theme.CardBg
import io.github.freewebmovement.zz.ui.theme.IncomeGreen
import io.github.freewebmovement.zz.ui.theme.TextMuted
import io.github.freewebmovement.zz.ui.theme.TextPrimary
import io.github.freewebmovement.zz.ui.theme.TextSecondary
import io.github.freewebmovement.zz.ui.theme.AppTheme

import kotlinx.coroutines.launch
import org.json.JSONObject
import rs.zz.coin.FwmcApi

private data class TxRow(
    val txType: String,
    val from: String,
    val to: String,
    val amount: String,
    val timestamp: Long,
    val hash: String,
)

private fun parseTxs(raw: String, addr: String): List<TxRow> = runCatching {
    val obj = JSONObject(raw)
    if (!obj.optBoolean("success", false)) return emptyList()
    obj.optJSONArray("transactions")?.let { arr ->
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            TxRow(
                txType = o.optString("tx_type"),
                from = o.optString("from_address"),
                to = o.optString("to_address"),
                amount = o.optString("amount"),
                timestamp = o.optLong("timestamp", 0) * 1000,
                hash = o.optString("hash"),
            )
        }
    } ?: emptyList()
}.getOrDefault(emptyList())

/**
 * 钱包页（我的 -> 钱包）: balance hero + 转账 / 记录 / 浏览器 sub-pages.
 */
@Composable
fun WalletScreen() {
    val node = rememberFwmcNodeSnapshot()
    val running = node.running

    var balance by remember { mutableStateOf(0L) }
    var myAddress by remember { mutableStateOf("") }

    LaunchedEffect(running) {
        while (running) {
            runCatching {
                val obj = JSONObject(FwmcApi.getData())
                if (obj.optBoolean("success", false)) {
                    balance = obj.optLong("my_balance", 0)
                    myAddress = obj.optString("my_address")
                }
            }
            kotlinx.coroutines.delay(5000)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(io.github.freewebmovement.zz.ui.theme.WxBg)) {
        if (running) {
            BalanceHero(balance = balance, address = myAddress)
        }
        SectionCard {
            if (!running) {
                Text("fwmc 节点未运行，请在「网络」页启动节点", color = TextSecondary)
            }
        }
        WalletTabs(running = running, myAddress = myAddress, onBalanceChanged = { /* refreshed by polling */ })
    }
}

@Composable
private fun BalanceHero(balance: Long, address: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .background(Brush.horizontalGradient(listOf(AppTheme.preset.primary, AppTheme.preset.primaryDark)))
                .padding(horizontal = 18.dp, vertical = 20.dp),
        ) {
            Text(text = "总余额（ZZ）", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
            Text(
                text = formatAmount(balance),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (address.isNotEmpty()) {
                MonoText(
                    text = address,
                    fontSize = 11,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    // wrapped in SelectionContainer by caller if needed
                )
            }
        }
    }
}

@Composable
private fun WalletTabs(running: Boolean, myAddress: String, onBalanceChanged: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    SubTabs(tabs = listOf("转账", "交易记录", "地址浏览器"), selected = tab, onSelect = { tab = it })
    when (tab) {
        0 -> TransferSection(running = running)
        1 -> HistorySection(running = running, myAddress = myAddress)
        else -> ExplorerSection(running = running)
    }
}

// ---------------- 转账 ----------------

@Composable
private fun TransferSection(running: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var toAddr by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var resultMsg by remember { mutableStateOf("") }
    var resultOk by remember { mutableStateOf(false) }

    SectionCard(title = "转账") {
        OutlinedTextField(
            value = toAddr,
            onValueChange = { toAddr = it },
            label = { Text("接收地址") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it.filter { c -> c.isDigit() } },
            label = { Text("金额（分）") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
        )
        Button(
            enabled = !sending && running && toAddr.isNotBlank() && (amountText.toLongOrNull() ?: 0L) > 0,
            onClick = {
                val amount = amountText.toLongOrNull() ?: return@Button
                sending = true
                resultMsg = ""
                scope.launch {
                    val raw = FwmcApi.transfer(toAddr.trim(), amount)
                    sending = false
                    val obj = runCatching { JSONObject(raw) }.getOrNull()
                    val ok = obj?.optBoolean("success", false) == true
                    if (ok) {
                        resultMsg = "转账成功！交易：${obj?.optString("tx_hash")}"
                        resultOk = true
                        toAddr = ""
                        amountText = ""
                        Toast.makeText(context, "转账成功", Toast.LENGTH_SHORT).show()
                    } else {
                        resultMsg = "错误：${obj?.optString("error", "请求失败")}"
                        resultOk = false
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(if (sending) "发送中…" else "发送")
        }
        if (resultMsg.isNotEmpty()) {
            Text(
                text = resultMsg,
                fontSize = 12.sp,
                color = if (resultOk) IncomeGreen else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

// ---------------- 交易记录 ----------------

@Composable
private fun HistorySection(running: Boolean, myAddress: String) {
    var txs by remember { mutableStateOf<List<TxRow>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(running, myAddress) {
        if (!running || myAddress.isEmpty()) return@LaunchedEffect
        txs = parseTxs(FwmcApi.getAddressInfo(myAddress), myAddress)
        loaded = true
    }

    SectionCard(title = "交易记录") {
        when {
            !running -> Text("节点未运行", color = TextSecondary, fontSize = 13.sp)
            loaded && txs.isEmpty() -> EmptyHint("暂无交易")
            else -> txs.forEach { tx ->
                val incoming = tx.to == myAddress
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(
                                text = when (tx.txType) {
                                    "mint" -> "铸造"
                                    "burn" -> "销毁"
                                    else -> if (incoming) "转入" else "转出"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                            )
                            Spacer(modifier = Modifier.height(0.dp))
                        }
                        MonoText(
                            text = if (incoming) "来自 ${tx.from.take(16)}.." else "发往 ${tx.to.take(16)}..",
                            fontSize = 11,
                            color = TextSecondary,
                        )
                        Text(
                            text = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
                                .format(java.util.Date(tx.timestamp)),
                            fontSize = 11.sp,
                            color = TextMuted,
                        )
                    }
                    Text(
                        text = "${if (tx.txType == "mint" || incoming) "+" else "-"}${formatAmount(tx.amount.toLongOrNull() ?: 0)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (tx.txType == "mint" || incoming) IncomeGreen else Color(0xFFE64340),
                    )
                }
            }
        }
    }
}

// ---------------- 地址浏览器 ----------------

@Composable
private fun ExplorerSection(running: Boolean) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var resultBalance by remember { mutableStateOf<Long?>(null) }
    var txs by remember { mutableStateOf<List<TxRow>>(emptyList()) }
    var err by remember { mutableStateOf("") }

    SectionCard(title = "地址浏览器") {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("输入 FWMC 地址") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Button(
                enabled = running && query.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp),
                onClick = {
                    err = ""
                    resultBalance = null
                    txs = emptyList()
                    scope.launch {
                        val raw = FwmcApi.getAddressInfo(query.trim())
                        runCatching {
                            val obj = JSONObject(raw)
                            if (obj.optBoolean("success", false)) {
                                resultBalance = obj.optLong("balance", 0)
                                txs = parseTxs(raw, query.trim())
                            } else {
                                err = obj.optString("error", "查询失败")
                            }
                        }.onFailure { err = it.message ?: "解析失败" }
                    }
                },
                modifier = Modifier.padding(start = 8.dp),
            ) { Text("查询") }
        }
        err.ifEmpty { null }?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        resultBalance?.let { bal ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "余额 ${formatAmount(bal)} ZZ",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.preset.primaryDark,
            )
        }
        if (txs.isNotEmpty()) {
            Text(
                text = "最近 ${txs.size} 笔交易",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            txs.take(20).forEach { tx ->
                val outgoing = tx.from == query.trim()
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(
                        text = "${if (outgoing) "-" else "+"}${formatAmount(tx.amount.toLongOrNull() ?: 0)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (outgoing) Color(0xFFE64340) else IncomeGreen,
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        MonoText(text = "from ${tx.from.take(14)}..", fontSize = 10, color = TextSecondary, maxLines = 1)
                        MonoText(text = "to   ${tx.to.take(14)}..", fontSize = 10, color = TextSecondary, maxLines = 1)
                    }
                }
            }
        }
    }
}
