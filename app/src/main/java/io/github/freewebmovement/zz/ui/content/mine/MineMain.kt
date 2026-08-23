package io.github.freewebmovement.zz.ui.content.mine

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.R
import io.github.freewebmovement.zz.ui.common.MonoText
import io.github.freewebmovement.zz.ui.common.PageType
import io.github.freewebmovement.zz.ui.common.RowItem
import io.github.freewebmovement.zz.ui.common.avatarColor
import io.github.freewebmovement.zz.ui.common.formatAmount
import io.github.freewebmovement.zz.ui.theme.CardBg
import io.github.freewebmovement.zz.ui.theme.TextMuted
import io.github.freewebmovement.zz.ui.theme.TextPrimary
import io.github.freewebmovement.zz.ui.theme.TextSecondary
import io.github.freewebmovement.zz.ui.theme.WxBg
import io.github.freewebmovement.zz.ui.theme.AppTheme

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import rs.zz.coin.FwmcApi


@Composable
fun MineMain(updatePage: (value: PageType) -> Unit) {
    Column(modifier = Modifier.background(WxBg)) {
        val app = MainApplication.getApp()
        val settings = app.settings
        with(settings) {
            val acctName = io.github.freewebmovement.zz.ui.content.FwmcSession.current?.second ?: ""
            var nickname by remember(acctName) { mutableStateOf(acctName.ifEmpty { settings.profile.nickname }) }
            var intro by remember { mutableStateOf(settings.profile.intro) }
            val imageUri by remember {
                mutableStateOf<Uri?>(
                    settings.profile.imageUri.takeUnless { it.isEmpty() || it == "null" }?.let { Uri.parse(it) }
                )
            }
            if (nickname == "") {
                nickname = stringResource(R.string.tab_mine_nickname)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBg)
                    .clickable(onClick = { updatePage(PageType.MineProfile) })
                    .padding(horizontal = 16.dp, vertical = 18.dp),
            ) {
                val ctx = LocalContext.current
                val scope = androidx.compose.runtime.rememberCoroutineScope()
                val avatarPicker = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
                ) { picked: Uri? ->
                    if (picked == null) return@rememberLauncherForActivityResult
                    // 持久化读取权限：照片选择器授权默认随进程结束失效，否则重启后无法加载
                    runCatching {
                        ctx.contentResolver.takePersistableUriPermission(
                            picked,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    }
                    settings.profile.imageUri = picked.toString()
                    scope.launch {
                        runCatching {
                            readJpegBytes(ctx, picked, maxDim = 512)?.let { FwmcApi.setAvatar(it) }
                        }
                    }
                }
                Box(modifier = Modifier.clickable {
                    avatarPicker.launch(androidx.activity.result.PickVisualMediaRequest(
                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    if (!imageUri?.toString().isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(ctx)
                                .data(imageUri)
                                .build(),
                            contentDescription = stringResource(id = R.string.tab_mine_avatar),
                            contentScale = ContentScale.Crop,
                            // 加载失败/空时回落到默认头像，避免空白圆圈
                            placeholder = painterResource(id = R.drawable.ic_default_avatar),
                            error = painterResource(id = R.drawable.ic_default_avatar),
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0E0E0)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_default_avatar),
                                contentDescription = stringResource(id = R.string.tab_mine_avatar),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(60.dp).clip(CircleShape),
                            )
                        }
                    }
                    // 相机角标：提示可上传头像
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(AppTheme.preset.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("＋", color = Color.White, fontSize = 13.sp)
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(
                        text = nickname,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                        MonoText(
                            text = io.github.freewebmovement.zz.ui.content.FwmcSession.current?.first ?: "",
                            fontSize = 11,
                            color = TextMuted,
                            maxLines = 1,
                        )
                        val clipId = androidx.compose.ui.platform.LocalClipboardManager.current
                        Icon(
                            painter = painterResource(id = R.drawable.ic_copy),
                            contentDescription = "copy",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(16.dp)
                                .clickable {
                                    clipId.setText(androidx.compose.ui.text.AnnotatedString(
                                        io.github.freewebmovement.zz.ui.content.FwmcSession.current?.first ?: ""))
                                    android.widget.Toast.makeText(ctx, ctx.getString(R.string.copied), android.widget.Toast.LENGTH_SHORT).show()
                                },
                        )
                    }
                    if (intro.isNotEmpty()) {
                        Text(
                            text = intro,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 2,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = stringResource(R.string.tab_mine_profile),
                    tint = Color(0xFFC8C8C8),
                )
            }
        }

        WalletEntryCard(onClick = { updatePage(PageType.MineWallet) })

        RowItem2(label = "帐号管理", icon = R.drawable.ic_account, trailing = {
            val cur = io.github.freewebmovement.zz.ui.content.FwmcSession.current
            Text(cur?.second?.ifEmpty { "未登录" } ?: "未登录", fontSize = 13.sp, color = TextSecondary)
            Text("  ›", color = TextMuted)
        }, onClick = { updatePage(PageType.MineAccounts) })

        RowItem2(label = "钱包管理", icon = R.drawable.ic_wallet, trailing = {
            Text("多钱包  ›", fontSize = 13.sp, color = TextSecondary)
        }, onClick = { updatePage(PageType.MineWallet) })

        SettingsRow()
    }
}

/** 设置行：主题配色等应用外观。 */
@Composable
private fun SettingsRow() {
    RowItem2(label = "设置", icon = R.drawable.ic_settings, trailing = {
        Text(AppTheme.preset.label, fontSize = 13.sp, color = TextSecondary)
        Text("  ›", color = TextMuted)
    }, onClick = { showThemeDialog.value = true })
    ThemeDialogHost()
}

private val showThemeDialog = mutableStateOf(false)

/** 主题配色切换（绿/蓝/粉/紫/橙）。 */
@Composable
private fun ThemeDialogHost() {
    if (!showThemeDialog.value) return
    ThemeDialog(onDismiss = { showThemeDialog.value = false })
}

@Composable
private fun ThemeDialog(onDismiss: () -> Unit) {
    val app = MainApplication.getApp()
    androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("主题配色") },
            text = {
                Column {
                    io.github.freewebmovement.zz.ui.theme.THEMES.forEachIndexed { i, t ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    AppTheme.select(i, app.preference)
                                    onDismiss()
                                }
                                .padding(vertical = 10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(t.primary)
                                    .border(
                                        width = if (t == AppTheme.preset) 3.dp else 0.dp,
                                        color = Color(0xFFB0B0B0),
                                        shape = CircleShape,
                                    ),
                            )
                            Text(
                                t.label,
                                fontSize = 15.sp,
                                color = TextPrimary,
                                modifier = Modifier.padding(start = 12.dp).weight(1f),
                            )
                            if (t == AppTheme.preset) Text("✓", color = AppTheme.preset.primary)
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = onDismiss) { Text("关闭") }
            },
        )
}

/** 简化行（label + 自定义尾部 + 点击），供主题行使用。 */
@Composable
private fun RowItem2(
    label: String,
    icon: Int = 0,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (icon != 0) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = AppTheme.preset.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(label, fontSize = 15.sp, color = TextPrimary, modifier = Modifier.weight(1f).padding(start = if (icon != 0) 12.dp else 0.dp))
        trailing()
    }
}

/** 钱包入口卡片：多钱包管理入口，显示钱包数量与绑定主钱包余额。 */
@Composable
private fun WalletEntryCard(onClick: () -> Unit) {
    var balance by remember { mutableLongStateOf(0L) }
    var walletCount by remember { mutableStateOf(0) }
    val node = io.github.freewebmovement.zz.ui.common.rememberFwmcNodeSnapshot()

    LaunchedEffect(node.running) {
        while (node.running) {
            runCatching {
                val obj = JSONObject(FwmcApi.getData())
                if (obj.optBoolean("success", false)) {
                    balance = obj.optLong("my_balance", 0)
                }
            }
            runCatching {
                val w = JSONObject(FwmcApi.listWallets())
                walletCount = w.optJSONArray("wallets")?.length() ?: 0
            }
            delay(5000)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Brush.horizontalGradient(listOf(AppTheme.preset.primary, AppTheme.preset.primaryDark)))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_wallet),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(30.dp),
            )
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(text = "我的钱包 · $walletCount 个", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(
                    text = "${formatAmount(balance)} ZZ",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(text = "管理 · 创建  ›", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
        }
    }
}
