package io.github.freewebmovement.zz.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.freewebmovement.zz.ui.common.ContentType
import io.github.freewebmovement.zz.ui.common.PageType
import io.github.freewebmovement.zz.ui.common.TabType
import io.github.freewebmovement.zz.ui.content.Tabs
import io.github.freewebmovement.zz.ui.i18n.LocalAppStrings
import kotlinx.coroutines.launch


@Composable
fun TabView() {
    var selectedTab by remember { mutableStateOf(TabType.Sessions) }
    var stacked by remember { mutableStateOf(ContentType.NonStacked) }
    var page by remember { mutableStateOf(PageType.MineMain) }
    var chatRequest by remember { mutableStateOf<Pair<String, String>?>(null) }
    var dataTab by remember { mutableIntStateOf(0) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current
    val ledger = strings.ledger
    val dataItems = listOf(
        ledger.overview, ledger.transactions, ledger.epochs, ledger.blocks,
        ledger.rings, ledger.rewards, ledger.balances,
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = selectedTab == TabType.Data,
        drawerContent = {
            if (selectedTab == TabType.Data) {
                ModalDrawerSheet {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            text = strings.tabs.data,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        )
                        HorizontalDivider()
                        dataItems.forEachIndexed { index, label ->
                            NavigationDrawerItem(
                                label = { Text(label) },
                                selected = dataTab == index,
                                onClick = {
                                    dataTab = index
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                selectedTab = selectedTab,
                stacked = stacked,
                onOpenDataMenu = { scope.launch { drawerState.open() } },
                updater = { vPage, vStacked ->
                    stacked = vStacked
                    page = vPage
                }
            )
            Tabs(
                selectedTab = selectedTab,
                modifier = Modifier.weight(1f),
                page = page,
                pendingChat = chatRequest,
                dataTab = dataTab,
                onDataTabChange = { dataTab = it },
                onOpenChat = { addr, name ->
                    chatRequest = addr to name
                    selectedTab = TabType.Sessions
                },
                updater = { vPage, vStacked ->
                    stacked = vStacked
                    page = vPage
                }
            )
            BottomBar(
                selectedTab = selectedTab,
                onClickTab = {
                    selectedTab = it
                    chatRequest = null
                    stacked = ContentType.NonStacked
                    page = when(selectedTab) {
                        TabType.Mine -> PageType.MineMain
                        TabType.Sessions -> PageType.SessionMain
                        TabType.Contacts -> PageType.ContactMain
                        TabType.Peers -> PageType.PeerMain
                        TabType.Data -> PageType.DataMain
                    }
                    scope.launch { drawerState.close() }
                },
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
