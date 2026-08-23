package io.github.freewebmovement.zz.ui.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.freewebmovement.zz.ui.common.Avatar
import io.github.freewebmovement.zz.ui.common.EmptyHint
import io.github.freewebmovement.zz.ui.common.MonoText
import io.github.freewebmovement.zz.ui.common.SectionCard
import io.github.freewebmovement.zz.ui.common.UnreadBadge
import io.github.freewebmovement.zz.ui.common.rememberFwmcNodeSnapshot
import io.github.freewebmovement.zz.ui.theme.CardBg
import io.github.freewebmovement.zz.ui.theme.TextMuted
import io.github.freewebmovement.zz.ui.theme.TextPrimary
import io.github.freewebmovement.zz.ui.theme.TextSecondary

import kotlinx.coroutines.launch
import org.json.JSONObject
import rs.zz.coin.FwmcApi

/**
 * 联系人 tab: contact list with add/delete and direct chat entry.
 */
@Composable
fun ContactsContent() {
    var selectedChat by remember { mutableStateOf<Pair<String, String>?>(null) }
    if (selectedChat != null) {
        ChatScreen(
            contact = selectedChat!!.first,
            name = selectedChat!!.second,
            onBack = { selectedChat = null },
        )
    } else {
        ContactList(onOpenChat = { addr, name -> selectedChat = addr to name })
    }
}

@Composable
private fun ContactList(onOpenChat: (String, String) -> Unit) {
    val node = rememberFwmcNodeSnapshot()
    val running = node.running
    val scope = rememberCoroutineScope()
    var contacts by remember { mutableStateOf<List<ContactRow>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ContactRow?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    androidx.compose.runtime.LaunchedEffect(running, refresh) {
        if (!running) return@LaunchedEffect
        contacts = parseContacts(FwmcApi.getContacts())
    }

    Column(modifier = Modifier.fillMaxSize().background(io.github.freewebmovement.zz.ui.theme.WxBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(CardBg).padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "联系人", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary,
                modifier = Modifier.weight(1f))
            Text(
                text = "+ 添加",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { showAddDialog = true }.padding(6.dp),
            )
        }
        when {
            !running -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("fwmc 节点未运行，请在「网络」页启动节点", color = TextSecondary)
            }
            contacts.isEmpty() -> EmptyHint("暂无联系人，点击右上角添加")
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(contacts, key = { it.address }) { c ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardBg)
                            .clickable { onOpenChat(c.address, c.name.ifEmpty { c.address.take(12) }) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Avatar(name = c.name.ifEmpty { c.address }, dataUri = null, size = 42.dp)
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(
                                text = c.name.ifEmpty { "节点 ${c.address.take(8)}.." },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                maxLines = 1,
                            )
                            MonoText(text = c.address, fontSize = 11, color = TextMuted, maxLines = 1)
                        }
                        UnreadBadge(c.unread)
                        Text(
                            text = "删除",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier
                                .clickable { deleteTarget = c }
                                .padding(start = 10.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
                        )
                    }
                }
                item { Box(modifier = Modifier.padding(bottom = 12.dp)) }
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

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除联系人") },
            text = { Text("确定删除 ${target.name.ifEmpty { target.address.take(12) + ".." }} ？") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    scope.launch {
                        FwmcApi.deleteContact(target.address)
                        refresh++
                    }
                }) { Text("删除", color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            },
        )
    }
}
