package io.github.freewebmovement.zz.ui.content.peer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.freewebmovement.zz.R
import io.github.freewebmovement.zz.ui.common.PageType


@Composable
fun AddPeer(updatePage: (value: PageType) -> Unit) {
    val scrollState = rememberScrollState()
    var ip by remember { mutableStateOf(TextFieldValue("")) }
    var port by remember { mutableStateOf(TextFieldValue("")) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
//            modifier = Modifier.fillMaxWidth(),
            value = ip,
            onValueChange = { newText ->
                ip = newText
            },
            label = { Text(text = stringResource(R.string.please_input_your_ip_here)) },
            placeholder = { Text(text = stringResource(R.string.please_input_your_ip_here)) },
        )
        OutlinedTextField(
//            modifier = Modifier.fillMaxWidth(),
            value = port,
            onValueChange = { newText ->
                port = newText
            },
            label = { Text(text = stringResource(R.string.please_input_your_port_here)) },
            placeholder = { Text(text = stringResource(R.string.please_input_your_port_here)) },
        )

        Button(
            modifier = Modifier.padding(top = 16.dp),
            onClick = {
            }
        ) {
            Text(
                stringResource(R.string.add)
            )
        }
    }
}


@Preview
@Composable
private fun Preview() {
    AddPeer() {
    }
}
