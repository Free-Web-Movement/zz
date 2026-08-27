package io.github.freewebmovement.zz.ui.content.user

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.freewebmovement.zz.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import rs.zz.coin.FwmcApi

/**
 * 分享我的身份：Peer ID 二维码 + 数字别名 + 复制。
 * peerId 为钱包地址（Peer ID）。
 */
@Composable
fun PeerIdShareDialog(peerId: String, onDismiss: () -> Unit) {
    val s = io.github.freewebmovement.zz.ui.i18n.LocalAppStrings.current
    var qr by remember { mutableStateOf<Pair<Int, String>?>(null) }

    androidx.compose.runtime.LaunchedEffect(peerId) {
        if (peerId.isEmpty()) return@LaunchedEffect
        val (w, bits) = withContext(Dispatchers.IO) {
            runCatching {
                val o = JSONObject(FwmcApi.qrMatrix(peerId))
                o.optInt("width") to o.optString("data", "")
            }.getOrDefault(0 to "")
        }
        qr = w to bits
    }

    val ctx = LocalContext.current
    val clip = androidx.compose.ui.platform.LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.peerIdDialog.title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (qr != null && qr!!.first > 0) {
                    val (w, bits) = qr!!
                    // 白底 + 深色模块，含静区
                    val module = 5f
                    Canvas(
                        modifier = Modifier
                            .size(220.dp)
                            .align(Alignment.CenterHorizontally)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                    ) {
                        val n = w
                        val cells = n.toFloat()
                        val cell = (size.minDimension - 24.dp.toPx()) / cells
                        val start = 12.dp.toPx()
                        for (i in 0 until n) {
                            for (j in 0 until n) {
                                if (bits.getOrNull(i * n + j) == '1') {
                                    drawRect(
                                        color = Color.Black,
                                        topLeft = Offset(start + j * cell, start + i * cell),
                                        size = androidx.compose.ui.geometry.Size(cell + 0.5f, cell + 0.5f),
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(s.peerIdDialog.generating, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Peer ID " + peerId,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_copy),
                        contentDescription = s.common.copy,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(16.dp)
                            .clickable {
                                clip.setText(androidx.compose.ui.text.AnnotatedString(peerId))
                                android.widget.Toast.makeText(ctx, s.common.copied, android.widget.Toast.LENGTH_SHORT).show()
                            },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(s.peerIdDialog.close) }
        },
    )
}
