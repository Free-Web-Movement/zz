package io.github.freewebmovement.zz.ui.content.mine

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
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
import io.github.freewebmovement.zz.ui.common.PageType

@Composable
fun ProfileEditor(updatePage: (value: PageType) -> Unit) {
    Column(
        modifier = Modifier.fillMaxHeight(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_default_image),
                contentDescription = stringResource(id = R.string.tab_mine_avatar),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(128.dp)
                    .size(128.dp)
                    .clip(CircleShape)
            )
        }
        Row {
            var text = ""
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.tab_mine_nickname)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row {
            var text = ""
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.signature)) },
                modifier = Modifier.fillMaxWidth()
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
                Text(stringResource(R.string.action_save))
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