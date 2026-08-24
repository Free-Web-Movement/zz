package io.github.freewebmovement.zz.ui.content.user

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import io.github.freewebmovement.zz.R
import io.github.freewebmovement.zz.ui.common.PageType
import io.github.freewebmovement.zz.ui.theme.CardBg
import io.github.freewebmovement.zz.ui.theme.LineColor
import io.github.freewebmovement.zz.ui.theme.TextPrimary
import io.github.freewebmovement.zz.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import org.json.JSONObject
import rs.zz.coin.FwmcApi

/**
 * 帐号资料编辑器（user/profileeditor）：完全独立的实现，不依赖 mine 的 ProfileEditor。
 * 从帐号管理「编辑」进入，编辑指定帐号的全部资料（含头像）。
 * 保存后回到帐号列表并触发刷新。
 */
@Composable
fun UserProfileEditor(updatePage: (value: PageType) -> Unit) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val target = remember { editAccount }
        .also { editAccount = null }
    // 强绑定：用户信息绑定到节点/钱包地址（Peer ID），帐号只是本地皮肤
    val targetId = target?.first ?: ""
    val skinName = target?.second ?: ""

    var nickname by remember { mutableStateOf(skinName) }
    var intro by remember(targetId) { mutableStateOf("") }
    var imageUri by remember(targetId) { mutableStateOf<android.net.Uri?>(null) }
    var gender by remember(targetId) { mutableStateOf("") }
    var age by remember(targetId) { mutableStateOf("") }
    var country by remember(targetId) { mutableStateOf("") }
    var ethnicity by remember(targetId) { mutableStateOf("") }
    var bloodType by remember(targetId) { mutableStateOf("") }
    var heightCm by remember(targetId) { mutableStateOf("") }
    var weightKg by remember(targetId) { mutableStateOf("") }
    var education by remember(targetId) { mutableStateOf("") }
    var homeAddress by remember(targetId) { mutableStateOf("") }
    var occupation by remember(targetId) { mutableStateOf("") }
    var zodiac by remember(targetId) { mutableStateOf("") }
    var province by remember(targetId) { mutableStateOf("") }
    var city by remember(targetId) { mutableStateOf("") }
    var county by remember(targetId) { mutableStateOf("") }
    var town by remember(targetId) { mutableStateOf("") }
    var village by remember(targetId) { mutableStateOf("") }
    var pickerTarget by remember { mutableStateOf<String?>(null) }
    var showCountryPicker by remember { mutableStateOf(false) }

    // 载入目标帐号资料（节点未就绪时重试）
    LaunchedEffect(targetId) {
        repeat(30) {
            val ok = runCatching {
                val p = JSONObject(FwmcApi.getProfile(targetId)).optJSONObject("profile")
                if (p != null) {
                    nickname = p.optString("nickname").ifEmpty { p.optString("name") }
                        .takeIf { it.isNotEmpty() } ?: nickname
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
                    val av = p.optString("avatar_path")
                    if (av.startsWith("data:image/")) imageUri = android.net.Uri.parse(av)
                }
                true
            }.getOrDefault(false)
            if (ok) return@LaunchedEffect
            kotlinx.coroutines.delay(500)
        }
    }

    val pickAvatar = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        if (uri != null && targetId.isNotEmpty()) {
            val bytes = readJpegBytes(ctx, uri, maxDim = 512)
            if (bytes != null) {
                scope.launch {
                    val ok = runCatching {
                        JSONObject(FwmcApi.setAvatarFor(targetId, bytes)).optBoolean("success", false)
                    }.getOrDefault(false)
                    if (ok) {
                        val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        imageUri = android.net.Uri.parse("data:image/jpeg;base64,$b64")
                    } else {
                        android.widget.Toast.makeText(ctx, "头像上传失败", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState())) {
        // ---- 头像（可点更换）----
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(modifier = Modifier.clickable {
                pickAvatar.launch(androidx.activity.result.PickVisualMediaRequest(
                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }) {
                if (imageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(ctx).data(imageUri).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.ic_default_avatar),
                        error = painterResource(R.drawable.ic_default_avatar),
                        modifier = Modifier.size(128.dp).clip(CircleShape),
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.ic_default_avatar),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(128.dp).clip(CircleShape),
                    )
                }
                Box(modifier = Modifier.matchParentSize().border(3.dp, MaterialTheme.colorScheme.primary, CircleShape))
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-3).dp, y = (-3).dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("＋", color = Color.White, fontSize = 17.sp)
                }
            }
        }

        // ---- 昵称 / 签名 卡 ----
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CardBg,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                val fieldColors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                )
                Text("昵称", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(top = 12.dp))
                TextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                    colors = fieldColors,
                    singleLine = true,
                )
                HorizontalDivider(color = LineColor)
                Text("签名", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(top = 12.dp))
                TextField(
                    value = intro,
                    onValueChange = { intro = it },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                    colors = fieldColors,
                )
            }
        }

        // ---- 详细信息卡 ----
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CardBg,
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                val fc = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                )
                Text("详细信息", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(top = 12.dp))
                Spacer(Modifier.height(4.dp))

                @Composable fun RowScope.ExtField(
                    label: String,
                    value: String,
                    onValue: (String) -> Unit,
                    number: Boolean = false,
                    picker: String? = null,
                ) {
                    if (picker != null) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, LineColor, RoundedCornerShape(8.dp))
                                .clickable {
                                    when (picker) {
                                        "国家" -> showCountryPicker = true
                                        else -> pickerTarget = picker
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Text(label, fontSize = 12.sp, color = TextSecondary)
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    value.ifEmpty { "请选择" },
                                    fontSize = 14.sp,
                                    color = if (value.isEmpty()) TextSecondary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text("›", fontSize = 16.sp, color = TextSecondary)
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = value,
                            onValueChange = onValue,
                            label = { Text(label, fontSize = 12.sp, color = TextSecondary) },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Number else KeyboardType.Text),
                            colors = fc,
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

        // ---- 所在地区卡 ----
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CardBg,
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                val rc = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                )
                Text("所在地区", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(top = 12.dp))
                Spacer(Modifier.height(4.dp))

                @Composable fun RowScope.RegField(label: String, value: String, onValue: (String) -> Unit) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = onValue,
                        label = { Text(label, fontSize = 12.sp, color = TextSecondary) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                        colors = rc,
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

        // ---- 更多卡（星座/职业）----
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CardBg,
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                val mc = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                )
                Text("更多", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(top = 12.dp))
                Spacer(Modifier.height(4.dp))

                @Composable fun RowScope.ZodiacField() {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, LineColor, RoundedCornerShape(8.dp))
                            .clickable { pickerTarget = "星座" }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painter = painterResource(R.drawable.ic_zodiac), contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("星座", fontSize = 12.sp, color = TextSecondary)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                zodiac.ifEmpty { "请选择" },
                                fontSize = 14.sp,
                                color = if (zodiac.isEmpty()) TextSecondary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text("›", fontSize = 16.sp, color = TextSecondary)
                        }
                    }
                }

                Row(Modifier.fillMaxWidth()) {
                    ZodiacField()
                    Spacer(Modifier.width(10.dp))
                    OutlinedTextField(
                        value = occupation,
                        onValueChange = { occupation = it },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(painter = painterResource(R.drawable.ic_briefcase), contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("职业", fontSize = 12.sp, color = TextSecondary)
                            }
                        },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                        colors = mc,
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                    )
                }
            }
        }

        // ---- 保存 ----
        Button(
            onClick = {
                scope.launch {
                    runCatching {
                        val base = JSONObject(FwmcApi.getProfile(targetId)).optJSONObject("profile")
                        val body = base ?: JSONObject()
                        if (body.optString("avatar_path").startsWith("data:")) body.remove("avatar_path")
                        body.put("nickname", nickname)
                        body.put("name", nickname)
                        body.put("notes", intro)
                        body.put("gender", gender)
                        body.put("age", age.toIntOrNull() ?: 0)
                        body.put("country", country)
                        body.put("ethnicity", ethnicity)
                        body.put("blood_type", bloodType)
                        body.put("height_cm", heightCm.toIntOrNull() ?: 0)
                        body.put("weight_kg", weightKg.toIntOrNull() ?: 0)
                        body.put("education", education)
                        body.put("home_address", homeAddress)
                        body.put("occupation", occupation)
                        body.put("zodiac", zodiac)
                        body.put("province", province)
                        body.put("city", city)
                        body.put("county", county)
                        body.put("town", town)
                        body.put("village", village)
                        JSONObject(FwmcApi.saveProfileFor(targetId, body.toString()))
                    }
                    if (nickname.isNotBlank()) {
                        runCatching { JSONObject(FwmcApi.renameAccount(targetId, nickname)) }
                    }
                    accountsRefreshSignal++
                    mineRefreshSignal++
                    updatePage(PageType.MineAccounts)
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
            Text(ctx.getString(R.string.action_save))
        }
        Spacer(Modifier.height(24.dp))
    }

    // ---- 单选弹窗 ----
    val options: Map<String, List<String>> = mapOf(
        "性别" to listOf("男", "女", "保密"),
        "血型" to listOf("A", "B", "AB", "O"),
        "学历" to listOf("小学", "初中", "高中", "大专", "本科", "硕士", "博士"),
        "星座" to listOf("白羊座", "金牛座", "双子座", "巨蟹座", "狮子座", "处女座", "天秤座", "天蝎座", "射手座", "摩羯座", "水瓶座", "双鱼座"),
    )
    pickerTarget?.let { targetName ->
        val opts = options[targetName].orEmpty()
        val current = when (targetName) {
            "性别" -> gender
            "血型" -> bloodType
            "星座" -> zodiac
            else -> education
        }
        AlertDialog(
            onDismissRequest = { pickerTarget = null },
            title = { Text(targetName) },
            text = {
                Column {
                    opts.forEach { opt ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable {
                                when (targetName) {
                                    "性别" -> gender = opt
                                    "血型" -> bloodType = opt
                                    "星座" -> zodiac = opt
                                    "学历" -> education = opt
                                }
                                pickerTarget = null
                            },
                        ) {
                            RadioButton(selected = opt == current, onClick = null)
                            Text(opt, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }


    // ---- 国家选择器 ----
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
