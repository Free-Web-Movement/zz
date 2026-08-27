package io.github.freewebmovement.zz.ui.content.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.freewebmovement.zz.ui.common.Divider
import io.github.freewebmovement.zz.ui.common.EmptyHint
import io.github.freewebmovement.zz.ui.common.SectionCard
import io.github.freewebmovement.zz.ui.common.rememberFwmcNodeSnapshot
import io.github.freewebmovement.zz.ui.i18n.LocalAppStrings
import io.github.freewebmovement.zz.ui.theme.TextPrimary
import io.github.freewebmovement.zz.ui.theme.TextSecondary
import io.github.freewebmovement.zz.ui.theme.WxBg
import org.json.JSONObject
import rs.zz.coin.FwmcApi

/* 节点状态（不可修改的系统参数）与节点配置（只读）展示页。
 * 所有参数均不可在本页修改：端口在「服务器管理」配置、资源在「资源与权重配置」配置、
 * 每纪元 Tick 数与见证环最大节点数均为系统参数，仅作展示。 */
@Composable
fun SettingsScreen(onBack: () -> Unit = {}) {
    val s = LocalAppStrings.current
    val node = rememberFwmcNodeSnapshot()

    var maxWpl by remember { mutableStateOf("") }
    var ticksPerEpoch by remember { mutableStateOf("") }

    LaunchedEffect(node.running) {
        maxWpl = ""
        ticksPerEpoch = ""
        if (!node.running) return@LaunchedEffect
        runCatching {
            val obj = JSONObject(FwmcApi.getConfig())
            if (obj.optBoolean("success", false)) {
                maxWpl = obj.optString("max_witness_per_layer", "")
                ticksPerEpoch = obj.optString("ticks_per_epoch", "")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WxBg)
            .verticalScroll(rememberScrollState()),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(io.github.freewebmovement.zz.ui.theme.CardBg)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.common.back)
            }
            Text(s.settings.nodeStatusAndConfig, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }

        // 节点状态（不可修改的系统参数）
        SectionCard(title = s.settings.nodeStatus) {
            if (node.running) {
                StatusRow(s.settings.ticksPerEpoch, ticksPerEpoch)
            } else {
                EmptyHint(s.server.noData)
            }
        }

        // 节点配置
        SectionCard(title = s.settings.nodeConfig) {
            if (node.running) {
                StatusRow(s.settings.maxWitness, maxWpl)
            } else {
                EmptyHint(s.server.noData)
            }
        }

        Divider()

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** 只读信息行：左标签 + 右值。 */
@Composable
private fun StatusRow(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(label, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = TextSecondary)
    }
}