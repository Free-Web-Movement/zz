package io.github.freewebmovement.zz.ui.content

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.freewebmovement.zz.ui.TabContainer
import io.github.freewebmovement.zz.ui.TabType


@Composable
fun Tabs(
    modifier: Modifier = Modifier,
    selectedTab: TabType,
) {
    TabContainer(
        modifier = modifier.fillMaxSize(),
        selectedTab = selectedTab,
    ) {
        Tab(TabType.Chats) {
            ChatContent(TabType.Chats)
        }
        Tab(TabType.Contacts) {
            ContactContent(TabType.Contacts)
        }
        Tab(TabType.Apps) {
            AppContent(TabType.Apps)
        }
        Tab(TabType.Mine) {
            MinContent(TabType.Mine)
        }
    }
}
