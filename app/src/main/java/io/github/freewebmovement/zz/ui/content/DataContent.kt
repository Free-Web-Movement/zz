package io.github.freewebmovement.zz.ui.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.freewebmovement.zz.ui.common.EmptyHint
import io.github.freewebmovement.zz.ui.common.KeyValueRow
import io.github.freewebmovement.zz.ui.common.MonoText
import io.github.freewebmovement.zz.ui.common.SectionCard
import io.github.freewebmovement.zz.ui.common.SubTabs
import io.github.freewebmovement.zz.ui.common.rememberFwmcNodeSnapshot
import io.github.freewebmovement.zz.ui.common.shortAddr
import io.github.freewebmovement.zz.ui.i18n.LedgerStrings
import io.github.freewebmovement.zz.ui.i18n.LocalAppStrings
import io.github.freewebmovement.zz.ui.theme.IncomeGreen
import io.github.freewebmovement.zz.ui.theme.TextMuted
import io.github.freewebmovement.zz.ui.theme.TextPrimary
import io.github.freewebmovement.zz.ui.theme.TextSecondary
import io.github.freewebmovement.zz.ui.theme.WxBg
import org.json.JSONArray
import org.json.JSONObject
import rs.zz.coin.FwmcApi

private fun JSONObject.items(): List<JSONObject> = runCatching {
    val arr = optJSONArray("items") ?: return emptyList()
    (0 until arr.length()).map { arr.getJSONObject(it) }
}.getOrDefault(emptyList())

private fun JSONObject.array(key: String): List<JSONObject> = runCatching {
    val arr = optJSONArray(key) ?: return emptyList()
    (0 until arr.length()).map { arr.getJSONObject(it) }
}.getOrDefault(emptyList())

private fun JSONObject.num(key: String): Long = optLong(key, 0)

private fun ok(raw: String): JSONObject? =
    runCatching { JSONObject(raw) }.getOrNull()?.takeIf { it.optBoolean("success", false) }

/**
 * 数据页：直接浏览本节点账本（概览 / 交易 / 纪元 / 区块 / 见证环 / 发币 / 余额）。
 *
 * 数据来自 JNI 账本 API（`FwmcApi.getLedger*`），与网页端同名账本接口
 * 返回的是同一份 JSON 契约。
 */
@Composable
fun DataContent(tab: Int = 0, onTabChange: (Int) -> Unit = {}) {
    val s = LocalAppStrings.current.ledger
    val running = rememberFwmcNodeSnapshot().running

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WxBg)
            .verticalScroll(rememberScrollState()),
    ) {
        if (!running) {
            SectionCard { Text(s.nodeNotRunning, color = TextSecondary) }
            return@Column
        }
        when (tab) {
            0 -> OverviewSection(s)
            1 -> TransactionsSection(s)
            2 -> EpochsSection(s)
            3 -> BlocksSection(s)
            4 -> RingsSection(s)
            5 -> RewardsSection(s)
            else -> BalancesSection(s)
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

// ---------------- 概览 ----------------

@Composable
private fun OverviewSection(s: LedgerStrings) {
    var summary by remember { mutableStateOf<JSONObject?>(null) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        summary = ok(FwmcApi.getLedgerSummary())
        loaded = true
    }

    val o = summary
    if (!loaded) {
        SectionCard(title = s.overview) { Text(s.noData, color = TextMuted, fontSize = 13.sp) }
        return
    }
    if (o == null) {
        SectionCard(title = s.overview) { Text(s.loadFailed, color = TextSecondary) }
        return
    }

    SectionCard(title = s.overview) {
        KeyValueRow(s.myBalance, o.optString("my_balance_display", "0.00"))
        KeyValueRow(s.address, shortAddr(o.optString("my_address"), 20))
    }
    SectionCard {
        KeyValueRow(s.txCount, o.num("tx_count").toString())
        KeyValueRow(s.rewardCount, o.num("reward_count").toString())
        KeyValueRow(s.epochRingCount, o.num("epoch_ring_count").toString())
        KeyValueRow(s.ticksPerEpoch, o.num("ticks_per_epoch").toString())
    }

    val latestEpoch = o.optJSONObject("latest_epoch")
    SectionCard(title = s.latestEpoch) {
        if (latestEpoch == null) {
            Text(s.noData, color = TextMuted, fontSize = 13.sp)
        } else {
            KeyValueRow(s.epochLabel, latestEpoch.num("epoch").toString())
            KeyValueRow(s.tickCount, latestEpoch.num("tick_count").toString())
            KeyValueRow(s.totalReward, latestEpoch.optString("total_reward_display"))
            KeyValueRow(s.hash, shortAddr(latestEpoch.optString("hash"), 18))
        }
    }

    val currentEpoch = o.optJSONObject("current_epoch")
    SectionCard(title = s.currentEpoch) {
        if (currentEpoch == null) {
            Text(s.noData, color = TextMuted, fontSize = 13.sp)
        } else {
            KeyValueRow(s.epochLabel, currentEpoch.num("epoch").toString())
            KeyValueRow(s.tickCount, currentEpoch.num("tick_count").toString())
            KeyValueRow(
                s.pending,
                if (currentEpoch.optBoolean("settled", false)) s.settled else s.pending,
            )
        }
    }

    val latestBlock = o.optJSONObject("latest_block")
    SectionCard(title = s.latestBlock) {
        if (latestBlock == null) {
            Text(s.noData, color = TextMuted, fontSize = 13.sp)
        } else {
            KeyValueRow(s.epochLabel, latestBlock.num("epoch").toString())
            KeyValueRow(s.blockHeight, latestBlock.num("height").toString())
            KeyValueRow(s.txCount, latestBlock.num("tx_count").toString())
            KeyValueRow(s.hash, shortAddr(latestBlock.optString("hash"), 18))
            KeyValueRow(s.time, latestBlock.optString("time"))
        }
    }
}

// ---------------- 交易 ----------------

@Composable
private fun TransactionsSection(s: LedgerStrings) {
    var address by remember { mutableStateOf("") }
    var applied by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var total by remember { mutableStateOf(0L) }
    var items by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(applied, page) {
        val obj = ok(FwmcApi.getLedgerTransactions(applied, page, 20))
        if (obj != null) {
            items = obj.items()
            totalPages = obj.optInt("total_pages", 1).coerceAtLeast(1)
            total = obj.num("total")
        } else {
            items = emptyList()
        }
        loaded = true
    }

    SectionCard(title = s.transactions) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(s.filterAddress, fontSize = 12.sp) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 12.sp),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { page = 1; applied = address.trim() },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(start = 8.dp),
            ) { Text(s.query, fontSize = 12.sp) }
        }
        Text(
            text = s.totalTemplate.format(total),
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 6.dp),
        )
    }

    SectionCard {
        when {
            !loaded -> Text(s.noData, color = TextMuted, fontSize = 13.sp)
            items.isEmpty() -> EmptyHint(s.noData)
            else -> items.forEach { TxRowView(s, it) }
        }
    }

    if (loaded && items.isNotEmpty()) {
        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(enabled = page > 1, onClick = { page -= 1 }) { Text(s.prev) }
                Text(s.pageTemplate.format(page, totalPages), fontSize = 12.sp, color = TextSecondary)
                TextButton(enabled = page < totalPages, onClick = { page += 1 }) { Text(s.next) }
            }
        }
    }
}

@Composable
private fun TxRowView(s: LedgerStrings, tx: JSONObject) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = tx.optString("tx_type"),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = tx.optString("amount_display"),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = IncomeGreen,
            )
        }
        MonoText(
            text = "${s.from} ${shortAddr(tx.optString("from"), 12)}",
            fontSize = 10,
            color = TextSecondary,
            maxLines = 1,
        )
        MonoText(
            text = "${s.to} ${shortAddr(tx.optString("to"), 12)}",
            fontSize = 10,
            color = TextSecondary,
            maxLines = 1,
        )
        Text(
            text = "${tx.optString("time")}  ·  ${s.epochLabel} ${tx.num("epoch")}",
            fontSize = 10.sp,
            color = TextMuted,
        )
    }
}

// ---------------- 纪元 ----------------

@Composable
private fun EpochsSection(s: LedgerStrings) {
    var items by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        items = ok(FwmcApi.getLedgerEpochs(100))?.items() ?: emptyList()
        loaded = true
    }

    SectionCard(title = s.epochs) {
        when {
            !loaded -> Text(s.noData, color = TextMuted, fontSize = 13.sp)
            items.isEmpty() -> EmptyHint(s.noData)
            else -> items.forEach { e ->
                val settled = e.optBoolean("settled", false)
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${s.epochLabel} ${e.num("epoch")}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = if (settled) s.settled else s.pending,
                            fontSize = 11.sp,
                            color = if (settled) IncomeGreen else TextSecondary,
                        )
                    }
                    Text(
                        text = "${s.tickCount} ${e.num("tick_count")}  ·  " +
                            "${s.totalReward} ${e.optString("total_reward_display")}",
                        fontSize = 11.sp,
                        color = TextSecondary,
                    )
                    Text(text = e.optString("start_time_display"), fontSize = 10.sp, color = TextMuted)
                }
            }
        }
    }
}

// ---------------- 区块 ----------------

@Composable
private fun BlocksSection(s: LedgerStrings) {
    var items by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        items = ok(FwmcApi.getLedgerBlocks(50))?.items() ?: emptyList()
        loaded = true
    }

    SectionCard(title = s.blocks) {
        when {
            !loaded -> Text(s.noData, color = TextMuted, fontSize = 13.sp)
            items.isEmpty() -> EmptyHint(s.noData)
            else -> items.forEach { b ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${s.epochLabel} ${b.num("epoch")}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${s.blockHeight} ${b.num("height")}",
                            fontSize = 11.sp,
                            color = TextSecondary,
                        )
                    }
                    Text(
                        text = "${s.txCount} ${b.num("tx_count")}  ·  " +
                            "${s.tickCount} ${b.num("tick_count")}  ·  " +
                            "${s.totalReward} ${b.optString("total_amount_display")}",
                        fontSize = 11.sp,
                        color = TextSecondary,
                    )
                    MonoText(text = b.optString("hash"), fontSize = 10, color = TextMuted, maxLines = 1)
                    Text(text = b.optString("time"), fontSize = 10.sp, color = TextMuted)
                }
            }
        }
    }
}

// ---------------- 见证环 ----------------

@Composable
private fun RingsSection(s: LedgerStrings) {
    var obj by remember { mutableStateOf<JSONObject?>(null) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        obj = ok(FwmcApi.getLedgerRings(0, 50))
        loaded = true
    }

    val o = obj
    when {
        !loaded -> SectionCard { Text(s.noData, color = TextMuted, fontSize = 13.sp) }
        o == null -> SectionCard(title = s.rings) { Text(s.loadFailed, color = TextSecondary) }
        else -> {
            val current = o.array("current")
            SectionCard(title = s.rings) {
                if (current.isEmpty()) {
                    Text(s.noData, color = TextMuted, fontSize = 13.sp)
                } else {
                    current.forEach { r ->
                        val type = r.optString("ring_type")
                        Text(
                            text = if (type == "locked") s.lockedRing else s.activeRing,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                        )
                        MonoText(text = r.optString("ring_hash"), fontSize = 10, color = TextSecondary)
                        Text(
                            text = "${s.memberCount} ${r.num("member_count")}  ·  " +
                                "${s.epochLabel} ${r.num("epoch")}",
                            fontSize = 11.sp,
                            color = TextSecondary,
                        )
                        r.array("members").forEach { m ->
                            MonoText(
                                text = "${m.optString("ip")}:${m.optInt("port")}  " +
                                    shortAddr(m.optString("node_id"), 10),
                                fontSize = 10,
                                color = TextMuted,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            val history = o.array("history")
            SectionCard(title = s.historyRings) {
                if (history.isEmpty()) {
                    Text(s.noData, color = TextMuted, fontSize = 13.sp)
                } else {
                    history.forEach { r ->
                        MonoText(text = r.optString("ring_hash"), fontSize = 10, color = TextSecondary)
                        Text(
                            text = "${s.epochLabel} ${r.num("epoch")}  ·  " +
                                "${s.memberCount} ${r.num("member_count")}  ·  ${r.optString("time")}",
                            fontSize = 11.sp,
                            color = TextSecondary,
                        )
                    }
                }
            }

            val records = o.array("tick_records")
            SectionCard(title = "${s.tickRecords} · ${s.epochLabel} ${o.num("record_epoch")}") {
                if (records.isEmpty()) {
                    Text(s.noData, color = TextMuted, fontSize = 13.sp)
                } else {
                    records.forEach { r ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MonoText(
                                text = shortAddr(r.optString("address"), 14),
                                fontSize = 12,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "${s.tickCount} ${r.num("tick_count")}",
                                fontSize = 12.sp,
                                color = TextSecondary,
                            )
                            if (r.optBoolean("is_full_time", false)) {
                                Text(text = "  ${s.fullTime}", fontSize = 11.sp, color = IncomeGreen)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- 发币 / 分配 ----------------

@Composable
private fun RewardsSection(s: LedgerStrings) {
    var rewards by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var allocations by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        rewards = ok(FwmcApi.getLedgerRewards(0, 100))?.items() ?: emptyList()
        allocations = ok(FwmcApi.getLedgerAllocations(0, 100))?.items() ?: emptyList()
        loaded = true
    }

    SectionCard(title = s.rewards) {
        when {
            !loaded -> Text(s.noData, color = TextMuted, fontSize = 13.sp)
            rewards.isEmpty() -> EmptyHint(s.noData)
            else -> rewards.forEach { r ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MonoText(
                        text = shortAddr(r.optString("address"), 14),
                        fontSize = 12,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${s.epochLabel} ${r.num("epoch")}",
                        fontSize = 11.sp,
                        color = TextSecondary,
                    )
                    Text(
                        text = "  ${r.optString("amount_display")}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen,
                    )
                }
            }
        }
    }

    SectionCard(title = s.allocations) {
        when {
            !loaded -> Text(s.noData, color = TextMuted, fontSize = 13.sp)
            allocations.isEmpty() -> EmptyHint(s.noData)
            else -> allocations.forEach { a ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        MonoText(
                            text = shortAddr(a.optString("address"), 14),
                            fontSize = 12,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = a.optString("amount_display"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen,
                        )
                    }
                    Text(
                        text = "${s.epochLabel} ${a.num("epoch")}  ·  " +
                            "${s.round} ${a.num("round")}  ·  ${s.weight} ${a.optString("weight")}",
                        fontSize = 11.sp,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

// ---------------- 余额 ----------------

@Composable
private fun BalancesSection(s: LedgerStrings) {
    var items by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        items = ok(FwmcApi.getLedgerBalances(100))?.items() ?: emptyList()
        loaded = true
    }

    SectionCard(title = s.balances) {
        when {
            !loaded -> Text(s.noData, color = TextMuted, fontSize = 13.sp)
            items.isEmpty() -> EmptyHint(s.noData)
            else -> items.forEach { b ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = b.num("rank").toString(),
                        fontSize = 12.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    MonoText(
                        text = shortAddr(b.optString("address"), 14),
                        fontSize = 12,
                        color = if (b.optBoolean("is_me", false)) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            TextPrimary
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = b.optString("balance_display"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                }
            }
        }
    }
}
