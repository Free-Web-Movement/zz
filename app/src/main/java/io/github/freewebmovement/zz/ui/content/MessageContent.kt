package io.github.freewebmovement.zz.ui.content

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.ui.common.TabType
import rs.zz.coin.Address


@Composable
fun MessageContent(tabType: TabType, messages: ArrayList<String>) {
    Column {
        val app = MainApplication.getApp()
        if(messages.isEmpty()) {
//            Text(text = "No Message available")
            Text(text = app.address.prefix())
        } else {
            messages.forEach { it
                Text(text = it)
            }
        }
    }
}

@Preview(locale = "en")
@Composable
private fun Preview() {
    val messages = ArrayList<String>()
    MessageContent(tabType = TabType.Apps, messages)
}

@Preview(locale = "zh")
@Composable
private fun PreviewData() {
    val messages = ArrayList<String>()
    messages.add("Hello world! 1")
    messages.add("Hello world! 2")
    MessageContent(tabType = TabType.Apps, messages)
}
