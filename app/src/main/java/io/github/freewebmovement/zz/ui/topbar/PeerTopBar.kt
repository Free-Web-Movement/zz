package io.github.freewebmovement.zz.ui.topbar

import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.R
import io.github.freewebmovement.zz.ui.common.ContentType
import io.github.freewebmovement.zz.ui.common.PageType
import io.github.freewebmovement.zz.ui.common.TabType
import io.github.freewebmovement.zz.ui.getTitle


fun getUri(): String {
    return MainApplication.instance?.ipList?.getPublicUri() ?: ""
}

fun getIntent(uri: String, title: String): Intent {
    val i = Intent(Intent.ACTION_SEND)
    if (uri != "") {
        i.setType("text/plain")
        i.putExtra(Intent.EXTRA_SUBJECT, title)
        i.putExtra(Intent.EXTRA_TEXT, "$uri/app/download/apk")
    }
    return i
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerTopBar(
    selectedTab: TabType,
    stacked: ContentType,
    updater: (page: PageType, value: ContentType) -> Unit
) {
    var showDropDownMenu by remember { mutableStateOf(false) }

    val uri = getUri()
    val title = stringResource(R.string.share_app_apk)
    stringResource(R.string.share_app_apk_no_public_ip)
    getIntent(uri, title)
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
        actions = {
            IconButton(onClick = { /* do something */ }) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.search)
                )
            }

            IconButton(onClick = {
                showDropDownMenu = true
            }) {
                Icon(
                    imageVector = Icons.Filled.AddCircle,
                    contentDescription = stringResource(R.string.add)
                )
            }
            DropdownMenu(
                expanded = showDropDownMenu,
                onDismissRequest = { showDropDownMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.add_new_session)) },
                    leadingIcon = {
                        Icon(
                            painterResource(R.drawable.ic_chat_bubble_selected),
                            stringResource(R.string.add_new_session)
                        )
                    },
                    onClick = {
                        showDropDownMenu = false
//                        updater(PageType.MineServerShare, ContentType.Stacked)
                    }
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.add_new_peer)) },
                    leadingIcon = {
                        Icon(
                            painterResource(R.drawable.ic_server_selected),
                            stringResource(R.string.add_new_peer)
                        )
                    },
                    onClick = {
                        showDropDownMenu = false
                        updater(PageType.PeerAdd, ContentType.Stacked)
                    }
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.scan)) },
                    leadingIcon = {
                        Icon(
                            painterResource(R.drawable.ic_scan_qrcode),
                            stringResource(R.string.scan)
                        )
                    },
                    onClick = {
                        showDropDownMenu = false
                    }
                )
            }
        }
    )
}

@Preview
@Composable
private fun Preview() {
    PeerTopBar(TabType.Peers, ContentType.NonStacked) { _, _ ->
    }
}
