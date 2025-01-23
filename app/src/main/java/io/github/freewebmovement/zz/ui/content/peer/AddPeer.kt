package io.github.freewebmovement.zz.ui.content.peer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.freewebmovement.peer.IPType
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.R
import io.github.freewebmovement.zz.system.IsValid
import io.github.freewebmovement.zz.ui.common.PageType


@Composable
fun AddPeer(updatePage: ((value: PageType) -> Unit)) {
    val scrollState = rememberScrollState()
    var ip by remember { mutableStateOf(TextFieldValue("")) }
    var port by remember { mutableStateOf(TextFieldValue("")) }
    var isIPV4 by remember { mutableStateOf(true) }
    val openIPDialog = remember { mutableStateOf(false) }
    val openPortDialog = remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = ip,
            onValueChange = { newText ->
                ip = newText
            },
            label = { Text(text = stringResource(R.string.please_input_your_ip_here)) },
            placeholder = { Text(text = stringResource(R.string.please_input_your_ip_here)) },
        )
        OutlinedTextField(
            value = port,
            onValueChange = { newText ->
                port = newText
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text(text = stringResource(R.string.please_input_your_port_here)) },
            placeholder = { Text(text = stringResource(R.string.please_input_your_port_here)) },
        )

        Row(Modifier.selectableGroup()) {
            RadioButton(
                selected = isIPV4,
                onClick = { isIPV4 = true },
                modifier = Modifier.semantics { contentDescription = "Localized Description" }
            )

            Text(
                text = "IPV4",
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            RadioButton(
                selected = !isIPV4,
                onClick = { isIPV4 = false },
                modifier = Modifier.semantics { contentDescription = "Localized Description" }
            )

            Text(
                text = "IPV6",
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }

        Button(
            modifier = Modifier.padding(top = 16.dp),
            onClick = {
                val portNumber: Int

                if (!IsValid.IP(ip.text)) {
                    openIPDialog.value = true
                    return@Button
                }
                try {
                    portNumber = port.text.toInt()
                    if (!IsValid.port(portNumber)) {
                        openPortDialog.value = true
                        return@Button
                    }
                } catch (e: Exception) {
                    openPortDialog.value = true
                    return@Button
                }

                val type = if(isIPV4) {
                    IPType.IPV4
                } else {
                    IPType.IPV6
                }

                val app = MainApplication.getApp()
                app.handler.initPeer(ip.text, portNumber, type)
                updatePage(PageType.PeerMain)
            }
        ) {
            Text(
                stringResource(R.string.add)
            )
        }
    }

    if (openIPDialog.value) {
        AlertDialog(
            onDismissRequest = {
                // Dismiss the dialog when the user clicks outside the dialog or on the back
                // button. If you want to disable that functionality, simply use an empty
                // onDismissRequest.
                openIPDialog.value = false
            },
            title = { Text(text = stringResource(R.string.invalid_ip)) },
            text = { Text(text = stringResource(R.string.please_input_valid_ip)) },
            confirmButton = {
                TextButton(onClick = {
                    openIPDialog.value = false
                }) { Text(stringResource(R.string.ok)) }
            }
        )
    }
    if (openPortDialog.value) {
        AlertDialog(
            onDismissRequest = {
                // Dismiss the dialog when the user clicks outside the dialog or on the back
                // button. If you want to disable that functionality, simply use an empty
                // onDismissRequest.
                openPortDialog.value = false
            },
            title = { Text(text = stringResource(R.string.invalid_port)) },
            text = { Text(text = stringResource(R.string.please_input_valid_port)) },
            confirmButton = {
                TextButton(onClick = {
                    openPortDialog.value = false
                }) { Text(stringResource(R.string.ok)) }
            }
        )
    }
}

@Preview
@Composable
private fun Preview() {
    AddPeer {
    }
}
