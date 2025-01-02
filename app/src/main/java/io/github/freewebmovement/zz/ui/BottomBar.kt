package io.github.freewebmovement.zz.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.freewebmovement.zz.R
import io.github.freewebmovement.zz.ui.theme.black

enum class TabType {
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
fun getTitle(tab: TabType): String {
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
fun BottomBar(
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
                    )
                },
                label = { Text(text = getTitle(tab), color = getColor(tab, selectedTab)) }
            )
        }
    }
}