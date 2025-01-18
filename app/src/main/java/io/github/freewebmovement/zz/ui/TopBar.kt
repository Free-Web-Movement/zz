package io.github.freewebmovement.zz.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.freewebmovement.zz.R
import io.github.freewebmovement.zz.ui.common.ContentType
import io.github.freewebmovement.zz.ui.common.PageType
import io.github.freewebmovement.zz.ui.common.TabType
import io.github.freewebmovement.zz.ui.topbar.ContactTopBar
import io.github.freewebmovement.zz.ui.topbar.MineTopBar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionTopBar(
    selectedTab: TabType,
    stacked: ContentType,
) {
    var showDropDownMenu by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = {
            Text(getTitle(selectedTab))
        },
        navigationIcon = {
            IconButton(onClick = {

            }) {
                if (stacked == ContentType.Stacked) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = { /* do something */ }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.menu)
                )
            }
            DropdownMenu(
                expanded = showDropDownMenu,
                onDismissRequest = { showDropDownMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(text = "Drop down item") },
                    leadingIcon = { Icon(Icons.Filled.Home, null) },
                    onClick = { showDropDownMenu = false }
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    selectedTab: TabType,
    stacked: ContentType

) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = {
            Text(getTitle(selectedTab))
        },
        navigationIcon = {
            IconButton(onClick = { /* do something */ }) {
                if (stacked == ContentType.Stacked) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Localized description"
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = { /* do something */ }) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Localized description"
                )
            }
        }
    )
}

@Composable
fun TopBar(
    selectedTab: TabType,
    stacked: ContentType,
    page: PageType,
    updater: (page: PageType, value: ContentType) -> Unit
) {
    when (selectedTab) {
        TabType.Sessions -> SessionTopBar(selectedTab, stacked)
        TabType.Peers -> ContactTopBar(selectedTab, stacked, updater)
        TabType.Apps -> AppTopBar(selectedTab, stacked)
        TabType.Mine -> MineTopBar(selectedTab, stacked, page, updater)
    }
}

@Preview
@Composable
private fun Preview() {
    TopBar(TabType.Sessions, ContentType.NonStacked, PageType.MineMain) { _, _ ->
    }
}

@Preview
@Composable
private fun Preview_Stacked() {
    TopBar(TabType.Sessions, ContentType.Stacked, PageType.MineMain) { _, _ ->
    }
}