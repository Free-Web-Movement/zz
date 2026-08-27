package io.github.freewebmovement.zz.ui.topbar

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.freewebmovement.peer.system.IPList
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.R
import io.github.freewebmovement.zz.ui.common.ContentType
import io.github.freewebmovement.zz.ui.common.PageType
import io.github.freewebmovement.zz.ui.common.TabType
import io.github.freewebmovement.zz.ui.getTitle


fun share(title: String, uri: String, context: Context) {
    val serverIntent = Intent(Intent.ACTION_SEND)
    serverIntent.setType("text/plain")
    serverIntent.putExtra(Intent.EXTRA_SUBJECT, title)
    serverIntent.putExtra(Intent.EXTRA_TEXT, uri)
    context.startActivity(Intent.createChooser(serverIntent, title))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineTopBar(
    selectedTab: TabType,
    stacked: ContentType,
    updater: (page: PageType, value: ContentType) -> Unit
) {
    val s = io.github.freewebmovement.zz.ui.i18n.LocalAppStrings.current
    var showDropDownMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val app = MainApplication.getApp()
    val port = app.fwmc?.port ?: app.settings.network.port
    val uri = IPList.getPublicUri(port)
    val localUri = IPList.getLocalUri(port)
    val shareServerString = s.mine.shareServer
    val shareApkString = s.mine.shareApk
    val noIpStr = s.mine.noPublicIp
//    val i = Intent(Intent.ACTION_SEND)
//    if (uri != "") {
//        i.setType("text/plain")
//        i.putExtra(Intent.EXTRA_SUBJECT, title)
//        i.putExtra(Intent.EXTRA_TEXT, "$uri/app/download/apk")
//    }
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
                        contentDescription = s.common.back
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = {
                showDropDownMenu = true
            }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = s.common.menu
                )
            }
            DropdownMenu(
                expanded = showDropDownMenu,
                onDismissRequest = { showDropDownMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(text = shareServerString) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Share,
                            shareServerString
                        )
                    },
                    onClick = {
                        showDropDownMenu = false
                        if (uri != "") {
                            share(shareServerString, uri, context)
                        } else {
                            Toast.makeText(context, noIpStr, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text(text = shareApkString) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Share,
                            shareApkString
                        )
                    },
                    onClick = {
                        showDropDownMenu = false
                        app.share.apk(app.share.myApk())
                    }
                )
            }
        }
    )
}


@Preview
@Composable
private fun Preview() {
    MineTopBar(TabType.Sessions, ContentType.NonStacked) { _, _ ->
    }
}