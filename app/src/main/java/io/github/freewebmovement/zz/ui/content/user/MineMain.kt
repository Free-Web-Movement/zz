package io.github.freewebmovement.zz.ui.content.user

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import io.github.freewebmovement.zz.ui.common.rememberFwmcNodeSnapshot
import io.github.freewebmovement.zz.R
import io.github.freewebmovement.zz.ui.common.MonoText
import io.github.freewebmovement.zz.ui.common.PageType
import io.github.freewebmovement.zz.ui.common.RowItem
import io.github.freewebmovement.zz.ui.i18n.AppLang
import io.github.freewebmovement.zz.ui.i18n.LocalAppStrings
import io.github.freewebmovement.zz.ui.common.avatarColor
import io.github.freewebmovement.zz.ui.common.formatAmount
import io.github.freewebmovement.zz.ui.theme.CardBg
import io.github.freewebmovement.zz.ui.theme.OnlineGreen
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
    Column(modifier = Modifier.background(WxBg).verticalScroll(rememberScrollState())) {
        val app = MainApplication.getApp()
        val settings = app.settings
        val s = LocalAppStrings.current
        with(settings) {
            val acctId = io.github.freewebmovement.zz.ui.content.FwmcSession.current?.first ?: ""
            val acctName = io.github.freewebmovement.zz.ui.content.FwmcSession.current?.second ?: ""
            var nickname by remember(acctId) { mutableStateOf("") }
            var intro by remember(acctId) { mutableStateOf("") }
            var imageUri by remember(acctId) { mutableStateOf<Uri?>(null) }
            val activity = LocalContext.current as? androidx.activity.ComponentActivity
            val resumeScope = androidx.compose.runtime.rememberCoroutineScope()
            androidx.compose.runtime.DisposableEffect(activity, acctId) {
                val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
                    if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME && acctId.isNotEmpty()) {
                        mineRefreshSignal++
                        resumeScope.launch { io.github.freewebmovement.zz.ui.content.FwmcSession.refresh() }
                    }
                }
                activity?.lifecycle?.addObserver(obs)
                onDispose { activity?.lifecycle?.removeObserver(obs) }
            }
            val ctx2 = LocalContext.current
            androidx.compose.runtime.LaunchedEffect(acctId, mineRefreshSignal) {
                if (acctId.isEmpty()) return@LaunchedEffect
                runCatching {
                    val p = JSONObject(rs.zz.coin.FwmcApi.getProfile(acctId))
                    val prof = p.optJSONObject("profile")
                    if (prof != null) {
                        val nm = prof.optString("nickname").ifEmpty { prof.optString("name") }
                        if (nm.isNotEmpty()) nickname = nm
                        intro = prof.optString("notes").ifEmpty { intro }
                        val av = prof.optString("avatar_path")
                        if (av.startsWith("data:image/")) {
                            imageUri = AvatarLocalStore.fromDataUrl(ctx2, acctId, av)
                        }
                    }
                }
            }

            // ── 一、账号与钱包 ──
            SectionTitle(s.mine.accountRow)

            // 头像/昵称/签名
            ProfileCard(acctId, nickname, intro, imageUri, updatePage)

            // 钱包地址/节点号/身份号
            PeerIdCard()

            // 帐号管理
            RowItem2(label = s.mine.accountMgmt, icon = R.drawable.ic_account, trailing = {
                val cur = io.github.freewebmovement.zz.ui.content.FwmcSession.current
                Text(cur?.second?.ifEmpty { s.mine.unnamed } ?: s.mine.unnamed, fontSize = 13.sp, color = TextSecondary)
                Text("  ›", color = TextMuted)
            }, onClick = { updatePage(PageType.MineAccounts) })

            // 钱包管理
            RowItem2(label = s.wallets.title, icon = R.drawable.ic_wallet, trailing = {
                Text(s.mine.multiWallet + "  ›", fontSize = 13.sp, color = TextSecondary)
            }, onClick = { updatePage(PageType.MineWallet) })

            // ── 二、节点信息（资源与服务） ──
            SectionTitle(s.mine.currentNodeWeight)

            // 节点配置
            FwmcConfigRow(updatePage)

            // 服务器
            ServerRow(updatePage)

            // 静态服务器配置
            StaticFileRow(updatePage)

            // 资源与权重配置
            WeightsRow(updatePage)

            // 权重对照表
            WeightTableRow(updatePage)

            // 节点连接情况
            ConnectionsRow(updatePage)

            // ── 三、设置 ──
            SectionTitle(s.settings.title)

            // 语言设置
            LanguageRow()

            // 主题设置
            ThemeRow()

            ThemeDialogHost()
            LanguageDialogHost()
        }
    }
}

/** 分区标题。 */
@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextMuted,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

/** 头像/昵称/签名卡片。 */
@Composable
private fun ProfileCard(
    acctId: String,
    nickname: String,
    intro: String,
    imageUri: Uri?,
    updatePage: (value: PageType) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg)
            .clickable(onClick = { updatePage(PageType.MineProfile) })
            .padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        val s = LocalAppStrings.current
        val ctx = LocalContext.current
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        val avatarPicker = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
        ) { picked: Uri? ->
            if (picked == null) return@rememberLauncherForActivityResult
            scope.launch {
                val bytes = readJpegBytes(ctx, picked, maxDim = 512)
                if (bytes != null) {
                    val ok = runCatching {
                        JSONObject(rs.zz.coin.FwmcApi.setAvatarFor(acctId, bytes)).optBoolean("success", false)
                    }.getOrDefault(false)
                    if (ok) {
                        io.github.freewebmovement.zz.ui.content.user.AvatarLocalStore.saveJpeg(ctx, acctId, bytes)
                        mineRefreshSignal++
                    } else {
                        android.widget.Toast.makeText(ctx, s.profile.avatarUploadFailed, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        Box(modifier = Modifier.clickable {
            avatarPicker.launch(androidx.activity.result.PickVisualMediaRequest(
                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
        }) {
            if (!imageUri?.toString().isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(imageUri).build(),
                    contentDescription = s.profile.avatar,
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.ic_default_avatar),
                    error = painterResource(id = R.drawable.ic_default_avatar),
                    modifier = Modifier.size(60.dp).clip(CircleShape),
                )
            } else {
                Box(
                    modifier = Modifier.size(60.dp).clip(CircleShape).background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_default_avatar),
                        contentDescription = s.profile.avatar,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(60.dp).clip(CircleShape),
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                text = nickname.ifEmpty { s.mine.unnamed },
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Text(
                text = intro.ifEmpty { s.mine.editSignature },
                fontSize = 12.sp,
                color = if (intro.isEmpty()) TextMuted else TextSecondary,
                maxLines = 2,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right),
            contentDescription = s.fwmcProfile.title,
            tint = Color(0xFFC8C8C8),
        )
    }
}

/** 身份号展示框（Peer ID = 钱包地址/节点号/身份号）：完整地址多行显示，支持复制与二维码。 */
@Composable
private fun PeerIdCard() {
    val s = LocalAppStrings.current
    val peerId = io.github.freewebmovement.zz.ui.content.FwmcSession.current?.first ?: ""
    if (peerId.isEmpty()) return
    var showQr by remember { mutableStateOf(false) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = CardBg,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.mine.walletAddrHint, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                val clip = androidx.compose.ui.platform.LocalClipboardManager.current
                Icon(
                    painter = painterResource(id = R.drawable.ic_copy),
                    contentDescription = s.common.copy,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable {
                            clip.setText(androidx.compose.ui.text.AnnotatedString(peerId))
                            android.widget.Toast.makeText(ctx, s.common.copied, android.widget.Toast.LENGTH_SHORT).show()
                        },
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_scan_qrcode),
                    contentDescription = s.mine.qrCode,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .size(18.dp)
                        .clickable { showQr = true },
                )
            }
            Text(
                text = peerId,
                fontSize = 13.sp,
                color = TextPrimary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
    if (showQr) PeerIdShareDialog(peerId = peerId, onDismiss = { showQr = false })
}

/** 服务器入口。 */
@Composable
private fun ServerRow(updatePage: (value: PageType) -> Unit) {
    val s = LocalAppStrings.current
    val node = rememberFwmcNodeSnapshot()
    RowItem2(label = s.mine.serverRow, icon = R.drawable.ic_server, trailing = {
        Text(
            text = if (node.running) s.mine.runningTemplate.format(node.port) else s.mine.notRunning,
            fontSize = 13.sp,
            color = if (node.running) OnlineGreen else TextSecondary,
        )
        Text("  ›", color = TextMuted)
    }, onClick = { updatePage(PageType.MineServer) })
}

/** 静态服务器配置入口。 */
@Composable
private fun StaticFileRow(updatePage: (value: PageType) -> Unit) {
    val s = LocalAppStrings.current
    val app = MainApplication.getApp()
    val enabled = app.settings.network.staticFileEnabled
    RowItem2(label = s.mine.staticFileRow, icon = R.drawable.ic_mine_local_server_share, trailing = {
        Text(if (enabled) s.common.enabled else s.common.disabled, fontSize = 13.sp, color = TextSecondary)
        Text("  ›", color = TextMuted)
    }, onClick = { updatePage(PageType.MineStaticFile) })
}

/** 资源与权重配置入口。首页此处实时显示节点权重，点击进入详情。 */
@Composable
private fun WeightsRow(updatePage: (value: PageType) -> Unit) {
    val s = LocalAppStrings.current
    val node = rememberFwmcNodeSnapshot()
    var totalWeight by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(node.running) {
        while (node.running) {
            runCatching {
                val obj = JSONObject(FwmcApi.getWeights())
                if (obj.optBoolean("success", false)) {
                    totalWeight = obj
                        .optJSONObject("resources")
                        ?.optString("composite_weight")
                }
            }
            delay(5000)
        }
    }
    RowItem2(label = s.mine.weightsRow, icon = R.drawable.ic_briefcase, trailing = {
        Text(
            text = when {
                !node.running -> s.mine.notRunning
                totalWeight != null -> s.mine.weightTemplate.format(totalWeight)
                else -> ""
            },
            fontSize = 13.sp,
            color = if (node.running) OnlineGreen else TextSecondary,
        )
        Text("  ›", color = TextMuted)
    }, onClick = { updatePage(PageType.MineWeights) })
}

/** 节点连接情况入口。 */
@Composable
private fun ConnectionsRow(updatePage: (value: PageType) -> Unit) {
    val s = LocalAppStrings.current
    RowItem2(label = s.mine.connectionRow, icon = R.drawable.ic_server, trailing = {
        Text("  ›", color = TextMuted)
    }, onClick = { updatePage(PageType.MineConnections) })
}

/** fwmc 配置入口。 */
@Composable
private fun FwmcConfigRow(updatePage: (value: PageType) -> Unit) {
    val s = LocalAppStrings.current
    RowItem2(label = s.settings.nodeStatusAndConfig, icon = R.drawable.ic_settings, trailing = {
        Text("  ›", color = TextMuted)
    }, onClick = { updatePage(PageType.MineSettings) })
}

/** 主题设置入口。 */
@Composable
private fun ThemeRow() {
    val s = LocalAppStrings.current
    RowItem2(label = s.mine.themeRow, icon = R.drawable.ic_settings, trailing = {
        Text(io.github.freewebmovement.zz.ui.theme.localizedPresetLabel(io.github.freewebmovement.zz.ui.theme.AppTheme.index), fontSize = 13.sp, color = TextSecondary)
        Text("  ›", color = TextMuted)
    }, onClick = { showThemeDialog.value = true })
}

private val showThemeDialog = mutableStateOf(false)

/** 主题配色切换（绿/蓝/粉/紫/橙）。 */
@Composable
private fun ThemeDialogHost() {
    if (!showThemeDialog.value) return
    ThemeDialog(onDismiss = { showThemeDialog.value = false })
}

@Composable
fun ThemeDialog(onDismiss: () -> Unit) {
    val app = MainApplication.getApp()
    val s = LocalAppStrings.current
    androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(s.settings.theme) },
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
                                io.github.freewebmovement.zz.ui.theme.localizedPresetLabel(i),
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
                androidx.compose.material3.TextButton(onClick = onDismiss) { Text(s.common.close) }
            },
        )
}

private val showLanguageDialog = mutableStateOf(false)

/** 语言设置入口。 */
@Composable
private fun LanguageRow() {
    val s = LocalAppStrings.current
    RowItem2(label = s.settings.language, icon = R.drawable.ic_settings, trailing = {
        Text(languageLabel(s.settings), fontSize = 13.sp, color = TextSecondary)
        Text("  ›", color = TextMuted)
    }, onClick = { showLanguageDialog.value = true })
}

/** 语言切换弹窗。 */
@Composable
private fun LanguageDialogHost() {
    if (!showLanguageDialog.value) return
    LanguageDialog(onDismiss = { showLanguageDialog.value = false })
}

@Composable
fun LanguageDialog(onDismiss: () -> Unit) {
    val app = MainApplication.getApp()
    val s = LocalAppStrings.current
    val options = listOf(
        Triple(0, s.settings.languageFollowSystem, "System"),
        Triple(1, s.settings.languageZh, "简体中文"),
        Triple(2, s.settings.languageEn, "English"),
    )
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.settings.language) },
        text = {
            Column {
                options.forEach { (mode, label, _) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                AppLang.select(mode, app.preference)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                    ) {
                        Text(label, fontSize = 15.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                        if (AppLang.mode == mode) Text("✓", color = TextSecondary)
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text(s.common.close) }
        },
    )
}

private fun languageLabel(st: io.github.freewebmovement.zz.ui.i18n.SettingsStrings): String = when (AppLang.mode) {
    1 -> st.languageZh
    2 -> st.languageEn
    else -> st.languageFollowSystem
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
    val s = LocalAppStrings.current
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
                Text(text = s.mine.walletRow.format(walletCount), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(
                    text = "${formatAmount(balance)} ZZ",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(text = s.mine.manageCreate + "  ›", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

/** 权重对照表导航行。 */
@Composable
private fun WeightTableRow(updatePage: (value: PageType) -> Unit) {
    val s = LocalAppStrings.current
    RowItem2(label = s.mine.weightTableRow, icon = R.drawable.ic_briefcase, trailing = {
        Text("  ›", color = TextMuted)
    }, onClick = { updatePage(PageType.MineWeightTable) })
}

/** 权重对照表子页。 */
@Composable
fun WeightTableScreen(onBack: () -> Unit = {}) {
    val s = LocalAppStrings.current
    val m = s.mine
    val rows = listOf(
        Triple(m.wtPublicIpv4, m.wtPublicIpv4Formula, m.wtPublicIpv4Desc),
        Triple(m.wtPrivateIpv4, m.wtPrivateIpv4Formula, m.wtPrivateIpv4Desc),
        Triple(m.wtPublicIpv6, m.wtPublicIpv6Formula, m.wtPublicIpv6Desc),
        Triple(m.wtStorage, m.wtStorageFormula, m.wtStorageDesc),
        Triple(m.wtBandwidth, m.wtBandwidthFormula, m.wtBandwidthDesc),
        Triple(m.wtCpu, m.wtCpuFormula, m.wtCpuDesc),
        Triple(m.wtMemory, m.wtMemoryFormula, m.wtMemoryDesc),
        Triple(m.wtApi, m.wtApiFormula, m.wtApiDesc),
        Triple(m.wtFloor, m.wtFloorFormula, m.wtFloorDesc),
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(io.github.freewebmovement.zz.ui.theme.WxBg)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(io.github.freewebmovement.zz.ui.theme.CardBg)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.common.back)
            }
            Text(s.mine.weightTableRow, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }

        io.github.freewebmovement.zz.ui.common.SectionCard(title = s.mine.weightFormula) {
            rows.forEachIndexed { i, (name, formula, desc) ->
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
                        Text(formula, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(desc, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
                }
                if (i < rows.lastIndex) {
                    io.github.freewebmovement.zz.ui.common.Divider()
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
