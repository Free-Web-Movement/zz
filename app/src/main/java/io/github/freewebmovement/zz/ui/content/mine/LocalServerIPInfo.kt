package io.github.freewebmovement.zz.ui.content.mine

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.ui.common.PageType

@Composable
fun LocalServerIPInfo(updatePage: (value: PageType) -> Unit) {
    val app = MainApplication.instance!!
    Column(
        modifier = Modifier
        .verticalScroll(rememberScrollState())
    ) {
        Text(text = "IPV4")
        Text(text = "Local")
        app.ipList.toHTTPV4Uris(app.ipList.ipv4IPLocal).forEach {
            Text(it)
        }
        Text(text = "Public")
        app.ipList.toHTTPV4Uris(app.ipList.ipv4IPPublic).forEach {
            Text(it)
        }

        Text(text = "IPV6")
        Text(text = "Local")
        app.ipList.toHTTPV6Uris(app.ipList.ipv6IPLocal).forEach {
            Text(it)
        }
        Text(text = "Public")
        app.ipList.toHTTPV6Uris(app.ipList.ipv6IPPublic).forEach {
            Text(it)
        }
    }
}