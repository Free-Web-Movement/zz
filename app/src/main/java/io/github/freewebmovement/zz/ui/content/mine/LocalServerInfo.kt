package io.github.freewebmovement.zz.ui.content.mine

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.github.freewebmovement.zz.ui.common.PageType

@Composable
fun LocalServerInfo(updatePage: (value: PageType) -> Unit) {
    Column {
        Text(text = "Server Info")
    }
}