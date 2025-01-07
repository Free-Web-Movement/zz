package io.github.freewebmovement.zz.ui.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.freewebmovement.zz.ui.common.ContentType
import io.github.freewebmovement.zz.ui.common.TabType


@Composable
fun Tabs(
    modifier: Modifier = Modifier,
    selectedTab: TabType,
    stackUpdater:(value: ContentType) -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (selectedTab) {
            TabType.Chats -> ChatContent(TabType.Chats, stackUpdater)
            TabType.Contacts -> ContactContent(TabType.Contacts, stackUpdater)
            TabType.Apps -> AppContent(TabType.Apps, stackUpdater)
            TabType.Mine -> MinContent(stackUpdater)
        }
    }
}
