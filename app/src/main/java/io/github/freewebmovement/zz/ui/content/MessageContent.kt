package io.github.freewebmovement.zz.ui.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.freewebmovement.zz.ui.common.rememberFwmcNodeSnapshot
import io.github.freewebmovement.zz.ui.theme.backColor
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import rs.zz.coin.FwmcApi

// ============================================================
//  Data
// ============================================================

private data class Conv(
    val address: String,
    val lastContent: String,
    val unread: Int,
)

private data class ChatMsg(
    val content: String,
    val isSent: Boolean,
    val status: String,
)

private fun parseConversations(raw: String): List<Conv> = runCatching {
    val obj = JSONObject(raw)
    if (!obj.optBoolean("success", false)) return emptyList()
    val arr: JSONArray = obj.optJSONArray("conversations") ?: return emptyList()
    (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        Conv(
            address = o.optString("contact_address"),
            lastContent = o.optString("last_content"),
            unread = o.optInt("unread_count", 0),
        )
    }
}.getOrDefault(emptyList())

private fun parseMessages(raw: String): List<ChatMsg> = runCatching {
    val obj = JSONObject(raw)
    if (!obj.optBoolean("success", false)) return emptyList()
    val arr: JSONArray = obj.optJSONArray("messages") ?: return emptyList()
    (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        ChatMsg(
            content = o.optString("content"),
            isSent = o.optBoolean("is_sent", false),
            status = o.optString("status"),
        )
    }
}.getOrDefault(emptyList())

// ============================================================
//  Tab root: conversation list <-> chat screen
// ============================================================

@Composable
fun MessageContent() {
    var selectedContact by remember { mutableStateOf<String?>(null) }
    if (selectedContact == null) {
        ConversationList(onOpen = { selectedContact = it })
    } else {
        ChatScreen(contact = selectedContact!!, onBack = { selectedContact = null })
    }
}

// ============================================================
//  Conversation list
// ============================================================

@Composable
private fun ConversationList(onOpen: (String) -> Unit) {
    val node = rememberFwmcNodeSnapshot()
    val running = node.running
    var convs by remember { mutableStateOf<List<Conv>>(emptyList()) }
    var errorMsg by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(running, refresh) {
        if (!running) return@LaunchedEffect
        val raw = FwmcApi.getConversations()
        convs = parseConversations(raw)
        val err = runCatching { JSONObject(raw).optString("error") }.getOrDefault("")
        errorMsg = if (convs.isEmpty() && err.isNotEmpty()) err else ""
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "会话", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row {
                TextButton(onClick = { refresh++ }) { Text(text = "刷新") }
                TextButton(onClick = { showAddDialog = true }) { Text(text = "+ 联系人") }
            }
        }
        when {
            !running -> HintBox("fwmc 节点未运行，请在“我的”页启动节点")
            errorMsg.isNotEmpty() -> HintBox(errorMsg)
            convs.isEmpty() -> HintBox("暂无会话")
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(convs, key = { it.address }) { conv ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(backColor)
                            .clickable { onOpen(conv.address) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = conv.address,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                            )
                            if (conv.unread > 0) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.error,
                                ) {
                                    Text(
                                        text = conv.unread.toString(),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    )
                                }
                            }
                        }
                        Text(
                            text = conv.lastContent,
                            fontSize = 13.sp,
                            color = Color.Gray,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddContactDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, addr ->
                showAddDialog = false
                scope.launch {
                    FwmcApi.addContact(name, addr)
                    refresh++
                }
            },
        )
    }
}

// ============================================================
//  Chat screen
// ============================================================

@Composable
private fun ChatScreen(contact: String, onBack: () -> Unit) {
    var msgs by remember { mutableStateOf<List<ChatMsg>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(contact, refresh) {
        msgs = parseMessages(FwmcApi.getChatMessages(contact))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backColor)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("< 返回") }
            Text(
                text = contact,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }
        // messages
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = true,
        ) {
            items(msgs.asReversed()) { msg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = if (msg.isSent) Arrangement.End else Arrangement.Start,
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (msg.isSent) MaterialTheme.colorScheme.primaryContainer else backColor,
                    ) {
                        Column(modifier = Modifier.widthIn(max = 280.dp)) {
                            Text(
                                text = msg.content,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
        // input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backColor)
                .padding(8.dp)
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入消息…") },
                maxLines = 3,
            )
            Button(
                onClick = {
                    val text = input.trim()
                    if (text.isEmpty()) return@Button
                    input = ""
                    scope.launch {
                        FwmcApi.sendChat(contact, text)
                        refresh++
                    }
                },
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text("发送")
            }
        }
    }
}

// ============================================================
//  Shared widgets
// ============================================================

@Composable
private fun HintBox(text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = text, textAlign = TextAlign.Center, color = Color.Gray)
    }
}

@Composable
private fun AddContactDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, address: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加联系人") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("FWMC 地址") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank() && address.isNotBlank()) onConfirm(name.trim(), address.trim()) },
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
