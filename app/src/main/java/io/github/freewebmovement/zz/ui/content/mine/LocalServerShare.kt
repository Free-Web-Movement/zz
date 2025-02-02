package io.github.freewebmovement.zz.ui.content.mine

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import io.github.freewebmovement.peer.system.IPList
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.R

fun toClipboard(label: String, text: String, context: Context) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}

@Composable
fun LocalServerShare() {
    val app = MainApplication.getApp()
    Column(
        modifier = Modifier
        .verticalScroll(rememberScrollState())
    ) {
        Text(text = "IPV4")
        val context = LocalContext.current
        val label = stringResource(R.string.clip_data_copied)
        val uriHandler = LocalUriHandler.current
        val port = app.settings.network.localServerPort
        // Enable local IP interaction
        IPList.toHTTPV4Uris(
            IPList.ipv4IPLocal,
            port
        ).forEach {
            Text(it)
            Button(onClick = {
                uriHandler.openUri(it)
            }) { Text(stringResource(R.string.open)) }
            Button(onClick = {
                toClipboard(label, it, context)
            }) { Text(stringResource(R.string.copy)) }
        }
        IPList.toHTTPV4Uris(
            IPList.ipv4IPPublic,
            port
        ).forEach {
            Text(it)
            Button(onClick = {
                uriHandler.openUri(it)
            }) { Text(stringResource(R.string.open)) }
            Button(onClick = {
                toClipboard(label, it, context)
            }) { Text(stringResource(R.string.copy)) }
        }

        Text(text = "IPV6")
        IPList.toHTTPV6Uris(
            IPList.ipv6IPPublic,
            port
        ).forEach {
            Text(it)
            Button(onClick = {
                uriHandler.openUri(it)
            }) { Text(stringResource(R.string.open)) }
            Button(onClick = {
                toClipboard(label, it, context)
            }) { Text(stringResource(R.string.copy)) }
        }
    }
}