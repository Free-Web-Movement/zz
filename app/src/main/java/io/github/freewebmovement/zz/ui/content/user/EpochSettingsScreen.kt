package io.github.freewebmovement.zz.ui.content.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.ui.common.Divider
import io.github.freewebmovement.zz.ui.common.SectionCard
import io.github.freewebmovement.zz.ui.i18n.LocalAppStrings
import io.github.freewebmovement.zz.ui.theme.CardBg
import io.github.freewebmovement.zz.ui.theme.TextMuted
import io.github.freewebmovement.zz.ui.theme.TextPrimary
import io.github.freewebmovement.zz.ui.theme.TextSecondary
import io.github.freewebmovement.zz.ui.theme.WxBg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* 纪元设置：生产模式 / 测试模式切换，以及测试模式下每 tick 时长（秒）。
 * 修改后需重启节点生效；生产模式下「每 tick 时长」无效。 */
@Composable
fun EpochSettingsScreen(onBack: () -> Unit = {}) {
    val s = LocalAppStrings.current
    val app = MainApplication.getApp()
    val ctx = LocalContext.current

    var testMode by remember { mutableStateOf(app.settings.debug.epochTestMode) }
    var secondsText by remember { mutableStateOf(app.settings.debug.epochTickSeconds.toString()) }
    var startTickText by remember { mutableStateOf(app.settings.debug.epochStartTick.toString()) }

    val secondsValid = secondsText.toIntOrNull()?.let { it in 1..3600 } == true
    val startTickValid = startTickText.toIntOrNull()?.let { it in 0..95 } == true
    val canSave = !testMode || (secondsValid && startTickValid)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WxBg)
            .verticalScroll(rememberScrollState()),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(CardBg)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.common.back)
            }
            Text(s.epoch.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }

        // 运行模式
        SectionCard(title = s.epoch.mode) {
            ModeOption(
                label = s.epoch.production,
                note = s.epoch.productionNote,
                selected = !testMode,
                onClick = { testMode = false },
            )
            Divider()
            ModeOption(
                label = s.epoch.test,
                note = s.epoch.testNote,
                selected = testMode,
                onClick = { testMode = true },
            )
        }

        // 每 tick 时长（仅测试模式有效）
        SectionCard(title = s.epoch.testTickSeconds) {
            OutlinedTextField(
                value = secondsText,
                onValueChange = { secondsText = it.filter { c -> c.isDigit() }.take(4) },
                enabled = testMode,
                label = { Text(s.epoch.testTickSecondsHint) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = testMode && !secondsValid,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (testMode && !secondsValid) {
                Text(
                    text = s.epoch.tickSecondsInvalid,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }
        }

        // 初始 TICK 值（仅测试模式有效）
        SectionCard(title = s.epoch.startTick) {
            OutlinedTextField(
                value = startTickText,
                onValueChange = { startTickText = it.filter { c -> c.isDigit() }.take(2) },
                enabled = testMode,
                label = { Text(s.epoch.startTickHint) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = testMode && !startTickValid,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (testMode && !startTickValid) {
                Text(
                    text = s.epoch.startTickInvalid,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }
        }

        Text(
            text = s.epoch.restartHint,
            fontSize = 12.sp,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Button(
            onClick = {
                app.settings.debug.epochTestMode = testMode
                if (testMode) {
                    app.settings.debug.epochTickSeconds = secondsText.toInt()
                    app.settings.debug.epochStartTick = startTickText.toInt()
                }
                // 运行模式需重启节点生效：ticker 在节点启动时一次性选定。
                io.github.freewebmovement.android.noui.FwmcService.stop(ctx)
                CoroutineScope(Dispatchers.IO).launch {
                    delay(500)
                    io.github.freewebmovement.android.noui.FwmcService.start(ctx)
                }
                android.widget.Toast.makeText(ctx, s.epoch.saveRestartSuccess, android.widget.Toast.LENGTH_SHORT).show()
            },
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(s.epoch.applyAndRestart)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** 模式选项行：标签 + 说明 + 选中标记。 */
@Composable
private fun ModeOption(label: String, note: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, color = TextPrimary)
            Text(note, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
        }
        if (selected) {
            Text("✓", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
        }
    }
}
