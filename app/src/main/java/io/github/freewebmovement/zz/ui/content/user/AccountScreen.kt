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
    val s = io.github.freewebmovement.zz.ui.i18n.LocalAppStrings.current
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.common.back)
            }
            Text(s.account.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
            Text("+ " + s.account.addAccount, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp,
                modifier = Modifier.clickable { showAdd = true })
        }
        if (accounts.isEmpty()) {
            EmptyHint(s.account.noneTitle)
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
                                            Text(s.account.currentAccount, color = Color.White, fontSize = 11.sp, maxLines = 1)
                                        }
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                    Text(a.id, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                                    val clip = LocalClipboardManager.current
                                    val ctx = androidx.compose.ui.platform.LocalContext.current
                                    androidx.compose.material3.Icon(
                                        painter = androidx.compose.ui.res.painterResource(io.github.freewebmovement.zz.R.drawable.ic_copy),
                                        contentDescription = s.common.copy,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                            .size(15.dp)
                                            .clickable {
                                                clip.setText(AnnotatedString(a.id))
                                                Toast.makeText(ctx, s.common.copied, Toast.LENGTH_SHORT).show()
                                            },
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                if (isCurrent) {
                                    Text(
                                        s.account.profileAvatar,
                                        color = MaterialTheme.colorScheme.primary, fontSize = 13.sp,
                                        modifier = Modifier.padding(bottom = 8.dp).clickable {
                                            updatePage(io.github.freewebmovement.zz.ui.common.PageType.MineFwmcProfile)
                                        },
                                    )
                                }
                                Text(
                                    if (isCurrent) s.account.logout else s.account.switch,
                                    color = MaterialTheme.colorScheme.primary, fontSize = 13.sp,
                                    modifier = Modifier.clickable {
                                        if (isCurrent) {
                                            scope.launch { runCatching { FwmcApi.logout() }; FwmcSession.refresh() }
                                        } else authTarget = a to "switch"
                                    },
                                )
                                Row {
                                    Text(s.account.edit, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 8.dp).clickable {
                                            editAccount = a.id to a.name
                                            updatePage(io.github.freewebmovement.zz.ui.common.PageType.MineUserEdit)
                                        })
                                    Text(s.account.privateChatProtect, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp,
                                        modifier = Modifier.padding(start = 10.dp, top = 8.dp).clickable { pwProtectTarget = a })
                                    Text(s.account.repair, color = TextMuted, fontSize = 13.sp,
                                        modifier = Modifier.padding(start = 10.dp, top = 8.dp).clickable { authTarget = a to "repair" })
                                    Text(s.account.delete, color = MaterialTheme.colorScheme.error, fontSize = 13.sp,
                                        modifier = Modifier.padding(start = 10.dp, top = 8.dp).clickable { deleteTarget = a })
                                }
                            }
                        }
                    }
                }
                item { EmptyHint(s.account.accountNote) }
            }
        }
    }

    if (showAdd) {
        AddAccountDialog(onDismiss = { showAdd = false }, onAdded = { showAdd = false; refresh++ })
    }

    deleteTarget?.let { a ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(s.account.deleteAccount) },
            text = { Text(s.account.deleteConfirmTemplate.format(a.name, a.id.take(17))) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        runCatching { FwmcApi.deleteAccount(a.id) }
                        FwmcSession.refresh()
                        deleteTarget = null; refresh++
                    }
                }) { Text(s.account.delete, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(s.common.cancel) } },
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
            title = if (mode == "switch") s.account.switchTemplate.format(a.name) else s.account.repairTemplate.format(a.name),
            hint = if (mode == "switch") s.account.switchHint else s.account.repairHint,
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
    val s = io.github.freewebmovement.zz.ui.i18n.LocalAppStrings.current
    val scope = rememberCoroutineScope()
    var oldPw by remember { mutableStateOf("") }
    var newPw by remember { mutableStateOf("") }
    var confirmPw by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (account.hasPassword) s.account.modifyPassword else s.account.setPassword) },
        text = {
            Column {
                Text(s.account.passwordHint, fontSize = 12.sp, color = TextMuted)
                if (account.hasPassword) {
                    OutlinedTextField(
                        value = oldPw,
                        onValueChange = { oldPw = it },
                        label = { Text(s.account.oldPassword) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
                OutlinedTextField(
                    value = newPw,
                    onValueChange = { newPw = it },
                    label = { Text(if (account.hasPassword) s.account.newPassword else s.account.password) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = confirmPw,
                    onValueChange = { confirmPw = it },
                    label = { Text(s.account.confirmPassword) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                if (newPw.isEmpty()) { err = s.account.enterPassword; return@TextButton }
                if (newPw != confirmPw) { err = s.account.passwordMismatch; return@TextButton }
                busy = true
                scope.launch {
                    val r = runCatching {
                        JSONObject(FwmcApi.changePassword(account.id, oldPw, newPw))
                    }.getOrDefault(JSONObject())
                    busy = false
                    if (r.optBoolean("success")) onDone() else err = r.optString("error", s.account.setFailed)
                }
            }) { Text(s.account.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(s.common.cancel)
            }
        },
    )
}

@Composable
private fun AddAccountDialog(onDismiss: () -> Unit, onAdded: () -> Unit) {
    val s = io.github.freewebmovement.zz.ui.i18n.LocalAppStrings.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    var mnemonic by remember { mutableStateOf<String?>(null) }
    var newName by remember { mutableStateOf("") }

    if (mnemonic != null) {
        // 新钱包助记词备份提示
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { mnemonic = null; onDismiss() },
            title = { Text(s.account.backupMnemonicDone) },
            text = {
                Column {
                    Text(s.wallets.backupMnemonicTemplate.format(24), fontSize = 13.sp, color = TextMuted)
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
                TextButton(onClick = { mnemonic = null; onAdded() }) { Text(s.account.backedUp) }
            },
            dismissButton = { TextButton(onClick = { mnemonic = null; onDismiss() }) { Text(s.account.later) } },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.account.addAccount) },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text(s.account.accountName) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text(s.account.createNote, fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(top = 8.dp))
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
                        } else err = r.optString("error", s.account.createFailed)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) { Text(s.account.create) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.common.cancel) } },
    )
}

@Composable
private fun AuthDialog(title: String, hint: String, onDismiss: () -> Unit, onAuth: suspend (String) -> Boolean) {
    val s = io.github.freewebmovement.zz.ui.i18n.LocalAppStrings.current
    val scope = rememberCoroutineScope()
    var pw by remember { mutableStateOf("") }
    var err by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(hint, fontSize = 13.sp, color = TextSecondary)
                OutlinedTextField(pw, { pw = it; err = false }, label = { Text(s.account.password) },
                    isError = err,
                    visualTransformation = PasswordVisualTransformation(), singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                if (err) Text(s.account.passwordWrong, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = { scope.launch { if (!onAuth(pw)) err = true } },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) { Text(s.common.ok) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.common.cancel) } },
    )
}
