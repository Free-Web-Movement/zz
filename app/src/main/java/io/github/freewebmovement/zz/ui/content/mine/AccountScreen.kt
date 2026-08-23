package io.github.freewebmovement.zz.ui.content.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.sp
import android.widget.Toast
import io.github.freewebmovement.zz.ui.common.EmptyHint
import io.github.freewebmovement.zz.ui.content.FwmcSession
import io.github.freewebmovement.zz.ui.theme.AppTheme
import io.github.freewebmovement.zz.ui.theme.TextMuted
import io.github.freewebmovement.zz.ui.theme.TextPrimary
import io.github.freewebmovement.zz.ui.theme.TextSecondary
import io.github.freewebmovement.zz.ui.theme.WxBg
import kotlinx.coroutines.launch
import org.json.JSONObject
import rs.zz.coin.FwmcApi

private data class AcctRow(val id: String, val name: String)

/**
 * 帐号管理：当前帐号标识 / 添加 / 删除 / 修复（重新验证密码）。
 * 帐号为本地临时数字 ID；删除帐号不影响任何钱包。
 */
@Composable
fun AccountScreen(updatePage: (io.github.freewebmovement.zz.ui.common.PageType) -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var accounts by remember { mutableStateOf<List<AcctRow>>(emptyList()) }
    var refresh by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<AcctRow?>(null) }
    // id to mode: switch | repair
    var authTarget by remember { mutableStateOf<Pair<AcctRow, String>?>(null) }

    suspend fun load() {
        runCatching {
            val o = JSONObject(FwmcApi.listAccounts())
            accounts = o.optJSONArray("accounts")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val e = arr.getJSONObject(i)
                    AcctRow(e.optString("id"), e.optString("name"))
                }
            } ?: emptyList()
        }
    }

    LaunchedEffect(refresh) { load() }

    Column(modifier = Modifier.fillMaxSize().background(WxBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(io.github.freewebmovement.zz.ui.theme.CardBg)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("帐号管理", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
            Text("+ 添加帐号", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp,
                modifier = Modifier.clickable { showAdd = true })
        }
        if (accounts.isEmpty()) {
            EmptyHint("暂无帐号\n点击右上角「添加帐号」")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(accounts, key = { it.id }) { a ->
                    val isCurrent = FwmcSession.current?.first == a.id
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(a.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    if (isCurrent) {
                                        Surface(
                                            color = AppTheme.preset.primary,
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.padding(start = 8.dp),
                                        ) {
                                            Text("当前帐号", color = Color.White, fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                    Text(a.id, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                                    val clip = LocalClipboardManager.current
                                    val ctx = androidx.compose.ui.platform.LocalContext.current
                                    androidx.compose.material3.Icon(
                                        painter = androidx.compose.ui.res.painterResource(io.github.freewebmovement.zz.R.drawable.ic_copy),
                                        contentDescription = "copy",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                            .size(15.dp)
                                            .clickable {
                                                clip.setText(AnnotatedString(a.id))
                                                Toast.makeText(ctx, ctx.getString(io.github.freewebmovement.zz.R.string.copied), Toast.LENGTH_SHORT).show()
                                            },
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                if (isCurrent) {
                                    Text(
                                        "个人资料·头像",
                                        color = MaterialTheme.colorScheme.primary, fontSize = 13.sp,
                                        modifier = Modifier.padding(bottom = 8.dp).clickable {
                                            updatePage(io.github.freewebmovement.zz.ui.common.PageType.MineFwmcProfile)
                                        },
                                    )
                                }
                                Text(
                                    if (isCurrent) "退出登录" else "切换",
                                    color = MaterialTheme.colorScheme.primary, fontSize = 13.sp,
                                    modifier = Modifier.clickable {
                                        if (isCurrent) {
                                            scope.launch { runCatching { FwmcApi.logout() }; FwmcSession.refresh() }
                                        } else authTarget = a to "switch"
                                    },
                                )
                                Row {
                                    Text("修复", color = TextMuted, fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 8.dp).clickable { authTarget = a to "repair" })
                                    Text("删除", color = MaterialTheme.colorScheme.error, fontSize = 13.sp,
                                        modifier = Modifier.padding(start = 16.dp, top = 8.dp).clickable { deleteTarget = a })
                                }
                            }
                        }
                    }
                }
                item { EmptyHint("帐号仅保存在本机，用于身份识别；\n删除帐号不影响钱包存留。") }
            }
        }
    }

    if (showAdd) {
        AddAccountDialog(onDismiss = { showAdd = false }, onAdded = { showAdd = false; refresh++ })
    }

    deleteTarget?.let { a ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除帐号") },
            text = { Text("确定删除帐号「${a.name}」（${a.id.take(17)}…）吗？\n\n钱包不受影响，可随时新建帐号。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        runCatching { FwmcApi.deleteAccount(a.id) }
                        FwmcSession.refresh()
                        deleteTarget = null; refresh++
                    }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } },
        )
    }

    authTarget?.let { (a, mode) ->
        AuthDialog(
            title = if (mode == "switch") "切换到「${a.name}」" else "修复「${a.name}」",
            hint = "输入该帐号的密码以${if (mode == "switch") "切换" else "验证并修复"}",
            onDismiss = { authTarget = null },
        ) { pw ->
            val r = runCatching { JSONObject(FwmcApi.login(a.id, pw)) }.getOrDefault(JSONObject())
            authTarget = null
            if (r.optBoolean("success")) {
                FwmcSession.refresh()
            }
            r.optBoolean("success")
        }
    }
}

@Composable
private fun AddAccountDialog(onDismiss: () -> Unit, onAdded: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加帐号") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("帐号名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(pw, { pw = it }, label = { Text("密码") },
                    visualTransformation = PasswordVisualTransformation(), singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                Text("将自动生成 24 位临时数字 ID（创建时间+随机数）", fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(top = 8.dp))
                if (err.isNotEmpty()) Text(err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        val r = runCatching { JSONObject(FwmcApi.createAccount(name.trim(), pw)) }.getOrDefault(JSONObject())
                        if (r.optBoolean("success")) {
                            FwmcSession.refresh(); onAdded()
                        } else err = r.optString("error", "创建失败")
                    }
                },
                enabled = name.isNotBlank() && pw.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun AuthDialog(title: String, hint: String, onDismiss: () -> Unit, onAuth: suspend (String) -> Boolean) {
    val scope = rememberCoroutineScope()
    var pw by remember { mutableStateOf("") }
    var err by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(hint, fontSize = 13.sp, color = TextSecondary)
                OutlinedTextField(pw, { pw = it; err = false }, label = { Text("密码") },
                    isError = err,
                    visualTransformation = PasswordVisualTransformation(), singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                if (err) Text("密码错误", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = { scope.launch { if (!onAuth(pw)) err = true } },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
