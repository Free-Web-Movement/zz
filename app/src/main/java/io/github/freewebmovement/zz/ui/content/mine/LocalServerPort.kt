package io.github.freewebmovement.zz.ui.content.mine

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
    var port by remember { mutableIntStateOf(app.settings.localServerPort) }
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
                            port = if(v.isNotEmpty()) {
                                v.toInt()
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

    }
}