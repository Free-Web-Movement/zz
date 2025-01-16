package io.github.freewebmovement.zz.ui.content.mine

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.R
import io.github.freewebmovement.zz.system.crypto.Crypto
import io.github.freewebmovement.zz.ui.common.PageType

@Composable
fun UpdateKeys(updatePage: (value: PageType) -> Unit) {
    val app = MainApplication.instance!!
    val scrollState = rememberScrollState()
    var privateKey by remember { mutableStateOf(app.crypto.privateKey.toString()) }
    var publicKey by remember { mutableStateOf(app.crypto.publicKey.toString()) }

    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
    ) {
        Text("Private Key:")
        Text(privateKey)
        Text("Private Key:")
        Text(publicKey)

        Row {
            Button(
                onClick = {
                    app.crypto = Crypto.refresh(app.preference)
                    publicKey = app.crypto.publicKey.toString()
                    privateKey = app.crypto.privateKey.toString()
                }
            ) {
                Text(stringResource(R.string.update))
            }
        }
    }
}