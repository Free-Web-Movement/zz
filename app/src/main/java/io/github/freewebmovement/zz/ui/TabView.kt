package io.github.freewebmovement.zz.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.freewebmovement.zz.ui.content.Tabs


@Composable
fun TabView() {
    var selectedTab by remember { mutableStateOf(TabType.Chats) }
    var stacked by remember { mutableStateOf(ContentType.NonStacked) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            selectedTab = selectedTab,
            stacked = stacked
        )
        Tabs(
            selectedTab = selectedTab,
            modifier = Modifier.weight(1f)
        )
        BottomBar(
            selectedTab = selectedTab,
            onClickTab = { selectedTab = it },
        )
    }
}

@Preview(locale = "en")
@Composable
private fun Preview() {
    TabView()
}

@Preview(locale = "zh")
@Composable
private fun PreviewZH() {
    TabView()
}