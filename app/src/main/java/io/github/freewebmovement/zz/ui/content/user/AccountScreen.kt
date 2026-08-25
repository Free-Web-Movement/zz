package io.github.freewebmovement.zz.ui.content.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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

private data class AcctRow(
    val id: String,
    val name: String,
    val hasPassword: Boolean = false,
    val avatarUri: android.net.Uri? = null,
)

/**
 * 帐号管理：当前帐号标识 / 添加 / 删除 / 修复（重新验证密码）。
 * 帐号为本地临时数字 ID；删除帐号不影响任何钱包。
 */
@Composable
fun AccountScreen(
    updatePage: (io.github.freewebmovement.zz.ui.common.PageType) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var accounts by remember { mutableStateOf<List<AcctRow>>(emptyList()) }
    var refresh by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<AcctRow?>(null) }
    var pwProtectTarget by remember { mutableStateOf<AcctRow?>(null) }
    // id to mode: switch | repair
    var authTarget by remember { mutableStateOf<Pair<AcctRow, String>?>(null) }
    val listCtx = androidx.compose.ui.platform.LocalContext.current

    suspend fun load() {
        val ctx = listCtx
        runCatching {
            val o = JSONObject(FwmcApi.listAccounts())
            accounts = o.optJSONArray("accounts")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val e = arr.getJSONObject(i)
                    val id = e.optString("id")
                    // 每行读取各自身份资料的头像，并转存为本地文件供界面可靠渲染
                    val avatarUri = runCatching {
                        val av = JSONObject(FwmcApi.getProfile(id)).optJSONObject("profile")
                            ?.optString("avatar_path", "") ?: ""
                        if (av.startsWith("data:image/")) AvatarLocalStore.fromDataUrl(ctx, id, av) else null
                    }.getOrNull()
                    AcctRow(id, e.optString("name"), e.optBoolean("has_password", false), avatarUri)
                }
            } ?: emptyList()
        }
    }

    LaunchedEffect(refresh) { load() }
    LaunchedEffect(accountsRefreshSignal) { load() }

    Column(modifier = Modifier.fillMaxSize().background(WxBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(io.github.freewebmovement.zz.ui.theme.CardBg)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
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
                            // 头像：有则显示本地文件，无则默认人形
                            if (a.avatarUri != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                        .data(a.avatarUri)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    error = painterResource(io.github.freewebmovement.zz.R.drawable.ic_default_avatar),
                                    modifier = Modifier.size(44.dp).clip(CircleShape),
                                )
                            } else {
                                Image(
                                    painter = painterResource(io.github.freewebmovement.zz.R.drawable.ic_default_avatar),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(44.dp).clip(CircleShape),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Column {
                                    Text(
                                        a.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary,
                                        maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    )
                                    if (isCurrent) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 3.dp)
                                                .background(AppTheme.preset.primary, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                        ) {
                                            Text("当前帐号", color = Color.White, fontSize = 11.sp, maxLines = 1)
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
                                    Text("编辑", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 8.dp).clickable {
                                            editAccount = a.id to a.name
                                            updatePage(io.github.freewebmovement.zz.ui.common.PageType.MineUserEdit)
                                        })
                                    Text("私聊保护", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp,
                                        modifier = Modifier.padding(start = 10.dp, top = 8.dp).clickable { pwProtectTarget = a })
                                    Text("修复", color = TextMuted, fontSize = 13.sp,
                                        modifier = Modifier.padding(start = 10.dp, top = 8.dp).clickable { authTarget = a to "repair" })
                                    Text("删除", color = MaterialTheme.colorScheme.error, fontSize = 13.sp,
                                        modifier = Modifier.padding(start = 10.dp, top = 8.dp).clickable { deleteTarget = a })
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

    pwProtectTarget?.let { a ->
        SetPasswordDialog(
            account = a,
            onDismiss = { pwProtectTarget = null },
            onDone = { pwProtectTarget = null; refresh++ },
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
private fun SetPasswordDialog(
    account: AcctRow,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var oldPw by remember { mutableStateOf("") }
    var newPw by remember { mutableStateOf("") }
    var confirmPw by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (account.hasPassword) "私聊保护 · 修改密码" else "私聊保护 · 设置密码") },
        text = {
            Column {
                Text("设置密码后，查看该帐号的私聊需要输入密码。帐号默认无密码，可随时清除。", fontSize = 12.sp, color = TextMuted)
                if (account.hasPassword) {
                    OutlinedTextField(
                        value = oldPw,
                        onValueChange = { oldPw = it },
                        label = { Text("原密码") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
                OutlinedTextField(
                    value = newPw,
                    onValueChange = { newPw = it },
                    label = { Text(if (account.hasPassword) "新密码" else "密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = confirmPw,
                    onValueChange = { confirmPw = it },
                    label = { Text("确认密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                if (newPw.isEmpty()) { err = "请输入密码"; return@TextButton }
                if (newPw != confirmPw) { err = "两次输入的密码不一致"; return@TextButton }
                busy = true
                scope.launch {
                    val r = runCatching {
                        JSONObject(FwmcApi.changePassword(account.id, oldPw, newPw))
                    }.getOrDefault(JSONObject())
                    busy = false
                    if (r.optBoolean("success")) onDone() else err = r.optString("error", "设置失败")
                }
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (account.hasPassword) "取消" else "取消")
            }
        },
    )
}

@Composable
private fun AddAccountDialog(onDismiss: () -> Unit, onAdded: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    var mnemonic by remember { mutableStateOf<String?>(null) }
    var newName by remember { mutableStateOf("") }

    if (mnemonic != null) {
        // 新钱包助记词备份提示
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { mnemonic = null; onDismiss() },
            title = { Text("帐号已创建 · 请备份助记词") },
            text = {
                Column {
                    Text("新帐号已绑定到新钱包。请抄下助记词（24 词），丢失无法恢复。", fontSize = 13.sp, color = TextMuted)
                    Text(
                        mnemonic!!,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                    )
                    if (err.isNotEmpty()) Text(err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = { mnemonic = null; onAdded() }) { Text("我已备份") }
            },
            dismissButton = { TextButton(onClick = { mnemonic = null; onDismiss() }) { Text("稍后") } },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加帐号") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("帐号名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("创建帐号将同时创建其绑定的钱包（Peer ID 身份）。", fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(top = 8.dp))
                if (err.isNotEmpty()) Text(err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        val r = runCatching { JSONObject(FwmcApi.createWallet(name.trim(), "english")) }.getOrDefault(JSONObject())
                        if (r.optBoolean("success")) {
                            newName = r.optString("name", name.trim())
                            mnemonic = r.optString("mnemonic", "")
                            FwmcSession.refresh()
                        } else err = r.optString("error", "创建失败")
                    }
                },
                enabled = name.isNotBlank(),
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
