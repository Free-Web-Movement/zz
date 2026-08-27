package io.github.freewebmovement.zz.ui.common

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.freewebmovement.zz.ui.theme.BadgeRed
import io.github.freewebmovement.zz.ui.theme.CardBg
import io.github.freewebmovement.zz.ui.theme.LineColor
import io.github.freewebmovement.zz.ui.theme.OnlineGreen
import io.github.freewebmovement.zz.ui.theme.TextMuted
import io.github.freewebmovement.zz.ui.theme.TextPrimary
import io.github.freewebmovement.zz.ui.theme.TextPrimary
import io.github.freewebmovement.zz.ui.theme.TextSecondary
import kotlin.math.absoluteValue

private val AVATAR_COLORS = listOf(
    Color(0xFF07C160), Color(0xFF2196F3), Color(0xFFFF9800), Color(0xFF9C27B0),
    Color(0xFF00BCD4), Color(0xFFF44336), Color(0xFF3F51B5), Color(0xFF009688),
)

fun avatarColor(seed: String): Color =
    AVATAR_COLORS[seed.hashCode().absoluteValue % AVATAR_COLORS.size]

/** Balance in cents -> "12.34" */
fun formatAmount(cents: Long): String = "${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"

/** Shorten a long address for list display. */
fun shortAddr(addr: String, keep: Int = 14): String =
    if (addr.length <= keep * 2) addr else addr.take(keep) + ".."

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = CardBg,
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            if (title != null || trailing != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title ?: "",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = TextPrimary,
                    )
                    if (trailing != null) trailing()
                }
                Box(modifier = Modifier.padding(bottom = 6.dp))
            }
            content()
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = TextSecondary,
        modifier = modifier.padding(start = 16.dp, top = 10.dp, bottom = 2.dp),
    )
}

@Composable
fun InfoItem(label: String, value: String, mono: Boolean = false) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, fontSize = 11.sp, color = TextSecondary)
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = if (mono) FontFamily.Monospace else null,
            color = TextPrimary,
        )
    }
}

@Composable
fun InfoGrid(items: List<Pair<String, String>>, columns: Int = 3) {
    items.chunked(columns).forEach { rowItems ->
        Row(modifier = Modifier.fillMaxWidth()) {
            rowItems.forEach { (label, value) ->
                Box(modifier = Modifier.weight(1f)) { InfoItem(label, value) }
            }
            repeat(columns - rowItems.size) {
                Box(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatusDot(active: Boolean, size: Dp = 8.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (active) OnlineGreen else TextMuted),
    )
}

@Composable
fun StatusText(active: Boolean, activeLabel: String? = null, inactiveLabel: String? = null) {
    val s = io.github.freewebmovement.zz.ui.i18n.LocalAppStrings.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusDot(active)
        Text(
            text = when {
                active -> activeLabel ?: s.common.active
                else -> inactiveLabel ?: s.common.offline
            },
            fontSize = 12.sp,
            color = if (active) OnlineGreen else TextMuted,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
fun UnreadBadge(count: Int) {
    if (count > 0) {
        Surface(shape = CircleShape, color = BadgeRed) {
            Text(
                text = if (count > 99) "99+" else count.toString(),
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
    }
}

/**
 * Circular avatar: decodes a `data:image/...;base64,...` URI when available,
 * otherwise renders the first character of [name] on a stable colored circle.
 */
@Composable
fun Avatar(name: String, dataUri: String?, size: Dp) {
    val bitmap = remember(dataUri) {
        runCatching {
            val b64 = dataUri?.takeIf { it.startsWith("data:image") }?.substringAfter("base64,", "")
            if (b64.isNullOrEmpty()) null
            else {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }.getOrNull()
    }
    val shownName = name.ifEmpty { "?" }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = shownName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(CircleShape),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(avatarColor(shownName)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = shownName.take(1).uppercase(),
                color = Color.White,
                fontSize = (size.value / 2.2f).sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun MonoText(
    text: String,
    fontSize: Int = 12,
    color: Color = TextPrimary,
    maxLines: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        fontSize = fontSize.sp,
        fontFamily = FontFamily.Monospace,
        color = color,
        maxLines = maxLines,
        modifier = modifier,
    )
}

@Composable
fun EmptyHint(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = TextMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
    )
}

@Composable
fun SubTabs(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    TabRow(
        selectedTabIndex = selected,
        containerColor = CardBg,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        tabs.forEachIndexed { i, label ->
            Tab(
                selected = selected == i,
                onClick = { onSelect(i) },
                text = { Text(label) },
            )
        }
    }
}

@Composable
fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(0.5.dp)
            .background(LineColor),
    )
}

@Composable
fun KeyValueRow(key: String, value: String, mono: Boolean = true) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            text = key,
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.weight(0.35f),
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = if (mono) FontFamily.Monospace else null,
            color = TextPrimary,
            modifier = Modifier.weight(0.65f),
        )
    }
}
