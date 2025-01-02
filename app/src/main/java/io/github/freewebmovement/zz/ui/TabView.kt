package io.github.freewebmovement.zz.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.freewebmovement.zz.R
import io.github.freewebmovement.zz.ui.theme.black


private enum class TabType {
    Chats,
    Contacts,
    Apps,
    Mine,
}

private fun getIcon(tab: TabType, selected: TabType): Int {
    return when(tab) {
        TabType.Chats -> if (selected == tab) R.drawable.ic_chat_bubble_selected else R.drawable.ic_chat_bubble
        TabType.Contacts -> if (selected == tab) R.drawable.ic_server_selected else R.drawable.ic_server
        TabType.Apps -> if (selected == tab) R.drawable.ic_app_selected else R.drawable.ic_app
        TabType.Mine -> if (selected == tab) R.drawable.ic_account_selected else R.drawable.ic_account
    }
}


@Composable
private fun getTitle(tab: TabType): String {
    return when(tab) {
        TabType.Chats -> stringResource(R.string.title_chat)
        TabType.Contacts -> stringResource(R.string.title_contact)
        TabType.Apps -> stringResource(R.string.title_app)
        TabType.Mine -> stringResource(R.string.title_mine)
    }
}

@Composable
private fun getColor(tab: TabType, selected: TabType): Color {
    return if (selected == tab) MaterialTheme.colorScheme.primary else
        black
}

@Composable
fun TabView() {
    var selectedTab by remember { mutableStateOf(TabType.Chats) }

    Column(modifier = Modifier.fillMaxSize()) {
        Tabs(
            selectedTab = selectedTab,
            modifier = Modifier.weight(1f),
        )
        BottomNavigation(
            selectedTab = selectedTab,
            onClickTab = { selectedTab = it },
        )
    }
}

@Composable
private fun Tabs(
    modifier: Modifier = Modifier,
    selectedTab: TabType,
) {
    TabContainer(
        modifier = modifier.fillMaxSize(),
        selectedTab = selectedTab,
    ) {
        // 设置Tab内容
        Tab(TabType.Chats) {
            TabContent(TabType.Chats)
        }

        // 设置Tab内容
        Tab(TabType.Contacts) {
            TabContent(TabType.Contacts)
        }

        // 设置Tab内容，eager = true，提前加载
        Tab(TabType.Apps, eager = true) {
            TabContent(TabType.Apps)
        }

        // 设置Tab内容，自定义display
        Tab(
            tab = TabType.Mine,
//            display = { content, selected -> if (selected) content() },
        ) {
            TabContent(TabType.Mine)
        }
    }
}

@Composable
private fun TabContent(
    tabType: TabType,
    modifier: Modifier = Modifier,
) {
//    DisposableEffect(tabType) {
//        logMsg { "tab:${tabType.name}" }
//        onDispose { logMsg { "tab:${tabType.name} onDispose" } }
//    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = tabType.name)
    }
}

/**
 * 底部导航
 */
@Composable
private fun BottomNavigation(
    selectedTab: TabType,
    onClickTab: (TabType) -> Unit,
) {
    NavigationBar {
        for (tab in TabType.entries) {
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onClickTab(tab) },
                icon = {
                    Icon(
                        painterResource(id = getIcon(tab, selectedTab)),
                        contentDescription = getTitle(tab),
                        tint = getColor(tab, selectedTab)
                    )},
                label = { Text(text = getTitle(tab), color = getColor(tab, selectedTab)) }
            )
        }
    }
}

@Preview(locale = "en")
@Composable
private fun Preview() {
    TabView()
}

@Preview(locale = "zh")
@Composable
private fun PreviewZH() {
    TabView()
}