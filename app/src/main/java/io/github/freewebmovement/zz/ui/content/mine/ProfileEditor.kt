package io.github.freewebmovement.zz.ui.content.mine

import androidx.compose.material3.MaterialTheme
import org.json.JSONObject
import kotlinx.coroutines.launch
import io.github.freewebmovement.zz.ui.content.FwmcSession
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.R
import io.github.freewebmovement.zz.ui.common.PageType


@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun ProfileEditor(updatePage: (value: PageType) -> Unit) {
    val saveScope = androidx.compose.runtime.rememberCoroutineScope()
    with(MainApplication.getApp().settings) {
        var nickname by remember { mutableStateOf(FwmcSession.current?.second ?: profile.nickname) }
        var intro by remember { mutableStateOf("") }
        val userId = FwmcSession.current?.first ?: ""
        androidx.compose.runtime.LaunchedEffect(Unit) {
            // 昵称/签名与当前帐号对应：从 FWMC 资料接口载入
            runCatching {
                val p = JSONObject(rs.zz.coin.FwmcApi.getProfile()).optJSONObject("profile")
                if (p != null) {
                    p.optString("name").ifEmpty { p.optString("nickname") }.takeIf { it.isNotEmpty() }?.let { nickname = it }
                    intro = p.optString("notes").ifEmpty { p.optString("bio") }
                }
            }
        }
        var imageUri by remember { mutableStateOf(Uri.parse(profile.imageUri).takeIf { profile.imageUri.isNotEmpty() && profile.imageUri != "null" }) }
        val ctx = LocalContext.current
        val pickMedia =
            rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                // Callback is invoked after the user selects a media item or closes the
                // photo picker.
                if (uri != null) {
                    Log.d("PhotoPicker", "Selected URI: $uri")
                    runCatching {
                        ctx.contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    }
                    imageUri = uri
                } else {
                    Log.d("PhotoPicker", "No media selected")
                }
            }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
        ) {
            // 用户ID 卡片：微信风格 —— 页边距16，卡内边距 14/12
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = io.github.freewebmovement.zz.ui.theme.CardBg,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text("用户ID", fontSize = 13.sp, color = io.github.freewebmovement.zz.ui.theme.TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            userId.ifEmpty { "-" },
                            fontSize = 15.sp,
                            color = io.github.freewebmovement.zz.ui.theme.TextPrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        val clip = androidx.compose.ui.platform.LocalClipboardManager.current
                        val ctx = LocalContext.current
                        Icon(
                            painter = painterResource(id = R.drawable.ic_copy),
                            contentDescription = "copy",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp).size(18.dp).clickable {
                                clip.setText(androidx.compose.ui.text.AnnotatedString(userId))
                                android.widget.Toast.makeText(ctx, ctx.getString(R.string.copied), android.widget.Toast.LENGTH_SHORT).show()
                            },
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .clickable {
                            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                ) {
                    if (!imageUri?.toString().isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUri)
                                .build(),
                            contentDescription = stringResource(id = R.string.tab_mine_avatar),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(id = R.drawable.ic_default_avatar),
                            error = painterResource(id = R.drawable.ic_default_avatar),
                            modifier = Modifier
                                .size(128.dp)
                                .clip(CircleShape),
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_default_avatar),
                            contentDescription = stringResource(id = R.string.tab_mine_avatar),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(128.dp)
                                .clip(CircleShape),
                        )
                    }
                    // 明显装饰：主题色描边 + 相机角标
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    )
                    // 角标骑缝圆环右下45°：理想左上角(95,95)dp，BottomEnd锚点(98,98)dp → offset(-3)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-3).dp, y = (-3).dp)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("＋", color = androidx.compose.ui.graphics.Color.White, fontSize = 17.sp)
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = io.github.freewebmovement.zz.ui.theme.CardBg,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                    val fieldColors = TextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    )
                    Text(
                        stringResource(R.string.tab_mine_nickname),
                        fontSize = 13.sp,
                        color = io.github.freewebmovement.zz.ui.theme.TextSecondary,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    TextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.material3.LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                        colors = fieldColors,
                        singleLine = true,
                    )
                    HorizontalDivider(color = io.github.freewebmovement.zz.ui.theme.LineColor)
                    Text(
                        stringResource(R.string.signature),
                        fontSize = 13.sp,
                        color = io.github.freewebmovement.zz.ui.theme.TextSecondary,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    TextField(
                        value = intro,
                        onValueChange = { intro = it },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.material3.LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                        colors = fieldColors,
                    )
                }
            }

            Button(
                onClick = {
                    updatePage(PageType.MineMain)
                    with(MainApplication.getApp().settings) {
                        profile.imageUri = imageUri.toString()
                        profile.nickname = nickname
                        profile.intro = intro
                    }
                    // 同步到 FWMC 资料（对应当前帐号身份）
                    saveScope.launch {
                        runCatching {
                            org.json.JSONObject(rs.zz.coin.FwmcApi.getProfile()).optJSONObject("profile")?.let { existing ->
                                existing.put("name", nickname)
                                existing.put("nickname", nickname)
                                existing.put("notes", intro)
                                rs.zz.coin.FwmcApi.saveProfile(existing.toString())
                            } ?: run {
                                rs.zz.coin.FwmcApi.saveProfile(org.json.JSONObject().apply {
                                    put("name", nickname)
                                    put("nickname", nickname)
                                    put("notes", intro)
                                }.toString())
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 20.dp)
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(stringResource(R.string.action_save))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Preview(locale = "zh")
@Composable
private fun Preview() {
    ProfileEditor {

    }
}

@Preview(locale = "en")
@Composable
private fun Preview_en() {
    ProfileEditor {
    }
}