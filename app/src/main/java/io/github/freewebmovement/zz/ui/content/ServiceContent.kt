package io.github.freewebmovement.zz.ui.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.freewebmovement.android.noui.TrojanService
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.R
import io.github.freewebmovement.zz.ui.common.Divider
import io.github.freewebmovement.zz.ui.common.EmptyHint
import io.github.freewebmovement.zz.ui.common.SectionCard
import io.github.freewebmovement.zz.ui.common.StatusText
import io.github.freewebmovement.zz.ui.content.user.TrojanScreen
import io.github.freewebmovement.zz.ui.i18n.LocalAppStrings
import io.github.freewebmovement.zz.ui.theme.TextMuted
import io.github.freewebmovement.zz.ui.theme.TextPrimary
import io.github.freewebmovement.zz.ui.theme.TextSecondary
import io.github.freewebmovement.zz.ui.theme.WxBg

private enum class ServicePage {
    List,
    Trojan,
    Placeholder,
}

/**
 * 底部「服务」tab。
 *
 * 服务列表中的每个服务都有独立入口，详情页按「设置 / 服务器信息 / 运行信息」
 * 三部分组织。当前 zz 只内置 Trojan 客户端；HTTP/SOCKS/FRP 等保留入口，
 * 后续可逐个接入。
 */
@Composable
fun ServiceContent() {
    var page by remember { mutableStateOf(ServicePage.List) }
    var placeholderName by remember { mutableStateOf("") }

    when (page) {
        ServicePage.List -> ServiceListScreen(
            onOpenTrojan = { page = ServicePage.Trojan },
            onOpenPlaceholder = { name ->
                placeholderName = name
                page = ServicePage.Placeholder
            },
        )
        ServicePage.Trojan -> TrojanScreen(onBack = { page = ServicePage.List })
        ServicePage.Placeholder -> ServicePlaceholderScreen(
            name = placeholderName,
            onBack = { page = ServicePage.List },
        )
    }
}

@Composable
private fun ServiceListScreen(
    onOpenTrojan: () -> Unit,
    onOpenPlaceholder: (String) -> Unit,
) {
    val s = LocalAppStrings.current
    val sv = s.service
    val app = MainApplication.getApp()
    val trojan = app.settings.trojan
    val currentTrojan = trojan.currentServer()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WxBg)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = sv.subtitle,
            fontSize = 12.sp,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        SectionCard {
            ServiceEntry(
                name = sv.trojan,
                desc = if (currentTrojan != null) {
                    "${currentTrojan.remoteAddr}:${currentTrojan.remotePort}"
                } else {
                    sv.notConfigured
                },
                icon = R.drawable.ic_trojan_service,
                active = TrojanService.running,
                onClick = onOpenTrojan,
            )
            Divider()
            ServiceEntry(
                name = sv.staticFile,
                desc = s.peer.sharePort,
                icon = R.drawable.ic_mine_local_server_share,
                active = app.settings.network.staticFileEnabled,
                onClick = { onOpenPlaceholder(sv.staticFile) },
            )
            Divider()
            ServiceEntry(
                name = sv.httpProxy,
                desc = sv.unavailable,
                icon = R.drawable.ic_server,
                active = false,
                onClick = { onOpenPlaceholder(sv.httpProxy) },
            )
            Divider()
            ServiceEntry(
                name = sv.socksProxy,
                desc = sv.unavailable,
                icon = R.drawable.ic_server,
                active = false,
                onClick = { onOpenPlaceholder(sv.socksProxy) },
            )
            Divider()
            ServiceEntry(
                name = sv.frp,
                desc = sv.unavailable,
                icon = R.drawable.ic_server,
                active = false,
                onClick = { onOpenPlaceholder(sv.frp) },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ServiceEntry(
    name: String,
    desc: String,
    icon: Int,
    active: Boolean,
    onClick: () -> Unit,
) {
    val sv = LocalAppStrings.current.service
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = name,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        StatusText(
            active = active,
            activeLabel = sv.running,
            inactiveLabel = sv.stopped,
        )
        Text("  ›", color = TextMuted)
    }
}

@Composable
fun ServicePlaceholderScreen(name: String, onBack: () -> Unit) {
    val sv = LocalAppStrings.current.service
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WxBg)
            .verticalScroll(rememberScrollState()),
    ) {
        SubPageHeader(name, onBack)
        SectionCard(title = sv.settings) { EmptyHint(sv.unavailable) }
        SectionCard(title = sv.serverInfo) { EmptyHint(sv.emptyValue) }
        SectionCard(title = sv.runningInfo) { EmptyHint(sv.notRunning) }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
