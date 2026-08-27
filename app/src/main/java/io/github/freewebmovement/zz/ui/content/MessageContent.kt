package io.github.freewebmovement.zz.ui.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import io.github.freewebmovement.zz.ui.common.Avatar
import io.github.freewebmovement.zz.ui.common.EmptyHint
import io.github.freewebmovement.zz.ui.common.UnreadBadge
import io.github.freewebmovement.zz.ui.common.rememberFwmcNodeSnapshot
import io.github.freewebmovement.zz.ui.theme.AppTheme
import io.github.freewebmovement.zz.ui.theme.CardBg
import io.github.freewebmovement.zz.ui.theme.LineColor
import io.github.freewebmovement.zz.ui.theme.TextMuted
import io.github.freewebmovement.zz.ui.theme.TextPrimary
import io.github.freewebmovement.zz.ui.theme.TextSecondary
import io.github.freewebmovement.zz.ui.theme.WxBg

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import rs.zz.coin.FwmcApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================
//  Data
// ============================================================

internal data class ContactRow(
    val address: String,
    val name: String,
    val avatar: String?,
    val unread: Int,
)

private data class ConvRow(
    val address: String,
    val lastContent: String,
    val lastTimestamp: Long,
    val unread: Int,
)

private data class ChatMsg(
    val content: String,
    val isSent: Boolean,
    val status: String,
    val timestamp: Long,
)

private data class SessionItem(
    val address: String,
    val name: String,
    val avatar: String?,
    val preview: String,
    val timestamp: Long,
    val unread: Int,
)

internal fun parseContacts(raw: String): List<ContactRow> = runCatching {
    val obj = JSONObject(raw)
    if (!obj.optBoolean("success", false)) return emptyList()
    obj.optJSONArray("contacts")?.let { arr ->
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ContactRow(
                address = o.optString("address"),
                name = o.optString("nickname").ifEmpty { o.optString("name") },
                avatar = o.optString("avatar_path", "").ifEmpty { null },
                unread = o.optInt("unread_count", 0),
            )
        }
    } ?: emptyList()
}.getOrDefault(emptyList())

private fun parseConversations(raw: String): List<ConvRow> = runCatching {
    val obj = JSONObject(raw)
    if (!obj.optBoolean("success", false)) return emptyList()
    obj.optJSONArray("conversations")?.let { arr ->
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ConvRow(
                address = o.optString("contact_address"),
                lastContent = o.optString("last_content"),
                lastTimestamp = o.optLong("last_timestamp", 0),
                unread = o.optInt("unread_count", 0),
            )
        }
    } ?: emptyList()
}.getOrDefault(emptyList())

private fun parseMessages(raw: String): List<ChatMsg> = runCatching {
    val obj = JSONObject(raw)
    if (!obj.optBoolean("success", false)) return emptyList()
    obj.optJSONArray("messages")?.let { arr ->
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ChatMsg(
                content = o.optString("content"),
                isSent = o.optBoolean("is_sent", false),
                status = o.optString("status"),
                timestamp = o.optLong("timestamp", 0) * 1000,
            )
        }
    } ?: emptyList()
}.getOrDefault(emptyList())

/** 微信式单一会话列表：联系人与历史会话按最近时间合并排序。 */
private fun mergeSessions(contacts: List<ContactRow>, convs: List<ConvRow>): List<SessionItem> {
    val convByAddr = convs.associateBy { it.address }
    val items = mutableListOf<SessionItem>()
    val seen = mutableSetOf<String>()
    contacts.forEach { c ->
        val conv = convByAddr[c.address]
        seen.add(c.address)
        items.add(
            SessionItem(
                address = c.address,
                name = c.name.ifEmpty { c.address.take(12) },
                avatar = c.avatar,
                preview = conv?.lastContent ?: "",
                timestamp = conv?.lastTimestamp ?: 0,
                unread = maxOf(c.unread, conv?.unread ?: 0),
            )
        )
    }
    convs.forEach { c ->
        if (!seen.contains(c.address)) {
            items.add(
                SessionItem(
                    address = c.address,
                    name = c.address.take(12) + "..",
                    avatar = null,
                    preview = c.lastContent,
                    timestamp = c.lastTimestamp,
                    unread = c.unread,
                )
            )
        }
    }
    return items.sortedByDescending { it.timestamp }
}

private val FMT_HM = SimpleDateFormat("HH:mm", Locale.getDefault())
private val FMT_FULL_TIME = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

private fun formatListTime(epochSec: Long, yesterdayLabel: String, listDatePattern: String): String {
    if (epochSec <= 0) return ""
    val cal = java.util.Calendar.getInstance()
    val todayStart = cal.apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    val t = epochSec * 1000
    return when {
        t >= todayStart -> FMT_HM.format(Date(t))
        t >= todayStart - 86400_000L -> "$yesterdayLabel ${FMT_HM.format(Date(t))}"
        else -> SimpleDateFormat(listDatePattern, Locale.getDefault()).format(Date(t))
    }
}

// ============================================================
//  Tab root: session list <-> chat screen
// ============================================================

@Composable
fun MessageContent() {
    var selected by remember { mutableStateOf<Pair<String, String>?>(null) }
    if (selected == null) {
        SessionList(onOpen = { addr, name -> selected = addr to name })
    } else {
        ChatScreen(
            contact = selected!!.first,
            name = selected!!.second,
            onBack = { selected = null },
        )
    }
}

// ============================================================
//  Session list
// ============================================================

@Composable
private fun SessionList(onOpen: (String, String) -> Unit) {
    val s = io.github.freewebmovement.zz.ui.i18n.LocalAppStrings.current
    val node = rememberFwmcNodeSnapshot()
    val running = node.running
    var sessions by remember { mutableStateOf<List<SessionItem>>(emptyList()) }

    suspend fun loadSessions() {
        val contacts = parseContacts(FwmcApi.getContacts())
        val convs = parseConversations(FwmcApi.getConversations())
        sessions = mergeSessions(contacts, convs)
    }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        loadSessions()
        while (true) {
            delay(4000)
            loadSessions()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(WxBg)) {
        if (!running) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(s.message.nodeNotRunning, color = TextSecondary, textAlign = TextAlign.Center)
            }
        } else if (sessions.isEmpty()) {
            EmptyHint(s.message.noSessions)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sessions, key = { it.address }) { s ->
                    SessionRow(s) { onOpen(s.address, s.name) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 72.dp)
                            .height(0.5.dp)
                            .background(LineColor),
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionRow(s: SessionItem, onClick: () -> Unit) {
    val str = io.github.freewebmovement.zz.ui.i18n.LocalAppStrings.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Avatar(name = s.name, dataUri = s.avatar, size = 48.dp)
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = s.name,
                    fontSize = 16.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(text = formatListTime(s.timestamp, str.message.yesterday, str.message.listDateFormat), fontSize = 11.sp, color = TextMuted)
            }
            Text(
                text = s.preview.ifEmpty { str.message.startChat },
                fontSize = 13.sp,
                color = TextSecondary,
                maxLines = 1,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Box(modifier = Modifier.padding(start = 8.dp)) { UnreadBadge(s.unread) }
    }
}

// ============================================================
//  Chat screen
// ============================================================

private val EMOJIS = listOf(
    "😀","😁","😂","🤣","😊","😍","😘","😜","🤔","😎",
    "😢","😭","😡","🥳","🤗","😴","🙄","😏","🤝","👍",
    "👎","👏","🙏","💪","🔥","❤️","💔","✨","🎉","🎁",
    "🌹","🍀","☀️","🌙","⚡","🌈","🍕","🍺","☕","⚽",
)

private sealed class ChatEntry {
    data class Time(val text: String) : ChatEntry()
    data class Msg(val msg: ChatMsg) : ChatEntry()
}

/** 消息间隔超过 5 分钟时插入居中时间标签（微信样式）。 */
private fun decorate(msgs: List<ChatMsg>): List<ChatEntry> {
    val out = mutableListOf<ChatEntry>()
    var last = 0L
    msgs.forEach { m ->
        if (out.isEmpty() || m.timestamp - last >= 5 * 60_000L) {
            out.add(ChatEntry.Time(FMT_FULL_TIME.format(Date(m.timestamp))))
        }
        out.add(ChatEntry.Msg(m))
        last = m.timestamp
    }
    return out
}

@Composable
fun ChatScreen(contact: String, name: String, onBack: () -> Unit) {
    val s = io.github.freewebmovement.zz.ui.i18n.LocalAppStrings.current
    var msgs by remember { mutableStateOf<List<ChatMsg>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var showEmoji by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val errorColor = MaterialTheme.colorScheme.error

    LaunchedEffect(contact, refresh) {
        msgs = parseMessages(FwmcApi.getChatMessages(contact))
    }
    LaunchedEffect(contact) {
        while (true) {
            delay(4000)
            msgs = parseMessages(FwmcApi.getChatMessages(contact))
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(WxBg).imePadding()) {
        // 微信式头部：白色、返回在左、标题居中
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "〈",
                fontSize = 22.sp,
                color = TextPrimary,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(horizontal = 16.dp, vertical = 2.dp),
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                )
            }
            Spacer(modifier = Modifier.size(width = 54.dp, height = 1.dp))
        }
        // messages
        val entries = remember(msgs) { decorate(msgs) }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            reverseLayout = true,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp),
        ) {
            if (entries.isEmpty()) {
                item { EmptyHint(s.message.noMessages) }
            }
            items(entries.asReversed()) { entry ->
                when (entry) {
                    is ChatEntry.Time -> CenterTime(entry.text)
                    is ChatEntry.Msg -> Bubble(entry.msg)
                }
            }
        }
        // emoji picker
        if (showEmoji) {
            Surface(color = CardBg) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(10),
                    modifier = Modifier.fillMaxWidth().height(160.dp).padding(6.dp),
                ) {
                    items(EMOJIS) { e ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clickable { input += e },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = e, fontSize = 20.sp)
                        }
                    }
                }
            }
        }
        // input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "😊",
                fontSize = 24.sp,
                modifier = Modifier
                    .clickable { showEmoji = !showEmoji }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
            )
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text(s.message.inputHint, color = TextMuted) },
                modifier = Modifier.weight(1f),
                maxLines = 4,
                shape = RoundedCornerShape(8.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = LineColor,
                    focusedBorderColor = AppTheme.preset.primary,
                ),
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text(s.message.send)
            }
        }
    }
}

@Composable
private fun CenterTime(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
        Text(text = text, fontSize = 11.sp, color = TextMuted)
    }
}

@Composable
private fun Bubble(msg: ChatMsg) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (msg.isSent) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 12.dp, topEnd = 12.dp,
                bottomStart = if (msg.isSent) 12.dp else 4.dp,
                bottomEnd = if (msg.isSent) 4.dp else 12.dp,
            ),
            color = if (msg.isSent) AppTheme.preset.bubble else CardBg,
        ) {
            Text(
                text = msg.content,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = TextPrimary,
                modifier = Modifier
                    .widthIn(max = 272.dp)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
internal fun AddContactDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, address: String) -> Unit,
) {
    val s = io.github.freewebmovement.zz.ui.i18n.LocalAppStrings.current
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.message.addContact) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(s.message.name) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(s.message.fwmcAddress) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank() && address.isNotBlank()) onConfirm(name.trim(), address.trim()) },
            ) { Text(s.common.confirm, color = MaterialTheme.colorScheme.primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.common.cancel) }
        },
    )
}
