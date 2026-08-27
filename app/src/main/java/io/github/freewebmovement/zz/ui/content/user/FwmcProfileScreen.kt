package io.github.freewebmovement.zz.ui.content.user

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.freewebmovement.zz.ui.common.Avatar
import io.github.freewebmovement.zz.ui.common.SectionCard
import io.github.freewebmovement.zz.ui.theme.CardBg
import io.github.freewebmovement.zz.ui.theme.TextMuted
import io.github.freewebmovement.zz.ui.theme.TextPrimary
import io.github.freewebmovement.zz.ui.theme.WxBg

import kotlinx.coroutines.launch
import org.json.JSONObject
import rs.zz.coin.FwmcApi
import java.io.ByteArrayOutputStream

private data class FwmcProfile(
    val nickname: String = "",
    val gender: String = "",
    val age: String = "",
    val country: String = "",
    val ethnicity: String = "",
    val bloodType: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val education: String = "",
    val homeAddress: String = "",
    val bio: String = "",
    val avatarDataUri: String? = null,
)

/**
 * FWMC 资料编辑（对应 WebUI 的 profile 页面，字段完全一致 + 头像上传）。
 */
@Composable
fun FwmcProfileScreen() {
    val s = io.github.freewebmovement.zz.ui.i18n.LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf(FwmcProfile()) }
    var loaded by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf("") }
    var statusError by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching {
            val obj = JSONObject(FwmcApi.getProfile())
            if (obj.optBoolean("success", false)) {
                val p = obj.optJSONObject("profile") ?: JSONObject()
                fun str(k: String) = when {
                    p.isNull(k) -> ""
                    else -> p.optString(k)
                }
                profile = FwmcProfile(
                    nickname = str("nickname"),
                    gender = str("gender"),
                    age = if (p.optInt("age", 0) > 0) p.optInt("age", 0).toString() else "",
                    country = str("country"),
                    ethnicity = str("ethnicity"),
                    bloodType = str("blood_type"),
                    heightCm = if (p.optInt("height_cm", 0) > 0) p.optInt("height_cm", 0).toString() else "",
                    weightKg = if (p.optInt("weight_kg", 0) > 0) p.optInt("weight_kg", 0).toString() else "",
                    education = str("education"),
                    homeAddress = str("home_address"),
                    bio = str("bio"),
                    avatarDataUri = str("avatar_path").ifEmpty { null },
                )
            }
            loaded = true
        }
    }

    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = readJpegBytes(context, uri, maxDim = 512)
            if (bytes == null) {
                statusError = true
                statusMsg = s.fwmcProfile.loadImageError
                return@launch
            }
            statusError = false
            statusMsg = s.fwmcProfile.uploading
            val raw = FwmcApi.setAvatar(bytes)
            val ok = runCatching { JSONObject(raw).optBoolean("success", false) }.getOrDefault(false)
            if (ok) {
                statusError = false
                statusMsg = s.fwmcProfile.avatarUploaded
                // refresh embedded avatar
                runCatching {
                    val obj = JSONObject(FwmcApi.getProfile())
                    val p = obj.optJSONObject("profile")
                    profile = profile.copy(avatarDataUri = p?.optString("avatar_path")?.ifEmpty { null })
                }
            } else {
                statusError = true
                statusMsg = s.profile.avatarUploadFailed
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WxBg)
            .verticalScroll(rememberScrollState()),
    ) {
        SectionCard(title = s.fwmcProfile.title) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.clickable {
                    avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Avatar(name = profile.nickname.ifEmpty { s.mine.unnamed }, dataUri = profile.avatarDataUri, size = 64.dp)
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .align(Alignment.BottomEnd),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = s.fwmcProfile.clickAvatarToChange,
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(start = 14.dp),
                )
            }
        }

        SectionCard(title = s.fwmcProfile.basicInfo) {
            ProfileField(s.fwmcProfile.nickname, profile.nickname) { profile = profile.copy(nickname = it) }
            ProfileField(s.fwmcProfile.gender, profile.gender) { profile = profile.copy(gender = it) }
            ProfileField(s.fwmcProfile.age, profile.age, number = true) { profile = profile.copy(age = it) }
            ProfileField(s.fwmcProfile.country, profile.country) { profile = profile.copy(country = it) }
            ProfileField(s.fwmcProfile.ethnicity, profile.ethnicity) { profile = profile.copy(ethnicity = it) }
            ProfileField(s.fwmcProfile.bloodType, profile.bloodType) { profile = profile.copy(bloodType = it) }
            ProfileField(s.fwmcProfile.height, profile.heightCm, number = true) { profile = profile.copy(heightCm = it) }
            ProfileField(s.fwmcProfile.weight, profile.weightKg, number = true) { profile = profile.copy(weightKg = it) }
            ProfileField(s.fwmcProfile.education, profile.education) { profile = profile.copy(education = it) }
            ProfileField(s.fwmcProfile.homeAddress, profile.homeAddress) { profile = profile.copy(homeAddress = it) }
            ProfileField(s.fwmcProfile.bio, profile.bio, singleLine = false) { profile = profile.copy(bio = it) }
        }

        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Button(
                enabled = !saving && loaded,
                onClick = {
                    saving = true
                    statusError = false
                    statusMsg = s.fwmcProfile.saving
                    scope.launch {
                        val json = JSONObject().apply {
                            put("nickname", profile.nickname.trim().ifEmpty { JSONObject.NULL })
                            put("gender", profile.gender.trim().ifEmpty { JSONObject.NULL })
                            put("age", profile.age.toIntOrNull() ?: JSONObject.NULL)
                            put("country", profile.country.trim().ifEmpty { JSONObject.NULL })
                            put("ethnicity", profile.ethnicity.trim().ifEmpty { JSONObject.NULL })
                            put("blood_type", profile.bloodType.trim().ifEmpty { JSONObject.NULL })
                            put("height_cm", profile.heightCm.toIntOrNull() ?: JSONObject.NULL)
                            put("weight_kg", profile.weightKg.toIntOrNull() ?: JSONObject.NULL)
                            put("education", profile.education.trim().ifEmpty { JSONObject.NULL })
                            put("home_address", profile.homeAddress.trim().ifEmpty { JSONObject.NULL })
                            put("bio", profile.bio.trim().ifEmpty { JSONObject.NULL })
                        }
                        val raw = FwmcApi.saveProfile(json.toString())
                        saving = false
                        val ok = runCatching { JSONObject(raw).optBoolean("success", false) }.getOrDefault(false)
                        statusError = !ok
                        statusMsg = if (ok) s.fwmcProfile.saved else s.fwmcProfile.saveError
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            ) {
                Text(if (saving) s.fwmcProfile.saving else s.fwmcProfile.saveProfile)
            }
            Text(
                text = statusMsg,
                fontSize = 12.sp,
                color = if (statusError) Color.Red else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 14.dp),
            )
        }
        Box(modifier = Modifier.padding(bottom = 16.dp))
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    number: Boolean = false,
    singleLine: Boolean = true,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        singleLine = singleLine,
        keyboardOptions = if (number) {
            androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
            )
        } else {
            androidx.compose.foundation.text.KeyboardOptions.Default
        },
    )
}

/** Decode, downscale and JPEG-compress an image Uri into <= ~200KB bytes. */
internal fun readJpegBytes(
    context: android.content.Context,
    uri: Uri,
    maxDim: Int,
): ByteArray? = runCatching {
    val raw = context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input)
    } ?: android.graphics.ImageDecoder.decodeBitmap(
        android.graphics.ImageDecoder.createSource(context.contentResolver, uri),
    ) { decoder, _, _ -> decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE }
    raw ?: return null
    val scale = maxOf(1, maxOf(raw.width, raw.height) / maxDim)
    val bmp = Bitmap.createScaledBitmap(raw, raw.width / scale, raw.height / scale, true)
    val out = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.JPEG, 82, out)
    out.toByteArray()
}.getOrNull()
