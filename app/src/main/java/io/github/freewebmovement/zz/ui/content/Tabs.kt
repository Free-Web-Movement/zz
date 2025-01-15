package io.github.freewebmovement.zz.ui.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.freewebmovement.zz.ui.common.ContentType
import io.github.freewebmovement.zz.ui.common.PageType
import io.github.freewebmovement.zz.ui.common.TabType
import java.util.ArrayList


@Composable
fun Tabs(
    modifier: Modifier = Modifier,
    selectedTab: TabType,
    page: PageType,
    updater:(page: PageType, value: ContentType) -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (selectedTab) {
            TabType.Chats -> MessageContent(TabType.Chats, ArrayList())
            TabType.Contacts -> ContactContent(TabType.Contacts, updater)
            TabType.Apps -> AppContent(TabType.Apps, updater)
            TabType.Mine -> MinContent(page, updater)
        }
    }
}
