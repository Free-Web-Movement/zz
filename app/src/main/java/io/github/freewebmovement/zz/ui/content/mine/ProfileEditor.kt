package io.github.freewebmovement.zz.ui.content.mine

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
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

val rainbowColors: List<Color> = listOf(
    Color.Red,
    Color.Magenta,
    Color.Yellow,
    Color.Green,
    Color.Blue,
    Color.Cyan
)

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun ProfileEditor(updatePage: (value: PageType) -> Unit) {
    val brush = remember {
        Brush.linearGradient(
            colors = rainbowColors
        )
    }


    with(MainApplication.instance!!.settings) {
        var nickname by remember { mutableStateOf(mineProfileNickname) }
        var signature by remember { mutableStateOf(mineProfileSignature) }
        var imageUri by remember { mutableStateOf<Uri?>(Uri.parse(mineProfileImageUri)) }
        val pickMedia =
            rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                // Callback is invoked after the user selects a media item or closes the
                // photo picker.
                if (uri != null) {
                    Log.d("PhotoPicker", "Selected URI: $uri")
                    imageUri = uri
                } else {
                    Log.d("PhotoPicker", "No media selected")
                }
            }

        Column(
            modifier = Modifier.fillMaxHeight(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUri)
                            .build(),
                        contentDescription = stringResource(id = R.string.tab_mine_avatar),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(128.dp)
                            .size(128.dp)
                            .clip(CircleShape)
                            .clickable {
                                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_default_image),
                        contentDescription = stringResource(id = R.string.tab_mine_avatar),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(128.dp)
                            .size(128.dp)
                            .clip(CircleShape)
                            .clickable {
                                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                    )
                }
            }
            Row {
                TextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text(stringResource(R.string.tab_mine_nickname)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(2.dp),
                    textStyle = TextStyle(brush = brush)
                )
            }
            Row {
                TextField(
                    value = signature,
                    onValueChange = { signature = it },
                    label = { Text(stringResource(R.string.signature)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(2.dp),
                    textStyle = TextStyle(brush = brush)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Button(onClick = {
                    updatePage(PageType.MineMain)
                }) {
                    with(MainApplication.instance!!.settings) {
                            mineProfileImageUri = imageUri.toString()
                        mineProfileNickname = nickname
                        mineProfileSignature = signature
                    }
                    Text(stringResource(R.string.action_save))
                }
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