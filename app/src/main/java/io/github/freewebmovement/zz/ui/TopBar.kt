package io.github.freewebmovement.zz.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.freewebmovement.zz.ui.common.ContentType
import io.github.freewebmovement.zz.ui.common.PageType
import io.github.freewebmovement.zz.ui.common.TabType
import io.github.freewebmovement.zz.ui.topbar.AppTopBar
import io.github.freewebmovement.zz.ui.topbar.MineTopBar
import io.github.freewebmovement.zz.ui.topbar.PeerTopBar
import io.github.freewebmovement.zz.ui.topbar.SessionTopBar


@Composable
fun TopBar(
    selectedTab: TabType,
    stacked: ContentType,
    updater: (page: PageType, value: ContentType) -> Unit
) {
    when (selectedTab) {
        TabType.Sessions -> SessionTopBar(selectedTab, stacked)
        TabType.Peers -> PeerTopBar(selectedTab, stacked, updater)
        TabType.Apps -> AppTopBar(selectedTab, stacked)
        TabType.Mine -> MineTopBar(selectedTab, stacked, updater)
    }
}

@Preview
@Composable
private fun Preview() {
    TopBar(TabType.Sessions, ContentType.NonStacked) { _, _ ->
    }
}

@Preview
@Composable
private fun Preview_Stacked() {
    TopBar(TabType.Sessions, ContentType.Stacked) { _, _ ->
    }
}