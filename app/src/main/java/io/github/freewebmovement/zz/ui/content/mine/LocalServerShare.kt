package io.github.freewebmovement.zz.ui.content.mine

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.R
import io.github.freewebmovement.zz.ui.common.PageType

@Composable
fun LocalServerShare(updatePage: (value: PageType) -> Unit) {
    val app = MainApplication.instance!!
    Column {
        Text(text = "IPV4")
        app.ipList.toHTTPV4Uris(app.ipList.ipv4IPPublic).forEach {
            Text(it)
            LocalContext.current
            val uriHandler = LocalUriHandler.current
            Button(onClick = {
                uriHandler.openUri(it)
            }) { Text(stringResource(R.string.open)) }
            Button(onClick = {
                uriHandler.openUri(it)
            }) { Text(stringResource(R.string.copy)) }
        }

        Text(text = "IPV6")
        app.ipList.toHTTPV6Uris(app.ipList.ipv6IPPublic).forEach {
            Text(it)
            LocalContext.current
            val uriHandler = LocalUriHandler.current
            Button(onClick = {
                uriHandler.openUri(it)
            }) { Text(stringResource(R.string.open)) }
            Button(onClick = {
                uriHandler.openUri(it)
            }) { Text(stringResource(R.string.copy)) }
        }
    }
}