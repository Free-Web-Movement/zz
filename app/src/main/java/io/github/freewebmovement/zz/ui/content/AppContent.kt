package io.github.freewebmovement.zz.ui.content

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.ui.common.rememberFwmcNodeSnapshot
import io.github.freewebmovement.zz.ui.theme.backColor
import kotlinx.coroutines.launch
import org.json.JSONObject
import rs.zz.coin.FwmcApi

private data class TxRow(
    val txType: String,
    val from: String,
    val to: String,
    val amount: String,
)

@Composable
fun AppContent() {
    val app = MainApplication.getApp()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val node = rememberFwmcNodeSnapshot()
    val running = node.running

    var myAddress by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf(0L) }
    var txs by remember { mutableStateOf<List<TxRow>>(emptyList()) }
    var errorMsg by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }

    // transfer form
    var toAddr by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

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
            val addrInfo = JSONObject(FwmcApi.getAddressInfo(myAddress))
            txs = addrInfo.optJSONArray("transactions")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    TxRow(
                        txType = o.optString("tx_type"),
                        from = o.optString("from_address"),
                        to = o.optString("to_address"),
                        amount = o.optString("amount"),
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
            Text(text = "钱包", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { refresh++ }) { Text(text = "刷新") }
        }
        when {
            !running -> SectionCard {
                Text("fwmc 节点未运行，请在“我的”页启动节点", color = Color.Gray)
            }
            else -> {
                SectionCard(title = "余额") {
                    Text(
                        text = "${balance / 100}.${balance % 100}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "FWMC-ZZ",
                        fontSize = 12.sp,
                        color = Color.Gray,
                    )
                }
                SectionCard(title = "本机地址") {
                    SelectionContainer {
                        Text(text = myAddress, fontSize = 12.sp)
                    }
                }
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
                        label = { Text("金额") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    )
                    Button(
                        enabled = !sending && toAddr.isNotBlank() && amountText.toLongOrNull() ?: 0L > 0,
                        onClick = {
                            val amount = amountText.toLongOrNull() ?: return@Button
                            sending = true
                            scope.launch {
                                val raw = FwmcApi.transfer(toAddr.trim(), amount)
                                sending = false
                                val ok = runCatching {
                                    JSONObject(raw).optBoolean("success", false)
                                }.getOrDefault(false)
                                Toast.makeText(
                                    context,
                                    if (ok) "转账成功" else "转账失败",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                if (ok) {
                                    toAddr = ""
                                    amountText = ""
                                    refresh++
                                }
                            }
                        },
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(if (sending) "发送中…" else "发送")
                    }
                }
                SectionCard(title = "交易记录") {
                    if (errorMsg.isNotEmpty()) {
                        Text(errorMsg, color = MaterialTheme.colorScheme.error)
                    } else if (txs.isEmpty()) {
                        Text("暂无交易", color = Color.Gray)
                    } else {
                        txs.forEach { tx ->
                            val dir = if (tx.from == myAddress) "-" else "+"
                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)) {
                                Text(
                                    text = "$dir${tx.amount}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dir == "+") Color(0xFF2E7D32) else Color.Red,
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                                Column {
                                    Text(
                                        text = "${tx.txType} ${if (tx.from == myAddress) "-> 我" else "我 ->"}",
                                        fontSize = 12.sp,
                                    )
                                    Text(
                                        text = if (tx.from == myAddress) tx.to else tx.from,
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
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
                Text(text = "", modifier = Modifier.padding(bottom = 2.dp))
            }
            content()
        }
    }
}
