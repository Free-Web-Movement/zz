package io.github.freewebmovement.zz.ui.content.mine

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.freewebmovement.zz.R

@Composable
fun ProfileEditor() {
    Column {
        Row() {
            Image(
                painter = painterResource(id = R.drawable.ic_default_image),
                contentDescription = stringResource(id = R.string.tab_mine_avatar),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape)
                    .align(Alignment.CenterVertically)
            )
        }
        Row {
            var text = ""
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Nick Name") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row {
            var text = ""
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Signature") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(locale = "en")
@Composable
private fun Preview_en() {
    ProfileEditor()
}