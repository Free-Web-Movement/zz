package io.github.freewebmovement.zz.ui.content.mine

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.freewebmovement.peer.system.IPList
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.ui.common.PageType

@Composable
fun LocalServerIPInfo(updatePage: (value: PageType) -> Unit) {
    val app = MainApplication.getApp()
    Column(
        modifier = Modifier
        .verticalScroll(rememberScrollState())
    ) {
        Text(text = "IPV4")
        Text(text = "Local")
        val port = app.settings.network.localServerPort
        IPList.toHTTPV4Uris(
            IPList.ipv4IPLocal,
            port
        ).forEach {
            Text(it)
        }
        Text(text = "Public")
        IPList.toHTTPV4Uris(
            IPList.ipv4IPPublic,
            port
        ).forEach {
            Text(it)
        }

        Text(text = "IPV6")
        Text(text = "Local")
        IPList.toHTTPV6Uris(
            IPList.ipv6IPLocal,
            port
        ).forEach {
            Text(it)
        }
        Text(text = "Public")
        IPList.toHTTPV6Uris(
            IPList.ipv6IPPublic,
            port
        ).forEach {
            Text(it)
        }
    }
}