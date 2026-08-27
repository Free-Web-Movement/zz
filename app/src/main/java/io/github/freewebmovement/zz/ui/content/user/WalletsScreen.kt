package io.github.freewebmovement.zz.ui.content.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.freewebmovement.zz.ui.common.EmptyHint
import io.github.freewebmovement.zz.ui.common.MonoText
import io.github.freewebmovement.zz.ui.common.formatAmount
import io.github.freewebmovement.zz.ui.theme.AppTheme
import io.github.freewebmovement.zz.ui.theme.TextMuted
import io.github.freewebmovement.zz.ui.theme.TextPrimary
import io.github.freewebmovement.zz.ui.theme.TextSecondary
import io.github.freewebmovement.zz.ui.theme.WxBg
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import rs.zz.coin.FwmcApi

private data class WalletRow(
    val name: String,
    val address: String,
    val bound: Boolean,
)

/** 助记词语言选项（bip39 all-languages 子集，含用户要求的英/中）。 */
private val MNEMONIC_LANGUAGES = listOf(
    "english" to "English 英语",
    "chinese_simplified" to "简体中文",
    "chinese_traditional" to "繁體中文",
    "japanese" to "日本語",
    "korean" to "한국어",
    "spanish" to "Español",
    "french" to "Français",
    "italian" to "Italiano",
    "czech" to "Čeština",
    "portuguese" to "Português",
)

/**
 * 多钱包管理页：列表（绑定主钱包置顶）/ 创建（助记词语言可选 + 抄写确认）/
 * 删除（非绑定）/ 设为绑定钱包（重启节点生效）。
 */
@Composable
fun WalletsScreen(onBack: () -> Unit = {}) {
    val s = io.github.freewebmovement.zz.ui.i18n.LocalAppStrings.current
    val scope = rememberCoroutineScope()
    var wallets by remember { mutableStateOf<List<WalletRow>>(emptyList()) }
    var balances by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var showCreate by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<WalletRow?>(null) }
    var bindTarget by remember { mutableStateOf<WalletRow?>(null) }
    var mnemonicBackup by remember { mutableStateOf<Pair<String, String>?>(null) } // name to words
    var refresh by remember { mutableIntStateOf(0) }

    suspend fun load() {
        runCatching {
            val o = JSONObject(FwmcApi.listWallets())
            wallets = o.optJSONArray("wallets")?.map { it as JSONObject }?.map {
                WalletRow(it.optString("name"), it.optString("address"), it.optBoolean("bound"))
            } ?: emptyList()
        }
        val b = mutableMapOf<String, Long>()
        wallets.forEach { w ->
            runCatching {
                b[w.address] = JSONObject(FwmcApi.getBalance(w.address)).optLong("balance", 0)
            }
        }
        balances = b
    }

    LaunchedEffect(refresh) { load() }
    LaunchedEffect(Unit) {
        while (true) {
            delay(8000)
            load()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(WxBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(io.github.freewebmovement.zz.ui.theme.CardBg).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.common.back)
            }
            Text(s.wallets.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
            Text("+ " + s.wallets.createWallet, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp,
                modifier = Modifier.clickable { showCreate = true })
        }
        if (wallets.isEmpty()) {
            EmptyHint(s.wallets.noneTitle)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(wallets, key = { it.name }) { w ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(w.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
                                if (w.bound) {
                                    Surface(color = AppTheme.preset.primary, shape = RoundedCornerShape(4.dp)) {
                                        Text(s.wallets.bound, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            // 地址完整可见：放不下自动换行 + 复制按钮
                            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(top = 6.dp)) {
                                SelectionContainer {
                                    MonoText(
                                        text = w.address,
                                        fontSize = 10,
                                        color = TextSecondary,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                }
                                val clip = androidx.compose.ui.platform.LocalClipboardManager.current
                                val ctx = androidx.compose.ui.platform.LocalContext.current
                                androidx.compose.material3.Icon(
                                    painter = androidx.compose.ui.res.painterResource(io.github.freewebmovement.zz.R.drawable.ic_copy),
                                    contentDescription = s.common.copy,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .size(18.dp)
                                        .clickable {
                                            clip.setText(androidx.compose.ui.text.AnnotatedString(w.address))
                                            android.widget.Toast.makeText(ctx, s.common.copied, android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                )
                            }
                            Text(
                                "${formatAmount(balances[w.address] ?: 0L)} ZZ",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                if (!w.bound) {
                                    Text(s.wallets.setBinding, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp,
                                        modifier = Modifier.clickable { bindTarget = w })
                                    Text(s.wallets.delete, color = MaterialTheme.colorScheme.error, fontSize = 13.sp,
                                        modifier = Modifier.clickable { deleteTarget = w })
                                }
                            }
                        }
                    }
                }
                item { EmptyHint(s.wallets.bindNote) }
            }
        }
    }

    // ---- 创建钱包对话框 ----
    if (showCreate) {
        CreateWalletDialog(
            onDismiss = { showCreate = false },
            onCreated = { name, mnemonic ->
                showCreate = false
                mnemonicBackup = name to mnemonic
                refresh++
            },
        )
    }

    // ---- 助记词抄写确认页 ----
    mnemonicBackup?.let { (name, words) ->
        MnemonicConfirmDialog(name = name, words = words, onDone = { mnemonicBackup = null })
    }

    // ---- 删除确认 ----
    deleteTarget?.let { w ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(s.wallets.deleteWallet) },
            text = { Text(s.wallets.deleteConfirmTemplate.format(w.name)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { runCatching { FwmcApi.deleteWallet(w.name) }; deleteTarget = null; refresh++ }
                }) { Text(s.wallets.delete, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(s.wallets.cancel) } },
        )
    }

    // ---- 绑定确认 ----
    bindTarget?.let { w ->
        AlertDialog(
            onDismissRequest = { bindTarget = null },
            title = { Text(s.wallets.setBindingButton) },
            text = { Text(s.wallets.bindConfirmTemplate.format(w.name)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        runCatching { FwmcApi.bindWallet(w.name) }
                        val ctx = io.github.freewebmovement.android.noui.MyApp.getContext()
                        io.github.freewebmovement.android.noui.FwmcService.stop(ctx)
                        kotlinx.coroutines.delay(500)
                        io.github.freewebmovement.android.noui.FwmcService.start(ctx)
                        bindTarget = null; refresh++
                    }
                }) { Text(s.wallets.bindAndRestart, color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = { TextButton(onClick = { bindTarget = null }) { Text(s.wallets.restartLater, color = TextSecondary) } },
        )
    }
}

@Composable
private fun CreateWalletDialog(onDismiss: () -> Unit, onCreated: (String, String) -> Unit) {
    val s = io.github.freewebmovement.zz.ui.i18n.LocalAppStrings.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var lang by remember { mutableStateOf(MNEMONIC_LANGUAGES[0]) }
    var langMenu by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.wallets.createWallet) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(s.wallets.walletName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(modifier = Modifier.padding(top = 12.dp)) {
                    OutlinedTextField(
                        value = lang.second,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(s.wallets.mnemonicLang) },
                        trailingIcon = { TextButton(onClick = { langMenu = !langMenu }) { Text(if (langMenu) "▲" else "▼") } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DropdownMenu(expanded = langMenu, onDismissRequest = { langMenu = false }) {
                        MNEMONIC_LANGUAGES.forEach { l ->
                            DropdownMenuItem(
                                text = { Text(l.second) },
                                onClick = { lang = l; langMenu = false },
                            )
                        }
                    }
                }
                if (err.isNotEmpty()) Text(err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        val r = runCatching { JSONObject(FwmcApi.createWallet(name.trim(), lang.first)) }
                            .getOrDefault(JSONObject())
                        if (r.optBoolean("success")) {
                            onCreated(r.optString("name"), r.optString("mnemonic"))
                        } else {
                            err = r.optString("error", s.wallets.createFailed)
                        }
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) { Text(s.wallets.generateMnemonic) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.wallets.cancel) } },
    )
}

@Composable
private fun MnemonicConfirmDialog(name: String, words: String, onDone: () -> Unit) {
    val s = io.github.freewebmovement.zz.ui.i18n.LocalAppStrings.current
    val wordList = words.split(' ', '　').filter { it.isNotBlank() }
    AlertDialog(
        onDismissRequest = { /* 必须显式确认 */ },
        title = { Text(s.wallets.backupNote.format(name)) },
        text = {
            Column {
                Text(s.wallets.backupMnemonicTemplate.format(wordList.size), fontSize = 13.sp, color = TextSecondary)
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    wordList.chunked(4).forEachIndexed { rowIdx, rowWords ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            rowWords.forEachIndexed { i, wd ->
                                Surface(shape = RoundedCornerShape(6.dp), color = WxBg) {
                                    Text("${rowIdx * 4 + i + 1}. $wd", fontSize = 13.sp, color = TextPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) { Text(s.wallets.backedUpCopied) }
        },
    )
}

private inline fun <T> JSONArray.map(transform: (Any) -> T): List<T> =
    (0 until length()).map { transform(get(it)) }
