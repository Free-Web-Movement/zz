package io.github.freewebmovement.zz.ui.topbar

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
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.R
import io.github.freewebmovement.zz.ui.common.ContentType
import io.github.freewebmovement.zz.ui.common.PageType
import io.github.freewebmovement.zz.ui.common.TabType
import io.github.freewebmovement.zz.ui.getTitle


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactTopBar(
    selectedTab: TabType,
    stacked: ContentType,
    page: PageType,
    updater: (page: PageType, value: ContentType) -> Unit
) {
    var showDropDownMenu by remember { mutableStateOf(false) }
    val content = LocalContext.current
    val app = MainApplication.instance!!
    val uri = app.ipList.getPublicUri()
    val title = stringResource(R.string.share_app_apk)
    val noIpStr = stringResource(R.string.share_app_apk_no_public_ip)
    val i = Intent(Intent.ACTION_SEND)
    if (uri != "") {
        i.setType("text/plain")
        i.putExtra(Intent.EXTRA_SUBJECT, title)
        i.putExtra(Intent.EXTRA_TEXT, "$uri/app/download/apk")
    }
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
                    text = { Text(text = stringResource(R.string.share_server)) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Share,
                            stringResource(R.string.share_server)
                        )
                    },
                    onClick = {
                        showDropDownMenu = false
                        if (uri != "") {
                            content.startActivity(Intent.createChooser(i, title))
                        } else {
                            Toast.makeText(content, noIpStr, Toast.LENGTH_SHORT).show()
                        }
                        updater(PageType.MineServerShare, ContentType.Stacked)
                    }
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.share_app_apk_through_public_ip)) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Share,
                            stringResource(R.string.share_app_apk_through_public_ip)
                        )
                    },
                    onClick = {
                        showDropDownMenu = false
                        if (uri != "") {
                            content.startActivity(Intent.createChooser(i, title))
                        } else {
                            Toast.makeText(content, noIpStr, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.share_app_apk)) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Share,
                            stringResource(R.string.share_app_apk)
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
