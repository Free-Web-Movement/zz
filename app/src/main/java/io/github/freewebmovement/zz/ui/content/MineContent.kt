package io.github.freewebmovement.zz.ui.content

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.freewebmovement.zz.R
import io.github.freewebmovement.zz.ui.common.RowItem
import io.github.freewebmovement.zz.ui.theme.backColor

@Composable
fun MinContent() {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Image(
                    painter = painterResource(id = R.drawable.ic_default_image),
                    contentDescription = stringResource(id = R.string.tab_mine_profile),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                )
            }
            Column(modifier = Modifier.weight(5f)) {
                Text(
                    text = "名字", modifier = Modifier
                        .padding(horizontal = 0.dp)
                        .height(32.dp),
                    fontSize = 24.sp
                )
                Text(text = "名字", modifier = Modifier.padding(horizontal = 0.dp))
            }

            Column(
                modifier = Modifier.weight(0.8f)
            ) {

                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = "名字"
                )
            }

        }

        RowItem(R.drawable.ic_settings, R.drawable.ic_chevron_right, R.string.tab_mine_setting)
        RowItem(R.drawable.ic_settings, R.drawable.ic_chevron_right, R.string.tab_mine_setting)
        RowItem(R.drawable.ic_settings, R.drawable.ic_chevron_right, R.string.tab_mine_setting)
    }
}

@Preview
@Composable
private fun Preview() {
    MinContent()
}

@Preview(locale = "en")
@Composable
private fun Preview_en() {
    MinContent()
}