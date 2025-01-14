package io.github.freewebmovement.zz.ui.content.mine

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.freewebmovement.zz.R
import io.github.freewebmovement.zz.ui.common.PageType
import io.github.freewebmovement.zz.ui.common.RowItem


@Composable
fun MineMain(updatePage: (value: PageType) -> Unit) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = {
                    updatePage(PageType.MineProfile)
                }),
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Image(
                    painter = painterResource(id = R.drawable.ic_default_image),
                    contentDescription = stringResource(id = R.string.tab_mine_avatar),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                )
            }
            Column(modifier = Modifier.weight(5f)) {
                Text(
                    text = stringResource(R.string.tab_mine_nickname),
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .height(32.dp),
                    fontSize = 24.sp,
                )
                Text(
                    text = stringResource(R.string.signature),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Column(
                modifier = Modifier.weight(0.8f)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = stringResource(R.string.tab_mine_profile)
                )
            }

        }

        RowItem(R.drawable.ic_settings, R.string.tab_mine_setting, onClick = {
            println("Setting Clicked")
            updatePage(PageType.MineProfile)
        })
        RowItem(R.drawable.ic_refresh, R.string.tab_mine_refresh_key, onClick = {
            println("Refresh Clicked")
            updatePage(PageType.MineKey)

        })
        RowItem(R.drawable.ic_port, R.string.tab_mine_port, onClick = {
            println("Port Clicked")
            updatePage(PageType.MineServerPort)

        })
        RowItem(R.drawable.ic_mine_local_server, R.string.tab_mine_local_server_ips, onClick = {
            println("Server Clicked")
            updatePage(PageType.MineServerIP)
        })

        RowItem(R.drawable.ic_mine_local_server_share, R.string.tab_mine_local_server_share, onClick = {
            println("Server Clicked")
            updatePage(PageType.MineServerShare)
        })
    }
}
