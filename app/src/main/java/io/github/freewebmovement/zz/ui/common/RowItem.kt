package io.github.freewebmovement.zz.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.freewebmovement.zz.R
import io.github.freewebmovement.zz.ui.theme.backColor

@Composable
fun RowItem(
    leftIcon: Int,
    rightIcon: Int,
    title: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(40.dp).background(
            backColor
        ),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = leftIcon),
            contentDescription = stringResource(title),
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 4.dp
            ).weight(1f)
        )
        Text(
            text = stringResource(title),
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 4.dp
            ).weight(4f)
        )
        Icon(
            painter = painterResource(id = rightIcon),
            contentDescription = stringResource(title),
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 4.dp
            ).weight(1f)
        )
    }
}