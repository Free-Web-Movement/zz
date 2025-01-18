package io.github.freewebmovement.zz.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.freewebmovement.zz.ui.common.ContentType
import io.github.freewebmovement.zz.ui.common.PageType
import io.github.freewebmovement.zz.ui.common.TabType
import io.github.freewebmovement.zz.ui.content.Tabs


@Composable
fun TabView() {
    var selectedTab by remember { mutableStateOf(TabType.Sessions) }
    var stacked by remember { mutableStateOf(ContentType.NonStacked) }
    var page by remember { mutableStateOf(PageType.MineMain) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            selectedTab = selectedTab,
            stacked = stacked,
            updater = { vPage, vStacked ->
                stacked = vStacked
                page = vPage
            }
        )
        Tabs(
            selectedTab = selectedTab,
            modifier = Modifier.weight(1f),
            page = page,
            updater = { vPage, vStacked ->
                stacked = vStacked
                page = vPage
            }
        )
        BottomBar(
            selectedTab = selectedTab,
            onClickTab = {
                selectedTab = it
                stacked = ContentType.NonStacked
                page = when(selectedTab) {
                    TabType.Mine -> PageType.MineMain
                    TabType.Sessions -> PageType.SessionMain
                    TabType.Peers -> PageType.PeerMain
                    TabType.Apps -> PageType.AppMain
                }
            },
        )
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