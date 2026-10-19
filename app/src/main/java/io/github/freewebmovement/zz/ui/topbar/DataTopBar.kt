package io.github.freewebmovement.zz.ui.topbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.freewebmovement.zz.ui.common.TabType
import io.github.freewebmovement.zz.ui.getTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataTopBar(selectedTab: TabType, onOpenMenu: () -> Unit = {}) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = io.github.freewebmovement.zz.ui.theme.CardBg,
            titleContentColor = io.github.freewebmovement.zz.ui.theme.TextPrimary,
        ),
        title = { Text(getTitle(selectedTab)) },
        actions = {
            IconButton(onClick = onOpenMenu) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "menu",
                )
            }
        },
    )
}

@Preview
@Composable
private fun Preview() {
    DataTopBar(TabType.Data)
}
