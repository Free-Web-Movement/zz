package io.github.freewebmovement.zz.ui.content.user

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import io.github.freewebmovement.android.noui.TrojanService
import io.github.freewebmovement.peer.system.TrojanServer
import io.github.freewebmovement.peer.system.TrojanSetting
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.ui.common.Divider
import io.github.freewebmovement.zz.ui.common.EmptyHint
import io.github.freewebmovement.zz.ui.common.KeyValueRow
import io.github.freewebmovement.zz.ui.common.SectionCard
import io.github.freewebmovement.zz.ui.common.StatusText
import io.github.freewebmovement.zz.ui.common.SubTabs
import io.github.freewebmovement.zz.ui.content.SubPageHeader
import io.github.freewebmovement.zz.ui.i18n.LocalAppStrings
import io.github.freewebmovement.zz.ui.i18n.TrojanStrings
import io.github.freewebmovement.zz.ui.theme.CardBg
import io.github.freewebmovement.zz.ui.theme.OnlineGreen
import io.github.freewebmovement.zz.ui.theme.TextMuted
import io.github.freewebmovement.zz.ui.theme.TextPrimary
import io.github.freewebmovement.zz.ui.theme.TextSecondary
import io.github.freewebmovement.zz.ui.theme.WxBg
import org.json.JSONObject
import rs.zz.coin.FwmcApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.net.URI
import java.net.URL
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 「服务 → Trojan 客户端」：多服务器管理 + 启停。
 *
 * 每个服务器条目都可以单独选择、编辑、删除和测试；启动时使用当前选中的
 * 服务器。设置、服务器信息、运行信息分区展示。
 */
@Composable
fun TrojanScreen(onBack: () -> Unit = {}) {
    val s = LocalAppStrings.current
    val t = s.trojan
    val sv = s.service
    val ctx = LocalContext.current
    val setting = MainApplication.getApp().settings.trojan
    val scope = rememberCoroutineScope()

    var servers by remember { mutableStateOf(setting.loadServers()) }
    var selectedId by remember { mutableStateOf(setting.currentServer()?.id) }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TrojanServer?>(null) }
    var deleteTarget by remember { mutableStateOf<TrojanServer?>(null) }
    var running by remember { mutableStateOf(TrojanService.running) }
    var vpnActive by remember { mutableStateOf(TrojanService.vpnActive) }
    var startedAt by remember { mutableLongStateOf(TrojanService.startedAt) }
    var error by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }
    var testResults by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var serverTesting by remember { mutableStateOf(false) }
    var serverLatency by remember { mutableStateOf<Long?>(null) }
    var googleTesting by remember { mutableStateOf(false) }
    var googleResult by remember { mutableStateOf<Pair<Boolean, Long>?>(null) }
    var retestTick by remember { mutableIntStateOf(0) }

    val vpnLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            TrojanService.start(ctx)
        } else {
            error = t.vpnDenied
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            running = TrojanService.running
            vpnActive = TrojanService.vpnActive
            startedAt = TrojanService.startedAt
            delay(1000)
        }
    }

    if (adding || editing != null) {
        TrojanServerEditor(
            initial = editing,
            onSave = { server ->
                setting.upsertServer(server)
                servers = setting.loadServers()
                selectedId = setting.currentServer()?.id
                adding = false
                editing = null
            },
            onCancel = {
                adding = false
                editing = null
            },
        )
        return
    }

    BackHandler(onBack = onBack)

    if (scanning) {
        QrScannerScreen(
            onResult = { text ->
                val parsed = parseTrojanUri(text)
                if (parsed == null) {
                    error = t.invalidUri
                } else {
                    val server = parsed.copy(
                        id = UUID.randomUUID().toString(),
                        name = parsed.name.ifBlank { parsed.remoteAddr },
                    )
                    setting.upsertServer(server)
                    servers = setting.loadServers()
                    selectedId = setting.currentServer()?.id
                    error = ""
                }
                scanning = false
            },
            onClose = { scanning = false },
        )
        return
    }

    var tab by remember { mutableIntStateOf(0) }
    var bypassLan by remember { mutableStateOf(setting.bypassLan) }
    var cnDirect by remember { mutableStateOf(setting.cnDirectEnabled) }
    var directDomains by remember { mutableStateOf(setting.loadDirectDomains()) }
    var proxyDomains by remember { mutableStateOf(setting.loadProxyDomains()) }
    var blockedDomains by remember { mutableStateOf(setting.loadBlockedDomains()) }
    var unreachableDomains by remember { mutableStateOf(setting.loadUnreachableDomains()) }
    var ipv6 by remember { mutableStateOf(setting.enableIpv6) }
    var autoStart by remember { mutableStateOf(setting.autoStart) }
    var qrServer by remember { mutableStateOf<TrojanServer?>(null) }

    val current = servers.firstOrNull { it.id == selectedId } ?: servers.firstOrNull()

    LaunchedEffect(running, current?.id, current?.localPort, retestTick) {
        if (!running || current == null) {
            serverTesting = false
            serverLatency = null
            googleTesting = false
            googleResult = null
            return@LaunchedEffect
        }
        serverTesting = true
        serverLatency = null
        serverLatency = probeServer(current)
        serverTesting = false
        googleTesting = true
        googleResult = null
        googleResult = probeGoogle(current.localPort)
        googleTesting = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WxBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.common.back)
            }
            Text(sv.trojan, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }

        SubTabs(
            tabs = listOf(t.tabServers, t.proxyApps, t.tabSplit, t.tabSettings),
            selected = tab,
            onSelect = { tab = it },
        )

        when (tab) {
            0 -> Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                // ── 运行信息 ──
                SectionCard(title = sv.runningInfo) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        StatusText(
                            active = running,
                            activeLabel = sv.running,
                            inactiveLabel = sv.stopped,
                        )
                        Text(
                            text = if (running && current != null) {
                                "  " + t.runningTemplate.format(current.localAddr, current.localPort)
                            } else {
                                ""
                            },
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = {
                                if (running) {
                                    TrojanService.stop(ctx)
                                    running = false
                                } else if (current == null) {
                                    error = t.notConfigured
                                } else {
                                    val prepare = runCatching { VpnService.prepare(ctx) }.getOrNull()
                                    if (prepare != null) vpnLauncher.launch(prepare) else TrojanService.start(ctx)
                                }
                            },
                            colors = if (running) {
                                ButtonDefaults.buttonColors(containerColor = Color(0xFFE64340))
                            } else {
                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            },
                        ) {
                            Text(if (running) t.stop else t.start)
                        }
                    }
                    KeyValueRow(t.currentServer, current?.displayName ?: sv.notConfigured)
                    KeyValueRow(
                        sv.localListen,
                        if (current == null) sv.emptyValue else "${current.localAddr}:${current.localPort}",
                    )
                    KeyValueRow(
                        sv.remoteServer,
                        if (current == null) sv.emptyValue else "${current.remoteAddr}:${current.remotePort}",
                    )
                    KeyValueRow(sv.vpn, if (vpnActive) sv.vpnOn else sv.vpnOff)
                    KeyValueRow(
                        sv.uptime,
                        if (running && startedAt > 0L) {
                            sv.uptimeTemplate.format(formatUptime(System.currentTimeMillis() - startedAt))
                        } else {
                            sv.emptyValue
                        },
                    )
                    SpeedRow(
                        label = t.serverLatency,
                        value = when {
                            !running -> sv.emptyValue
                            serverTesting -> t.testingServer
                            serverLatency != null -> t.reachableTemplate.format(serverLatency!!)
                            else -> t.unreachable
                        },
                        color = when {
                            serverTesting || !running -> TextMuted
                            serverLatency != null -> OnlineGreen
                            else -> Color(0xFFE64340)
                        },
                        showRetest = false,
                        onRetest = { retestTick++ },
                    )
                    SpeedRow(
                        label = t.googleSpeed,
                        value = when {
                            !running -> sv.emptyValue
                            googleTesting -> t.testingGoogle
                            googleResult == null -> sv.emptyValue
                            googleResult!!.first -> t.googleSuccessTemplate.format(googleResult!!.second)
                            else -> t.googleFailed
                        },
                        color = when {
                            googleTesting || !running -> TextMuted
                            googleResult?.first == true -> OnlineGreen
                            else -> Color(0xFFE64340)
                        },
                        showRetest = true,
                        onRetest = { retestTick++ },
                    )
                    if (error.isNotEmpty()) {
                        Text(error, fontSize = 12.sp, color = Color(0xFFE64340), modifier = Modifier.padding(top = 6.dp))
                    }
                }

                // ── 服务器列表 ──
                SectionCard(
                    title = t.serverList,
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { scanning = true }) {
                                Text(t.scanQr)
                            }
                            TextButton(onClick = { adding = true }) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(t.addServer)
                            }
                        }
                    },
                ) {
                    if (servers.isEmpty()) {
                        EmptyHint(t.noServers)
                    } else {
                        servers.forEachIndexed { index, server ->
                            ServerRow(
                                server = server,
                                selected = server.id == selectedId,
                                running = running,
                                testResult = testResults[server.id],
                                onSelect = {
                                    setting.selectServer(server.id)
                                    selectedId = server.id
                                    if (running) {
                                        TrojanService.stop(ctx)
                                        running = false
                                        info = t.selectHint
                                    } else {
                                        info = ""
                                    }
                                },
                                onEdit = { editing = server },
                                onDelete = { deleteTarget = server },
                                onTest = {
                                    testResults = testResults + (server.id to t.testing)
                                    scope.launch {
                                        testResults = testResults + (server.id to testServer(server, t))
                                    }
                                },
                                onQr = { qrServer = server },
                            )
                            if (index < servers.lastIndex) Divider()
                        }
                    }
                    if (info.isNotEmpty()) {
                        Text(info, fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(top = 6.dp))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            1 -> ProxyAppsContent(modifier = Modifier.weight(1f))

            2 -> Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                SplitSettingsCard(
                    running = running,
                    bypassLan = bypassLan,
                    onBypassLanChange = {
                        bypassLan = it
                        setting.bypassLan = it
                    },
                    cnDirect = cnDirect,
                    onCnDirectChange = {
                        cnDirect = it
                        setting.cnDirectEnabled = it
                    },
                    directDomains = directDomains,
                    onDirectDomainsChange = {
                        directDomains = it
                        setting.saveDirectDomains(it)
                    },
                    proxyDomains = proxyDomains,
                    onProxyDomainsChange = {
                        proxyDomains = it
                        setting.saveProxyDomains(it)
                    },
                    blockedDomains = blockedDomains,
                    onBlockedDomainsChange = {
                        blockedDomains = it
                        setting.saveBlockedDomains(it)
                    },
                    unreachableDomains = unreachableDomains,
                    onUnreachableDomainsChange = {
                        unreachableDomains = it
                        setting.saveUnreachableDomains(it)
                    },
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            3 -> Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                SettingsCard(
                    running = running,
                    ipv6 = ipv6,
                    onIpv6Change = {
                        ipv6 = it
                        setting.enableIpv6 = it
                    },
                    autoStart = autoStart,
                    onAutoStartChange = {
                        autoStart = it
                        setting.autoStart = it
                    },
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    deleteTarget?.let { server ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(t.deleteConfirm) },
            text = { Text(server.displayName) },
            confirmButton = {
                TextButton(onClick = {
                    setting.deleteServer(server.id)
                    servers = setting.loadServers()
                    selectedId = setting.currentServer()?.id
                    deleteTarget = null
                }) { Text(s.common.delete) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(s.common.cancel) }
            },
        )
    }

    qrServer?.let { server ->
        ServerQrDialog(server = server, onDismiss = { qrServer = null })
    }
}

@Composable
private fun SplitSettingsCard(
    running: Boolean,
    bypassLan: Boolean,
    onBypassLanChange: (Boolean) -> Unit,
    cnDirect: Boolean,
    onCnDirectChange: (Boolean) -> Unit,
    directDomains: Set<String>,
    onDirectDomainsChange: (Set<String>) -> Unit,
    proxyDomains: Set<String>,
    onProxyDomainsChange: (Set<String>) -> Unit,
    blockedDomains: Set<String>,
    onBlockedDomainsChange: (Set<String>) -> Unit,
    unreachableDomains: Set<String>,
    onUnreachableDomainsChange: (Set<String>) -> Unit,
) {
    val s = LocalAppStrings.current
    val t = s.trojan
    SectionCard(title = t.tabSplit) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(t.bypassLan, fontSize = 14.sp, color = TextPrimary)
                Text(t.bypassLanHint, fontSize = 11.sp, color = TextMuted)
            }
            Switch(checked = bypassLan, onCheckedChange = onBypassLanChange)
        }

        Divider()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(t.cnDirect, fontSize = 14.sp, color = TextPrimary)
                Text(t.cnDirectHint, fontSize = 11.sp, color = TextMuted)
            }
            Switch(checked = cnDirect, onCheckedChange = onCnDirectChange)
        }

        Divider()
        RuleSection(
            proxyDomains = proxyDomains,
            onProxyDomainsChange = onProxyDomainsChange,
            directDomains = directDomains,
            onDirectDomainsChange = onDirectDomainsChange,
            blockedDomains = blockedDomains,
            onBlockedDomainsChange = onBlockedDomainsChange,
            unreachableDomains = unreachableDomains,
            onUnreachableDomainsChange = onUnreachableDomainsChange,
        )

        if (running) {
            Text(
                text = t.restartHint,
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

private enum class RuleType { PROXY, DIRECT, BLOCK, UNREACHABLE }

@Composable
private fun RuleSection(
    proxyDomains: Set<String>,
    onProxyDomainsChange: (Set<String>) -> Unit,
    directDomains: Set<String>,
    onDirectDomainsChange: (Set<String>) -> Unit,
    blockedDomains: Set<String>,
    onBlockedDomainsChange: (Set<String>) -> Unit,
    unreachableDomains: Set<String>,
    onUnreachableDomainsChange: (Set<String>) -> Unit,
) {
    val s = LocalAppStrings.current
    val t = s.trojan
    var selected by remember { mutableStateOf(RuleType.PROXY) }
    var input by remember { mutableStateOf("") }

    fun label(type: RuleType): String = when (type) {
        RuleType.PROXY -> t.ruleProxy
        RuleType.DIRECT -> t.ruleDirect
        RuleType.BLOCK -> t.ruleBlock
        RuleType.UNREACHABLE -> t.ruleUnreachable
    }

    fun hint(type: RuleType): String = when (type) {
        RuleType.PROXY -> t.ruleProxyHint
        RuleType.DIRECT -> t.ruleDirectHint
        RuleType.BLOCK -> t.ruleBlockHint
        RuleType.UNREACHABLE -> t.ruleUnreachableHint
    }

    fun emptyLabel(type: RuleType): String = when (type) {
        RuleType.PROXY -> t.noProxyDomains
        RuleType.DIRECT -> t.noDirectDomains
        RuleType.BLOCK -> t.noBlockedDomains
        RuleType.UNREACHABLE -> t.noUnreachableDomains
    }

    fun domains(type: RuleType): Set<String> = when (type) {
        RuleType.PROXY -> proxyDomains
        RuleType.DIRECT -> directDomains
        RuleType.BLOCK -> blockedDomains
        RuleType.UNREACHABLE -> unreachableDomains
    }

    fun update(type: RuleType, domains: Set<String>) {
        when (type) {
            RuleType.PROXY -> onProxyDomainsChange(domains)
            RuleType.DIRECT -> onDirectDomainsChange(domains)
            RuleType.BLOCK -> onBlockedDomainsChange(domains)
            RuleType.UNREACHABLE -> onUnreachableDomainsChange(domains)
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 4.dp),
    ) {
        RuleType.entries.forEach { type ->
            FilterChip(
                selected = selected == type,
                onClick = { selected = type },
                label = { Text("${label(type)} (${domains(type).size})") },
            )
        }
    }
    Text(
        text = hint(selected),
        fontSize = 11.sp,
        color = TextMuted,
        modifier = Modifier.padding(top = 6.dp),
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            placeholder = { Text(t.domainPlaceholder) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = {
                val domain = input.trim().lowercase()
                if (domain.isNotEmpty()) {
                    onProxyDomainsChange(proxyDomains - domain)
                    onDirectDomainsChange(directDomains - domain)
                    onBlockedDomainsChange(blockedDomains - domain)
                    onUnreachableDomainsChange(unreachableDomains - domain)
                    update(selected, domains(selected) + domain)
                    input = ""
                }
            },
            modifier = Modifier.padding(start = 8.dp),
        ) {
            Text(t.addDomain)
        }
    }
    val current = domains(selected)
    if (current.isEmpty()) {
        Text(
            text = emptyLabel(selected),
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 6.dp),
        )
    } else {
        current.sorted().forEach { domain ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = domain,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { update(selected, current - domain) }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = s.common.delete,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    running: Boolean,
    ipv6: Boolean,
    onIpv6Change: (Boolean) -> Unit,
    autoStart: Boolean,
    onAutoStartChange: (Boolean) -> Unit,
) {
    val s = LocalAppStrings.current
    val t = s.trojan
    SectionCard(title = t.tabSettings) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(t.ipv6, fontSize = 14.sp, color = TextPrimary)
                Text(t.ipv6Hint, fontSize = 11.sp, color = TextMuted)
            }
            Switch(checked = ipv6, onCheckedChange = onIpv6Change)
        }
        Divider()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(t.autoStart, fontSize = 14.sp, color = TextPrimary)
                Text(t.autoStartHint, fontSize = 11.sp, color = TextMuted)
            }
            Switch(checked = autoStart, onCheckedChange = onAutoStartChange)
        }
        if (running) {
            Text(
                text = t.restartHint,
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

private data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val category: AppCategory,
    val country: String?,
)

@Composable
private fun ProxyAppsContent(modifier: Modifier = Modifier) {
    val s = LocalAppStrings.current
    val t = s.trojan
    val ctx = LocalContext.current
    val setting = MainApplication.getApp().settings.trojan
    val scope = rememberCoroutineScope()
    var proxyAll by remember { mutableStateOf(setting.proxyAllApps) }
    var selected by remember { mutableStateOf(setting.loadProxiedApps()) }
    var onlineLoading by remember { mutableStateOf(false) }
    var onlineMessage by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<AppCategory?>(null) }

    fun loadApps(): List<AppEntry> {
        val pm = ctx.packageManager
        val online = setting.loadAppCountries()
        return pm.getInstalledApplications(0)
            .asSequence()
            .filter { it.packageName != ctx.packageName }
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map {
                val classification = classifyApp(it, pm, online)
                AppEntry(
                    packageName = it.packageName,
                    label = pm.getApplicationLabel(it).toString(),
                    icon = it.loadIcon(pm),
                    category = classification.category,
                    country = classification.country,
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    var apps by remember { mutableStateOf(loadApps()) }
    val grouped = apps.groupBy { it.category }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            SectionCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(t.proxyAllApps, fontSize = 14.sp, color = TextPrimary)
                        Text(t.proxyAppsHint, fontSize = 11.sp, color = TextMuted)
                    }
                    Switch(
                        checked = proxyAll,
                        onCheckedChange = { all ->
                            proxyAll = all
                            setting.proxyAllApps = all
                            if (!all && selected.isEmpty()) {
                                selected = apps
                                    .filter { it.category == AppCategory.NON_FREE }
                                    .map { it.packageName }
                                    .toSet()
                                setting.saveProxiedApps(selected)
                            }
                        },
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(t.onlineHint, fontSize = 11.sp, color = TextMuted)
                        if (onlineMessage.isNotEmpty()) {
                            Text(
                                text = onlineMessage,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            scope.launch {
                                onlineLoading = true
                                onlineMessage = ""
                                val cache = setting.loadAppCountries().toMutableMap()
                                val socksPort = if (TrojanService.running) {
                                    setting.currentServer()?.localPort
                                } else {
                                    null
                                }
                                val candidates = apps
                                    .filter { it.country == null && it.category != AppCategory.SYSTEM }
                                    .take(10)
                                var found = 0
                                for (app in candidates) {
                                    val country = TrojanAppMetadata.lookup(app.packageName, socksPort)
                                    if (country != null) {
                                        cache[app.packageName] = country
                                        found++
                                    }
                                }
                                if (found > 0) {
                                    setting.saveAppCountries(cache)
                                    apps = loadApps()
                                }
                                onlineMessage = t.onlineClassifiedTemplate.format(found)
                                onlineLoading = false
                            }
                        },
                        enabled = !onlineLoading,
                    ) {
                        Text(if (onlineLoading) t.onlineClassifying else t.onlineClassify)
                    }
                }
            }
        }
        item {
            Text(
                text = t.classificationHint,
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 2.dp),
            )
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                FilterChip(
                    selected = filter == null,
                    onClick = { filter = null },
                    label = { Text("${t.allApps} (${apps.size})") },
                )
                AppCategory.entries.forEach { category ->
                    val count = grouped[category]?.size ?: 0
                    val label = when (category) {
                        AppCategory.SYSTEM -> t.systemShort
                        AppCategory.NON_FREE -> t.nonFreeShort
                        AppCategory.FREE -> t.freeShort
                    }
                    FilterChip(
                        selected = filter == category,
                        onClick = { filter = if (filter == category) null else category },
                        label = { Text("$label ($count)") },
                    )
                }
            }
        }
        if (apps.isEmpty()) {
            item { SectionCard { EmptyHint(t.noApps) } }
        } else {
            val visibleCategories = filter?.let { listOf(it) } ?: AppCategory.entries.toList()
            if (filter != null && grouped[filter].isNullOrEmpty()) {
                item { SectionCard { EmptyHint(t.noApps) } }
            }
            visibleCategories.forEach { category ->
                val categoryApps = grouped[category].orEmpty()
                if (categoryApps.isNotEmpty()) {
                    item {
                        CategoryHeader(
                            category = category,
                            apps = categoryApps,
                            selected = selected,
                            proxyAll = proxyAll,
                            onToggleAll = { proxy ->
                                if (proxy) {
                                    if (!proxyAll) {
                                        selected = selected + categoryApps.map { it.packageName }
                                    }
                                } else {
                                    if (proxyAll) {
                                        proxyAll = false
                                        setting.proxyAllApps = false
                                        val base = apps.map { it.packageName }.toSet()
                                        selected = base - categoryApps.map { it.packageName }.toSet()
                                    } else {
                                        selected = selected - categoryApps.map { it.packageName }.toSet()
                                    }
                                }
                                setting.saveProxiedApps(selected)
                            },
                        )
                    }
                    items(categoryApps) { app ->
                        AppRow(
                            app = app,
                            checked = proxyAll || app.packageName in selected,
                            onCheckedChange = { checked ->
                                if (proxyAll && !checked) {
                                    proxyAll = false
                                    setting.proxyAllApps = false
                                    val base = apps.map { it.packageName }.toSet()
                                    selected = base - app.packageName
                                } else {
                                    selected = if (checked) {
                                        selected + app.packageName
                                    } else {
                                        selected - app.packageName
                                    }
                                }
                                setting.saveProxiedApps(selected)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(
    category: AppCategory,
    apps: List<AppEntry>,
    selected: Set<String>,
    proxyAll: Boolean,
    onToggleAll: (Boolean) -> Unit,
) {
    val t = LocalAppStrings.current.trojan
    val allSelected = proxyAll || apps.all { it.packageName in selected }
    val noneSelected = !proxyAll && apps.none { it.packageName in selected }
    val state = when {
        allSelected -> ToggleableState.On
        noneSelected -> ToggleableState.Off
        else -> ToggleableState.Indeterminate
    }
    val label = when (category) {
        AppCategory.SYSTEM -> t.systemApps
        AppCategory.NON_FREE -> t.nonFreeApps
        AppCategory.FREE -> t.freeApps
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 2.dp),
    ) {
        TriStateCheckbox(
            state = state,
            onClick = { onToggleAll(state != ToggleableState.On) },
        )
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(
                t.appCountTemplate.format(apps.size),
                fontSize = 11.sp,
                color = TextMuted,
            )
        }
    }
}

@Composable
private fun AppRow(
    app: AppEntry,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        val bitmap = remember(app.packageName) {
            runCatching { app.icon.toBitmap(48, 48).asImageBitmap() }.getOrNull()
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = app.label,
                modifier = Modifier.size(32.dp),
            )
        } else {
            Icon(
                painter = painterResource(id = io.github.freewebmovement.zz.R.drawable.ic_app),
                contentDescription = app.label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(app.label, fontSize = 14.sp, color = TextPrimary)
            Text(
                text = if (app.country != null) "${app.packageName} · ${app.country}" else app.packageName,
                fontSize = 10.sp,
                color = TextMuted,
            )
        }
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ServerQrDialog(server: TrojanServer, onDismiss: () -> Unit) {
    val s = LocalAppStrings.current
    val t = s.trojan
    val ctx = LocalContext.current
    var qr by remember { mutableStateOf<Pair<Int, String>?>(null) }
    val uri = remember(server) {
        val user = java.net.URLEncoder.encode(server.password, "UTF-8")
        val fragment = if (server.name.isNotBlank()) {
            "#" + java.net.URLEncoder.encode(server.name, "UTF-8")
        } else {
            ""
        }
        "trojan://$user@${server.remoteAddr}:${server.remotePort}$fragment"
    }
    LaunchedEffect(uri) {
        qr = withContext(Dispatchers.IO) {
            runCatching {
                val o = JSONObject(FwmcApi.qrMatrix(uri))
                o.optInt("width") to o.optString("data", "")
            }.getOrDefault(0 to "")
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t.qrTitle) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val value = qr
                if (value != null && value.first > 0) {
                    val (w, bits) = value
                    Canvas(
                        modifier = Modifier
                            .size(220.dp)
                            .align(Alignment.CenterHorizontally)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                    ) {
                        val n = w
                        val cell = (size.minDimension - 24.dp.toPx()) / n.toFloat()
                        val start = 12.dp.toPx()
                        for (i in 0 until n) {
                            for (j in 0 until n) {
                                if (bits.getOrNull(i * n + j) == '1') {
                                    drawRect(
                                        color = Color.Black,
                                        topLeft = Offset(start + j * cell, start + i * cell),
                                        size = Size(cell + 0.5f, cell + 0.5f),
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(t.qrGenerating, fontSize = 13.sp, color = TextSecondary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uri,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, uri)
                }
                ctx.startActivity(
                    android.content.Intent.createChooser(send, s.common.share)
                )
            }) { Text(s.common.share) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.common.close) }
        },
    )
}

@Composable
private fun ServerRow(
    server: TrojanServer,
    selected: Boolean,
    running: Boolean,
    testResult: String?,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit,
    onQr: () -> Unit,
) {
    val s = LocalAppStrings.current
    val t = s.trojan
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 4.dp),
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(modifier = Modifier.weight(1f).padding(start = 2.dp)) {
            Text(
                text = server.displayName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Text(
                text = "${server.remoteAddr}:${server.remotePort}",
                fontSize = 11.sp,
                color = TextSecondary,
            )
            if (testResult != null) {
                Text(
                    text = testResult,
                    fontSize = 11.sp,
                    color = if (testResult == t.testing) TextMuted else MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (selected && running) {
            Text(s.service.running, fontSize = 11.sp, color = OnlineGreen, modifier = Modifier.padding(end = 2.dp))
        }
        IconButton(onClick = onTest) {
            Icon(Icons.Filled.Refresh, contentDescription = t.test, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onQr) {
            Icon(Icons.Filled.Share, contentDescription = s.common.share, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = t.editServer, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = s.common.delete, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SpeedRow(
    label: String,
    value: String,
    color: Color,
    showRetest: Boolean,
    onRetest: () -> Unit,
) {
    val t = LocalAppStrings.current.trojan
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(0.35f))
        Text(value, fontSize = 12.sp, color = color, modifier = Modifier.weight(0.65f))
        if (showRetest) {
            IconButton(onClick = onRetest, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = t.test,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private suspend fun testServer(server: TrojanServer, t: TrojanStrings): String {
    val ms = probeServer(server)
    return if (ms != null) t.reachableTemplate.format(ms) else t.unreachable
}

/** TCP 连接服务器，返回毫秒延迟；失败返回 null。 */
private suspend fun probeServer(server: TrojanServer): Long? = withContext(Dispatchers.IO) {
    val start = System.currentTimeMillis()
    val ok = runCatching {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(server.remoteAddr, server.remotePort), 3000)
        }
    }.isSuccess
    if (ok) System.currentTimeMillis() - start else null
}

/** 通过本地 SOCKS5 代理访问 Google，返回成功/延迟。 */
private suspend fun probeGoogle(localPort: Int): Pair<Boolean, Long> = withContext(Dispatchers.IO) {
    val start = System.currentTimeMillis()
    var connection: HttpURLConnection? = null
    try {
        val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", localPort))
        connection = URL("https://www.google.com/generate_204").openConnection(proxy) as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.connect()
        connection.inputStream.close()
        true to (System.currentTimeMillis() - start)
    } catch (_: Exception) {
        false to 0L
    } finally {
        connection?.disconnect()
    }
}

@Composable
private fun TrojanServerEditor(
    initial: TrojanServer?,
    onSave: (TrojanServer) -> Unit,
    onCancel: () -> Unit,
) {
    val s = LocalAppStrings.current
    val t = s.trojan
    val id = remember(initial?.id) { initial?.id ?: UUID.randomUUID().toString() }
    var name by remember(id) { mutableStateOf(initial?.name ?: "") }
    var remoteAddr by remember(id) { mutableStateOf(initial?.remoteAddr ?: "") }
    var remotePort by remember(id) { mutableStateOf((initial?.remotePort ?: 443).toString()) }
    var password by remember(id) { mutableStateOf(initial?.password ?: "") }
    var localAddr by remember(id) { mutableStateOf(initial?.localAddr ?: "127.0.0.1") }
    var localPort by remember(id) { mutableStateOf((initial?.localPort ?: 1080).toString()) }
    var verifyCert by remember(id) { mutableStateOf(initial?.verifyCert ?: true) }
    var sni by remember(id) { mutableStateOf(initial?.sni ?: "") }
    var quickUri by remember(id) { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }

    BackHandler(onBack = onCancel)

    if (scanning) {
        QrScannerScreen(
            onResult = { text ->
                val parsed = parseTrojanUri(text)
                if (parsed == null) {
                    error = t.invalidUri
                } else {
                    remoteAddr = parsed.remoteAddr
                    remotePort = parsed.remotePort.toString()
                    password = parsed.password
                    if (parsed.sni.isNotBlank()) sni = parsed.sni
                    if (parsed.name.isNotBlank()) name = parsed.name
                    error = ""
                }
                scanning = false
            },
            onClose = { scanning = false },
        )
        return
    }

    fun applyUri() {
        val parsed = parseTrojanUri(quickUri)
        if (parsed == null) {
            error = t.invalidUri
            return
        }
        remoteAddr = parsed.remoteAddr
        remotePort = parsed.remotePort.toString()
        password = parsed.password
        if (parsed.sni.isNotBlank()) sni = parsed.sni
        if (parsed.name.isNotBlank()) name = parsed.name
        error = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WxBg)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.common.back)
            }
            Text(
                text = if (initial == null) t.addServerTitle else t.editServerTitle,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
        }

        SectionCard(title = s.service.settings) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = quickUri,
                    onValueChange = { quickUri = it },
                    label = { Text(t.quickUri) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = { applyUri() }, modifier = Modifier.padding(start = 8.dp)) {
                    Text(t.parseUri)
                }
            }
            Button(
                onClick = { scanning = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text(t.scanQr)
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(t.serverName) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = remoteAddr,
                onValueChange = { remoteAddr = it },
                label = { Text(t.remoteAddr) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = remotePort,
                onValueChange = { remotePort = it.filter { c -> c.isDigit() } },
                label = { Text(t.remotePort) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(t.password) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = localAddr,
                onValueChange = { localAddr = it },
                label = { Text(t.localAddr) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = localPort,
                onValueChange = { localPort = it.filter { c -> c.isDigit() } },
                label = { Text(t.localPort) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text(t.verifyCert, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                Switch(checked = verifyCert, onCheckedChange = { verifyCert = it })
            }
            OutlinedTextField(
                value = sni,
                onValueChange = { sni = it },
                label = { Text(t.sni) },
                placeholder = { Text(t.sniHint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            if (error.isNotEmpty()) {
                Text(error, fontSize = 12.sp, color = Color(0xFFE64340), modifier = Modifier.padding(top = 6.dp))
            }
            Button(
                onClick = {
                    val rp = remotePort.toIntOrNull()
                    val lp = localPort.toIntOrNull()
                    if (remoteAddr.isBlank() || password.isBlank() || rp == null || lp == null) {
                        error = t.notConfigured
                        return@Button
                    }
                    onSave(
                        TrojanServer(
                            id = id,
                            name = name.trim(),
                            remoteAddr = remoteAddr.trim(),
                            remotePort = rp.coerceIn(1, 65535),
                            password = password,
                            localAddr = localAddr.ifBlank { "127.0.0.1" },
                            localPort = lp.coerceIn(1, 65535),
                            verifyCert = verifyCert,
                            sni = sni.trim(),
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Text(t.saveServer)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** 解析 `trojan://password@host:port?...#name`。 */
private fun parseTrojanUri(text: String): TrojanServer? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null
    return runCatching {
        val uri = URI(trimmed)
        if (!uri.scheme.equals("trojan", ignoreCase = true)) return null
        val host = uri.host ?: return null
        val user = uri.userInfo ?: return null
        val port = if (uri.port > 0) uri.port else 443
        val sni = uri.query
            ?.split("&")
            ?.firstOrNull { it.startsWith("sni=") }
            ?.substringAfter("sni=")
            ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
            ?: ""
        TrojanServer(
            id = "",
            name = uri.fragment ?: "",
            remoteAddr = host,
            remotePort = port,
            password = java.net.URLDecoder.decode(user, "UTF-8"),
            sni = sni,
        )
    }.getOrNull()
}

/** 全屏 QR 扫描：CameraX + ML Kit，识别后回调原始文本。 */
@Composable
private fun QrScannerScreen(onResult: (String) -> Unit, onClose: () -> Unit) {
    val s = LocalAppStrings.current
    val t = s.trojan
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val handled = remember { AtomicBoolean(false) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var barcodeScanner by remember { mutableStateOf<BarcodeScanner?>(null) }
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var message by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (!granted) message = t.cameraDenied
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { cameraProvider?.unbindAll() }
            runCatching { barcodeScanner?.close() }
            runCatching { executor.shutdown() }
        }
    }

    BackHandler(onBack = onClose)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (permissionGranted) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView = it }
                },
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .align(Alignment.Center)
                    .border(3.dp, Color(0xFF00E676), RoundedCornerShape(8.dp)),
            )
            Text(
                text = t.scanHint,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .background(Color(0x88000000))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
            LaunchedEffect(previewView) {
                val view = previewView ?: return@LaunchedEffect
                val future = ProcessCameraProvider.getInstance(context)
                future.addListener({
                    val provider = runCatching { future.get() }.getOrNull() ?: return@addListener
                    cameraProvider = provider
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(view.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    val scanner = BarcodeScanning.getClient(
                        BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                            .build()
                    )
                    barcodeScanner = scanner
                    analysis.setAnalyzer(executor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage == null) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        val input = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees,
                        )
                        scanner.process(input)
                            .addOnSuccessListener { barcodes ->
                                val text = barcodes.firstOrNull()?.rawValue
                                if (!text.isNullOrEmpty() && handled.compareAndSet(false, true)) {
                                    onResult(text)
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    }
                    runCatching {
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis,
                        )
                    }.onFailure {
                        message = it.message ?: t.cameraDenied
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        } else {
            Text(
                text = if (message.isNotEmpty()) message else t.cameraDenied,
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
        }

        Text(
            text = t.scanTitle,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp),
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 24.dp, start = 12.dp)
                .background(Color(0x66000000), RoundedCornerShape(50)),
        ) {
            Icon(Icons.Filled.Close, contentDescription = s.common.close, tint = Color.White)
        }
    }
}

private fun formatUptime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}
