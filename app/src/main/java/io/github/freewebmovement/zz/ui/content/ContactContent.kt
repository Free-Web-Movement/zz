package io.github.freewebmovement.zz.ui.content

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.github.freewebmovement.zz.ui.common.ContentType
import io.github.freewebmovement.zz.ui.common.TabType

@Composable
fun ContactContent(tabType: TabType, stackedUpdater: (value: ContentType) -> Unit) {
    Column {
        Text(text = tabType.name)
    }
}