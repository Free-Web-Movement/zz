package io.github.freewebmovement.zz.ui.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.freewebmovement.zz.R

@Composable
fun MinContent() {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().fillMaxHeight().height(48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_settings),
                contentDescription = stringResource(R.string.tab_mine_setting),
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 4.dp
                ).weight(1f)
            )
            Text(
                text = stringResource(R.string.tab_mine_setting),
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 4.dp
                ).weight(4f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.tab_mine_setting),
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 4.dp
                ).weight(1f)
            )
        }
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