package io.github.freewebmovement.zz.ui.content.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.freewebmovement.zz.ui.common.Divider
import io.github.freewebmovement.zz.ui.theme.TextPrimary
import io.github.freewebmovement.zz.ui.theme.AppTheme
import io.github.freewebmovement.zz.ui.theme.WxBg

/** 设置页：子目录入口（主题配色等）。 */
@Composable
fun SettingsScreen(onBack: () -> Unit = {}) {
    var showTheme by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(WxBg)) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(io.github.freewebmovement.zz.ui.theme.CardBg)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text("设置", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clickable { showTheme = true },
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("主题配色", fontSize = 15.sp, color = TextPrimary)
                Text("绿 / 蓝 / 粉 / 紫 / 橙", fontSize = 11.sp, color = io.github.freewebmovement.zz.ui.theme.TextMuted)
            }
            Text(AppTheme.preset.label, fontSize = 13.sp, color = io.github.freewebmovement.zz.ui.theme.TextSecondary)
            Text("  ›", color = io.github.freewebmovement.zz.ui.theme.TextMuted)
        }
        Divider()
    }

    if (showTheme) {
        ThemeDialog(onDismiss = { showTheme = false })
    }
}
