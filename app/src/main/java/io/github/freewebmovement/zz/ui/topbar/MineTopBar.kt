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
    var showDropDownMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val app = MainApplication.getApp()
    val uri = app.ipList.getPublicUri()
    val localUri = app.ipList.getLocalUri()
    val shareServerString = stringResource(R.string.share_server)
    val shareApkString = stringResource(R.string.share_app_apk)
    val sharePublicIPString = stringResource(R.string.share_app_apk_through_public_ip)
    val shareLocalIPString = stringResource(R.string.share_app_apk_through_local_ip)
//    val shareNoPublicIPString = stringResource(R.string.share_app_apk_no_public_ip)
    val noIpStr = stringResource(R.string.share_app_apk_no_public_ip)
//    val i = Intent(Intent.ACTION_SEND)
//    if (uri != "") {
//        i.setType("text/plain")
//        i.putExtra(Intent.EXTRA_SUBJECT, title)
//        i.putExtra(Intent.EXTRA_TEXT, "$uri/app/download/apk")
//    }
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
            IconButton(onClick = {
                showDropDownMenu = true
            }) {
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
                    text = { Text(text = shareServerString) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Share,
                            stringResource(R.string.share_server)
                        )
                    },
                    onClick = {
                        showDropDownMenu = false
                        if (uri != "") {
                            share(shareServerString, uri, context)
//                            content.startActivity(Intent.createChooser(i, title))
                        } else {
                            Toast.makeText(context, noIpStr, Toast.LENGTH_SHORT).show()
                        }
                        updater(PageType.MineServerShare, ContentType.Stacked)
                    }
                )
                DropdownMenuItem(
                    text = { Text(text = sharePublicIPString) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Share,
                            sharePublicIPString
                        )
                    },
                    onClick = {
                        showDropDownMenu = false
                        if (uri != "") {
                            share(sharePublicIPString, "$uri/app/download/apk", context)
//                            content.startActivity(Intent.createChooser(i, title))
                        } else {
                            Toast.makeText(context, noIpStr, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text(text = shareLocalIPString) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Share,
                            shareLocalIPString
                        )
                    },
                    onClick = {
                        showDropDownMenu = false
                        if (localUri != "") {
                            share(shareLocalIPString, "$localUri/app/download/apk", context)
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