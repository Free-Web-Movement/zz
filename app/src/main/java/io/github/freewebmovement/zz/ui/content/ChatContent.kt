package io.github.freewebmovement.zz.ui.content

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.github.freewebmovement.zz.ui.TabType


@Composable
fun ChatContent(tabType: TabType) {
    Column {
        Text(text = tabType.name)
    }
}