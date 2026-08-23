package io.github.freewebmovement.zz.ui.content.mine

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.R
import io.github.freewebmovement.zz.ui.common.PageType

/** 帐号管理点「编辑」时设置的目标帐号 (id to name)；其它入口为 null（编辑当前帐号）。 */
var editAccount: Pair<String, String>? = null

/** 帐号编辑保存后自增，通知帐号列表重新加载（含头像）。 */
var accountsRefreshSignal by androidx.compose.runtime.mutableIntStateOf(0)


@Composable
fun ProfileEditor(updatePage: (value: PageType) -> Unit) {
    ProfileEditorContent(updatePage, targetId = null, backTo = PageType.MineMain)
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
internal fun ProfileEditorContent(
    updatePage: (value: PageType) -> Unit,
    targetId: String?,
    backTo: PageType,
    onAfterSave: () -> Unit = {},
    targetName: String? = null,
) {
    val saveScope = androidx.compose.runtime.rememberCoroutineScope()
    with(MainApplication.getApp().settings) {
        var nickname by remember { mutableStateOf(targetName ?: (FwmcSession.current?.second ?: profile.nickname)) }
        var intro by remember { mutableStateOf("") }
        // ---- 扩展资料 ----
        var gender by remember { mutableStateOf("") }
        var age by remember { mutableStateOf("") }
        var country by remember { mutableStateOf("") }
        var ethnicity by remember { mutableStateOf("") }
        var bloodType by remember { mutableStateOf("") }
        var heightCm by remember { mutableStateOf("") }
        var weightKg by remember { mutableStateOf("") }
        var education by remember { mutableStateOf("") }
        var homeAddress by remember { mutableStateOf("") }
        var occupation by remember { mutableStateOf("") }
        var zodiac by remember { mutableStateOf("") }
        var province by remember { mutableStateOf("") }
        var city by remember { mutableStateOf("") }
        var county by remember { mutableStateOf("") }
        var town by remember { mutableStateOf("") }
        var village by remember { mutableStateOf("") }
        var showCountryPicker by remember { mutableStateOf(false) }
        var pickerTarget by remember { mutableStateOf<String?>(null) }
        val userId = targetId ?: FwmcSession.current?.first ?: ""
        var imageUri by remember { mutableStateOf<Uri?>(null) }
        androidx.compose.runtime.LaunchedEffect(Unit) {
            // 我的资料编辑：用本地设置的图片；帐号编辑：由资料头像覆盖
            if (targetId == null) {
                val v = profile.imageUri
                imageUri = if (v.isNotEmpty() && v != "null") Uri.parse(v) else null
            }
        }
        androidx.compose.runtime.LaunchedEffect(Unit) {
            // 从 FWMC 资料接口载入目标帐号资料；节点未就绪时重试，避免冷启动后空白
            repeat(30) {
                val ok = runCatching {
                    val p = JSONObject(rs.zz.coin.FwmcApi.getProfile(targetId ?: "")).optJSONObject("profile")
                    if (p != null) {
                        p.optString("nickname").ifEmpty { p.optString("name") }.takeIf { it.isNotEmpty() }?.let { nickname = it }
                        intro = p.optString("notes").ifEmpty { p.optString("bio") }
                        gender = p.optString("gender")
                        age = p.optInt("age", 0).takeIf { it > 0 }?.toString() ?: ""
                        country = p.optString("country")
                        ethnicity = p.optString("ethnicity")
                        bloodType = p.optString("blood_type")
                        heightCm = p.optInt("height_cm", 0).takeIf { it > 0 }?.toString() ?: ""
                        weightKg = p.optInt("weight_kg", 0).takeIf { it > 0 }?.toString() ?: ""
                        education = p.optString("education")
                        homeAddress = p.optString("home_address")
                        occupation = p.optString("occupation")
                        zodiac = p.optString("zodiac")
                        province = p.optString("province")
                        city = p.optString("city")
                        county = p.optString("county")
                        town = p.optString("town")
                        village = p.optString("village")
                    }
                    if (targetId != null) {
                        val av = p.optString("avatar_path")
                        if (av.startsWith("data:image/")) {
                            imageUri = Uri.parse(av)
                        }
                    }
                    true
                }.getOrDefault(false)
                if (ok) return@LaunchedEffect
                kotlinx.coroutines.delay(500)
            }
        }
        val ctx = LocalContext.current
        val pickMedia =
            rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    Log.d("PhotoPicker", "Selected URI: $uri")
                    if (targetId != null) {
                        // 帐号编辑：上传到头像目录并刷新列表
                        val bytes = readJpegBytes(ctx, uri, maxDim = 512)
                        if (bytes != null) {
                            saveScope.launch {
                                runCatching {
                                    JSONObject(rs.zz.coin.FwmcApi.setAvatarFor(targetId, bytes)).optBoolean("success", false)
                                }.getOrDefault(false).let { ok ->
                                    if (ok) {
                                        val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                                        imageUri = Uri.parse("data:image/jpeg;base64,$b64")
                                    } else {
                                        android.widget.Toast.makeText(ctx, "头像上传失败", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    } else {
                        runCatching {
                            ctx.contentResolver.takePersistableUriPermission(
                                uri,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                            )
                        }
                        imageUri = uri
                    }
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

            // ---- 扩展信息卡 ----
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = io.github.freewebmovement.zz.ui.theme.CardBg,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                    val fieldColors = TextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    )
                    Text("详细信息", fontSize = 13.sp, color = io.github.freewebmovement.zz.ui.theme.TextSecondary, modifier = Modifier.padding(top = 12.dp))
                    Spacer(Modifier.height(4.dp))

                    val pickerModifier: (String) -> Modifier = { target ->
                        Modifier.clickable { pickerTarget = target }
                    }
                    @Composable fun RowScope.ExtField(
                        label: String,
                        value: String,
                        onValue: (String) -> Unit,
                        number: Boolean = false,
                        picker: String? = null,
                        icon: Int? = null,
                    ) {
                        @Composable fun Label() {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (icon != null) {
                                    Icon(
                                        painter = painterResource(id = icon),
                                        contentDescription = null,
                                        tint = io.github.freewebmovement.zz.ui.theme.TextSecondary,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Spacer(Modifier.width(3.dp))
                                }
                                Text(label, fontSize = 12.sp, color = io.github.freewebmovement.zz.ui.theme.TextSecondary)
                            }
                        }
                        if (picker != null) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, io.github.freewebmovement.zz.ui.theme.LineColor, RoundedCornerShape(8.dp))
                                    .clickable {
                                        when (picker) {
                                            "国家" -> showCountryPicker = true
                                            else -> pickerTarget = picker
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                Label()
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        value.ifEmpty { "请选择" },
                                        fontSize = 14.sp,
                                        color = if (value.isEmpty()) io.github.freewebmovement.zz.ui.theme.TextSecondary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text("›", fontSize = 16.sp, color = io.github.freewebmovement.zz.ui.theme.TextSecondary)
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = value,
                                onValueChange = onValue,
                                label = { Label() },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                                keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Number else KeyboardType.Text),
                                colors = fieldColors,
                                modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                            )
                        }
                    }

                    Row(Modifier.fillMaxWidth()) {
                        ExtField("性别", gender, { gender = it }, picker = "性别")
                        Spacer(Modifier.width(10.dp))
                        ExtField("血型", bloodType, { bloodType = it }, picker = "血型")
                    }
                    Row(Modifier.fillMaxWidth()) {
                        ExtField("年龄", age, { age = it.filter { c -> c.isDigit() }.take(3) }, number = true)
                        Spacer(Modifier.width(10.dp))
                        ExtField("身高 (cm)", heightCm, { heightCm = it.filter { c -> c.isDigit() }.take(3) }, number = true)
                    }
                    Row(Modifier.fillMaxWidth()) {
                        ExtField("体重 (kg)", weightKg, { weightKg = it.filter { c -> c.isDigit() }.take(3) }, number = true)
                        Spacer(Modifier.width(10.dp))
                        ExtField("民族", ethnicity, { ethnicity = it })
                    }
                    Row(Modifier.fillMaxWidth()) {
                        ExtField("学历", education, { education = it }, picker = "学历")
                        Spacer(Modifier.width(10.dp))
                        ExtField("国家 / 地区", country, { country = it }, picker = "国家")
                    }
                }
            }

            // ---- 所在地区卡：省/市/县/镇/村 ----
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = io.github.freewebmovement.zz.ui.theme.CardBg,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                    val regionColors = TextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    )
                    Text("所在地区", fontSize = 13.sp, color = io.github.freewebmovement.zz.ui.theme.TextSecondary, modifier = Modifier.padding(top = 12.dp))
                    Spacer(Modifier.height(4.dp))

                    @Composable fun RowScope.RegField(label: String, value: String, onValue: (String) -> Unit) {
                        OutlinedTextField(
                            value = value,
                            onValueChange = onValue,
                            label = { Text(label, fontSize = 12.sp, color = io.github.freewebmovement.zz.ui.theme.TextSecondary) },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                            colors = regionColors,
                            modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                        )
                    }

                    Row(Modifier.fillMaxWidth()) {
                        RegField("省 / 州", province, { province = it })
                        Spacer(Modifier.width(10.dp))
                        RegField("市", city, { city = it })
                    }
                    Row(Modifier.fillMaxWidth()) {
                        RegField("区 / 县", county, { county = it })
                        Spacer(Modifier.width(10.dp))
                        RegField("镇 / 乡", town, { town = it })
                    }
                    Row(Modifier.fillMaxWidth()) {
                        RegField("村 / 社区", village, { village = it })
                        Spacer(Modifier.width(10.dp))
                        RegField("详细地址", homeAddress, { homeAddress = it })
                    }
                }
            }

            // ---- 更多卡：星座 / 职业（带图标）----
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = io.github.freewebmovement.zz.ui.theme.CardBg,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                    val moreColors = TextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    )
                    Text("更多", fontSize = 13.sp, color = io.github.freewebmovement.zz.ui.theme.TextSecondary, modifier = Modifier.padding(top = 12.dp))
                    Spacer(Modifier.height(4.dp))

                    @Composable fun MoreLabel(label: String, icon: Int) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painter = painterResource(id = icon), contentDescription = null, tint = io.github.freewebmovement.zz.ui.theme.TextSecondary, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(label, fontSize = 12.sp, color = io.github.freewebmovement.zz.ui.theme.TextSecondary)
                        }
                    }

                    @Composable fun RowScope.ZodiacField() {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, io.github.freewebmovement.zz.ui.theme.LineColor, RoundedCornerShape(8.dp))
                                .clickable { pickerTarget = "星座" }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            MoreLabel("星座", R.drawable.ic_zodiac)
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    zodiac.ifEmpty { "请选择" },
                                    fontSize = 14.sp,
                                    color = if (zodiac.isEmpty()) io.github.freewebmovement.zz.ui.theme.TextSecondary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text("›", fontSize = 16.sp, color = io.github.freewebmovement.zz.ui.theme.TextSecondary)
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth()) {
                        ZodiacField()
                        Spacer(Modifier.width(10.dp))
                        OutlinedTextField(
                            value = occupation,
                            onValueChange = { occupation = it },
                            label = { MoreLabel("职业", R.drawable.ic_briefcase) },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                            colors = moreColors,
                            modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                        )
                    }
                }
            }

            Button(
                onClick = {
                    with(MainApplication.getApp().settings) {
                        profile.imageUri = imageUri.toString()
                        profile.nickname = nickname
                        profile.intro = intro
                    }
                    // 同步到 FWMC 资料（对应当前帐号身份）
                    saveScope.launch {
                        runCatching {
                            org.json.JSONObject(rs.zz.coin.FwmcApi.getProfile(targetId ?: "")).optJSONObject("profile")?.let { existing ->
                                existing.put("nickname", nickname)
                                existing.put("name", nickname)
                                existing.put("notes", intro)
                                existing.put("gender", gender)
                                existing.put("age", age.toIntOrNull() ?: 0)
                                existing.put("country", country)
                                existing.put("ethnicity", ethnicity)
                                existing.put("blood_type", bloodType)
                                existing.put("height_cm", heightCm.toIntOrNull() ?: 0)
                                existing.put("weight_kg", weightKg.toIntOrNull() ?: 0)
                                existing.put("education", education)
                                existing.put("home_address", homeAddress)
                                existing.put("occupation", occupation)
                                existing.put("zodiac", zodiac)
                                existing.put("province", province)
                                existing.put("city", city)
                                existing.put("county", county)
                                existing.put("town", town)
                                existing.put("village", village)
                                // 不把 base64 数据 URI 回写存储（avatar_path 应为相对路径）
                                if (existing.optString("avatar_path").startsWith("data:")) {
                                    existing.remove("avatar_path")
                                }
                                rs.zz.coin.FwmcApi.saveProfileFor(targetId ?: "", existing.toString())
                            } ?: run {
                                rs.zz.coin.FwmcApi.saveProfileFor(targetId ?: "", org.json.JSONObject().apply {
                                    put("nickname", nickname)
                                    put("name", nickname)
                                    put("notes", intro)
                                    put("gender", gender)
                                    put("age", age.toIntOrNull() ?: 0)
                                    put("country", country)
                                    put("ethnicity", ethnicity)
                                    put("blood_type", bloodType)
                                    put("height_cm", heightCm.toIntOrNull() ?: 0)
                                    put("weight_kg", weightKg.toIntOrNull() ?: 0)
                                    put("education", education)
                                    put("home_address", homeAddress)
                                    put("occupation", occupation)
                                    put("zodiac", zodiac)
                                    put("province", province)
                                    put("city", city)
                                    put("county", county)
                                    put("town", town)
                                    put("village", village)
                                }.toString())
                            }
                        }
                        if (targetId != null && nickname.isNotBlank()) {
                            // 帐号名 = 昵称：列表显示昵称
                            runCatching { rs.zz.coin.FwmcApi.renameAccount(targetId, nickname) }
                        }
                        updatePage(backTo)
                        onAfterSave()
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

            val options: Map<String, List<String>> = mapOf(
                "性别" to listOf("男", "女", "保密"),
                "血型" to listOf("A", "B", "AB", "O"),
                "学历" to listOf("小学", "初中", "高中", "大专", "本科", "硕士", "博士"),
                "星座" to listOf("白羊座","金牛座","双子座","巨蟹座","狮子座","处女座","天秤座","天蝎座","射手座","摩羯座","水瓶座","双鱼座"),
            )
            val zodiacSymbols = mapOf(
                "白羊座" to "♈","金牛座" to "♉","双子座" to "♊","巨蟹座" to "♋","狮子座" to "♌","处女座" to "♍",
                "天秤座" to "♎","天蝎座" to "♏","射手座" to "♐","摩羯座" to "♑","水瓶座" to "♒","双鱼座" to "♓",
            )
            pickerTarget?.let { target ->
                val opts = options[target].orEmpty()
                val current = when (target) {
                    "性别" -> gender
                    "血型" -> bloodType
                    "星座" -> zodiac
                    else -> education
                }
                AlertDialog(
                    onDismissRequest = { pickerTarget = null },
                    title = { Text(target) },
                    text = {
                        Column {
                            opts.forEach { opt ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        when (target) {
                                            "性别" -> gender = opt
                                            "血型" -> bloodType = opt
                                            "星座" -> zodiac = opt
                                            "学历" -> education = opt
                                        }
                                        pickerTarget = null
                                    },
                                ) {
                                    RadioButton(selected = opt == current, onClick = null)
                                    if (target == "星座") {
                                        Text(zodiacSymbols[opt] ?: "", fontSize = 17.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
                                    }
                                    Text(opt, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    },
                    confirmButton = {},
                )
            }

            if (showCountryPicker) {
                val allCountries = remember {
                    java.util.Locale.getISOCountries()
                        .mapNotNull { code ->
                            val zh = java.util.Locale("zh", "", code).getDisplayCountry(java.util.Locale.SIMPLIFIED_CHINESE)
                            zh.ifEmpty { java.util.Locale("", code).displayCountry }.takeIf { it.isNotBlank() }
                        }
                        .distinct()
                        .sortedWith(java.text.Collator.getInstance(java.util.Locale.SIMPLIFIED_CHINESE))
                }
                var query by remember { mutableStateOf("") }
                val filtered = remember(query) {
                    if (query.isEmpty()) allCountries else allCountries.filter { it.contains(query, ignoreCase = true) }
                }
                AlertDialog(
                    onDismissRequest = { showCountryPicker = false },
                    title = { Text("国家 / 地区") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                placeholder = { Text("搜索国家…", fontSize = 13.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                                items(filtered.size) { i ->
                                    val name = filtered[i]
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                country = name
                                                showCountryPicker = false
                                            }
                                            .padding(vertical = 10.dp),
                                    ) {
                                        RadioButton(selected = name == country, onClick = null)
                                        Text(name, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 8.dp))
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                )
            }
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