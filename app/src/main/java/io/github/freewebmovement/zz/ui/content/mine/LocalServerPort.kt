package io.github.freewebmovement.zz.ui.content.mine

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.R
import io.github.freewebmovement.zz.ui.common.rainbowColors

@Composable
fun LocalServerPort() {
    val brush = remember {
        Brush.linearGradient(
            colors = rainbowColors
        )
    }
    val app = MainApplication.getApp()
    val context = LocalContext.current
    var port by remember { mutableIntStateOf(app.settings.network.port) }
    Column {
        when (port) {
            0 -> {
                Text("随机")
            }
            else -> {
//                TextField(value = port.toString())
                Row {
                    TextField(
                        value = port.toString(),
                        onValueChange = { v ->
                            port = if (v.isNotEmpty()) {
                                v.toIntOrNull() ?: port
                            } else {
                                0
                            }
                        },
                        label = { Text(stringResource(R.string.tab_mine_port)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(2.dp),
                        textStyle = TextStyle(brush = brush)
                    )
                }
            }
        }

        Row {
            Button(
                onClick = {
                    val failed = runCatching {
                        app.settings.network.port = port
                    }.isFailure
                    if (failed) {
                        Toast.makeText(context, "无效端口", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    // restart the fwmc node with the new port
                    app.restartFwmcNode(port)
                    Toast.makeText(context, "端口已保存，节点重启中", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.padding(2.dp)
            ) {
                Text(text = stringResource(R.string.action_save))
            }
        }
    }
}
