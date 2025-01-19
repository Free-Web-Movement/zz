package io.github.freewebmovement.zz.ui.content.peer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.freewebmovement.zz.ui.common.PageType


@Composable
fun PeerMain(updatePage: ((value: PageType) -> Unit)) {    Column(
        modifier = Modifier.padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
            Text(
                "PeerMain"
            )
    }
}


@Preview
@Composable
private fun Preview() {
    PeerMain {

    }
}
