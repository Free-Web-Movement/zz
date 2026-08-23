package io.github.freewebmovement.zz.ui.topbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.freewebmovement.zz.R
import io.github.freewebmovement.zz.ui.common.ContentType
import io.github.freewebmovement.zz.ui.common.PageType
import io.github.freewebmovement.zz.ui.common.TabType
import io.github.freewebmovement.zz.ui.getTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerTopBar(
    selectedTab: TabType,
    stacked: ContentType,
    updater: (page: PageType, value: ContentType) -> Unit
) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = io.github.freewebmovement.zz.ui.theme.CardBg,
            titleContentColor = io.github.freewebmovement.zz.ui.theme.TextPrimary,
        ),
        title = {
            Text(getTitle(selectedTab))
        },
        navigationIcon = {
            IconButton(onClick = {
                updater(PageType.MineMain, ContentType.NonStacked)
            }) {
                if (stacked == ContentType.Stacked) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        },
    )
}

@Preview
@Composable
private fun Preview() {
    PeerTopBar(TabType.Peers, ContentType.NonStacked) { _, _ ->
    }
}
