package io.github.freewebmovement.zz.ui.content

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.github.freewebmovement.zz.ui.TabType

@Composable
fun MinContent(tabType: TabType) {
    Text(text = tabType.name)
}